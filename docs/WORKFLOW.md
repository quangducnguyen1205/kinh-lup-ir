# Workflow chung

## Một task đi qua các trạng thái

```text
TODO → DOING → REVIEW → DONE
           ↘ BLOCKED
```

- `TODO`: chưa bắt đầu.
- `DOING`: owner đang thực hiện.
- `REVIEW`: đã có deliverable, chờ kiểm tra/tích hợp.
- `DONE`: đạt Acceptance Criteria và đã tích hợp ổn.
- `BLOCKED`: không thể tiếp tục vì dependency hoặc blocker cụ thể; phải ghi lý do ở Notes.

## Branch

Mỗi task tạo một branch từ `main`:

```text
t<task-id>-<short-name>
```

Không dùng branch cá nhân sống lâu. Không phát triển trực tiếp trên `main`.

## Commit

Commit ngắn, mô tả đúng thay đổi. Khuyến nghị:

```text
crawler: extract HUST article content
backend: add Lucene index builder
frontend: add search result list
nlp: add tokenizer benchmark fixtures
```

Không cần ép conventional commits ở giai đoạn này.

## Contract

- FE/BE đọc `API CONTRACT` trên Google Sheet.
- Crawler/NLP/BE đọc `DATA CONTRACT` trên Google Sheet.
- Thay đổi contract ảnh hưởng module khác phải cập nhật `DECISIONS` và Sheet trước khi coi task là DONE.
