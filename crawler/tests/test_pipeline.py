from types import SimpleNamespace

import psycopg
import pytest
from psycopg.types.json import Jsonb
from scrapy.utils.test import get_crawler

from kinhlup_crawler import crawl_stats
from kinhlup_crawler.items import DocumentItem
from kinhlup_crawler.pipelines import PostgresDocumentPipeline, run_status
from kinhlup_crawler.repository import FINISH_RUN_SQL, INSERT_DOCUMENT_SQL, START_RUN_SQL

RUN_ID = "11111111-1111-1111-1111-111111111111"
DATABASE_URL = "postgresql://user:secret@db.example/postgres"


class FakeCursor:
    def __init__(self, connection):
        self._connection = connection
        self._row = None

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        return False

    def execute(self, sql, params):
        self._connection.executed.append((sql, params))
        if sql is INSERT_DOCUMENT_SQL:
            if self._connection.fail_inserts:
                raise psycopg.OperationalError("server closed the connection")
            key = (params["url"], params["canonical_url"])
            known = any(value in self._connection.stored for value in key)
            self._connection.stored.update(key)
            self._row = None if known else ("doc-id",)
        elif sql is START_RUN_SQL:
            self._row = (RUN_ID,)

    def fetchone(self):
        return self._row


class FakeConnection:
    def __init__(self):
        self.executed = []
        self.stored = set()
        self.fail_inserts = False
        self.closed = False
        self.connect_kwargs = None

    def cursor(self):
        return FakeCursor(self)

    def close(self):
        self.closed = True

    def statements(self, sql):
        return [params for executed_sql, params in self.executed if executed_sql is sql]


def make_item(url="https://hust.edu.vn/a.html", canonical_url=None):
    return DocumentItem(
        url=url,
        canonical_url=canonical_url or url,
        domain="hust.edu.vn",
        title="Tiêu đề",
        content_type="text/html",
        raw_text="Nội dung",
        content_hash="abc",
        metadata={"http_status": 200},
    )


@pytest.fixture
def connection():
    return FakeConnection()


@pytest.fixture
def pipeline(connection):
    crawler = get_crawler()
    crawler.spider = SimpleNamespace(start_url="https://hust.edu.vn/")

    def connect(url, **kwargs):
        connection.connect_kwargs = {"url": url, **kwargs}
        return connection

    pipeline = PostgresDocumentPipeline(crawler, DATABASE_URL, connect=connect)
    pipeline.open_spider()
    return pipeline


def test_open_spider_starts_a_local_running_run(pipeline, connection):
    assert connection.connect_kwargs["url"] == DATABASE_URL
    assert connection.connect_kwargs["autocommit"] is True
    assert connection.statements(START_RUN_SQL) == [
        {"trigger_type": "LOCAL", "start_url": "https://hust.edu.vn/"}
    ]
    assert "'RUNNING'" in START_RUN_SQL


@pytest.mark.parametrize("value", [None, ""])
def test_missing_database_url_fails_before_crawling(monkeypatch, value):
    if value is None:
        monkeypatch.delenv("DATABASE_URL", raising=False)
    else:
        monkeypatch.setenv("DATABASE_URL", value)
    with pytest.raises(RuntimeError, match="DATABASE_URL"):
        PostgresDocumentPipeline.from_crawler(get_crawler())


def test_start_run_failure_closes_the_connection(connection):
    crawler = get_crawler()
    crawler.spider = SimpleNamespace(start_url="https://hust.edu.vn/")
    pipeline = PostgresDocumentPipeline(crawler, DATABASE_URL, connect=lambda url, **kw: connection)

    def failing_cursor():
        raise psycopg.errors.InsufficientPrivilege("permission denied for table crawl_runs")

    connection.cursor = failing_cursor
    with pytest.raises(psycopg.errors.InsufficientPrivilege):
        pipeline.open_spider()
    assert connection.closed


