package vn.edu.hust.kinhlup.lucene;

import org.springframework.stereotype.Service;
import vn.edu.hust.kinhlup.document.DocumentRepository;
import vn.edu.hust.kinhlup.text.TextNormalizer;

import java.io.IOException;

/** Điều phối PostgreSQL → Lucene → ghi nhận trạng thái index trong DB. */
@Service
public class ReindexService {
    private final DocumentRepository documents;
    private final LuceneIndexService index;
    private final TextNormalizer normalizer;

    public ReindexService(DocumentRepository documents, LuceneIndexService index, TextNormalizer normalizer) {
        this.documents = documents;
        this.index = index;
        this.normalizer = normalizer;
    }

    /**
     * Dựng lại index và trả số tài liệu của lần đọc DB này.
     * synchronized ngăn hai lần rebuild chạy đồng thời trên cùng đối tượng service.
     */
    public synchronized int rebuild() throws IOException {
        var snapshot = documents.findActive();
        index.rebuild(snapshot, normalizer);
        // Chỉ đánh dấu INDEXED sau khi commit Lucene thành công.
        // Nếu bước cập nhật DB lỗi, index đã commit vẫn tồn tại; đây không phải giao dịch chung DB/Lucene.
        documents.markIndexed(snapshot);
        return snapshot.size();
    }
}
