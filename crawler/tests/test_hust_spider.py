import pytest
import scrapy
from scrapy.http import HtmlResponse, Response, TextResponse
from scrapy.utils.test import get_crawler

from kinhlup_crawler import crawl_stats
from kinhlup_crawler.extraction import content_hash
from kinhlup_crawler.items import DocumentItem
from kinhlup_crawler.spiders.hust_spider import HustSpider

BODY_TEXT = (
    "Thông báo tuyển sinh đại học chính quy năm 2026 của Đại học Bách khoa Hà Nội. "
    "Thí sinh theo dõi lịch xét tuyển, chỉ tiêu và các phương thức tuyển sinh được công bố. "
    "Mọi thắc mắc vui lòng liên hệ Ban Tuyển sinh - Hướng nghiệp để được hỗ trợ kịp thời."
)


def make_spider(start_url="https://hust.edu.vn/", allowed_domain="hust.edu.vn"):
    crawler = get_crawler(HustSpider)
    return HustSpider.from_crawler(crawler, start_url=start_url, allowed_domain=allowed_domain)


def html_response(url, body, content_type="text/html; charset=utf-8", status=200):
    return HtmlResponse(
        url=url,
        body=body.encode("utf-8"),
        encoding="utf-8",
        status=status,
        headers={"Content-Type": content_type},
    )


def page(links="", canonical=None, extra_body=BODY_TEXT, title="Tuyển sinh 2026"):
    canonical_tag = f'<link rel="canonical" href="{canonical}">' if canonical else ""
    return f"""<html lang="vi"><head><title>{title}</title>{canonical_tag}</head>
      <body><article><h1>{title}</h1><p>{extra_body}</p></article>{links}</body></html>"""


def split_results(results):
    items = [result for result in results if isinstance(result, DocumentItem)]
    requests = [result for result in results if isinstance(result, scrapy.Request)]
    return items, requests


def test_content_page_becomes_document_item_matching_contract():
    spider = make_spider()
    response = html_response("https://hust.edu.vn/vi/tuyen-sinh.html#top", page())

    items, _ = split_results(list(spider.parse(response)))

    assert len(items) == 1
    item = items[0]
    assert item.url == "https://hust.edu.vn/vi/tuyen-sinh.html"
    assert item.canonical_url == item.url
    assert item.domain == "hust.edu.vn"
    assert item.title == "Tuyển sinh 2026"
    assert item.content_type == "text/html"
    assert BODY_TEXT in item.raw_text
    assert item.content_hash == content_hash(item.raw_text)
    assert item.metadata["http_status"] == 200
    assert item.metadata["charset"] == "utf-8"
    assert item.metadata["language"] == "vi"
    assert item.metadata["source"] == "kinhlup_crawler/hust"


def test_only_in_scope_html_links_are_followed_once():
    links = """
      <a href="/vi/news/a.html">A</a>
      <a href="/vi/news/a.html#comments">A again</a>
      <a href="https://soict.hust.edu.vn/tin-tuc">SoICT</a>
      <a href="https://www.facebook.com/dhbkhanoi">Facebook</a>
      <a href="mailto:tuyensinh@hust.edu.vn">Mail</a>
      <a href="tel:0123">Phone</a>
      <a href="javascript:void(0)">JS</a>
      <a href="/files/de-an.pdf">PDF</a>
      <a href="/wp-login.php">Login</a>
      <area href="https://ctsv.hust.edu.vn/">
    """
    spider = make_spider()
    response = html_response("https://hust.edu.vn/vi/", page(links=links))

    _, requests = split_results(list(spider.parse(response)))

    assert [request.url for request in requests] == [
        "https://hust.edu.vn/vi/news/a.html",
        "https://soict.hust.edu.vn/tin-tuc",
        "https://ctsv.hust.edu.vn/",
    ]
    assert all(request.errback == spider.on_request_error for request in requests)


