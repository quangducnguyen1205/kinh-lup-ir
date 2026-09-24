from datetime import datetime, timedelta, timezone

import pytest

from kinhlup_crawler.extraction import (
    MIN_CONTENT_CHARS,
    ExtractedPage,
    content_hash,
    extract_page,
    skip_reason,
)

ARTICLE_BODY = (
    "Đại học Bách khoa Hà Nội tổ chức lễ tốt nghiệp cho hơn 1000 kỹ sư và cử nhân. "
    "Buổi lễ có sự tham dự của lãnh đạo Đại học, giảng viên và gia đình sinh viên. "
    "Các tân kỹ sư được trao bằng và chia sẻ về hành trình học tập tại Bách khoa."
)

NEWS_PAGE = f"""<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <title>  Lễ tốt nghiệp   năm 2026 - HUST </title>
  <meta name="description" content="Tin tức lễ tốt nghiệp">
  <meta property="article:published_time" content="2026-07-15T08:30:00+07:00">
  <link rel="canonical" href="https://hust.edu.vn/vi/news/le-tot-nghiep.html">
  <style>.menu {{ color: red; }}</style>
  <script>var tracking = "không được lọt vào text";</script>
</head>
<body>
  <header><div class="logo">Đại học Bách khoa Hà Nội</div></header>
  <nav><ul><li>Trang chủ</li><li>Tuyển sinh</li></ul></nav>
  <main>
    <article>
      <h1>Lễ tốt nghiệp năm 2026</h1>
      <p>{ARTICLE_BODY}</p>
      <table><tr><td>Ngày</td><td>15/07/2026</td></tr></table>
      <noscript>Vui lòng bật JavaScript</noscript>
      <svg><text>icon text</text></svg>
    </article>
  </main>
  <footer>Số 1 Đại Cồ Việt, Hà Nội</footer>
  <script>console.log("footer script");</script>
</body>
</html>
"""


def test_title_is_taken_from_title_tag_with_whitespace_normalized():
    assert extract_page(NEWS_PAGE).title == "Lễ tốt nghiệp năm 2026 - HUST"


def test_title_falls_back_to_og_title_then_h1():
    og_page = '<html><head><meta property="og:title" content="OG title"></head><body></body></html>'
    h1_page = "<html><body><h1>Tiêu đề <b>chính</b></h1></body></html>"
    assert extract_page(og_page).title == "OG title"
    assert extract_page(h1_page).title == "Tiêu đề chính"
    assert extract_page("<html><body><p>no title</p></body></html>").title is None


def test_svg_title_is_not_used_as_page_title():
    page = "<html><body><svg><title>icon</title></svg><h1>Thông báo</h1></body></html>"
    assert extract_page(page).title == "Thông báo"


def test_raw_text_keeps_visible_article_text_with_vietnamese_unicode():
    raw_text = extract_page(NEWS_PAGE).raw_text
    assert "Lễ tốt nghiệp năm 2026" in raw_text
    assert ARTICLE_BODY in raw_text
    assert "Ngày\n15/07/2026" in raw_text


def test_raw_text_excludes_script_style_noscript_svg_and_boilerplate():
    raw_text = extract_page(NEWS_PAGE).raw_text
    for unwanted in (
        "tracking", "footer script", "color: red", "Vui lòng bật JavaScript",
        "icon text", "Trang chủ", "Số 1 Đại Cồ Việt", "Lễ tốt nghiệp năm 2026 - HUST",
    ):
        assert unwanted not in raw_text


def test_raw_text_whitespace_is_normalized():
    raw_text = extract_page(NEWS_PAGE).raw_text
    assert "  " not in raw_text
    assert "\n\n" not in raw_text
    assert raw_text == raw_text.strip()


def test_body_is_used_when_no_single_content_container_exists():
    page = f"""<html><body>
      <header>Site banner</header>
      <div class="post"><p>{ARTICLE_BODY}</p></div>
      <article><p>Teaser một</p></article><article><p>Teaser hai</p></article>
      <footer>Footer</footer>
    </body></html>"""
    raw_text = extract_page(page).raw_text
    assert ARTICLE_BODY in raw_text
    assert "Teaser một" in raw_text and "Teaser hai" in raw_text
    assert "Site banner" not in raw_text and "Footer" not in raw_text


def test_adjacent_inline_and_block_elements_do_not_merge_words():
    page = "<html><body><ul><li>Tin tức</li><li>Sự kiện</li></ul><span>A</span><span>B</span></body></html>"
    assert extract_page(page).raw_text == "Tin tức\nSự kiện\nA B"


def test_metadata_fields_are_extracted():
    page = extract_page(NEWS_PAGE)
    assert page.canonical_href == "https://hust.edu.vn/vi/news/le-tot-nghiep.html"
    assert page.description == "Tin tức lễ tốt nghiệp"
    assert page.language == "vi"
    assert page.published_at == datetime(2026, 7, 15, 8, 30, tzinfo=timezone(timedelta(hours=7)))


def test_invalid_published_time_is_ignored():
    page = '<html><head><meta property="article:published_time" content="hôm qua"></head></html>'
    assert extract_page(page).published_at is None


def test_empty_document_yields_empty_page():
    page = extract_page("")
    assert page.raw_text == ""
    assert skip_reason(page) == "too_short"


def test_content_page_is_kept():
    assert skip_reason(extract_page(NEWS_PAGE)) is None


def test_short_page_is_skipped():
    page = extract_page("<html><head><title>Trang chủ</title></head><body><p>Xin chào</p></body></html>")
    assert skip_reason(page) == "too_short"


def test_login_page_is_skipped():
    page = f"""<html><head><title>Đăng nhập hệ thống</title></head><body>
      <p>{ARTICLE_BODY}</p>
      <form><input name="user"><input type="Password" name="pw"></form>
    </body></html>"""
    assert skip_reason(extract_page(page)) == "login_form"


@pytest.mark.parametrize(
    "title",
    ["404", "404 - Không tìm thấy", "Page not found", "Lỗi 500", "Trang không tồn tại", "403 | Forbidden"],
)
def test_error_pages_are_skipped(title):
    page = ExtractedPage(title=title, raw_text="x" * MIN_CONTENT_CHARS)
    assert skip_reason(page) == "error_page"


def test_title_starting_with_a_number_is_not_an_error_page():
    page = ExtractedPage(title="500 sinh viên nhận học bổng", raw_text="x" * MIN_CONTENT_CHARS)
    assert skip_reason(page) is None


def test_noindex_page_is_skipped():
    page = f"""<html><head><meta name="robots" content="NOINDEX, follow"></head>
      <body><p>{ARTICLE_BODY}</p></body></html>"""
    assert skip_reason(extract_page(page)) == "noindex"


def test_content_hash_is_sha256_of_utf8_raw_text():
    assert content_hash("") == "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    assert content_hash("Bách khoa") == content_hash("Bách khoa")
    assert content_hash("Bách khoa") != content_hash("Bach khoa")
    assert len(content_hash(ARTICLE_BODY)) == 64


def test_same_page_always_produces_same_text_and_hash():
    first = extract_page(NEWS_PAGE)
    second = extract_page(NEWS_PAGE)
    assert first.raw_text == second.raw_text
    assert content_hash(first.raw_text) == content_hash(second.raw_text)
