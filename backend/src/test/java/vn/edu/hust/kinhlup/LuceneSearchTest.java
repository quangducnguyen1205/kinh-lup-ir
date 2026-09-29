package vn.edu.hust.kinhlup;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.BeforeEach;
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
import vn.edu.hust.kinhlup.lucene.LuceneSearchService;
import vn.edu.hust.kinhlup.lucene.SearchResult;
import vn.edu.hust.kinhlup.text.TextNormalizer;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@SpringBootTest
class LuceneSearchTest {

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

    @Autowired
    private LuceneSearchService searchService;

    @MockitoBean
    private TextNormalizer textNormalizer;

    @BeforeEach
    void rebuildCorpus() throws Exception {
        LuceneTestCorpus.stubPreprocessing(textNormalizer);
        LuceneTestCorpus.rebuild(indexService, documentLoader);
        try (var directory = FSDirectory.open(indexService.indexPath());
             var reader = DirectoryReader.open(directory)) {
            assertEquals(10, reader.numDocs());
        }
    }

    @Test
    void searchRawQueryWithMockPreprocessingAndRealTfIdf() throws Exception {
        List<SearchResult> results = searchService.search(LuceneTestCorpus.RAW_QUERY);
        verify(textNormalizer).normalize(LuceneTestCorpus.RAW_QUERY);
        String normalizedQuery = textNormalizer.normalize(LuceneTestCorpus.RAW_QUERY);
        assertEquals("sinh_viên nghiên_cứu khoa_hoc", normalizedQuery);

        // OR semantics: derive membership from fixture tokens, never expected scores or order.
        Set<String> expectedMatches = new HashSet<>();
        Set<String> queryTokens = Set.of(normalizedQuery.split(" "));
        boolean hasPartialMatch = false;
        for (int i = 1; i <= 10; i++) {
            Set<String> tokens = new HashSet<>(Arrays.asList(
                    Files.readString(LuceneTestCorpus.file(i)).split("\\s+")));
            long matchedTokens = queryTokens.stream().filter(tokens::contains).count();
            if (matchedTokens > 0) {
                expectedMatches.add("doc" + i);
                hasPartialMatch |= matchedTokens < queryTokens.size();
            }
        }
        assertTrue(hasPartialMatch, "Corpus must distinguish OR from AND semantics");
        assertEquals(expectedMatches,
                results.stream().map(SearchResult::docId).collect(Collectors.toSet()));

        System.out.println("RAW QUERY:");
        System.out.println(LuceneTestCorpus.RAW_QUERY);
        System.out.println("\nMOCK NORMALIZED QUERY:");
        System.out.println(normalizedQuery);
        System.out.println("\nRESULT:");
        printAndCheckResults(results);
    }

    private void printAndCheckResults(List<SearchResult> results) {
        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 10);
        assertEquals(results.size(), results.stream().map(SearchResult::docId).distinct().count());
        float previousScore = Float.POSITIVE_INFINITY;
        for (SearchResult result : results) {
            assertTrue(result.docId().matches("doc([1-9]|10)"));
            assertTrue(Float.isFinite(result.score()) && result.score() > 0);
            assertTrue(result.score() <= previousScore);
            previousScore = result.score();
            // Preserve the actual float score's precision rather than rounding to six decimals.
            System.out.println(result.docId() + " : " + result.score());
        }
        System.out.println("\nRANKING:");
        System.out.println(results.stream().map(SearchResult::docId).collect(Collectors.joining(" -> ")));
    }
}
