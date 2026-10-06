# Backend / Search

Phụ trách: **Chính**. T04 cung cấp PostgreSQL → Lucene → API; T05 bổ sung bóc tách tài liệu.

## Tiến độ T04

Trước khi hoàn thiện: có Spring Boot/health, loader file text, TF-IDF Lucene,
interface `TextNormalizer` và corpus 10 tài liệu; chưa có DB repository hoặc API search/detail.

Hiện có:

- Rebuild toàn bộ tài liệu `ACTIVE` từ PostgreSQL, thay thế bản cùng ID, loại tài liệu không còn active.
- Một commit Lucene cho mỗi rebuild; lỗi đọc DB/normalize/index giữ nguyên index đã commit trước đó.
- Chỉ cập nhật `index_status`/`last_indexed_at` sau commit; kiểm tra hash + `updated_at` để không đánh dấu phiên bản crawler vừa thay đổi. Không sửa thời gian nội dung nguồn.
- `GET /api/search` và `GET /api/documents/{id}` đúng API CONTRACT v0.
- TF-IDF (`ClassicSimilarity`), OR giữa các từ, tìm cả title và content (title boost 2).
- Document/query dùng cùng `StandardAnalyzer` (lowercase và tách dấu câu, giữ token có underscore). `TextNormalizer` vẫn là điểm tích hợp T06/T07; mặc định chưa tách từ tiếng Việt.

## Tiến độ T05

Trước khi hoàn thiện: mới có dependency `tika-core`, chưa có parser/service.
Hiện có `TikaExtractionService` dùng Apache Tika 4 tự nhận dạng **PDF, DOCX, PPTX**
và trả `ExtractedDocument(text, contentType, title, metadata)`. Metadata giữ mọi
giá trị của mỗi key dưới dạng `Map<String, List<String>>`, phù hợp JSONB; kết quả
là immutable và text chưa qua NLP. Không có title thì `title = null`.

Gọi từ một service khác bằng constructor injection `TikaExtractionService`:

```java
ExtractedDocument result = extractionService.extract(Path.of("hust.pdf"));
String rawText = result.text();
String contentType = result.contentType();
String title = result.title();
Map<String, List<String>> metadata = result.metadata();
```

Có overload `extract(InputStream input, String fileName)` cho dữ liệu tải bởi crawler.
Caller đóng stream của mình; overload Path tự đóng file. Service không tự tải URL
hoặc ghi DB. Khi tích hợp ingestion, ánh xạ text → `raw_text`, title → `title`,
contentType → `content_type`, metadata → `metadata`, rồi dùng pipeline cập nhật/hash
của crawler và rebuild T04. Test `ExtractionSearchTest` kiểm tra text sau Tika có
thể đưa vào Lucene và tìm lại với tiêu đề/URL gốc cho cả ba định dạng.

Cấu hình mặc định:

```dotenv
EXTRACTION_MAX_BYTES=20971520
EXTRACTION_MAX_CHARACTERS=2000000
```

File vượt 20 MiB hoặc text vượt 2 triệu ký tự báo lỗi, không trả kết quả bị cắt ngầm.
`ExtractionException.reason()` phân biệt `EMPTY_INPUT`, `FILE_TOO_LARGE`,
`TEXT_TOO_LARGE`, `UNSUPPORTED_TYPE`, `PARSE_FAILED` (bao gồm PDF hỏng/có mật khẩu).
Lỗi I/O khi mở file/đọc stream vẫn là `IOException`.

Baseline lấy lớp text của PDF, chưa OCR PDF scan; không bóc tách file đính kèm bên
trong. Không cần cài Tesseract hay Office. Các parser PDF/Microsoft được khai báo
riêng; thêm định dạng khác bằng parser dependency + MIME vào danh sách hỗ trợ và
test fixture tương ứng. Service không mở thêm endpoint upload ngoài API CONTRACT.

## Chạy backend

Yêu cầu Java 21, Maven 3.9+. Từ thư mục `backend`:

```bash
mvn test
mvn spring-boot:run
```

Spring đọc `.env` tại gốc repo hoặc thư mục `backend`; biến môi trường có thể ghi đè.
Điền kết nối PostgreSQL của nhóm trong `.env` local, không đưa credential vào Git:

