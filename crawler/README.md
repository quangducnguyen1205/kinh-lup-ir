# Crawler

**Người phụ trách:** Đức  
**Task:** T01 — Crawler HUST baseline (đã có); T02 — Detect new/updated content (sau)

Công nghệ: Python + Scrapy, ghi vào Supabase PostgreSQL dùng chung (xem `docs/DATABASE.md`, `docs/DB_SCHEMA.md`).

## Cài đặt

Chạy từ thư mục `crawler/` (Python 3.11+):

```bash
python -m venv .venv
# Windows: .venv\Scripts\activate
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
```

Tạo `.env` ở **thư mục gốc repo** (nếu chưa có). Crawler tự đọc file này; biến đã `export` trong shell được ưu tiên hơn.

```bash
cp ../.env.example ../.env
```

| Biến | Ý nghĩa | Mặc định |
|---|---|---|
| `DATABASE_URL` | Connection Shared Pooler (Session mode). Bắt buộc với spider `hust`. | — |
| `CRAWLER_START_URL` | URL bắt đầu crawl | `https://hust.edu.vn/` |
| `CRAWLER_ALLOWED_DOMAIN` | Chỉ crawl domain này và subdomain của nó | `hust.edu.vn` |
| `CRAWLER_MAX_PAGES` | Số response tối đa mỗi lượt (tính cả robots.txt, redirect, lỗi); `0` = không giới hạn | `50` |

## Chạy

```bash
scrapy list                      # hust, smoke
scrapy crawl smoke -O smoke.json # kiểm tra hạ tầng, dùng example.com, không cần DB
scrapy crawl hust                # crawl HUST và ghi vào PostgreSQL
```

Các biến thể hay dùng:

```bash
CRAWLER_MAX_PAGES=20 scrapy crawl hust                    # giới hạn số trang
scrapy crawl hust -a start_url=https://soict.hust.edu.vn/ # đổi URL bắt đầu (phải thuộc *.hust.edu.vn)
scrapy crawl hust -s ITEM_PIPELINES='{}' -O out.jsonl     # chạy thử, KHÔNG ghi DB
scrapy crawl hust -s LOG_LEVEL=DEBUG                      # xem lý do từng trang bị bỏ qua
```

Page cap là giới hạn an toàn: request đang chạy dở vẫn được hoàn tất nên lượt crawl có thể vượt cap vài trang.

Nếu thiếu `DATABASE_URL`, spider `hust` dừng ngay trước request đầu tiên với thông báo lỗi rõ ràng.

## Dữ liệu được ghi vào đâu

Mỗi lần `scrapy crawl hust`:

1. Tạo một row `crawl_runs` với `trigger_type = LOCAL`, `status = RUNNING`.
2. Mỗi trang nội dung → `INSERT` vào `documents` với `status = ACTIVE`, `index_status = PENDING`, `last_crawl_run_id` = run hiện tại.
3. Khi kết thúc, cập nhật run: `finished_at`, `discovered_count` (số document trích được), `new_count` (số row mới), `failed_count` (lỗi HTTP/mạng sau retry + lỗi ghi DB), `notes` (lý do dừng, số trang đã có sẵn, số trang bị bỏ qua) và `status`:
   - `SUCCEEDED`: chạy hết (hoặc chạm page cap) và không có lỗi;
   - `PARTIAL`: có lỗi hoặc bị ngắt nhưng vẫn trích được document;
   - `FAILED`: không trích được document nào và có lỗi/bị ngắt.

Request bị huỷ do chạm page cap và lỗi tải `robots.txt` không tính vào `failed_count`.

Kiểm tra nhanh bằng SQL (read-only):

```sql
select status, discovered_count, new_count, failed_count, notes
from public.crawl_runs order by started_at desc limit 5;

select url, domain, title, content_type, length(raw_text), index_status
from public.documents order by first_seen_at desc limit 20;
```

## Crawler làm gì

