"""SQL access to the shared ``crawl_runs`` and ``documents`` tables."""

from __future__ import annotations

from psycopg.types.json import Jsonb

from kinhlup_crawler.items import DocumentItem

# ON CONFLICT without a target covers both unique keys (url, canonical_url):
# the first row stored for a URL or canonical URL wins and later duplicates
# are skipped instead of failing the crawl. Rows that already exist are left
# untouched; refreshing them is the job of change detection.
INSERT_DOCUMENT_SQL = """
    INSERT INTO public.documents (
        url, canonical_url, domain, title, content_type, raw_text, metadata,
        published_at, content_hash, status, index_status, last_crawl_run_id
    )
    VALUES (
        %(url)s, %(canonical_url)s, %(domain)s, %(title)s, %(content_type)s,
        %(raw_text)s, %(metadata)s, %(published_at)s, %(content_hash)s,
        'ACTIVE', 'PENDING', %(crawl_run_id)s
    )
    ON CONFLICT DO NOTHING
    RETURNING id
"""

START_RUN_SQL = """
    INSERT INTO public.crawl_runs (trigger_type, status, start_url)
    VALUES (%(trigger_type)s, 'RUNNING', %(start_url)s)
    RETURNING id
"""

FINISH_RUN_SQL = """
    UPDATE public.crawl_runs
    SET status = %(status)s,
        finished_at = now(),
        discovered_count = %(discovered_count)s,
        new_count = %(new_count)s,
        failed_count = %(failed_count)s,
        notes = %(notes)s
    WHERE id = %(crawl_run_id)s
"""


class CrawlRepository:
    """Writes crawl runs and documents through one psycopg connection.

    The connection is expected to be in autocommit mode: every method issues a
    single statement, so each call is its own transaction and a failed insert
    never blocks the ones after it.
    """

    def __init__(self, connection):
        self._connection = connection

    def start_run(self, *, trigger_type: str, start_url: str):
        with self._connection.cursor() as cursor:
            cursor.execute(START_RUN_SQL, {"trigger_type": trigger_type, "start_url": start_url})
            return cursor.fetchone()[0]

    def insert_document(self, item: DocumentItem, *, crawl_run_id) -> bool:
        """Insert a new document; return False when its URL is already stored."""
        params = {
            "url": item.url,
            "canonical_url": item.canonical_url,
            "domain": item.domain,
            "title": item.title,
            "content_type": item.content_type,
            "raw_text": item.raw_text,
            "metadata": Jsonb(item.metadata),
            "published_at": item.published_at,
            "content_hash": item.content_hash,
            "crawl_run_id": crawl_run_id,
        }
        with self._connection.cursor() as cursor:
            cursor.execute(INSERT_DOCUMENT_SQL, params)
            return cursor.fetchone() is not None

    def finish_run(
        self,
        crawl_run_id,
        *,
        status: str,
        discovered_count: int,
        new_count: int,
        failed_count: int,
        notes: str,
    ) -> None:
        with self._connection.cursor() as cursor:
            cursor.execute(
                FINISH_RUN_SQL,
                {
                    "crawl_run_id": crawl_run_id,
                    "status": status,
                    "discovered_count": discovered_count,
                    "new_count": new_count,
                    "failed_count": failed_count,
                    "notes": notes,
                },
            )
