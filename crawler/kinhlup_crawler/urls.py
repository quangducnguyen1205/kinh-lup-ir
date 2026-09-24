"""URL normalization and crawl-scope rules for the HUST crawler."""

from __future__ import annotations

from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit

from w3lib.url import canonicalize_url

ALLOWED_SCHEMES = frozenset({"http", "https"})
DEFAULT_PORTS = {"http": 80, "https": 443}

# Query keys that only carry campaign tracking; dropping them merges URLs that
# point at the same page.
TRACKING_QUERY_KEYS = frozenset({"fbclid", "gclid", "zarsrc"})
TRACKING_QUERY_PREFIXES = ("utm_",)

# Documents handled later by Tika, plus assets that never hold searchable text.
NON_HTML_EXTENSIONS = frozenset(
    {
        "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "odt", "ods", "odp", "rtf",
        "txt", "csv", "zip", "rar", "7z", "gz", "tar",
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tif", "tiff",
        "mp3", "wav", "ogg", "mp4", "avi", "mov", "wmv", "flv", "webm", "mkv",
        "css", "js", "json", "xml", "rss", "exe", "msi", "apk", "iso",
    }
)

# Path segments (compared without file extension) of pages that sit behind a
# login or only exist to authenticate, e.g. /Account/Login.aspx or /adfs/ls/.
AUTH_PATH_SEGMENTS = frozenset(
    {
        "login", "logout", "signin", "sign-in", "signup", "sign-up", "register",
        "dang-nhap", "dang-xuat", "dang-ky", "wp-login", "wp-admin",
        "admin", "administrator", "auth", "oauth", "sso", "cas", "adfs", "saml",
        "account", "my-account",
    }
)


def normalize_url(url: str) -> str | None:
    """Return a deterministic form of an absolute http(s) URL, or None if unusable.

    The fragment and tracking parameters are removed, scheme and host are
    lower-cased, the default port is dropped, query parameters are sorted and
    the path is percent-encoded. URLs that embed credentials are rejected so
    the crawler never sends them.
    """
    try:
        parts = urlsplit(url.strip())
        port = parts.port
    except ValueError:
        return None

    scheme = parts.scheme.lower()
    host = (parts.hostname or "").rstrip(".")
    if scheme not in ALLOWED_SCHEMES or not host or parts.username or parts.password:
        return None

    netloc = host if port in (None, DEFAULT_PORTS[scheme]) else f"{host}:{port}"
    query = urlencode(
        [(key, value) for key, value in parse_qsl(parts.query, keep_blank_values=True)
         if not _is_tracking_key(key)]
    )
    return canonicalize_url(urlunsplit((scheme, netloc, parts.path or "/", query, "")))


def is_in_scope(url: str, allowed_domain: str) -> bool:
    """True when the URL host is the allowed domain or one of its subdomains."""
    host = (urlsplit(url).hostname or "").rstrip(".")
    domain = allowed_domain.lower()
    return host == domain or host.endswith("." + domain)


def is_auth_url(url: str) -> bool:
    """True when the URL path looks like a login, logout or admin page."""
    segments = urlsplit(url).path.lower().split("/")
    return any(segment.split(".", 1)[0] in AUTH_PATH_SEGMENTS for segment in segments)


def has_non_html_extension(url: str) -> bool:
    last_segment = urlsplit(url).path.rsplit("/", 1)[-1]
    _, dot, extension = last_segment.rpartition(".")
    return bool(dot) and extension.lower() in NON_HTML_EXTENSIONS


def is_feed_url(url: str) -> bool:
    return urlsplit(url).path.rstrip("/").endswith("/feed")


def should_follow(url: str, allowed_domain: str) -> bool:
    """Decide whether a normalized URL is worth requesting as an HTML page."""
    return (
        is_in_scope(url, allowed_domain)
        and not is_auth_url(url)
        and not has_non_html_extension(url)
        and not is_feed_url(url)
    )


def _is_tracking_key(key: str) -> bool:
    lowered = key.lower()
    return lowered in TRACKING_QUERY_KEYS or lowered.startswith(TRACKING_QUERY_PREFIXES)
