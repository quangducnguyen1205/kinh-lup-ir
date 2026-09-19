# Kính Lúp — Bài tập lớn Tìm kiếm thông tin (IT4863)

Mục tiêu của repository này là xây dựng một **search engine mini cho hệ sinh thái `hust.edu.vn`**. Nhóm ưu tiên dựng một luồng end-to-end chạy được trước; sau đó các bài học IR sẽ được tích hợp dần vào phần analyzer, indexing, query processing, ranking và evaluation.

## Luồng hệ thống

```text
HUST / *.hust.edu.vn
        ↓
     Scrapy
        ↓
   PostgreSQL
        ↓
Vietnamese Analysis + Apache Tika
        ↓
  Apache Lucene
        ↓
 Spring Boot API
        ↓
   React + Vite
```

Cơ chế cập nhật chính là crawl/rà soát **định kỳ**. Manual refresh (`POST /api/admin/crawl`) là chức năng bonus/admin và phải dùng lại cùng pipeline crawl-update.

## Nguồn điều phối chung

Task, API Contract, Data Contract và Architecture Decisions được quản lý tại Google Sheet của nhóm:

https://docs.google.com/spreadsheets/d/1Vq9XXLyeHBuoYnM28nu2P6et_BKhn4QEjnQ3sMuiOQE/edit

**Sheet là source of truth cho việc giao task và contract. GitHub dùng để quản lý code. Không dùng GitHub Issues ở giai đoạn hiện tại.**

## Cấu trúc repository

```text
kinh-lup-ir/
├── frontend/                    # React + Vite — Search UI
├── backend/                     # Java + Spring Boot + Lucene + Tika
├── crawler/                     # Python + Scrapy
├── research/
│   └── vietnamese-analysis/     # Research/test tokenizer & normalization
├── docs/                        # Quy ước repo và setup chung
├── data/                        # Chỉ giữ placeholder; dữ liệu/index thật không commit
├── .env.example
├── .editorconfig
├── .gitignore
└── README.md
```

## Workflow làm task

1. Mở tab `TASKS` trong Google Sheet, tìm task có Owner là mình và đọc **Acceptance Criteria**.
2. Cập nhật Status thành `DOING`.
3. Đồng bộ `main` và tạo branch mới theo Task ID:

```bash
git checkout main
git pull
git checkout -b t01-crawler-baseline
```

4. Code trong module tương ứng. Không commit secret, `.env`, database dump hay Lucene index.
5. Trước khi bàn giao, tự chạy/test phần mình phụ trách và cập nhật `Deliverable / Link` + `Notes` trên Sheet.
6. Chuyển Status thành `REVIEW`.
7. Sau khi review/integration ổn mới merge vào `main` và chuyển `DONE`.

### Quy ước branch

```text
t<task-id>-<short-name>
```

Ví dụ:

```text
t01-crawler-baseline
t03-search-ui
t04-lucene-backend
t06-vietnamese-analysis
```

Không push trực tiếp lên `main` khi đang phát triển feature.

## Stack đã chốt

- Frontend: React + Vite
- Backend: Java + Spring Boot
- Search: Apache Lucene
- Document extraction: Apache Tika
- Crawler: Python + Scrapy
- Shared DB: PostgreSQL cloud (ưu tiên Supabase)
- Vietnamese analysis: benchmark/chốt từ VnCoreNLP, Underthesea, PyVi

## Definition of Done — Setup phase

Setup phase kết thúc khi chạy được vertical slice:

```text
HUST → Crawl → PostgreSQL → Lucene → API → React
```

Người dùng nhập một query thật trên UI và nhận kết quả từ dữ liệu HUST thật.

## Secrets và cấu hình

Copy `.env.example` thành `.env` trên máy cá nhân và điền credential thật. File `.env` bị ignore và **không được commit**.
