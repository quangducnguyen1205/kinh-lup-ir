# Crawler

**Người phụ trách setup:** Đức  
**Task khởi đầu:** T01 — Crawler HUST baseline; T02 — Detect new/updated content

Công nghệ: Python + Scrapy.

## Trách nhiệm

Crawler phải tạo/cập nhật dữ liệu theo tab `DATA CONTRACT` trong Google Sheet.

Cơ chế cập nhật định kỳ là luồng chính. API cập nhật thủ công chỉ gọi lại cùng pipeline crawl/update.

## Skeleton hiện có

Step 5 mới cung cấp:

- Scrapy project chạy được;
- settings cơ bản;
- spider `smoke` dùng example.com để kiểm tra hạ tầng.

Spider HUST thật, discovery link, extract nội dung, dedupe, content hash và ghi PostgreSQL vẫn thuộc T01/T02.
