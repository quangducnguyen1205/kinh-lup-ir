package vn.edu.hust.kinhlup.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.springframework.stereotype.Service;
import vn.edu.hust.kinhlup.text.TextNormalizer;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Đọc index Lucene, tìm tài liệu khớp và đóng gói kết quả theo API contract. */
@Service
public class LuceneSearchService {
    private final TextNormalizer textNormalizer;
    private final LuceneIndexService indexService;

    public LuceneSearchService(TextNormalizer textNormalizer, LuceneIndexService indexService) {
        this.textNormalizer = textNormalizer;
        this.indexService = indexService;
    }

    /** Hàm tiện dụng cho test/demo: lấy tối đa 10 kết quả đầu bằng TF-IDF. */
    public List<SearchResult> search(String inputQuery) throws IOException {
        return search(inputQuery, 0, 10).results();
    }

    /** API gọi hàm này để tìm bằng TF-IDF; page bắt đầu từ 0. */
    public SearchPage search(String inputQuery, int page, int size) throws IOException {
        return search(inputQuery, page, size, new ClassicSimilarity());
    }

    /**
     * Tìm bằng thuật toán được truyền vào: ClassicSimilarity (TF-IDF) hoặc BM25Similarity.
     * Test dùng hàm này để so sánh; API hiện chưa nhận tham số chọn thuật toán.
     */
    public SearchPage search(String inputQuery, int page, int size, Similarity similarity) throws IOException {
        // Kiểm tra đầu vào và giới hạn cửa sổ tìm kiếm để tránh yêu cầu quá lớn.
        Objects.requireNonNull(similarity, "similarity");
        if (inputQuery == null || inputQuery.length() > 1000) {
            throw new IllegalArgumentException("q must contain at most 1000 characters");
        }
        if (page < 0 || size < 1 || size > 100 || ((long) page + 1) * size > 10000) {
            throw new IllegalArgumentException("page >= 0, size between 1 and 100, search window <= 10000 required");
        }
        var empty = new SearchPage(inputQuery, 0, page, size, List.of());
        if (inputQuery.isBlank()) return empty;
        // Query phải dùng cùng chiến lược tiền xử lý với tài liệu lúc lập index.
        String normalizedQuery = textNormalizer.normalize(inputQuery);
        // Analyzer tách token, chuyển chữ thường, giữ dấu tiếng Việt và token có gạch dưới.
        // FSDirectory mở index trên ổ đĩa; tìm kiếm không cần đọc lại PostgreSQL.
        try (Analyzer analyzer = new StandardAnalyzer();
             var directory = FSDirectory.open(indexService.indexPath())) {
            if (!DirectoryReader.indexExists(directory)) return empty;
            // QueryBuilder coi đầu vào là văn bản, không thực thi cú pháp truy vấn Lucene.
            // createBooleanQuery mặc định dùng OR: chỉ cần khớp một token trong field.
            QueryBuilder builder = new QueryBuilder(analyzer);
            Query content = builder.createBooleanQuery("content", normalizedQuery);
            Query title = builder.createBooleanQuery("title", normalizedQuery);
            // SHOULD kết hợp title/content bằng OR; phần điểm khớp title được nhân 2.
            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
            if (content != null) queryBuilder.add(content, BooleanClause.Occur.SHOULD);
            if (title != null) queryBuilder.add(new BoostQuery(title, 2f), BooleanClause.Occur.SHOULD);
            Query query = queryBuilder.build();
            // Reader mở bản index đã commit; searcher tìm và chấm điểm trên bản đó.
            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                searcher.setSimilarity(similarity);
                // Đếm toàn bộ tài liệu khớp để trang vượt phạm vi vẫn trả đúng total.
                long total = searcher.count(query);
                int offset = page * size;
                if (offset >= total) return new SearchPage(inputQuery, total, page, size, List.of());
                // Lấy top (offset + size) theo score giảm dần, rồi bỏ các trang trước.
                TopDocs topDocs = searcher.search(query, offset + size);
                var storedFields = searcher.storedFields();
                List<SearchResult> results = new ArrayList<>();
                for (int i = offset; i < topDocs.scoreDocs.length; i++) {
                    ScoreDoc hit = topDocs.scoreDocs[i];
                    // hit.doc là ID nội bộ Lucene; docId trong document mới là ID của dữ liệu nguồn.
                    var document = storedFields.document(hit.doc);
                    // Ưu tiên văn bản gốc; corpus test chỉ lưu content nên cần giá trị dự phòng.
                    String rawText = document.get("rawText");
                    if (rawText == null) rawText = document.get("content");
                    // Snippet lấy phần đầu nội dung, chưa chọn đoạn chứa từ khóa hay tô đậm.
                    String snippet = rawText.replaceAll("\\s+", " ").strip();
                    if (snippet.length() > 240) snippet = snippet.substring(0, 240) + "…";
                    String published = document.get("publishedAt");
                    results.add(new SearchResult(document.get("docId"), document.get("title"),
                            document.get("url"), snippet, hit.score,
                            published == null ? null : Instant.parse(published)));
                }
                return new SearchPage(inputQuery, total, page, size, results);
            }
        }
    }
}
