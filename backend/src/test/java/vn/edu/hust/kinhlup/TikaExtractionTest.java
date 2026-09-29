package vn.edu.hust.kinhlup;

import org.apache.tika.metadata.TikaCoreProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.edu.hust.kinhlup.extraction.ExtractionException;
import vn.edu.hust.kinhlup.extraction.TikaExtractionService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static vn.edu.hust.kinhlup.extraction.ExtractionException.Reason.*;

class TikaExtractionTest {
    @TempDir Path directory;
    TikaExtractionService service = new TikaExtractionService(20 * 1024 * 1024, 2_000_000);

    @Test
    void extractsRealPdfTextTitleAuthorAndMimeFromPath() throws Exception {
        Path file = directory.resolve("hust.pdf");
        Files.write(file, ExtractionTestDocuments.pdf(false));
        var result = service.extract(file);
        assertEquals(TikaExtractionService.PDF, result.contentType());
        assertTrue(result.text().contains("HUST scholarship information for students."));
        assertEquals("HUST tuyển sinh", result.title());
        assertTrue(result.metadata().get(TikaCoreProperties.CREATOR.getName()).contains("HUST test author"));
        assertThrows(UnsupportedOperationException.class, () -> result.metadata().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> result.metadata().get(TikaCoreProperties.CREATOR.getName()).clear());
        Files.delete(file); // Path overload released the file handle on Windows too.
    }

    @Test
    void autoDetectsPdfWithoutFilenameAndRetainsCallerStreamOwnership() throws Exception {
        boolean[] closed = {false};
        var stream = new ByteArrayInputStream(ExtractionTestDocuments.pdf(false)) {
            @Override public void close() { closed[0] = true; }
        };
        assertEquals(TikaExtractionService.PDF, service.extract(stream, null).contentType());
        assertFalse(closed[0]);
    }

    @Test
    void supportsDocxWithVietnameseTextAndMetadata() throws Exception {
        var result = service.extract(new ByteArrayInputStream(ExtractionTestDocuments.docx()), "hust.docx");
        assertEquals(TikaExtractionService.DOCX, result.contentType());
        assertTrue(result.text().contains("Học bổng dành cho sinh viên nghiên cứu khoa học."));
        assertEquals("Thông báo HUST", result.title());
    }

    @Test
    void supportsPptxWithVietnameseTextAndMetadata() throws Exception {
        var result = service.extract(new ByteArrayInputStream(ExtractionTestDocuments.pptx()), "hust.pptx");
        assertEquals(TikaExtractionService.PPTX, result.contentType());
        assertTrue(result.text().contains("Sinh viên HUST tham gia nghiên cứu khoa học."));
        assertEquals("Hội thảo HUST", result.title());
    }

    @Test
    void rejectsEmptyUnsupportedAndOversizedInput() throws Exception {
        assertEquals(EMPTY_INPUT, assertThrows(ExtractionException.class,
                () -> service.extract(new ByteArrayInputStream(new byte[0]), "empty.pdf")).reason());
        assertEquals(UNSUPPORTED_TYPE, assertThrows(ExtractionException.class,
                () -> service.extract(new ByteArrayInputStream("plain text".getBytes(StandardCharsets.UTF_8)), "test.txt")).reason());
        var small = new TikaExtractionService(10, 100);
        assertEquals(FILE_TOO_LARGE, assertThrows(ExtractionException.class,
                () -> small.extract(new ByteArrayInputStream(new byte[11]), "large.pdf")).reason());
        Path file = directory.resolve("large.pdf");
        Files.write(file, new byte[11]);
        assertEquals(FILE_TOO_LARGE, assertThrows(ExtractionException.class, () -> small.extract(file)).reason());
    }

    @Test
    void rejectsCorruptAndPasswordProtectedPdf() throws Exception {
        byte[] corrupt = "%PDF-1.7\nnot a valid document\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        assertEquals(PARSE_FAILED, assertThrows(ExtractionException.class,
                () -> service.extract(new ByteArrayInputStream(corrupt), "corrupt.pdf")).reason());
        byte[] encrypted = ExtractionTestDocuments.pdf(true);
        assertEquals(PARSE_FAILED, assertThrows(ExtractionException.class,
                () -> service.extract(new ByteArrayInputStream(encrypted), "protected.pdf")).reason());
    }

    @Test
    void textLimitFailsExplicitlyInsteadOfReturningTruncatedSuccess() throws Exception {
        var limited = new TikaExtractionService(20 * 1024 * 1024, 10);
        byte[] pdf = ExtractionTestDocuments.pdf(false);
        assertEquals(TEXT_TOO_LARGE, assertThrows(ExtractionException.class,
                () -> limited.extract(new ByteArrayInputStream(pdf), "large-text.pdf")).reason());
    }

    @Test
    void simultaneousCallsKeepMetadataAndTextSeparate() throws Exception {
        byte[] pdf = ExtractionTestDocuments.pdf(false);
        byte[] docx = ExtractionTestDocuments.docx();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.<Callable<String>>of(
                    () -> service.extract(new ByteArrayInputStream(pdf), "one.pdf").title(),
                    () -> service.extract(new ByteArrayInputStream(docx), "two.docx").title()));
            assertEquals("HUST tuyển sinh", results.get(0).get());
            assertEquals("Thông báo HUST", results.get(1).get());
        }
    }
}
