from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime


@dataclass
class DocumentItem:
    """One crawled page, shaped like a row of ``public.documents``.

    Lifecycle columns (``status``, ``index_status``, timestamps, run id) are
    owned by the persistence layer, not by the spider.
    """

    url: str
    canonical_url: str
    domain: str
    title: str | None
    content_type: str
    raw_text: str
    content_hash: str
    published_at: datetime | None = None
    metadata: dict = field(default_factory=dict)
