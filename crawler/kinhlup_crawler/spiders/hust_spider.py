from __future__ import annotations

from urllib.parse import urlsplit

import scrapy
from scrapy.exceptions import IgnoreRequest
from scrapy.http import HtmlResponse
from scrapy.spidermiddlewares.httperror import HttpError

from kinhlup_crawler import crawl_stats
from kinhlup_crawler.extraction import content_hash, extract_page, skip_reason
from kinhlup_crawler.items import DocumentItem
from kinhlup_crawler.urls import is_auth_url, is_in_scope, normalize_url, should_follow

HTML_CONTENT_TYPES = frozenset({"text/html", "application/xhtml+xml"})
STORED_CONTENT_TYPE = "text/html"


class HustSpider(scrapy.Spider):
    """Crawls public HTML pages of hust.edu.vn and its subdomains.

    Discovery follows links that stay inside ``CRAWLER_ALLOWED_DOMAIN``; every
    HTML page that passes the content heuristic becomes a ``DocumentItem``.
    Non-HTML documents (PDF, DOCX, ...) are not requested.
    """

    name = "hust"
    custom_settings = {
        "ITEM_PIPELINES": {"kinhlup_crawler.pipelines.PostgresDocumentPipeline": 300},
    }

    def __init__(self, start_url: str, allowed_domain: str, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.allowed_domain = allowed_domain.strip().lower()
        self.allowed_domains = [self.allowed_domain]
        normalized_start = normalize_url(start_url)
        if normalized_start is None or not is_in_scope(normalized_start, self.allowed_domain):
            raise ValueError(
                f"Start URL {start_url!r} is not an http(s) URL under {self.allowed_domain}"
            )
        self.start_url = normalized_start

    @classmethod
    def from_crawler(cls, crawler, *args, **kwargs):
        # `scrapy crawl hust -a start_url=...` overrides the configured start URL.
        kwargs.setdefault("start_url", crawler.settings.get("CRAWLER_START_URL"))
        kwargs.setdefault("allowed_domain", crawler.settings.get("CRAWLER_ALLOWED_DOMAIN"))
        return super().from_crawler(crawler, *args, **kwargs)

    async def start(self):
        yield scrapy.Request(self.start_url, callback=self.parse, errback=self.on_request_error)

    def parse(self, response):
        if not _is_html(response):
            self._count_skip("non_html")
            return

        # Redirects can leave the allowed domain or land on a login page.
        page_url = normalize_url(response.url)
        if page_url is None or not is_in_scope(page_url, self.allowed_domain):
            self._count_skip("out_of_scope")
            return
        if is_auth_url(page_url):
            self._count_skip("auth_url")
            return

        page = extract_page(response.text)
        reason = skip_reason(page)
        if reason is None:
            yield self._build_item(response, page_url, page)
        else:
            self._count_skip(reason)
            self.logger.debug("Skipped %s: %s", page_url, reason)

        if reason != "login_form":
            yield from self._follow_links(response)

    def on_request_error(self, failure):
        if failure.check(HttpError):
            self.crawler.stats.inc_value(crawl_stats.FETCH_FAILED)
            response = failure.value.response
            self.logger.info("HTTP %s for %s", response.status, response.url)
        elif failure.check(IgnoreRequest):
            # Disallowed by robots.txt or filtered as offsite: a decision, not a failure.
            return
        else:
            self.crawler.stats.inc_value(crawl_stats.FETCH_FAILED)
            self.logger.warning("Could not fetch %s: %r", failure.request.url, failure.value)

    def _build_item(self, response, page_url: str, page) -> DocumentItem:
        declared_canonical = (
            normalize_url(response.urljoin(page.canonical_href)) if page.canonical_href else None
        )
        trusted = declared_canonical and _is_trusted_canonical(
            declared_canonical, page_url, self.allowed_domain
        )
        canonical_url = declared_canonical if trusted else page_url

        metadata = {
            "source": f"kinhlup_crawler/{self.name}",
            "http_status": response.status,
            "charset": response.encoding,
            "declared_canonical": page.canonical_href,
            "language": page.language,
            "description": page.description,
        }
        return DocumentItem(
            url=page_url,
            canonical_url=canonical_url,
            domain=urlsplit(page_url).hostname,
            title=page.title,
            content_type=STORED_CONTENT_TYPE,
            raw_text=page.raw_text,
            content_hash=content_hash(page.raw_text),
            published_at=page.published_at,
            metadata={key: value for key, value in metadata.items() if value is not None},
        )

    def _follow_links(self, response):
        seen: set[str] = set()
        for href in response.css("a::attr(href), area::attr(href)").getall():
            url = normalize_url(response.urljoin(href))
            if url is None or url in seen or not should_follow(url, self.allowed_domain):
                continue
            seen.add(url)
            yield scrapy.Request(url, callback=self.parse, errback=self.on_request_error)

    def _count_skip(self, reason: str) -> None:
        self.crawler.stats.inc_value(crawl_stats.PAGES_SKIPPED)
        self.crawler.stats.inc_value(f"{crawl_stats.PAGES_SKIPPED}/{reason}")


def _is_trusted_canonical(canonical_url: str, page_url: str, allowed_domain: str) -> bool:
    """Accept a declared canonical unless it would merge unrelated pages.

    canonical_url is unique in the database, so a wrong canonical silently
    drops every later page that declares it. Two cases are rejected: an
    external canonical, which must not become the identity of a HUST document,
    and a canonical pointing at a site's home page from a deeper page, a
    common template misconfiguration.
    """
    if not is_in_scope(canonical_url, allowed_domain):
        return False
    canonical = urlsplit(canonical_url)
    points_to_home = canonical.path == "/" and not canonical.query
    return not points_to_home or urlsplit(page_url).path == "/"


def _is_html(response) -> bool:
    content_type = response.headers.get(b"Content-Type", b"").decode("latin-1")
    mime_type = content_type.split(";", 1)[0].strip().lower()
    if mime_type:
        return mime_type in HTML_CONTENT_TYPES
    return isinstance(response, HtmlResponse)
