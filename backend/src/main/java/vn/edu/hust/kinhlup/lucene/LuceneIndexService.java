package vn.edu.hust.kinhlup.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
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

@Service
public class LuceneIndexService {

    private final Path indexPath;

    public LuceneIndexService(@Value("${kinhlup.lucene.index-dir:./data/lucene-index}") String indexPath) {
        this.indexPath = Path.of(indexPath);
    }

    public Path indexPath() {
        return indexPath;
    }

    public void clearIndex() throws IOException {
        try (Analyzer analyzer = new WhitespaceAnalyzer();
             Directory directory = FSDirectory.open(indexPath);
             IndexWriter writer = new IndexWriter(directory, writerConfig(analyzer))) {
            writer.deleteAll();
            writer.commit();
        }
    }

    private IndexWriterConfig writerConfig(Analyzer analyzer) {
        return new IndexWriterConfig(analyzer).setSimilarity(new ClassicSimilarity());
    }

    /** Content must already be the output of TextNormalizer. */
    public void indexDocument(
            String docId,
            String title,
            String content,
            String url,
            String domain
    ) throws IOException {

        try (Analyzer analyzer = new WhitespaceAnalyzer();
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

            writer.updateDocument(new Term("docId", docId), document);
        }
    }
}