def test_new_document_is_inserted_as_active_pending_with_run_id(pipeline, connection):
    item = make_item()
    assert pipeline.process_item(item) is item

    [params] = connection.statements(INSERT_DOCUMENT_SQL)
    assert params["url"] == item.url
    assert params["canonical_url"] == item.canonical_url
    assert params["raw_text"] == item.raw_text
    assert params["content_hash"] == item.content_hash
    assert params["crawl_run_id"] == RUN_ID
    assert isinstance(params["metadata"], Jsonb)
    assert "'ACTIVE', 'PENDING'" in INSERT_DOCUMENT_SQL
    assert pipeline._stats.get_value(crawl_stats.DOCUMENTS_INSERTED) == 1


def test_insert_sql_is_parameterized_and_skips_conflicts():
    assert "ON CONFLICT DO NOTHING" in INSERT_DOCUMENT_SQL
    assert "%(url)s" in INSERT_DOCUMENT_SQL and "{" not in INSERT_DOCUMENT_SQL
    assert "updated_at" not in INSERT_DOCUMENT_SQL


def test_duplicate_url_or_canonical_is_counted_not_raised(pipeline):
    pipeline.process_item(make_item("https://hust.edu.vn/a.html"))
    pipeline.process_item(make_item("https://hust.edu.vn/a.html"))
    pipeline.process_item(
        make_item("https://hust.edu.vn/a.html?print=1", canonical_url="https://hust.edu.vn/a.html")
    )

    stats = pipeline._stats
    assert stats.get_value(crawl_stats.DOCUMENTS_DISCOVERED) == 3
    assert stats.get_value(crawl_stats.DOCUMENTS_INSERTED) == 1
    assert stats.get_value(crawl_stats.DOCUMENTS_ALREADY_STORED) == 2


def test_database_error_on_one_item_does_not_stop_the_crawl(pipeline, connection):
    connection.fail_inserts = True
    item = make_item()
    assert pipeline.process_item(item) is item
    assert pipeline._stats.get_value(crawl_stats.DOCUMENTS_STORE_FAILED) == 1


def test_spider_closed_finishes_run_with_counters_and_closes_connection(pipeline, connection):
    pipeline.process_item(make_item("https://hust.edu.vn/a.html"))
    pipeline.process_item(make_item("https://hust.edu.vn/a.html"))
    pipeline._stats.inc_value(crawl_stats.FETCH_FAILED)

    pipeline.spider_closed(spider=None, reason="closespider_pagecount")

    [params] = connection.statements(FINISH_RUN_SQL)
    assert params["crawl_run_id"] == RUN_ID
    assert params["status"] == "PARTIAL"
    assert params["discovered_count"] == 2
    assert params["new_count"] == 1
    assert params["failed_count"] == 1
    assert "already_stored=1" in params["notes"]
    assert connection.closed


def test_connection_is_closed_even_if_finishing_the_run_fails(pipeline, connection):
    def broken_finish(*args, **kwargs):
        raise psycopg.OperationalError("connection lost")

    pipeline._repository.finish_run = broken_finish
    with pytest.raises(psycopg.OperationalError):
        pipeline.spider_closed(spider=None, reason="finished")
    assert connection.closed


@pytest.mark.parametrize(
    ("reason", "discovered", "failed", "expected"),
    [
        ("finished", 10, 0, "SUCCEEDED"),
        ("closespider_pagecount", 10, 0, "SUCCEEDED"),
        ("finished", 0, 0, "SUCCEEDED"),
        ("finished", 10, 2, "PARTIAL"),
        ("shutdown", 10, 0, "PARTIAL"),
        ("finished", 0, 3, "FAILED"),
        ("shutdown", 0, 0, "FAILED"),
    ],
)
def test_run_status(reason, discovered, failed, expected):
    assert run_status(close_reason=reason, discovered=discovered, failed=failed) == expected
