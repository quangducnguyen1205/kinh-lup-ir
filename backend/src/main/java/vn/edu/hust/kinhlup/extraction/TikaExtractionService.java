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

/** Bóc tách text/metadata từ PDF, DOCX, PPTX; caller phụ trách lưu DB hoặc đưa text vào Lucene. */
@Service
public class TikaExtractionService {
    public static final String PDF = "application/pdf";
    public static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    private static final Set<String> SUPPORTED = Set.of(PDF, DOCX, PPTX);
    private final int maxBytes;
    private final int maxCharacters;

    /** Giới hạn byte đầu vào và số ký tự đầu ra để kiểm soát lượng dữ liệu mỗi lần xử lý. */
    public TikaExtractionService(@Value("${kinhlup.extraction.max-bytes:20971520}") int maxBytes,
                                 @Value("${kinhlup.extraction.max-characters:2000000}") int maxCharacters) {
        if (maxBytes < 1 || maxBytes == Integer.MAX_VALUE || maxCharacters < 1) {
            throw new IllegalArgumentException("Extraction limits must be positive and max-bytes < Integer.MAX_VALUE");
        }
        this.maxBytes = maxBytes;
        this.maxCharacters = maxCharacters;
    }

    /** Đọc file trên ổ đĩa, kiểm tra kích thước trước và tự đóng stream sau khi xử lý. */
    public ExtractedDocument extract(Path path) throws IOException {
        if (Files.size(path) > maxBytes) {
            throw new ExtractionException(FILE_TOO_LARGE, "Document exceeds the configured byte limit");
        }
        try (InputStream stream = Files.newInputStream(path)) {
            return extract(stream, path.getFileName().toString());
        }
    }

    /**
     * Nhận stream và tên file gợi ý, trả text cùng metadata chưa qua NLP.
     * Caller tự đóng stream đầu vào; service chỉ tự đóng stream nội bộ tạo từ mảng byte.
     */
    public ExtractedDocument extract(InputStream input, String fileName) throws IOException {
        // Đọc thêm tối đa 1 byte để phân biệt file đúng giới hạn với file vượt giới hạn.
        byte[] bytes = input.readNBytes(maxBytes + 1);
        if (bytes.length == 0) throw new ExtractionException(EMPTY_INPUT, "Document is empty");
        if (bytes.length > maxBytes) {
            throw new ExtractionException(FILE_TOO_LARGE, "Document exceeds the configured byte limit");
        }
        Metadata metadata = new Metadata();
        if (fileName != null && !fileName.isBlank()) metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        // Mỗi lần gọi có parser/context/metadata riêng để các luồng không trộn dữ liệu tài liệu.
        // Handler thu text và báo lỗi khi vượt giới hạn, không âm thầm trả nội dung bị cắt.
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(maxCharacters);
        ParseContext context = new ParseContext();
        PDFParserConfig pdfConfig = new PDFParserConfig();
        // Chỉ lấy lớp text có sẵn trong PDF; chưa nhận dạng chữ từ ảnh scan bằng OCR.
        pdfConfig.getOcr().setStrategy(OcrConfig.Strategy.NO_OCR);
        context.set(PDFParserConfig.class, pdfConfig);
        // Bỏ qua tài liệu nhúng/đính kèm, chỉ trích nội dung của file chính.
        context.set(EmbeddedDocumentExtractor.class, new EmbeddedDocumentExtractor() {
            @Override
            public boolean shouldParseEmbedded(Metadata embedded, ParseContext parseContext) { return false; }
            @Override
            public void parseEmbedded(TikaInputStream stream, ContentHandler contentHandler,
                                      Metadata embedded, ParseContext parseContext, boolean outputHtml) { }
        });
        try (TikaInputStream stream = TikaInputStream.get(bytes)) {
            // Nhận dạng MIME bằng detector của Tika trước khi chọn parser phù hợp.
            String contentType = parser.getDetector().detect(stream, metadata, context).toString();
            if (!SUPPORTED.contains(contentType)) {
                throw new ExtractionException(UNSUPPORTED_TYPE, "Supported formats are PDF, DOCX and PPTX");
            }
            parser.parse(stream, handler, metadata, context);
            // Một khóa metadata có thể có nhiều giá trị; giữ đủ các giá trị để lưu JSONB sau này.
            Map<String, List<String>> values = new LinkedHashMap<>();
            Arrays.stream(metadata.names()).sorted()
                    .forEach(name -> values.put(name, List.of(metadata.getValues(name))));
            return new ExtractedDocument(handler.toString().strip(), contentType,
                    metadata.get(TikaCoreProperties.TITLE), values);
        // Phân biệt lỗi vượt giới hạn text với lỗi parser/file hỏng để caller xử lý phù hợp.
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
