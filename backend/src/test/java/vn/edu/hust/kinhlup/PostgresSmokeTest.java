package vn.edu.hust.kinhlup;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import vn.edu.hust.kinhlup.api.DocumentController;
import vn.edu.hust.kinhlup.document.DocumentRepository;
import vn.edu.hust.kinhlup.lucene.LuceneIndexService;
import vn.edu.hust.kinhlup.lucene.LuceneSearchService;
import vn.edu.hust.kinhlup.text.TextNormalizer;

import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Optional real PostgreSQL read-only test; never acknowledges or updates shared source rows. */
@SpringBootTest
@EnabledIfSystemProperty(named = "kinhlup.postgres-smoke", matches = "true")
class PostgresSmokeTest {
    @TempDir static Path indexDirectory;
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("kinhlup.lucene.index-dir", indexDirectory::toString);
    }
    @Autowired DocumentRepository documents;
    @Autowired DocumentController detail;
    @Autowired LuceneIndexService index;
    @Autowired LuceneSearchService search;
    @Autowired TextNormalizer normalizer;

    @Test
    void realSourceDocumentsCanBeIndexedSearchedAndRead() throws Exception {
        var snapshot = documents.findActive();
        assertFalse(snapshot.isEmpty(), "Seed the shared DB with the HUST crawler before this smoke test");
        index.rebuild(snapshot, normalizer);
        try (var directory = FSDirectory.open(indexDirectory); var reader = DirectoryReader.open(directory)) {
            assertEquals(snapshot.size(), reader.numDocs());
        }
        var first = snapshot.getFirst();
        assertEquals(first.rawText(), detail.detail(first.id()).text());
        var query = java.util.regex.Pattern.compile("\\p{L}{3,}").matcher(first.rawText());
        assertTrue(query.find());
        assertTrue(search.search(query.group(), 0, 10).total() > 0);
    }
}
