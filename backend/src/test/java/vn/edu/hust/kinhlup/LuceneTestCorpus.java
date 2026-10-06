package vn.edu.hust.kinhlup;

import vn.edu.hust.kinhlup.lucene.DocumentLoader;
import vn.edu.hust.kinhlup.lucene.LuceneIndexService;
import vn.edu.hust.kinhlup.text.TextNormalizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.when;

final class LuceneTestCorpus {

    static final Path BASE_PATH = Path.of("src", "test", "resources", "col0");
    static final String RAW_QUERY = "sinh viên nghiên cứu khoa hoc";
    static final String NORMALIZED_QUERY = "sinh_viên nghiên_cứu khoa_hoc";
    static final String COOPERATION_RAW_QUERY = "hợp tác đào tạo";
    static final String COOPERATION_NORMALIZED_QUERY = "hợp_tác đào_tạo";

    private LuceneTestCorpus() {
    }

    static void stubPreprocessing(TextNormalizer normalizer) throws IOException {
        // Fixed test inputs only: no Vietnamese segmentation or NLP implementation.
        // Preserve the assignment's unaccented "hoc"; do not correct it to "học".
        when(normalizer.normalize(RAW_QUERY)).thenReturn(NORMALIZED_QUERY);
        when(normalizer.normalize(COOPERATION_RAW_QUERY)).thenReturn(COOPERATION_NORMALIZED_QUERY);
        for (int i = 1; i <= 10; i++) {
            String preprocessedContent = Files.readString(file(i));
            when(normalizer.normalize(preprocessedContent)).thenReturn(preprocessedContent);
        }
    }

    static void rebuild(LuceneIndexService indexService, DocumentLoader loader) throws IOException {
        indexService.clearIndex();
        for (int i = 1; i <= 10; i++) {
            loader.loadDocument("doc" + i, file(i));
        }
    }

    static Path file(int number) {
        return BASE_PATH.resolve(number == 10 ? "d10" : "doc" + number);
    }
}
