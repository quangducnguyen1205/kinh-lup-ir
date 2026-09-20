# Cách chạy các module

Step 5 chỉ tạo **khung kỹ thuật chạy được**, cố ý chưa triển khai phần feature của T01/T03/T04/T05/T06.

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

Skeleton đã khai báo Lucene và Tika nhưng **chưa** tạo index, search hay bóc tách tài liệu.

## Crawler

```bash
cd crawler
python -m venv .venv
# Windows: .venv\Scripts\activate
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
scrapy list
```

Kết quả mong đợi: thấy spider `smoke`.

Có thể chạy smoke test hạ tầng:

```bash
scrapy crawl smoke -O smoke.json
```

Spider `smoke` cố ý dùng example.com. Crawl HUST và ghi PostgreSQL thuộc T01/T02.

## Nghiên cứu xử lý tiếng Việt

```bash
cd research/vietnamese-analysis
python benchmark.py
```

Kết quả mong đợi: chương trình báo harness đã sẵn sàng và yêu cầu owner T06 bổ sung benchmark thật.

## CSDL dùng chung

Các skeleton không cần credential DB để khởi động. Khi bắt đầu task crawler/backend, copy giá trị cần thiết từ tab `CONNECTION` trên Google Sheet vào `.env` local.

**Không commit `.env` thật lên Git.**
