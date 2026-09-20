# Database schema v0

The shared PostgreSQL database intentionally starts with only two core tables.

## `crawl_runs`

One row = one logical crawl/update execution.

Important fields:

- `id`: UUID of the run.
- `trigger_type`: `MANUAL`, `SCHEDULED`, or `LOCAL`.
- `status`: `QUEUED`, `RUNNING`, `SUCCEEDED`, `PARTIAL`, or `FAILED`.
- `started_at`, `finished_at`: run timing.
- `discovered_count`, `new_count`, `updated_count`, `unchanged_count`, `failed_count`: summary counters.

The periodic scheduler and the optional manual-refresh API should create the same kind of `crawl_runs` row and invoke the same crawl/update pipeline.

## `documents`

One row = one crawled/searchable document.

Important fields:

- `url`: concrete crawled URL. Unique in v0.
- `canonical_url`: normalized/canonical URL when available.
- `domain`: HUST root/subdomain.
- `title`: extracted title.
- `content_type`: HTML/PDF/etc.
- `raw_text`: extracted text before Vietnamese analysis.
- `normalized_text`: optional persisted result of Vietnamese normalization.
- `metadata`: JSONB for flexible metadata from HTML/Tika.
- `content_hash`: used to detect changed content.
- `status`: `ACTIVE`, `FAILED`, or `DELETED`.
- `index_status`: `PENDING`, `INDEXED`, or `FAILED`.
- `first_seen_at`: first discovery.
- `last_crawled_at`: last time crawler checked the URL.
- `updated_at`: last time the **source content changed**.
- `last_indexed_at`: last successful Lucene sync.
- `last_crawl_run_id`: latest crawl run that touched the document.

## Critical update rule

Do **not** update `documents.updated_at` when an unchanged document is recrawled.

Use this behavior:

```text
new URL
→ INSERT document
→ index_status = PENDING

known URL + same content_hash
→ update last_crawled_at + last_crawl_run_id only
→ keep updated_at unchanged

known URL + different content_hash
→ update content/title/hash/metadata
→ updated_at = now()
→ index_status = PENDING
```

This rule lets the backend perform incremental Lucene indexing without reindexing every document after every crawl.

## RLS

RLS is enabled on both public tables and there are currently **no Data API policies**.

That is intentional for v0:

- React frontend does not access Supabase/Postgres directly.
- Crawler and Spring backend connect to PostgreSQL with team database credentials.
- If direct Supabase Data API access is introduced later, add explicit policies in a migration first.

## Migration files

- `supabase/migrations/2026092001_initial_search_schema.sql`
- `supabase/migrations/2026092002_index_documents_last_crawl_run.sql`

The production Supabase project already has these migrations applied.
