# Bàn giao công việc — Step 6

Tài liệu này là điểm bắt đầu cho từng thành viên sau khi phần setup chung đã hoàn tất.

## Trước khi làm

Mỗi người cần:

1. Mở Google Sheet chung.
2. Đọc tab `Bài tập lớn - Kính Lúp`.
3. Vào `TASKS`, tìm task có Owner là mình.
4. Đọc `Acceptance Criteria` trước khi code.
5. Clone repo và chạy skeleton module theo `docs/RUN_MODULES.md`.
6. Tạo branch đúng Task ID.
7. Chuyển task sang `DOING`.

Google Sheet:

https://docs.google.com/spreadsheets/d/1Vq9XXLyeHBuoYnM28nu2P6et_BKhn4QEjnQ3sMuiOQE/edit

## Đức — Crawler / Lead

### T01 — Crawler HUST baseline

Branch:

```text
t01-crawler-baseline
```

Đọc trước:

- `crawler/README.md`
- `docs/DB_SCHEMA.md`
- tab `DATA CONTRACT`
- tab `CONNECTION`

Mục tiêu: crawl HUST/subdomain, extract đúng contract và ghi dữ liệu vào PostgreSQL chung.

### T02 — Detect new / updated content

Branch:

```text
t02-content-update
```

Mục tiêu: phân biệt URL mới / nội dung đổi / nội dung không đổi theo `content_hash`.

## Chính — Frontend

### T03 — Search UI baseline

Branch:

```text
t03-search-ui
```

Đọc trước:

- `frontend/README.md`
- tab `API CONTRACT`

Mục tiêu: search box, loading/error state, danh sách kết quả, pagination và document detail.

Có thể dùng mock response đúng contract khi backend chưa xong.

## Sơn — Backend / Search

### T04 — Backend API + Lucene baseline

Branch:

```text
t04-lucene-backend
```

Đọc trước:

- `backend/README.md`
- `docs/DB_SCHEMA.md`
- tab `API CONTRACT`
- tab `DATA CONTRACT`
- tab `CONNECTION`

Mục tiêu: đọc documents từ PostgreSQL, build/rebuild Lucene index và triển khai search API.

### T05 — Tika extraction baseline

Branch:

```text
t05-tika-extraction
```

Mục tiêu: dùng Apache Tika extract text/metadata từ PDF và mở đường cho DOCX/PPTX.

## Nam — Phân tích tiếng Việt

### T06 — Vietnamese text analysis study

Branch:

```text
t06-vietnamese-analysis
```

Đọc trước:

- `research/vietnamese-analysis/README.md`
- tab `DATA CONTRACT`

Mục tiêu: benchmark VnCoreNLP / Underthesea / PyVi, chốt pipeline normalize + tokenize và tạo test cases.

### T07 — Integrate Vietnamese analyzer

Làm cùng Sơn sau khi T04 + T06 đủ điều kiện.

## Quốc Anh — Evaluation / QA

### T12 — Bộ truy vấn đánh giá + QA baseline

Branch:

```text
t12-evaluation-qa
```

Đọc trước:

- `research/evaluation/README.md`
- tab `TASKS`

Task này **không nằm trên critical path**, nhưng tạo tài sản rất hữu ích cho việc đánh giá search về sau.

Mục tiêu chính:

- tạo ít nhất 30 truy vấn thực tế về HUST;
- mỗi truy vấn có ít nhất 1 URL HUST liên quan được kiểm tra thủ công;
- phủ ít nhất 5 nhóm nhu cầu tìm kiếm;
- có các edge case như không dấu, viết tắt, cụm từ dài;
- ghi dữ liệu theo format `research/evaluation/queries.csv`.

Sau khi vertical slice chạy được, owner T12 tiếp tục dùng bộ truy vấn này để smoke-test chất lượng kết quả.

## Khi nào được chuyển REVIEW?

Chỉ chuyển `REVIEW` khi:

- đạt Acceptance Criteria trên Sheet;
- branch chạy/test được;
- không phá CI;
- đã cập nhật Deliverable/Link;
- Notes ghi rõ giới hạn hoặc blocker còn lại.

## Khi nào được merge?

Không merge trực tiếp theo cảm tính. Chỉ merge khi:

1. task đã self-check;
2. output đúng contract chung;
3. không có conflict chưa giải quyết;
4. CI pass;
5. lead/reviewer xác nhận đủ điều kiện.
