# Kính Lúp — Bài tập lớn Tìm kiếm thông tin (IT4863)

Repository này dùng để xây dựng một **công cụ tìm kiếm thu nhỏ cho hệ sinh thái `hust.edu.vn`**. Nhóm ưu tiên dựng luồng end-to-end chạy được trước; sau đó các kiến thức IR học trên lớp sẽ được tích hợp dần vào phân tích văn bản, lập chỉ mục, xử lý truy vấn, xếp hạng và đánh giá chất lượng.

## Luồng hệ thống

```text
HUST / *.hust.edu.vn
        ↓
     Scrapy
        ↓
   PostgreSQL
        ↓
Phân tích tiếng Việt + Apache Tika
        ↓
  Apache Lucene
        ↓
 Spring Boot API
        ↓
   React + Vite
```

Cơ chế cập nhật chính là crawl/rà soát **định kỳ**. Cập nhật thủ công (`POST /api/admin/crawl`) là chức năng bonus/admin và phải dùng lại cùng pipeline crawl-update.

## Nơi điều phối chung

Task, API Contract, Data Contract, Connection và các quyết định kiến trúc được quản lý tại Google Sheet của nhóm:

https://docs.google.com/spreadsheets/d/16iiLZQUk4thjs51NRptEC9BiBmRKls5_om0LWwEa1Ig/edit

**Google Sheet là nguồn chuẩn cho việc giao task và contract. GitHub dùng để quản lý code. Hiện tại nhóm không dùng GitHub Issues.**

## Cấu trúc repository

```text
kinh-lup-ir/
├── frontend/                    # React + Vite — giao diện tìm kiếm
├── backend/                     # Java + Spring Boot + Lucene + Tika
├── crawler/                     # Python + Scrapy
├── research/
│   ├── vietnamese-analysis/     # Nghiên cứu xử lý tiếng Việt
│   └── evaluation/              # Bộ truy vấn đánh giá / QA
├── docs/                        # Tài liệu dùng chung
├── supabase/
│   └── migrations/              # Migration PostgreSQL
├── data/                        # Chỉ giữ placeholder; không commit dữ liệu/index thật
├── .env.example
├── .editorconfig
├── .gitignore
└── README.md
```

## Quy trình làm một task

1. Mở tab `TASKS` trong Google Sheet, tìm task có Owner là mình và đọc kỹ **Acceptance Criteria**.
2. Chuyển Status thành `DOING`.
3. Đồng bộ `main` và tạo branch mới theo Task ID:

```bash
git checkout main
git pull
git checkout -b t01-crawler-baseline
```

4. Code trong module tương ứng. Không commit secret, `.env`, database dump, crawl output lớn hoặc Lucene index.
5. Trước khi bàn giao, tự chạy/test phần mình phụ trách và cập nhật `Deliverable / Link` + `Notes` trên Sheet.
6. Chuyển Status thành `REVIEW`.
7. Chỉ merge vào `main` và chuyển `DONE` sau khi review/tích hợp ổn.

### Quy ước branch

```text
t<task-id>-<ten-ngan>
```

Ví dụ:

```text
t01-crawler-baseline
t03-search-ui
t04-lucene-backend
t06-vietnamese-analysis
t12-evaluation-qa
```

Không phát triển feature trực tiếp trên `main`.

## Công nghệ đã chốt

- Frontend: React + Vite
- Backend: Java + Spring Boot
- Tìm kiếm: Apache Lucene
- Bóc tách tài liệu: Apache Tika
- Crawler: Python + Scrapy
- CSDL dùng chung: Supabase PostgreSQL
- Phân tích tiếng Việt: benchmark/chốt giữa VnCoreNLP, Underthesea, PyVi

## Khi nào giai đoạn setup được coi là hoàn thành?

Giai đoạn setup kết thúc khi chạy được một vertical slice:

```text
HUST → Crawl → PostgreSQL → Lucene → API → React
```

Người dùng nhập một truy vấn thật trên UI và nhận kết quả từ dữ liệu HUST thật.

## Secret và cấu hình

Copy `.env.example` thành `.env` trên máy cá nhân và điền credential thật. File `.env` đã bị ignore và **không được commit**.

Thông tin kết nối nội bộ của nhóm được tập trung tại tab `CONNECTION` trong Google Sheet.
