# Shared database — Supabase PostgreSQL

## Mục tiêu

Nhóm dùng **một Supabase project chung** làm PostgreSQL source of truth cho dữ liệu đã crawl.

```text
Crawler (Scrapy) ─┐
                  ├─→ Supabase PostgreSQL
Backend (Spring) ─┘

Frontend (React) ─X→ PostgreSQL
Frontend (React) ─→ Backend API
```

Lucene index **không** phải shared database. Nó là derived data và phải rebuild được từ PostgreSQL.

## Thông tin kết nối dùng chung

Lead đã quyết định tab **`CONNECTION`** trong Google Sheet là nơi tập trung thông tin kết nối nội bộ cho nhóm.

- Đức và Sơn lấy DB connection info từ tab `CONNECTION`.
- Nam chỉ cần DB credential khi task integration thực sự cần.
- Chính không cần DB credential; frontend chỉ gọi Backend API.
- Credential có thể nằm trên Sheet nội bộ theo quyết định của nhóm, nhưng **không được commit lên GitHub**.

Google Sheet:

https://docs.google.com/spreadsheets/d/1Vq9XXLyeHBuoYnM28nu2P6et_BKhn4QEjnQ3sMuiOQE/edit

## Local development

Ưu tiên connection string do Supabase cung cấp ở:

```text
Project → Connect → Shared Pooler → Session mode
```

Session mode phù hợp cho laptop/mạng IPv4 và client persistent như Spring Boot hoặc crawler.

Không tự đoán pooler host/username. Copy nguyên thông tin từ Supabase Connect và ghi vào tab `CONNECTION`.

### Environment variables

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

Mỗi người copy giá trị từ Sheet sang `.env` local. `.env` vẫn bị ignore và không commit.

## Project hiện tại

- Supabase project: `kinh-lup-ir`
- Project ref: `uxhzigjvjoygcqadvydo`
- Region: `ap-southeast-1` (Singapore)
- Direct DB host: `db.uxhzigjvjoygcqadvydo.supabase.co`
- Database: `postgres`
- Direct port: `5432`
- Cost: Free / `$0` mỗi tháng tại thời điểm tạo

## Supabase features chưa cần dùng

BTL hiện chỉ cần PostgreSQL. Không cần phụ thuộc vào Auth, Realtime, Edge Functions hoặc Data API từ frontend.

Có thể cân nhắc Storage sau này nếu nhóm muốn lưu file PDF/DOCX gốc trên cloud.

## Step 3

Project đã được tạo và SQL connectivity đã verify. DB password + exact Shared Pooler Session connection string cần được lead copy từ Supabase Dashboard → Connect vào tab `CONNECTION` một lần.

Schema bảng và migration thuộc **Step 4**.