**Phạm vi URL** (`kinhlup_crawler/urls.py`)

- Chỉ http/https thuộc `CRAWLER_ALLOWED_DOMAIN` hoặc subdomain; domain ngoài, kể cả redirect ra ngoài, bị loại.
- Chuẩn hoá URL: bỏ fragment, bỏ tham số tracking (`utm_*`, `fbclid`, `gclid`, `zarsrc`), hạ chữ thường scheme/host, bỏ port mặc định, sắp xếp query, percent-encode path.
- Không request: `mailto:`/`tel:`/`javascript:`, URL chứa user/password, file không phải HTML (PDF, DOCX, PPTX, ảnh, ...), RSS feed, trang đăng nhập/quản trị (`login`, `dang-nhap`, `wp-admin`, `adfs`, `account`, ...).
- Tuân thủ `robots.txt`, không gửi cookie, `DOWNLOAD_DELAY` + AutoThrottle, tối đa 2 request song song mỗi host, crawl theo chiều rộng.

**Bóc tách** (`kinhlup_crawler/extraction.py`)

- `title`: `<title>`, nếu không có thì `og:title`, rồi `<h1>`.
- `raw_text`: text hiển thị, ưu tiên một `<article>`/`<main>` duy nhất, nếu không thì `<body>`; bỏ `script`, `style`, `noscript`, `svg`, `iframe`, `nav`, `footer`, `aside`, `form` (và `header` khi dùng `<body>`). Chỉ chuẩn hoá khoảng trắng, **giữ nguyên Unicode tiếng Việt** — chưa chuẩn hoá/tách từ tiếng Việt.
- `content_type`: `text/html`.
- `canonical_url`: `<link rel="canonical">` nếu thuộc HUST, ngược lại là chính `url` đã chuẩn hoá. Canonical trỏ về trang chủ từ một trang con bị bỏ qua (thường là lỗi template, và vì `canonical_url` unique nên sẽ làm mất các trang khác).
- `published_at`: từ `article:published_time` nếu có.
- `metadata`: `source`, `http_status`, `charset`, `language`, `description`, `declared_canonical` (chỉ các giá trị có thật).
- `content_hash` = `sha256(raw_text.encode("utf-8")).hexdigest()` — tính lại được từ `raw_text` trong DB.

**Trang có phải nội dung không** — bỏ qua (không ghi DB) nếu: có `meta robots noindex`; có ô nhập password (trang đăng nhập — không follow link tiếp); title là trang lỗi (`404`, `Not Found`, `Trang không tồn tại`, ...); `raw_text` ngắn hơn 200 ký tự. Link trên trang bị bỏ qua (trừ trang đăng nhập) vẫn được follow.

**Chống trùng lặp**

- Trong một lượt: URL chuẩn hoá + dupefilter của Scrapy.
- Với DB: `INSERT ... ON CONFLICT DO NOTHING` trên cả `url` và `canonical_url` — row đầu tiên được giữ, bản trùng sau được đếm vào `already_stored` và không làm crawl lỗi.

## Test

```bash
python -m pytest -q
```

Test chạy offline, không cần mạng hay DB: URL scope/chuẩn hoá, bóc tách, heuristic nội dung, SHA-256, spider và pipeline (dùng connection giả).

## Chưa làm trong T01

- **T02**: URL đã có trong DB hiện được giữ nguyên (không cập nhật `last_crawled_at`, không so sánh hash). Phân loại new/updated/unchanged, cập nhật `updated_count`/`unchanged_count` là việc của T02 — `content_hash` đã sẵn sàng để so sánh.
- **T05**: PDF/DOCX/PPTX không được tải; bóc tách bằng Apache Tika thuộc T05.
- Crawl định kỳ (scheduler) và API crawl thủ công.
- Trang render bằng JavaScript (SPA) chỉ có text tĩnh nên thường bị bỏ qua vì quá ngắn.
