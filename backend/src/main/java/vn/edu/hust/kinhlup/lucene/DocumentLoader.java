package vn.edu.hust.kinhlup.lucene;

import org.springframework.stereotype.Component;
import vn.edu.hust.kinhlup.text.TextNormalizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Nạp file văn bản vào Lucene cho corpus test/demo; luồng database dùng ReindexService. */
@Component
public class DocumentLoader {

    private final TextNormalizer textNormalizer;
    private final LuceneIndexService luceneIndexService;

    public DocumentLoader(
            TextNormalizer textNormalizer,
            LuceneIndexService luceneIndexService
    ) {
        this.textNormalizer = textNormalizer;
        this.luceneIndexService = luceneIndexService;
    }

    /** Đọc file → tiền xử lý nội dung → ghi tài liệu với ID do caller cung cấp. */
    public void loadDocument(
            String docId,
            Path filePath
    ) throws IOException {

        String rawText = Files.readString(filePath);

        String normalizedText =
                textNormalizer.normalize(rawText);

        // File corpus không có metadata riêng: title và URL để rỗng, domain dùng giá trị mẫu.
        luceneIndexService.indexDocument(
                docId,
                "",
                normalizedText,
                "",
                "hust.edu.vn"
        );
    }
}
