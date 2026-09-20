# Schema CSDL v0

CSDL PostgreSQL dùng chung cố ý bắt đầu với **chỉ hai bảng core** để đủ cho luồng BTL mà không over-engineer.

## `crawl_runs`

Một row tương ứng với một lượt crawl/cập nhật logic.

Các trường quan trọng:

- `id`: UUID của lượt crawl.
- `trigger_type`: `MANUAL`, `SCHEDULED` hoặc `LOCAL`.
- `status`: `QUEUED`, `RUNNING`, `SUCCEEDED`, `PARTIAL` hoặc `FAILED`.
- `started_at`, `finished_at`: thời gian bắt đầu/kết thúc.
- `discovered_count`, `new_count`, `updated_count`, `unchanged_count`, `failed_count`: các bộ đếm tổng hợp.

Scheduler định kỳ và API cập nhật thủ công phải tạo cùng loại record `crawl_runs` và gọi lại cùng một pipeline crawl/update.

## `documents`

Một row tương ứng với một tài liệu đã crawl và có thể đưa vào hệ thống tìm kiếm.

Các trường quan trọng:

- `url`: URL thực tế đã crawl; unique ở v0.
- `canonical_url`: URL đã chuẩn hoá/canonical nếu xác định được.
- `domain`: domain/subdomain HUST.
- `title`: tiêu đề đã bóc tách.
- `content_type`: HTML/PDF/etc.
- `raw_text`: text đã extract trước khi xử lý tiếng Việt.
- `normalized_text`: kết quả chuẩn hoá tiếng Việt nếu quyết định persist.
- `metadata`: JSONB lưu metadata linh hoạt từ HTML/Tika.
- `content_hash`: dùng phát hiện nội dung thay đổi.
- `status`: `ACTIVE`, `FAILED` hoặc `DELETED`.
- `index_status`: `PENDING`, `INDEXED` hoặc `FAILED`.
- `first_seen_at`: lần đầu crawler thấy URL.
- `last_crawled_at`: lần gần nhất crawler kiểm tra URL.
- `updated_at`: lần gần nhất **nội dung nguồn thực sự thay đổi**.
- `last_indexed_at`: lần gần nhất Lucene index thành công.
- `last_crawl_run_id`: lượt crawl gần nhất chạm vào document.

## Quy tắc cập nhật quan trọng

**Không** cập nhật `documents.updated_at` khi recrawl một document mà nội dung không đổi.

```text
URL mới
→ INSERT document
→ index_status = PENDING

URL đã biết + content_hash giống
→ chỉ update last_crawled_at + last_crawl_run_id
→ giữ nguyên updated_at

URL đã biết + content_hash khác
→ update content/title/hash/metadata
→ updated_at = now()
→ index_status = PENDING
```

Quy tắc này cho phép backend làm incremental indexing bằng Lucene mà không phải reindex toàn bộ tài liệu sau mỗi lượt crawl.

## RLS

RLS được bật trên cả hai bảng public và hiện **không có Data API policy**.

Đây là chủ ý ở v0:

- React frontend không truy cập Supabase/PostgreSQL trực tiếp.
- Crawler và Spring backend dùng credential PostgreSQL của nhóm.
- Nếu sau này cần truy cập Supabase Data API trực tiếp thì phải thêm policy rõ ràng bằng migration trước.

## Migration

- `supabase/migrations/2026092001_initial_search_schema.sql`
- `supabase/migrations/2026092002_index_documents_last_crawl_run.sql`

Hai migration này đã được áp dụng lên Supabase project của nhóm.
