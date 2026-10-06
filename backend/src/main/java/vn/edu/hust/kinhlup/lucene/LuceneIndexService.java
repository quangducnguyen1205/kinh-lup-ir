package vn.edu.hust.kinhlup.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import vn.edu.hust.kinhlup.document.SourceDocument;
import vn.edu.hust.kinhlup.text.TextNormalizer;

/** Ghi chỉ mục đảo trên ổ đĩa; có thể dựng lại index từ dữ liệu nguồn trong PostgreSQL. */
@Service
public class LuceneIndexService {

    private final Path indexPath;

    /** Spring truyền đường dẫn index từ cấu hình; đường dẫn tương đối tính từ thư mục chạy. */
    public LuceneIndexService(@Value("${kinhlup.lucene.index-dir:./data/lucene-index}") String indexPath) {
        this.indexPath = Path.of(indexPath);
    }

    /** Cho service tìm kiếm và test biết thư mục chứa index. */
    public Path indexPath() {
        return indexPath;
    }

    /** Xóa toàn bộ tài liệu trong index và commit ngay; dùng để chuẩn bị corpus test. */
    public void clearIndex() throws IOException {
        try (Analyzer analyzer = new StandardAnalyzer();
             Directory directory = FSDirectory.open(indexPath);
             IndexWriter writer = new IndexWriter(directory, writerConfig(analyzer))) {
            writer.deleteAll();
            writer.commit();
        }
    }

    /**
     * Cấu hình analyzer và cách lưu thông tin chuẩn hóa độ dài phục vụ chấm điểm.
     * Chưa tính score ở bước lập index; score được tính khi có truy vấn.
     */
    private IndexWriterConfig writerConfig(Analyzer analyzer) {
        return new IndexWriterConfig(analyzer).setSimilarity(new ClassicSimilarity());
    }

    /**
     * Dựng lại toàn bộ index từ danh sách tài liệu ACTIVE do ReindexService lấy từ DB.
     * Reader vẫn đọc bản commit cũ cho đến khi toàn bộ tài liệu mới được ghi thành công.
     */
    public void rebuild(List<SourceDocument> documents, TextNormalizer normalizer) throws IOException {
        try (Analyzer analyzer = new StandardAnalyzer();
             Directory directory = FSDirectory.open(indexPath)) {
            IndexWriter writer = new IndexWriter(directory, writerConfig(analyzer));
            try {
                // Xóa trong phiên ghi hiện tại; chưa công bố index rỗng cho reader.
                writer.deleteAll();
                for (SourceDocument source : documents) {
                    Document document = new Document();
                    // StringField giữ nguyên ID thành một token, không đưa qua analyzer.
                    document.add(new StringField("docId", source.id().toString(), Field.Store.YES));
                    String title = source.title() == null ? "" : source.title();
                    // TextField phân tích bản đã normalize để tìm; StoredField giữ tiêu đề gốc để hiển thị.
                    document.add(new TextField("title", normalizer.normalize(title), Field.Store.NO));
                    document.add(new StoredField("title", title));
                    // Store.YES lưu thêm nội dung để đọc lại; rawText giữ riêng văn bản trước normalize.
                    document.add(new TextField("content", normalizer.normalize(source.rawText()), Field.Store.YES));
                    document.add(new StoredField("rawText", source.rawText()));
                    document.add(new StringField("url", source.url(), Field.Store.YES));
                    document.add(new StringField("domain", source.domain(), Field.Store.YES));
                    if (source.publishedAt() != null) {
                        document.add(new StoredField("publishedAt", source.publishedAt().toString()));
                    }
                    // Thay tài liệu cùng docId nếu đã có, tránh tạo bản trùng khi ghi lại.
                    writer.updateDocument(new Term("docId", source.id().toString()), document);
                }
                // Công bố đồng thời toàn bộ index mới sau khi vòng lặp thành công.
                writer.commit();
            } catch (IOException | RuntimeException failure) {
                try {
                    // Hủy phiên ghi lỗi và giữ bản index đã commit trước đó.
                    writer.rollback();
                } catch (IOException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            } finally {
                writer.close();
            }
        }
    }

    /**
     * Thêm/thay một tài liệu, dùng cho luồng đọc file test qua DocumentLoader.
     * Caller phải tiền xử lý content trước; hàm này không tự gọi TextNormalizer.
     * title/content đi qua analyzer, docId/url/domain được giữ nguyên thành từng token.
     * Đóng writer bình thường ở cuối try sẽ commit thay đổi của lần ghi này.
     */
    public void indexDocument(
            String docId,
            String title,
            String content,
            String url,
            String domain
    ) throws IOException {

        try (Analyzer analyzer = new StandardAnalyzer();
             Directory directory = FSDirectory.open(indexPath);
             IndexWriter writer = new IndexWriter(directory, writerConfig(analyzer))) {

            Document document = new Document();

            document.add(
                    new StringField(
                            "docId",
                            docId,
                            Field.Store.YES
                    )
            );

            document.add(
                    new TextField(
                            "title",
                            title,
                            Field.Store.YES
                    )
            );

            document.add(
                    new TextField(
                            "content",
                            content,
                            Field.Store.YES
                    )
            );

            document.add(
                    new StringField(
                            "url",
                            url,
                            Field.Store.YES
                    )
            );

            document.add(
                    new StringField(
                            "domain",
                            domain,
                            Field.Store.YES
                    )
            );

            // updateDocument thay tài liệu có cùng ID thay vì thêm một bản sao.
            writer.updateDocument(new Term("docId", docId), document);
        }
    }
}
