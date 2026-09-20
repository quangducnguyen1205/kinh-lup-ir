# Backend / Search

**Người phụ trách setup:** Chính  
**Task khởi đầu:** T04 — Backend API + Lucene baseline; T05 — Tika extraction baseline

Công nghệ: Java + Spring Boot + Apache Lucene + Apache Tika.

## Nguyên tắc

- PostgreSQL là nguồn dữ liệu chuẩn.
- Lucene index là dữ liệu dẫn xuất và phải rebuild được từ PostgreSQL.
- Endpoint và response phải bám tab `API CONTRACT`.
- Cách đọc/ghi dữ liệu phải bám tab `DATA CONTRACT`.
- Frontend không được phụ thuộc trực tiếp vào DB.

## Skeleton hiện có

Step 5 mới cung cấp:

- Spring Boot application chạy được;
- `GET /api/health`;
- dependency Lucene + Tika;
- test kiểm tra Spring context.

Build Lucene index, search API, document detail và Tika extraction vẫn thuộc T04/T05.
