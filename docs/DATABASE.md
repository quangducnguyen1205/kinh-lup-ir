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

- Đức và Sơn cần thông tin kết nối DB cho crawler/backend.
- Nam chỉ cần khi task tích hợp thực sự cần truy cập dữ liệu thật.
- Chính không cần DB credential; frontend chỉ gọi Backend API.
- Theo quyết định của lead, credential có thể lưu trên Sheet nội bộ nhưng **không được commit lên GitHub**.

Google Sheet:

https://docs.google.com/spreadsheets/d/16iiLZQUk4thjs51NRptEC9BiBmRKls5_om0LWwEa1Ig/edit

## Kết nối khi phát triển local

Ưu tiên connection string Supabase cung cấp tại:

```text
Project → Connect → Shared Pooler → Session mode
```

Session mode phù hợp cho laptop/mạng IPv4 và các client giữ kết nối lâu như Spring Boot hoặc crawler.

Không tự đoán pooler host/username. Copy nguyên thông tin từ Supabase Connect và ghi vào tab `CONNECTION`.

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
- Direct DB host: `db.uxhzigjvjoygcqadvydo.supabase.co`
- Database: `postgres`
- Direct port: `5432`
- Chi phí khi tạo: Free / `$0` mỗi tháng

## Các tính năng Supabase hiện chưa cần

BTL hiện chỉ cần PostgreSQL. Chưa cần phụ thuộc vào:

- Auth
- Realtime
- Edge Functions
- Data API trực tiếp từ frontend

Có thể cân nhắc Supabase Storage sau này nếu nhóm muốn lưu file PDF/DOCX gốc trên cloud.

## Trạng thái

Project đã được tạo và kết nối SQL đã được kiểm tra thành công. DB password và Session Pooler connection string chính xác sẽ được lead bổ sung vào tab `CONNECTION`.

Schema bảng hiện tại xem tại `docs/DB_SCHEMA.md`.
