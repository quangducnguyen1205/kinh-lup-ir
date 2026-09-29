package vn.edu.hust.kinhlup;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vn.edu.hust.kinhlup.document.SourceDocument;
import vn.edu.hust.kinhlup.extraction.TikaExtractionService;
import vn.edu.hust.kinhlup.lucene.LuceneIndexService;
import vn.edu.hust.kinhlup.lucene.LuceneSearchService;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ExtractionSearchTest {
    @TempDir Path directory;

    static Stream<Arguments> documents() throws Exception {
        return Stream.of(
                Arguments.of("hust.pdf", ExtractionTestDocuments.pdf(false), "scholarship"),
                Arguments.of("hust.docx", ExtractionTestDocuments.docx(), "học bổng"),
                Arguments.of("hust.pptx", ExtractionTestDocuments.pptx(), "nghiên cứu"));
    }

    @ParameterizedTest
    @MethodSource("documents")
    void extractedTextCanBeIndexedAndFoundWithOriginalMetadata(String filename, byte[] bytes, String query) throws Exception {
        var extraction = new TikaExtractionService(20 * 1024 * 1024, 2_000_000);
        var extracted = extraction.extract(new ByteArrayInputStream(bytes), filename);
        UUID id = UUID.randomUUID();
        var source = new SourceDocument(id, "https://hust.edu.vn/" + filename, "hust.edu.vn",
                extracted.title(), extracted.contentType(), extracted.text(), null,
                Instant.now(), "fixture-hash", Instant.now());
        var index = new LuceneIndexService(directory.toString());
        index.rebuild(List.of(source), text -> text);
        var search = new LuceneSearchService(text -> text, index);
        var page = search.search(query, 0, 10);
        assertEquals(1, page.total());
        var hit = page.results().getFirst();
        assertEquals(id.toString(), hit.id());
        assertEquals(source.url(), hit.url());
        assertEquals(extracted.title(), hit.title());
        assertTrue(hit.snippet().toLowerCase(java.util.Locale.ROOT).contains(query));
        assertTrue(Float.isFinite(hit.score()) && hit.score() > 0);
    }
}
