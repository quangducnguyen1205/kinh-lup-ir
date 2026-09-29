package vn.edu.hust.kinhlup.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
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

    /** Readers see the previous commit until every source document is indexed successfully. */
    public void rebuild(List<SourceDocument> documents, TextNormalizer normalizer) throws IOException {
        try (Analyzer analyzer = new WhitespaceAnalyzer();
             Directory directory = FSDirectory.open(indexPath)) {
            IndexWriter writer = new IndexWriter(directory, writerConfig(analyzer));
            try {
                writer.deleteAll();
                for (SourceDocument source : documents) {
                    Document document = new Document();
                    document.add(new StringField("docId", source.id().toString(), Field.Store.YES));
                    String title = source.title() == null ? "" : source.title();
                    document.add(new TextField("title", normalizer.normalize(title), Field.Store.NO));
                    document.add(new StoredField("title", title));
                    document.add(new TextField("content", normalizer.normalize(source.rawText()), Field.Store.YES));
                    document.add(new StoredField("rawText", source.rawText()));
                    document.add(new StringField("url", source.url(), Field.Store.YES));
                    document.add(new StringField("domain", source.domain(), Field.Store.YES));
                    if (source.publishedAt() != null) {
                        document.add(new StoredField("publishedAt", source.publishedAt().toString()));
                    }
                    writer.updateDocument(new Term("docId", source.id().toString()), document);
                }
                writer.commit();
            } catch (IOException | RuntimeException failure) {
                try {
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
