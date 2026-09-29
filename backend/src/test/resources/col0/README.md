# Fixture đã preprocessing cho bài test Lucene

`doc1`–`doc9` và `d10` là 10 tài liệu của corpus `col0`; `d10` được index với ID
`doc10`. Nội dung được chuẩn bị sẵn dưới dạng chữ thường, Unicode NFC, dấu câu
thay bằng khoảng trắng và các cụm mục tiêu được ghép thành token:

- `sinh viên` → `sinh_viên`
- `nghiên cứu` → `nghiên_cứu`
- `khoa học` → `khoa_học`
- `cựu sinh viên` → `cựu sinh_viên`

Đây là dữ liệu fixture cố định, không phải thuật toán preprocessing tiếng Việt.
Các từ khác được giữ ở mức phân tách bằng khoảng trắng; không giả định đã tách
từ tổng quát. Nội dung gốc của `doc6` dùng Unicode tổ hợp cũng đã được chuẩn bị
thành các token NFC.

`LuceneTestCorpus.stubPreprocessing` cấu hình mock duy nhất là `TextNormalizer`:
trả nguyên nội dung fixture và ánh xạ chính xác query
`sinh viên nghiên cứu khoa hoc` thành `sinh_viên nghiên_cứu khoa_hoc`.
Mock chỉ ghép các cụm trong query, giữ nguyên `hoc` không dấu theo đề bài.
Corpus giữ `khoa_học` theo nội dung tài liệu. `WhitespaceAnalyzer` không sửa
dấu nên `khoa_hoc` không khớp `khoa_học`; OR vẫn cho phép tài liệu khớp qua
`sinh_viên` hoặc `nghiên_cứu`.
Mock chỉ được tiêm vào hai lớp test qua `@MockitoBean`.

Chạy `mvn clean test` tại thư mục `backend`. Test rebuild 10 tài liệu qua các
service Lucene thật, dùng `WhitespaceAnalyzer`, `ClassicSimilarity`, OR mặc
định của `QueryParser` và in nguyên giá trị float lấy từ `ScoreDoc.score`.
Tập tài liệu khớp được kiểm tra từ token của fixture; score và thứ tự ranking
không được hard-code. Index test còn rebuild hai lần để kiểm tra không trùng
tài liệu. Các test dùng thư mục tạm riêng (`@TempDir`), không chạm index runtime.
