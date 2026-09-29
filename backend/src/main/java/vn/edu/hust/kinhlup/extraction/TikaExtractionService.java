package vn.edu.hust.kinhlup.extraction;

import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.OcrConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static vn.edu.hust.kinhlup.extraction.ExtractionException.Reason.*;

@Service
public class TikaExtractionService {
    public static final String PDF = "application/pdf";
    public static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    private static final Set<String> SUPPORTED = Set.of(PDF, DOCX, PPTX);
    private final int maxBytes;
    private final int maxCharacters;

    public TikaExtractionService(@Value("${kinhlup.extraction.max-bytes:20971520}") int maxBytes,
                                 @Value("${kinhlup.extraction.max-characters:2000000}") int maxCharacters) {
        if (maxBytes < 1 || maxBytes == Integer.MAX_VALUE || maxCharacters < 1) {
            throw new IllegalArgumentException("Extraction limits must be positive and max-bytes < Integer.MAX_VALUE");
        }
        this.maxBytes = maxBytes;
        this.maxCharacters = maxCharacters;
    }

    public ExtractedDocument extract(Path path) throws IOException {
        if (Files.size(path) > maxBytes) {
            throw new ExtractionException(FILE_TOO_LARGE, "Document exceeds the configured byte limit");
        }
        try (InputStream stream = Files.newInputStream(path)) {
            return extract(stream, path.getFileName().toString());
        }
    }

    /** Reads a bounded payload; the caller retains ownership of the supplied stream. */
    public ExtractedDocument extract(InputStream input, String fileName) throws IOException {
        byte[] bytes = input.readNBytes(maxBytes + 1);
        if (bytes.length == 0) throw new ExtractionException(EMPTY_INPUT, "Document is empty");
        if (bytes.length > maxBytes) {
            throw new ExtractionException(FILE_TOO_LARGE, "Document exceeds the configured byte limit");
        }
        Metadata metadata = new Metadata();
        if (fileName != null && !fileName.isBlank()) metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        // Each invocation owns parser/context/metadata, so concurrent calls cannot mix documents.
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(maxCharacters);
        ParseContext context = new ParseContext();
        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.getOcr().setStrategy(OcrConfig.Strategy.NO_OCR);
        context.set(PDFParserConfig.class, pdfConfig);
        context.set(EmbeddedDocumentExtractor.class, new EmbeddedDocumentExtractor() {
            @Override
            public boolean shouldParseEmbedded(Metadata embedded, ParseContext parseContext) { return false; }
            @Override
            public void parseEmbedded(TikaInputStream stream, ContentHandler contentHandler,
                                      Metadata embedded, ParseContext parseContext, boolean outputHtml) { }
        });
        try (TikaInputStream stream = TikaInputStream.get(bytes)) {
            String contentType = parser.getDetector().detect(stream, metadata, context).toString();
            if (!SUPPORTED.contains(contentType)) {
                throw new ExtractionException(UNSUPPORTED_TYPE, "Supported formats are PDF, DOCX and PPTX");
            }
            parser.parse(stream, handler, metadata, context);
            Map<String, List<String>> values = new LinkedHashMap<>();
            Arrays.stream(metadata.names()).sorted()
                    .forEach(name -> values.put(name, List.of(metadata.getValues(name))));
            return new ExtractedDocument(handler.toString().strip(), contentType,
                    metadata.get(TikaCoreProperties.TITLE), values);
        } catch (TikaException | SAXException exception) {
            if (WriteLimitReachedException.isWriteLimitReached(exception)) {
                throw new ExtractionException(TEXT_TOO_LARGE, "Extracted text exceeds the configured character limit", exception);
            }
            throw new ExtractionException(PARSE_FAILED, "Cannot extract document text and metadata", exception);
        } catch (ExtractionException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ExtractionException(PARSE_FAILED, "Cannot read the document structure", exception);
        }
    }
}
