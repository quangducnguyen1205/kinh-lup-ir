"""Item pipeline that stores crawled documents in the shared PostgreSQL database."""

from __future__ import annotations

import logging
import os

import psycopg
from scrapy import signals

from kinhlup_crawler import crawl_stats
from kinhlup_crawler.repository import CrawlRepository

logger = logging.getLogger(__name__)

TRIGGER_TYPE = "LOCAL"
CONNECT_TIMEOUT_SECONDS = 15

# Close reasons that mean the crawl ran to its planned end, including the
# page limit used for local runs.
COMPLETED_CLOSE_REASONS = frozenset(
    {"finished", "closespider_pagecount", "closespider_itemcount", "closespider_timeout"}
)


def run_status(*, close_reason: str, discovered: int, failed: int) -> str:
    """Map the outcome of a crawl to a ``crawl_runs.status`` value."""
    if close_reason in COMPLETED_CLOSE_REASONS and failed == 0:
        return "SUCCEEDED"
    if discovered > 0:
        return "PARTIAL"
    return "FAILED"


class PostgresDocumentPipeline:
    """Records one LOCAL crawl run and inserts every new document into it.

    Only new URLs are inserted. A document whose URL or canonical URL is
    already stored is counted and left unchanged.
    """

    def __init__(self, crawler, database_url: str, connect=psycopg.connect):
        self._crawler = crawler
        self._stats = crawler.stats
        self._database_url = database_url
        self._connect = connect
        self._connection = None
        self._repository: CrawlRepository | None = None
        self._crawl_run_id = None

    @classmethod
    def from_crawler(cls, crawler):
        # Checked here so a missing configuration stops the crawl before any request.
        database_url = os.environ.get("DATABASE_URL")
        if not database_url:
            raise RuntimeError(
                "DATABASE_URL is not set. Copy .env.example to .env at the repository root "
                "or export DATABASE_URL before running the HUST spider."
            )
        pipeline = cls(crawler, database_url)
        # The close reason is only available through this signal, not close_spider().
        crawler.signals.connect(pipeline.spider_closed, signal=signals.spider_closed)
        return pipeline

    def open_spider(self) -> None:
        # Never log the database URL: it carries the database password.
        self._connection = self._connect(
            self._database_url, autocommit=True, connect_timeout=CONNECT_TIMEOUT_SECONDS
        )
        self._repository = CrawlRepository(self._connection)
        try:
            self._crawl_run_id = self._repository.start_run(
                trigger_type=TRIGGER_TYPE, start_url=self._crawler.spider.start_url
            )
        except Exception:
            self._connection.close()
            self._connection = None
            raise
        logger.info("Started crawl run %s", self._crawl_run_id)

    def process_item(self, item):
        self._stats.inc_value(crawl_stats.DOCUMENTS_DISCOVERED)
        try:
            inserted = self._repository.insert_document(item, crawl_run_id=self._crawl_run_id)
        except psycopg.Error as error:
            self._stats.inc_value(crawl_stats.DOCUMENTS_STORE_FAILED)
            logger.error("Could not store %s: %s", item.url, error)
            return item

        if inserted:
            self._stats.inc_value(crawl_stats.DOCUMENTS_INSERTED)
            logger.debug("Stored %s", item.url)
        else:
            self._stats.inc_value(crawl_stats.DOCUMENTS_ALREADY_STORED)
            logger.debug("Already stored, left unchanged: %s", item.url)
        return item

    def spider_closed(self, spider, reason: str) -> None:
        if self._connection is None:
            return
        try:
            if self._crawl_run_id is not None:
                self._finish_run(reason)
        finally:
            self._connection.close()
            self._connection = None

    def _finish_run(self, reason: str) -> None:
        def count(key: str) -> int:
            return self._stats.get_value(key, 0)

        discovered = count(crawl_stats.DOCUMENTS_DISCOVERED)
        inserted = count(crawl_stats.DOCUMENTS_INSERTED)
        fetch_failed = count(crawl_stats.FETCH_FAILED)
        store_failed = count(crawl_stats.DOCUMENTS_STORE_FAILED)
        failed = fetch_failed + store_failed
        status = run_status(close_reason=reason, discovered=discovered, failed=failed)
        notes = (
            f"close_reason={reason}; insert-only run; "
            f"already_stored={count(crawl_stats.DOCUMENTS_ALREADY_STORED)}; "
            f"pages_skipped={count(crawl_stats.PAGES_SKIPPED)}; "
            f"fetch_failed={fetch_failed}; store_failed={store_failed}"
        )
        self._repository.finish_run(
            self._crawl_run_id,
            status=status,
            discovered_count=discovered,
            new_count=inserted,
            failed_count=failed,
            notes=notes,
        )
        logger.info(
            "Finished crawl run %s: %s, %d discovered, %d new, %d failed",
            self._crawl_run_id, status, discovered, inserted, failed,
        )
