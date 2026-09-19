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

Lucene index **không** được coi là shared database. Nó là derived data và phải rebuild được từ PostgreSQL.

## Quy tắc kết nối

### Local development

Ưu tiên connection string do Supabase cung cấp ở:

```text
Project → Connect → Shared Pooler → Session mode
```

Lý do: phù hợp cho laptop/mạng IPv4 và client chạy lâu như Spring Boot hoặc crawler.

Không tự ghép hostname/project ref. Copy nguyên host, port và username do Supabase hiển thị.

### Environment variables

Crawler dùng:

```env
DATABASE_URL=
```

Backend dùng:

```env
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
```

Credential thật chỉ nằm trong file `.env` local hoặc secret store của môi trường chạy.

**Không commit `.env`, password, service-role key, connection string thật hoặc database dump vào Git.**

## Access model v0

- Đức: cần DB connection cho crawler/ingestion.
- Sơn: cần DB connection cho backend/indexing.
- Nam: chưa cần DB credential để làm research tokenizer ở bước đầu.
- Chính: không cần DB credential; frontend chỉ dùng Backend API.

Nếu sau này Nam cần integration trực tiếp với dữ liệu thật, lead cấp credential private theo nhu cầu.

## Supabase features chưa cần dùng

BTL hiện chỉ cần PostgreSQL. Không cần phụ thuộc vào Auth, Realtime, Edge Functions hoặc Data API từ frontend.

Có thể cân nhắc Storage sau này nếu nhóm quyết định lưu file PDF/DOCX gốc trên cloud.

## Step 3 hoàn thành khi

- Có Supabase project chung.
- Lead lưu credential ngoài Git.
- Đức và Sơn test được kết nối từ máy/dev environment.
- Connection contract trong `.env.example` được giữ ổn định.

Schema bảng và migration thuộc **Step 4**, không nằm trong Step 3.
