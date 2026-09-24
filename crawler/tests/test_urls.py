import pytest

from kinhlup_crawler.urls import is_auth_url, is_in_scope, normalize_url, should_follow

DOMAIN = "hust.edu.vn"


@pytest.mark.parametrize(
    "url",
    [
        "https://hust.edu.vn/",
        "https://soict.hust.edu.vn/tin-tuc",
        "http://ctsv.hust.edu.vn/page",
        "https://a.b.hust.edu.vn/",
        "https://HUST.EDU.VN/",
    ],
)
def test_hust_and_subdomains_are_in_scope(url):
    assert is_in_scope(url, DOMAIN)


@pytest.mark.parametrize(
    "url",
    [
        "https://example.com/",
        "https://evilhust.edu.vn/",
        "https://hust.edu.vn.evil.com/",
        "https://facebook.com/hust.edu.vn",
        "https://edu.vn/",
    ],
)
def test_external_and_lookalike_hosts_are_out_of_scope(url):
    assert not is_in_scope(url, DOMAIN)


@pytest.mark.parametrize(
    ("raw", "expected"),
    [
        ("https://soict.hust.edu.vn", "https://soict.hust.edu.vn/"),
        ("HTTPS://SoICT.HUST.edu.vn/tin-tuc", "https://soict.hust.edu.vn/tin-tuc"),
        ("https://hust.edu.vn:443/a", "https://hust.edu.vn/a"),
        ("http://hust.edu.vn:80/a", "http://hust.edu.vn/a"),
        ("http://hust.edu.vn:8080/a", "http://hust.edu.vn:8080/a"),
        ("https://hust.edu.vn/a?b=2&a=1", "https://hust.edu.vn/a?a=1&b=2"),
        ("https://hust.edu.vn/a?id=3&utm_source=fb&fbclid=x", "https://hust.edu.vn/a?id=3"),
        ("  https://hust.edu.vn/a  ", "https://hust.edu.vn/a"),
        ("https://dlib.hust.edu.vn/tài liệu", "https://dlib.hust.edu.vn/t%C3%A0i%20li%E1%BB%87u"),
    ],
)
def test_normalize_url_is_deterministic(raw, expected):
    assert normalize_url(raw) == expected
    assert normalize_url(expected) == expected


def test_fragments_collapse_to_one_url():
    variants = [
        "https://hust.edu.vn/vi/news.html",
        "https://hust.edu.vn/vi/news.html#top",
        "https://hust.edu.vn/vi/news.html#section-2",
    ]
    assert {normalize_url(url) for url in variants} == {"https://hust.edu.vn/vi/news.html"}


@pytest.mark.parametrize(
    "url",
    [
        "mailto:tuyensinh@hust.edu.vn",
        "tel:+84243869xxxx",
        "javascript:void(0)",
        "ftp://hust.edu.vn/file",
        "https://user:secret@hust.edu.vn/",
        "/relative/path",
        "",
    ],
)
def test_unusable_urls_are_rejected(url):
    assert normalize_url(url) is None


@pytest.mark.parametrize(
    "url",
    [
        "https://hust.edu.vn/wp-login.php",
        "https://ctsv.hust.edu.vn/dang-nhap",
        "https://soict.hust.edu.vn/wp-admin/edit.php",
        "https://hust.edu.vn/user/login?next=/",
        "https://ctt-sis.hust.edu.vn/Account/Login.aspx",
        "https://asso.hust.edu.vn/adfs/ls/?wa=wsignin1.0",
    ],
)
def test_auth_urls_are_detected(url):
    assert is_auth_url(url)
    assert not should_follow(url, DOMAIN)


@pytest.mark.parametrize(
    "url",
    [
        "https://hust.edu.vn/vi/tuyen-sinh/dang-ky-xet-tuyen.html",
        "https://soict.hust.edu.vn/login-tips-for-students.html",
        "https://hust.edu.vn/vi/news/account-management.html",
    ],
)
def test_similar_but_public_paths_are_not_auth_urls(url):
    assert not is_auth_url(url)


@pytest.mark.parametrize(
    "url",
    [
        "https://hust.edu.vn/files/quy-che.pdf",
        "https://hust.edu.vn/files/bieu-mau.DOCX",
        "https://hust.edu.vn/images/logo.png",
        "https://soict.hust.edu.vn/feed",
        "https://soict.hust.edu.vn/tin-tuc/feed/",
        "https://example.com/page",
    ],
)
def test_non_html_feed_and_external_urls_are_not_followed(url):
    assert not should_follow(url, DOMAIN)


@pytest.mark.parametrize(
    "url",
    [
        "https://hust.edu.vn/vi/news/tin-tuc-su-kien.html",
        "https://soict.hust.edu.vn/category/tin-tuc",
        "https://sami.hust.edu.vn/",
    ],
)
def test_public_html_pages_are_followed(url):
    assert should_follow(url, DOMAIN)
