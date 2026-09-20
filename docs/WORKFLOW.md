# Quy trình làm việc chung

## Vòng đời của một task

```text
TODO → DOING → REVIEW → DONE
           ↘ BLOCKED
```

- `TODO`: chưa bắt đầu.
- `DOING`: người phụ trách đang thực hiện.
- `REVIEW`: đã có deliverable, chờ kiểm tra/tích hợp.
- `DONE`: đạt Acceptance Criteria và đã tích hợp ổn.
- `BLOCKED`: chưa thể tiếp tục do phụ thuộc hoặc blocker cụ thể; phải ghi rõ lý do ở cột Notes.

Task và trạng thái được quản lý ở tab `TASKS` của Google Sheet, không quản lý bằng GitHub Issues.

## Branch

Mỗi task tạo một branch từ `main`:

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

Không dùng branch cá nhân sống lâu. Không phát triển feature trực tiếp trên `main`.

## Commit

Commit ngắn và mô tả đúng thay đổi. Ví dụ:

```text
crawler: bóc tách nội dung bài viết HUST
backend: thêm Lucene index builder
frontend: thêm danh sách kết quả tìm kiếm
nlp: thêm dữ liệu benchmark tokenizer
evaluation: thêm bộ truy vấn đánh giá
```

Không bắt buộc conventional commits ở giai đoạn hiện tại.

## Contract chung

- Frontend/Backend đọc tab `API CONTRACT`.
- Crawler/NLP/Backend đọc tab `DATA CONTRACT`.
- Thông tin kết nối nằm trong tab `CONNECTION`.
- Thay đổi contract ảnh hưởng module khác phải cập nhật `DECISIONS` và Sheet trước khi coi task là `DONE`.

## Trước khi chuyển sang REVIEW

Người làm task phải tự kiểm tra tối thiểu:

1. Chạy được module hoặc test liên quan.
2. Không commit secret hay file runtime lớn.
3. Không phá CI hiện có.
4. Cập nhật Deliverable/Link trên Sheet.
5. Ghi blocker hoặc giới hạn còn lại vào Notes nếu có.
