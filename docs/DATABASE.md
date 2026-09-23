# CSDL dùng chung — Supabase PostgreSQL

## Mục tiêu

Nhóm dùng **một Supabase project chung** làm nguồn dữ liệu chuẩn cho dữ liệu đã crawl.

```text
Crawler (Scrapy) ─┐
                  ├─→ Supabase PostgreSQL
Backend (Spring) ─┘

Frontend (React) ─X→ PostgreSQL
Frontend (React) ─→ Backend API
```

Lucene index **không** phải CSDL dùng chung. Nó là dữ liệu dẫn xuất và phải rebuild được từ PostgreSQL.

## Thông tin kết nối dùng chung

Tab **`CONNECTION`** trong Google Sheet là nơi tập trung thông tin kết nối nội bộ của nhóm.

- Đức và Chính cần thông tin kết nối DB cho crawler/backend.
- Nam chỉ cần khi task tích hợp thực sự cần truy cập dữ liệu thật.
- Sơn không cần DB credential; frontend chỉ gọi Backend API.
- Theo quyết định của lead, credential có thể lưu trên Sheet nội bộ nhưng **không được commit lên GitHub**.

Google Sheet:

https://docs.google.com/spreadsheets/d/16iiLZQUk4thjs51NRptEC9BiBmRKls5_om0LWwEa1Ig/edit

## Kết nối khi phát triển local

Nhóm thống nhất dùng **một connection canonical duy nhất**: Supabase Shared Pooler — Session mode.

```text
Host:     aws-0-ap-southeast-1.pooler.supabase.com
Port:     5432
Database: postgres
Username: postgres.uxhzigjvjoygcqadvydo
Password: xem tab CONNECTION
SSL:      require
```

Connection này dùng chung cho DataGrip, crawler và backend. Direct PostgreSQL endpoint không dùng trong workflow của nhóm để tránh hai cách kết nối song song gây nhầm lẫn.

### Biến môi trường

Crawler:

```env
DATABASE_URL=
```

Backend:

```env
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
```

Mỗi người copy giá trị từ Sheet sang `.env` local. `.env` vẫn phải nằm ngoài Git.

## Project hiện tại

- Supabase project: `kinh-lup-ir`
- Project ref: `uxhzigjvjoygcqadvydo`
- Region: `ap-southeast-1` (Singapore)
- Canonical DB host: `aws-0-ap-southeast-1.pooler.supabase.com`
- Database: `postgres`
- Port: `5432`
- Username: `postgres.uxhzigjvjoygcqadvydo`
- Chi phí khi tạo: Free / `$0` mỗi tháng

## Các tính năng Supabase hiện chưa cần

BTL hiện chỉ cần PostgreSQL. Chưa cần phụ thuộc vào:

- Auth
- Realtime
- Edge Functions
- Data API trực tiếp từ frontend

Có thể cân nhắc Supabase Storage sau này nếu nhóm muốn lưu file PDF/DOCX gốc trên cloud.

## Trạng thái

Project đã được tạo và Shared Pooler Session connection đã được kiểm tra thành công bằng DataGrip. Credential/connection string đầy đủ nằm ở tab `CONNECTION`; không đưa password vào Git.

Schema bảng hiện tại xem tại `docs/DB_SCHEMA.md`.
