package vn.edu.hust.kinhlup;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import vn.edu.hust.kinhlup.lucene.DocumentLoader;
import vn.edu.hust.kinhlup.lucene.LuceneIndexService;
import vn.edu.hust.kinhlup.text.TextNormalizer;

import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class LuceneIndexTest {

    @TempDir
    static Path temporaryIndex;

    @DynamicPropertySource
    static void indexProperties(DynamicPropertyRegistry registry) {
        registry.add("kinhlup.lucene.index-dir", temporaryIndex::toString);
    }

    @Autowired
    private DocumentLoader documentLoader;

    @Autowired
    private LuceneIndexService indexService;

    @MockitoBean
    private TextNormalizer textNormalizer;

    @Test
    void indexingSameIdReplacesContent() throws Exception {
        indexService.clearIndex();
        indexService.indexDocument("same-id", "old", "old content", "https://hust.edu.vn/old", "hust.edu.vn");
        indexService.indexDocument("same-id", "new", "new content", "https://hust.edu.vn/new", "hust.edu.vn");
        try (var directory = FSDirectory.open(indexService.indexPath());
             var reader = DirectoryReader.open(directory)) {
            assertEquals(1, reader.numDocs());
            var hits = new org.apache.lucene.search.IndexSearcher(reader)
                    .search(new org.apache.lucene.search.MatchAllDocsQuery(), 10);
            var document = reader.storedFields().document(hits.scoreDocs[0].doc);
            assertEquals("new content", document.get("content"));
            assertEquals("new", document.get("title"));
        }
    }

    @Test
    void indexTenDocumentsWithoutDuplicatesOnRebuild() throws Exception {
        LuceneTestCorpus.stubPreprocessing(textNormalizer);
        // Rebuild twice to verify that repeated runs do not accumulate documents.
        LuceneTestCorpus.rebuild(indexService, documentLoader);
        LuceneTestCorpus.rebuild(indexService, documentLoader);

        try (var directory = FSDirectory.open(indexService.indexPath());
             var reader = DirectoryReader.open(directory)) {
            assertEquals(10, reader.numDocs());
            var storedFields = reader.storedFields();
            Set<String> actualIds = new HashSet<>();
            for (int i = 0; i < reader.maxDoc(); i++) {
                var document = storedFields.document(i);
                String docId = document.get("docId");
                actualIds.add(docId);
                int number = Integer.parseInt(docId.substring(3));
                assertEquals(Files.readString(LuceneTestCorpus.file(number)),
                        document.get("content"));
                assertNotNull(document.get("title"));
                assertNotNull(document.get("url"));
                assertEquals("hust.edu.vn", document.get("domain"));
            }
            assertEquals(Set.of("doc1", "doc2", "doc3", "doc4", "doc5",
                    "doc6", "doc7", "doc8", "doc9", "doc10"), actualIds);
            System.out.println("INDEXED DOCUMENTS: " + reader.numDocs());
        }
    }
}
