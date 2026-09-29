package vn.edu.hust.kinhlup;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import vn.edu.hust.kinhlup.document.DocumentRepository;
import vn.edu.hust.kinhlup.lucene.LuceneIndexService;
import vn.edu.hust.kinhlup.lucene.ReindexService;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseIndexTest {
    @TempDir Path directory;
    JdbcTemplate jdbc;
    DocumentRepository repository;
    LuceneIndexService index;
    ReindexService reindex;

    @BeforeEach
    void setup() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("""
                CREATE TABLE documents (
                  id UUID PRIMARY KEY, url TEXT, domain TEXT, title TEXT, content_type TEXT,
                  raw_text TEXT, published_at TIMESTAMP WITH TIME ZONE,
                  last_crawled_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                  content_hash TEXT, updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                  status TEXT DEFAULT 'ACTIVE', index_status TEXT DEFAULT 'PENDING',
                  last_indexed_at TIMESTAMP WITH TIME ZONE)
                """);
        repository = new DocumentRepository(jdbc);
        index = new LuceneIndexService(directory.toString());
        reindex = new ReindexService(repository, index, text -> text);
    }

    UUID insert(String status, String title, String text) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO documents (id, url, domain, title, content_type, raw_text, content_hash, status)
                VALUES (?, ?, 'hust.edu.vn', ?, 'text/html', ?, 'hash-v1', ?)
                """, id, "https://hust.edu.vn/" + id, title, text, status);
        return id;
    }

    @Test
    void rebuildReadsActiveRowsMapsNullableMetadataAndAcknowledgesWithoutChangingSourceTime() throws Exception {
        UUID active = insert("ACTIVE", null, "sinh viên HUST");
        UUID deleted = insert("DELETED", "deleted", "hidden");
        insert("FAILED", "failed", "hidden");
        var before = repository.findActiveById(active).orElseThrow();
        assertEquals(1, reindex.rebuild());
        assertEquals(1, reindex.rebuild());
        assertEquals(before.updatedAt(), repository.findActiveById(active).orElseThrow().updatedAt());
        assertNull(before.publishedAt());
        assertTrue(repository.findActiveById(deleted).isEmpty());
        assertEquals("INDEXED", jdbc.queryForObject("SELECT index_status FROM documents WHERE id = ?", String.class, active));
        assertNotNull(jdbc.queryForObject("SELECT last_indexed_at FROM documents WHERE id = ?", java.sql.Timestamp.class, active));
        assertEquals("PENDING", jdbc.queryForObject("SELECT index_status FROM documents WHERE id = ?", String.class, deleted));
        try (var store = FSDirectory.open(directory); var reader = DirectoryReader.open(store)) {
            assertEquals(1, reader.numDocs());
            var hit = new IndexSearcher(reader).search(new MatchAllDocsQuery(), 1).scoreDocs[0];
            var doc = reader.storedFields().document(hit.doc);
            assertEquals(active.toString(), doc.get("docId"));
            assertEquals("", doc.get("title"));
            assertEquals("sinh viên HUST", doc.get("rawText"));
        }
    }

    @Test
    void rebuildRemovesDeletedDocumentsAndSupportsEmptyDatabase() throws Exception {
        UUID id = insert("ACTIVE", "title", "body");
        reindex.rebuild();
        jdbc.update("UPDATE documents SET status = 'DELETED' WHERE id = ?", id);
        assertEquals(0, reindex.rebuild());
        try (var store = FSDirectory.open(directory); var reader = DirectoryReader.open(store)) {
            assertEquals(0, reader.numDocs());
        }
    }

    @Test
    void acknowledgementDoesNotMarkConcurrentlyChangedOrDeletedRowsIndexed() {
        UUID changed = insert("ACTIVE", "one", "body");
        UUID deleted = insert("ACTIVE", "two", "body");
        var snapshot = repository.findActive();
        jdbc.update("UPDATE documents SET content_hash = 'hash-v2' WHERE id = ?", changed);
        jdbc.update("UPDATE documents SET status = 'DELETED' WHERE id = ?", deleted);
        repository.markIndexed(snapshot);
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM documents WHERE index_status = 'PENDING'", Integer.class));
    }

    @Test
    void failedRebuildPreservesPreviousCommittedIndexAndDoesNotAcknowledgeNewRows() throws Exception {
        insert("ACTIVE", "old", "original");
        reindex.rebuild();
        UUID added = insert("ACTIVE", "new", "reject");
        var failing = new ReindexService(repository, index, text -> {
            if (text.equals("reject")) throw new IllegalStateException("normalization failed");
            return text;
        });
        assertThrows(IllegalStateException.class, failing::rebuild);
        try (var store = FSDirectory.open(directory); var reader = DirectoryReader.open(store)) {
            assertEquals(1, reader.numDocs());
        }
        assertEquals("PENDING", jdbc.queryForObject("SELECT index_status FROM documents WHERE id = ?", String.class, added));
        assertEquals(2, reindex.rebuild()); // Rollback also released the writer lock.
    }
}