def test_in_scope_canonical_is_used_and_external_canonical_is_ignored():
    spider = make_spider()
    same_site = html_response(
        "https://soict.hust.edu.vn/bai-viet?id=1&utm_source=zalo",
        page(canonical="/bai-viet.html"),
    )
    external = html_response(
        "https://soict.hust.edu.vn/bai-viet-2.html",
        page(canonical="https://mirror.example.com/bai-viet-2.html"),
    )

    [same_site_item], _ = split_results(list(spider.parse(same_site)))
    [external_item], _ = split_results(list(spider.parse(external)))

    assert same_site_item.url == "https://soict.hust.edu.vn/bai-viet?id=1"
    assert same_site_item.canonical_url == "https://soict.hust.edu.vn/bai-viet.html"
    assert external_item.canonical_url == "https://soict.hust.edu.vn/bai-viet-2.html"
    assert external_item.metadata["declared_canonical"] == "https://mirror.example.com/bai-viet-2.html"


def test_canonical_pointing_to_home_page_is_ignored_on_deeper_pages():
    spider = make_spider()
    article = html_response(
        "https://fami.hust.edu.vn/su-mang/", page(canonical="https://fami.hust.edu.vn/")
    )
    home = html_response("https://soict.hust.edu.vn/", page(canonical="https://soict.hust.edu.vn"))

    [article_item], _ = split_results(list(spider.parse(article)))
    [home_item], _ = split_results(list(spider.parse(home)))

    assert article_item.canonical_url == "https://fami.hust.edu.vn/su-mang/"
    assert home_item.canonical_url == "https://soict.hust.edu.vn/"


def test_short_page_is_not_stored_but_its_links_are_followed():
    spider = make_spider()
    response = html_response(
        "https://hust.edu.vn/vi/",
        page(extra_body="Menu", links='<a href="/vi/news/a.html">A</a>'),
    )

    items, requests = split_results(list(spider.parse(response)))

    assert items == []
    assert [request.url for request in requests] == ["https://hust.edu.vn/vi/news/a.html"]
    assert spider.crawler.stats.get_value(f"{crawl_stats.PAGES_SKIPPED}/too_short") == 1


def test_login_page_is_neither_stored_nor_followed():
    spider = make_spider()
    body = page(links='<form><input type="password"></form><a href="/private.html">x</a>')
    response = html_response("https://qldt.hust.edu.vn/", body)

    assert list(spider.parse(response)) == []
    assert spider.crawler.stats.get_value(f"{crawl_stats.PAGES_SKIPPED}/login_form") == 1


def test_redirect_to_external_or_login_url_is_dropped():
    spider = make_spider()
    external = html_response("https://login.microsoftonline.com/common", page())
    login = html_response("https://ctsv.hust.edu.vn/dang-nhap", page())

    assert list(spider.parse(external)) == []
    assert list(spider.parse(login)) == []
    assert spider.crawler.stats.get_value(crawl_stats.PAGES_SKIPPED) == 2


@pytest.mark.parametrize(
    "response",
    [
        Response(
            url="https://hust.edu.vn/file",
            body=b"%PDF-1.7",
            headers={"Content-Type": "application/pdf"},
        ),
        TextResponse(
            url="https://hust.edu.vn/api",
            body=b"{}",
            encoding="utf-8",
            headers={"Content-Type": "application/json"},
        ),
    ],
)
def test_non_html_responses_are_skipped(response):
    spider = make_spider()
    assert list(spider.parse(response)) == []
    assert spider.crawler.stats.get_value(f"{crawl_stats.PAGES_SKIPPED}/non_html") == 1


def test_start_url_is_normalized_and_must_be_in_scope():
    assert make_spider(start_url="HTTPS://SoICT.hust.edu.vn").start_url == "https://soict.hust.edu.vn/"
    with pytest.raises(ValueError):
        make_spider(start_url="https://example.com/")
    with pytest.raises(ValueError):
        make_spider(start_url="not a url")


def test_start_url_and_domain_default_to_settings():
    crawler = get_crawler(
        HustSpider,
        {"CRAWLER_START_URL": "https://sami.hust.edu.vn/", "CRAWLER_ALLOWED_DOMAIN": "hust.edu.vn"},
    )
    spider = HustSpider.from_crawler(crawler)
    assert spider.start_url == "https://sami.hust.edu.vn/"
    assert spider.allowed_domains == ["hust.edu.vn"]
