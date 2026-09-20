create table public.crawl_runs (
  id uuid primary key default gen_random_uuid(),
  trigger_type text not null default 'MANUAL'
    check (trigger_type in ('MANUAL', 'SCHEDULED', 'LOCAL')),
  status text not null default 'QUEUED'
    check (status in ('QUEUED', 'RUNNING', 'SUCCEEDED', 'PARTIAL', 'FAILED')),
  start_url text,
  started_at timestamptz not null default now(),
  finished_at timestamptz,
  discovered_count integer not null default 0 check (discovered_count >= 0),
  new_count integer not null default 0 check (new_count >= 0),
  updated_count integer not null default 0 check (updated_count >= 0),
  unchanged_count integer not null default 0 check (unchanged_count >= 0),
  failed_count integer not null default 0 check (failed_count >= 0),
  notes text,
  created_at timestamptz not null default now(),
  constraint crawl_runs_finished_after_started
    check (finished_at is null or finished_at >= started_at)
);

create table public.documents (
  id uuid primary key default gen_random_uuid(),
  url text not null unique,
  canonical_url text unique,
  domain text not null,
  title text,
  content_type text not null default 'text/html',
  raw_text text not null default '',
  normalized_text text,
  metadata jsonb not null default '{}'::jsonb,
  published_at timestamptz,
  content_hash text not null,
  status text not null default 'ACTIVE'
    check (status in ('ACTIVE', 'FAILED', 'DELETED')),
  index_status text not null default 'PENDING'
    check (index_status in ('PENDING', 'INDEXED', 'FAILED')),
  first_seen_at timestamptz not null default now(),
  last_crawled_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  last_indexed_at timestamptz,
  last_crawl_run_id uuid references public.crawl_runs(id) on delete set null
);

create index crawl_runs_started_at_idx
  on public.crawl_runs (started_at desc);

create index crawl_runs_status_idx
  on public.crawl_runs (status);

create index documents_domain_idx
  on public.documents (domain);

create index documents_last_crawled_at_idx
  on public.documents (last_crawled_at desc);

create index documents_updated_at_idx
  on public.documents (updated_at desc);

create index documents_index_status_idx
  on public.documents (index_status);

create index documents_content_hash_idx
  on public.documents (content_hash);

alter table public.crawl_runs enable row level security;
alter table public.documents enable row level security;

comment on table public.documents is
  'Shared source-of-truth documents crawled from hust.edu.vn and subdomains.';

comment on column public.documents.updated_at is
  'Time source content last changed. Do not update this on an unchanged recrawl.';

comment on column public.documents.last_crawled_at is
  'Time this URL was most recently checked by the crawler.';

comment on column public.documents.index_status is
  'Lucene synchronization state. Changed/new content should be marked PENDING.';

comment on table public.crawl_runs is
  'One logical crawl/update execution, scheduled or manual.';
