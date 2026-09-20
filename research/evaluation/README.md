# Evaluation / QA

Module này phục vụ T12 — **Bộ truy vấn đánh giá + QA baseline**.

Mục đích là chuẩn bị một tập dữ liệu nhỏ nhưng có cấu trúc để về sau nhóm có thể đánh giá chất lượng search thay vì chỉ nhìn kết quả bằng cảm tính.

## Deliverable chính

File:

```text
research/evaluation/queries.csv
```

Mỗi dòng là một cặp query–document relevance đã được kiểm tra thủ công.

Các cột:

- `query_id`: mã truy vấn, ví dụ `Q001`.
- `query`: câu truy vấn người dùng có thể gõ.
- `category`: nhóm nhu cầu.
- `relevant_url`: URL HUST được xem là liên quan.
- `why_relevant`: lý do ngắn gọn.
- `edge_case`: loại edge case nếu có.
- `status`: `VERIFIED` hoặc `TODO`.

Nếu một query có nhiều URL liên quan, lặp lại `query_id` trên nhiều dòng.

## Yêu cầu tối thiểu của T12

- ít nhất **30 query_id khác nhau**;
- ít nhất **5 category**;
- mỗi query có ít nhất **1 relevant_url** thuộc `hust.edu.vn` hoặc subdomain;
- URL phải được mở và kiểm tra thật, không tự bịa;
- có ít nhất **10 query edge case** tổng cộng.

Gợi ý category:

- tuyển sinh / học vụ;
- học bổng / sinh viên;
- đơn vị / phòng ban / cán bộ;
- nghiên cứu / sự kiện / tin tức;
- quy chế / biểu mẫu / PDF.

Gợi ý edge case:

- `NO_ACCENT`: truy vấn không dấu;
- `ABBREVIATION`: viết tắt như HUST/BKHN;
- `LONG_QUERY`: cụm từ dài;
- `ENTITY`: tên đơn vị/người;
- `DOCUMENT`: tìm PDF/quy chế/biểu mẫu.

## Cách làm

1. Tạo branch `t12-evaluation-qa`.
2. Tìm các nhu cầu tìm kiếm thực tế liên quan đến HUST.
3. Mở URL HUST tương ứng và kiểm tra mức liên quan.
4. Ghi vào `queries.csv`.
5. Cập nhật README này nếu phát hiện convention mới.
6. Khi đủ Acceptance Criteria, cập nhật Deliverable/Link trên Google Sheet rồi chuyển T12 sang `REVIEW`.

## Sau khi hệ thống search chạy được

Tập dữ liệu này có thể được dùng để:

- smoke-test top-k;
- tính Precision@K / Recall khi môn học đi tới phần evaluation;
- so sánh analyzer/ranking trước và sau khi thay đổi;
- tạo regression test thủ công cho search.
