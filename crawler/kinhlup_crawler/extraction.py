"""Title, visible text and metadata extraction for HTML pages.

The output is the ``raw_text`` of the data contract: visible text with
whitespace normalized and Unicode kept as-is. Vietnamese normalization and
tokenization happen later in the pipeline, not here.
"""

from __future__ import annotations

import hashlib
import re
from dataclasses import dataclass
from datetime import datetime

from lxml import html as lxml_html
from lxml.etree import ParserError

# Pages shorter than this are menus, redirects or placeholders rather than content.
MIN_CONTENT_CHARS = 200

CONTENT_CONTAINERS = ("article", "main")
NON_VISIBLE_TAGS = ("head", "script", "style", "noscript", "svg", "template", "iframe", "canvas")
BOILERPLATE_TAGS = ("nav", "footer", "aside", "form")

BLOCK_TAGS = frozenset(
    {
        "address", "article", "aside", "blockquote", "br", "dd", "div", "dl", "dt",
        "figcaption", "figure", "footer", "h1", "h2", "h3", "h4", "h5", "h6", "header",
        "hr", "li", "main", "nav", "ol", "p", "pre", "section", "table", "tbody",
        "td", "th", "thead", "tfoot", "tr", "ul",
    }
)

# A status code must stand alone ("404", "404 - ...") so that titles such as
# "500 sinh viên nhận học bổng" are not mistaken for error pages.
ERROR_TITLE_PATTERN = re.compile(
    r"^\s*(?:40\d|50\d)\s*(?:$|[-–—|:])"
    r"|\b(?:error|lỗi)\s*(?:40\d|50\d)\b"
    r"|not found|access denied|forbidden"
    r"|không tìm thấy trang|trang không tồn tại",
    re.IGNORECASE,
)

_HORIZONTAL_SPACE = re.compile(r"[^\S\n]+")


@dataclass(frozen=True)
class ExtractedPage:
    """What the crawler keeps from one HTML page."""

    title: str | None
    raw_text: str
    canonical_href: str | None = None
    description: str | None = None
    language: str | None = None
    published_at: datetime | None = None
    has_password_field: bool = False
    robots_noindex: bool = False


def extract_page(html: str) -> ExtractedPage:
    """Parse an HTML document and extract its title, visible text and metadata."""
    try:
        document = lxml_html.document_fromstring(
            html.encode("utf-8"),
            parser=lxml_html.HTMLParser(encoding="utf-8", remove_comments=True),
        )
    except ParserError:
        return ExtractedPage(title=None, raw_text="")

    # Everything that reads <head> must run before the visible-text pass removes it.
    title = _extract_title(document)
    canonical_href = _clean(_first(document.xpath('//link[@rel="canonical"]/@href')))
    description = _meta(document, name="description") or _meta(document, prop="og:description")
    language = _clean(_first(document.xpath("/html/@lang")))
    published_at = _parse_datetime(_meta(document, prop="article:published_time"))
    robots_noindex = "noindex" in (_meta(document, name="robots") or "").lower()
    has_password_field = bool(
        document.xpath('//input[translate(@type, "PASSWORD", "password")="password"]')
    )

    return ExtractedPage(
        title=title,
        raw_text=_extract_visible_text(document),
        canonical_href=canonical_href,
        description=description,
        language=language,
        published_at=published_at,
        has_password_field=has_password_field,
        robots_noindex=robots_noindex,
    )


def skip_reason(page: ExtractedPage) -> str | None:
    """Return why a page is not a content page, or None when it should be kept.

    The rules are deliberately simple and deterministic so that every skip can
    be explained from the page itself.
    """
    if page.robots_noindex:
        return "noindex"
    if page.has_password_field:
        return "login_form"
    if page.title and ERROR_TITLE_PATTERN.search(page.title):
        return "error_page"
    if len(page.raw_text) < MIN_CONTENT_CHARS:
        return "too_short"
    return None


def content_hash(raw_text: str) -> str:
    """SHA-256 hex digest of the UTF-8 encoded ``raw_text``.

    This is the ``documents.content_hash`` contract: recomputing it from the
    stored ``raw_text`` gives the same value, so change detection can compare
    hashes without refetching pages.
    """
    return hashlib.sha256(raw_text.encode("utf-8")).hexdigest()


def normalize_whitespace(text: str) -> str:
    """Collapse runs of spaces inside each line and drop blank lines."""
    lines = (_HORIZONTAL_SPACE.sub(" ", line).strip() for line in text.split("\n"))
    return "\n".join(line for line in lines if line)


def _extract_title(document) -> str | None:
    headings = document.xpath("//h1")
    candidates = (
        " ".join(document.xpath("//head/title//text()")),
        _meta(document, prop="og:title"),
        " ".join(headings[0].itertext()) if headings else None,
    )
    return next((title for title in map(_clean, candidates) if title), None)


def _extract_visible_text(document) -> str:
    for element in document.xpath(" | ".join(f"//{tag}" for tag in NON_VISIBLE_TAGS)):
        _drop(element)

    root = _content_root(document)
    # Inside <article>/<main> a <header> usually holds the headline; around
    # <body> it is the site banner.
    boilerplate = BOILERPLATE_TAGS + (("header",) if root.tag == "body" else ())
    for element in root.xpath(" | ".join(f".//{tag}" for tag in boilerplate)):
        _drop(element)

    chunks: list[str] = []
    _collect_text(root, chunks)
    return normalize_whitespace("".join(chunks))


def _content_root(document):
    """Prefer a single <article> or <main> holding real content, else <body>."""
    for tag in CONTENT_CONTAINERS:
        matches = document.xpath(f"//{tag}")
        if len(matches) == 1:
            text = normalize_whitespace(" ".join(matches[0].itertext()))
            if len(text) >= MIN_CONTENT_CHARS:
                return matches[0]
    body = document.find("body")
    return body if body is not None else document


def _collect_text(element, chunks: list[str]) -> None:
    # Block elements become line breaks and inline elements a space, so text
    # from adjacent cells or list items never merges into one word.
    separator = "\n" if element.tag in BLOCK_TAGS else " "
    chunks.append(separator)
    if element.text:
        chunks.append(element.text)
    for child in element:
        _collect_text(child, chunks)
        if child.tail:
            chunks.append(child.tail)
    chunks.append(separator)


def _drop(element) -> None:
    """Remove an element but keep its tail text, which belongs to the parent."""
    parent = element.getparent()
    if parent is None:
        return
    if element.tail:
        previous = element.getprevious()
        if previous is not None:
            previous.tail = (previous.tail or "") + " " + element.tail
        else:
            parent.text = (parent.text or "") + " " + element.tail
    parent.remove(element)


def _meta(document, *, name: str | None = None, prop: str | None = None) -> str | None:
    attribute, value = ("name", name) if name else ("property", prop)
    return _clean(_first(document.xpath(f'//meta[@{attribute}="{value}"]/@content')))


def _parse_datetime(value: str | None) -> datetime | None:
    if not value:
        return None
    try:
        return datetime.fromisoformat(value)
    except ValueError:
        return None


def _first(values):
    return values[0] if values else None


def _clean(value) -> str | None:
    if value is None:
        return None
    cleaned = " ".join(str(value).split())
    return cleaned or None
