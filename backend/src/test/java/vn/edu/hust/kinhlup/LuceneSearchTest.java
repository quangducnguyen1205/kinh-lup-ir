package vn.edu.hust.kinhlup;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/** So sánh 2 query × 2 thuật toán trên corpus cố định, kiểm tra tập tài liệu và score hợp lệ. */
@SpringBootTest
class LuceneSearchTest {

    @TempDir
    static Path temporaryIndex;

    /** Chuyển index sang thư mục tạm, tránh chạm index đang dùng của ứng dụng. */
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

    // Thay bean tiền xử lý thật bằng mock, không thay service Lucene hay thuật toán chấm điểm.
    @MockitoBean
    private TextNormalizer textNormalizer;

    /** Mỗi trường hợp bắt đầu với cùng 10 tài liệu để so sánh công bằng. */
    @BeforeEach
    void rebuildCorpus() throws Exception {
        LuceneTestCorpus.stubPreprocessing(textNormalizer);
        LuceneTestCorpus.rebuild(indexService, documentLoader);
        try (var directory = FSDirectory.open(indexService.indexPath());
             var reader = DirectoryReader.open(directory)) {
            assertEquals(10, reader.numDocs());
        }
    }

    /** Tạo 4 bộ tham số: mỗi query chạy TF-IDF và BM25 với k1=1.2, b=0.75. */
    static Stream<Arguments> queriesAndScoringModels() {
        return Stream.of(
                new String[]{LuceneTestCorpus.RAW_QUERY, LuceneTestCorpus.NORMALIZED_QUERY},
                new String[]{LuceneTestCorpus.COOPERATION_RAW_QUERY, LuceneTestCorpus.COOPERATION_NORMALIZED_QUERY}
        ).flatMap(query -> Stream.of(
                Arguments.of(query[0], query[1], "TF-IDF", new ClassicSimilarity()),
                Arguments.of(query[0], query[1], "BM25 (k1=1.2, b=0.75)", new BM25Similarity(1.2f, 0.75f))
        ));
    }

    /** Gọi search thật, kiểm tra mock được sử dụng, rồi đối chiếu tập tài liệu khớp và in ranking. */
    @ParameterizedTest(name = "{2} | Truy vấn: {0}")
    @MethodSource("queriesAndScoringModels")
    void searchRawQueryWithMockPreprocessingAndRealScoring(
            String rawQuery, String expectedNormalizedQuery, String scoringModel, Similarity similarity)
            throws Exception {
        List<SearchResult> results = searchService.search(rawQuery, 0, 10, similarity).results();
        verify(textNormalizer).normalize(rawQuery);
        String normalizedQuery = textNormalizer.normalize(rawQuery);
        assertEquals(expectedNormalizedQuery, normalizedQuery);

        // Tự xác định tài liệu khớp OR từ token của fixture; không gán sẵn điểm hoặc thứ hạng.
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

        System.out.println("\nSCORING MODEL: " + scoringModel);
        System.out.println("RAW QUERY:");
        System.out.println(rawQuery);
        System.out.println("\nMOCK NORMALIZED QUERY:");
        System.out.println(normalizedQuery);
        System.out.println("\nRESULT:");
        printAndCheckResults(results);
    }

    /** Kiểm tra ID không trùng, score dương/hữu hạn/giảm dần rồi in kết quả thực tế. */
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
            // In nguyên giá trị float Lucene trả về để không làm mất độ chính xác do làm tròn.
            System.out.println(result.docId() + " : " + result.score());
        }
        System.out.println("\nRANKING:");
        System.out.println(results.stream().map(SearchResult::docId).collect(Collectors.joining(" -> ")));
    }
}
