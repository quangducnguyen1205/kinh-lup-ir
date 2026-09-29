package vn.edu.hust.kinhlup.lucene;

import org.springframework.stereotype.Component;
import vn.edu.hust.kinhlup.text.TextNormalizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    public void loadDocument(
            String docId,
            Path filePath
    ) throws IOException {

        String rawText = Files.readString(filePath);

        String normalizedText =
                textNormalizer.normalize(rawText);

        luceneIndexService.indexDocument(
                docId,
                "",
                normalizedText,
                "",
                "hust.edu.vn"
        );
    }
}