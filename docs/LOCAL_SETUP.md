# Thiết lập môi trường local

Tài liệu này mô tả các nguyên tắc chung để clone và chạy project trên máy cá nhân.

## Bước chung

```bash
git clone https://github.com/quangducnguyen1205/kinh-lup-ir.git
cd kinh-lup-ir
cp .env.example .env
```

Điền thông tin kết nối cần thiết từ tab `CONNECTION` trong Google Sheet vào file `.env` local.

**Không commit `.env` lên Git.**

## Công cụ cần cài

- Node.js 22+
- Java 21
- Maven 3.9+
- Python 3.11+

Lệnh chạy cụ thể của từng module nằm trong `docs/RUN_MODULES.md`.

## Port mặc định

- Backend: `8080`
- Frontend: Vite cấp port khi chạy development server
- PostgreSQL / Shared Pooler Session mode: `5432`

## Nguyên tắc dữ liệu

- PostgreSQL là **nguồn dữ liệu chuẩn dùng chung**.
- Lucene index là dữ liệu dẫn xuất và phải rebuild được từ PostgreSQL.
- Không commit database dump, crawl output lớn hoặc Lucene index vào Git.
- Frontend không kết nối PostgreSQL trực tiếp; frontend gọi Backend API.
