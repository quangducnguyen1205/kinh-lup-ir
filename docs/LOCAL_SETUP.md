# Local setup — nguyên tắc chung

Repository hiện mới là skeleton. Từng module sẽ bổ sung lệnh chạy cụ thể khi task setup của module hoàn thành.

## Bước chung

```bash
git clone https://github.com/quangducnguyen1205/kinh-lup-ir.git
cd kinh-lup-ir
cp .env.example .env
```

Điền credential DB thật vào `.env` trên máy cá nhân. Không commit `.env`.

## Port mặc định

- Backend: `8080`
- Frontend dev server: do Vite cấp khi module frontend được tạo
- PostgreSQL: `5432` nếu kết nối trực tiếp

## Nguyên tắc dữ liệu

- PostgreSQL là source of truth.
- Lucene index nằm local/runtime và phải rebuild được.
- Không commit database dump, crawl output lớn hoặc Lucene index vào Git.
