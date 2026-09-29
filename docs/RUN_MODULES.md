# Cách chạy các module

Step 5 tạo **khung kỹ thuật chạy được** cho mọi module. Tính năng được bổ sung dần theo task; hiện crawler đã có spider HUST của T01, backend đã có luồng PostgreSQL → Lucene → API; xem README từng module cho tiến độ chi tiết.

## Yêu cầu môi trường

- Node.js 22+
- Java 21
- Maven 3.9+
- Python 3.11+ (Python 3.13 dùng được)

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Kết quả mong đợi: Vite in ra local URL và trình duyệt hiển thị trang skeleton Kính Lúp.

Kiểm tra production build:

```bash
npm run build
```

## Backend

```bash
cd backend
mvn spring-boot:run
```

Kiểm tra:

```text
GET http://localhost:8080/api/health

{
  "status": "UP",
  "service": "kinh-lup-backend"
}
```

Backend đã có rebuild index từ DB, search phân trang, document detail và service Tika cho PDF/DOCX/PPTX. Xem `backend/README.md` để cấu hình `.env`, bật rebuild local và chạy test.

## Crawler

```bash
cd crawler
python -m venv .venv
# Windows: .venv\Scripts\activate
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
scrapy list
```

Kết quả mong đợi: thấy spider `hust` và `smoke`.

Có thể chạy smoke test hạ tầng (example.com, không cần DB):

```bash
scrapy crawl smoke -O smoke.json
```

Crawl HUST và ghi vào PostgreSQL (cần `DATABASE_URL` trong `.env` ở thư mục gốc):

```bash
CRAWLER_MAX_PAGES=20 scrapy crawl hust
python -m pytest -q
```

Chi tiết cấu hình, dữ liệu được ghi và phạm vi T01 xem `crawler/README.md`.

## Nghiên cứu xử lý tiếng Việt

```bash
cd research/vietnamese-analysis
python benchmark.py
```

Kết quả mong đợi: chương trình báo harness đã sẵn sàng và yêu cầu owner T06 bổ sung benchmark thật.

## CSDL dùng chung

Các skeleton không cần credential DB để khởi động. Khi bắt đầu task crawler/backend, copy giá trị cần thiết từ tab `CONNECTION` trên Google Sheet vào `.env` local.

**Không commit `.env` thật lên Git.**
