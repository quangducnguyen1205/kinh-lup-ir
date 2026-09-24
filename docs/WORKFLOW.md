# Quy trình làm việc chung

## Vòng đời của một task

```text
TODO → DOING → REVIEW → DONE
           ↘ BLOCKED
```

- `TODO`: chưa bắt đầu.
- `DOING`: người phụ trách đang thực hiện.
- `REVIEW`: đã có deliverable và **một Pull Request của task** để review/tích hợp.
- `DONE`: đạt Acceptance Criteria, CI/review ổn và PR đã merge.
- `BLOCKED`: chưa thể tiếp tục do phụ thuộc hoặc blocker cụ thể; phải ghi rõ lý do ở cột Notes.

Task và trạng thái được quản lý ở tab `TASKS` của Google Sheet, không quản lý bằng GitHub Issues.

## Nguyên tắc Git: 1 task = 1 branch = 1 PR

Mỗi task chỉ có **một branch riêng** được tạo từ `main`:

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

Toàn bộ commit trong quá trình làm task được đẩy lên **chính branch đó**. Không tạo thêm branch phụ cho cùng một task.

Khi task đủ điều kiện review, tạo **đúng một Pull Request** từ branch task vào `main`. Nếu review yêu cầu sửa, tiếp tục commit/push lên branch cũ; PR tự cập nhật. Không mở PR thứ hai cho cùng task.

Khi merge task, ưu tiên **Squash and merge** để `main` có một commit gọn tương ứng với một task. **Giữ lại branch task trên remote sau khi merge** để lưu toàn bộ lịch sử commit quá trình của task.

## `main`

`main` là nhánh tích hợp ổn định và luôn nên ở trạng thái có thể clone/build.

Feature/task của thành viên **không code trực tiếp trên `main`**.

Ngoại lệ: các cập nhật chung không phải feature task như tài liệu điều phối, cấu hình workspace, sửa link, phân công hoặc housekeeping repository có thể được **Đức (lead) commit trực tiếp lên `main`**. Những thay đổi này không cần tạo branch/PR riêng chỉ để làm repo rối hơn.

## Commit

Commit trên branch task là lịch sử quá trình thực hiện task, nên commit ngắn và mô tả đúng thay đổi. Ví dụ:

```text
crawler: bóc tách nội dung bài viết HUST
crawler: lưu document vào PostgreSQL
backend: thêm Lucene index builder
backend: thêm search endpoint
frontend: thêm danh sách kết quả tìm kiếm
nlp: thêm dữ liệu benchmark tokenizer
evaluation: thêm bộ truy vấn đánh giá
```

Không bắt buộc conventional commits ở giai đoạn hiện tại, nhưng tránh commit kiểu `update`, `fix`, `abc` không nói rõ nội dung.

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
6. Push toàn bộ commit lên branch của task.
7. Mở **một PR duy nhất** từ branch task vào `main`.

Sau khi review/CI đạt yêu cầu, PR được squash-merge và task chuyển `DONE`. Branch task **được giữ lại** để lưu vết chi tiết các commit của quá trình thực hiện.
