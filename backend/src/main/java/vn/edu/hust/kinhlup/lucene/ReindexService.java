package vn.edu.hust.kinhlup.lucene;

import org.springframework.stereotype.Service;
import vn.edu.hust.kinhlup.document.DocumentRepository;
import vn.edu.hust.kinhlup.text.TextNormalizer;

import java.io.IOException;

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

    public synchronized int rebuild() throws IOException {
        var snapshot = documents.findActive();
        index.rebuild(snapshot, normalizer);
        // Only acknowledge after the Lucene commit has succeeded.
        documents.markIndexed(snapshot);
        return snapshot.size();
    }
}
