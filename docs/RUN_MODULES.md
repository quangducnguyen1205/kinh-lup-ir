# Run the module skeletons

Step 5 only creates **runnable technical shells**. It deliberately does not implement T01/T03/T04/T05/T06.

## Prerequisites

- Node.js 22+
- Java 21
- Maven 3.9+
- Python 3.11+ (3.13 is fine)

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Expected: Vite prints a local URL and the browser renders the Kính Lúp skeleton page.

Production build check:

```bash
npm run build
```

## Backend

```bash
cd backend
mvn spring-boot:run
```

Check:

```text
GET http://localhost:8080/api/health

{
  "status": "UP",
  "service": "kinh-lup-backend"
}
```

The skeleton already pins Lucene and Tika libraries, but does not create an index or parse documents yet.

## Crawler

```bash
cd crawler
python -m venv .venv
# Windows: .venv\Scripts\activate
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
scrapy list
```

Expected: `smoke`.

Optional infrastructure test:

```bash
scrapy crawl smoke -O smoke.json
```

The `smoke` spider uses example.com on purpose. HUST crawling and PostgreSQL ingestion belong to T01/T02.

## Vietnamese analysis research

```bash
cd research/vietnamese-analysis
python benchmark.py
```

Expected: a message telling the T06 owner to add the real benchmark.

## Shared database

The module skeletons do not require database credentials to start. When crawler/backend tasks begin, copy the required values from the Google Sheet `CONNECTION` tab into a local `.env`.

Never commit the real `.env`.