```dotenv
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=YOUR_USER
SPRING_DATASOURCE_PASSWORD=YOUR_PASSWORD
LUCENE_INDEX_DIR=./data/lucene-index
BACKEND_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:5173
REINDEX_ENABLED=true
```

Đường dẫn index tương đối tính từ thư mục chạy backend. Khi chưa cấu hình DB,
health và search trên index local vẫn chạy; thao tác cần DB trả 503 nếu không kết nối được.

Bật `REINDEX_ENABLED=true` cho rebuild thủ công ở môi trường local, rồi:

```bash
curl -X POST http://localhost:8080/api/admin/reindex
curl 'http://localhost:8080/api/search?q=hust&page=0&size=10'
curl http://localhost:8080/api/documents/UUID_FROM_SEARCH
```

Reindex trả `{ "status": "SUCCEEDED", "indexedCount": 123 }`. Endpoint admin
mặc định tắt; chỉ bật trên mạng tin cậy/local vì baseline chưa có authentication.
CORS cho frontend chỉ cho phép GET search/detail, danh sách origin phân tách bằng dấu phẩy.

## API

| Endpoint | Kết quả |
| --- | --- |
| `GET /api/health` | `{status,service}` |
| `GET /api/search?q=...&page=0&size=10` | `{query,total,page,size,results:[{id,title,url,snippet,score,publishedAt}]}` |
| `GET /api/documents/{uuid}` | `{id,url,title,contentType,text,publishedAt,lastCrawledAt}` |
| `POST /api/admin/reindex` (opt-in) | `{status,indexedCount}` |

`q` bắt buộc, tối đa 1000 ký tự; chuỗi trắng trả danh sách rỗng. Truy vấn được
xử lý như text thường, không thực thi cú pháp Lucene. `page >= 0`, `size` từ 1–100,
cửa sổ `(page + 1) * size <= 10000`; tham số sai trả 400. `total` là tổng số kết
quả chính xác; trang vượt số kết quả trả mảng rỗng và giữ total. Chưa có index trả
kết quả rỗng. Snippet lấy tối đa 240 ký tự đầu nội dung gốc, không HTML/highlight.
UUID sai trả 400, tài liệu không tồn tại/không active trả 404. Lỗi DB/index trả 503
và không trả thông tin kết nối cho client.

## Test

```bash
mvn clean test
```

- Corpus 10 tài liệu: chạy cả hai truy vấn mẫu với TF-IDF và BM25 (`k1=1.2`, `b=0.75`), in score/ranking thật; kiểm tra OR, score giảm dần và ID không trùng; mock chỉ `TextNormalizer`. API vẫn mặc định TF-IDF.
- Repository/rebuild: H2 chế độ PostgreSQL, dữ liệu nullable, lọc trạng thái, rebuild rỗng, rollback khi lỗi, update cùng ID và crawler thay đổi đồng thời.
- API: Spring context + MockMvc + Lucene thật; kiểm tra JSON contract, tìm title, phân trang, lỗi đầu vào, CORS, reindex và lỗi DB.
- Tika: file PDF/DOCX/PPTX thật tạo bằng PDFBox/POI; kiểm tra Unicode, metadata, giới hạn, PDF hỏng/mật khẩu, stream ownership, gọi đồng thời và luồng extraction → search.
- Tất cả index test nằm trong `@TempDir`, không đụng index runtime.

Smoke test PostgreSQL thật (cần `.env`/biến môi trường và dữ liệu crawler):

```bash
mvn '-Dtest=PostgresSmokeTest' '-Dkinhlup.postgres-smoke=true' test
```

Smoke test chỉ SELECT DB, rebuild vào thư mục tạm, rồi search/detail; không cập nhật
DB chung. Test này mặc định bị skip trong CI. H2 không thay thế hoàn toàn kiểm thử PostgreSQL thật.

## Phạm vi baseline

Rebuild đọc toàn bộ tập `ACTIVE` vào bộ nhớ, phù hợp dataset BTL. Khi crawler đổi dữ
liệu, gọi lại rebuild; incremental/scheduler thuộc T02/T09. Chi tiết đọc trực tiếp
DB nên có thể mới hơn index đến lần rebuild sau. Nếu cập nhật trạng thái DB thất bại
sau commit Lucene, API trả 503; chạy lại rebuild để đồng bộ. Mỗi máy có index riêng,
không dùng cờ `INDEXED` của DB chung để bỏ qua dữ liệu khi rebuild.
