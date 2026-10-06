package vn.edu.hust.kinhlup.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.hust.kinhlup.document.DocumentRepository;
import java.time.Instant;
import java.util.UUID;

/** API chi tiết lấy dữ liệu gốc từ PostgreSQL, có thể mới hơn bản đang nằm trong index. */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentRepository documents;

    public DocumentController(DocumentRepository documents) { this.documents = documents; }

    /** Tìm theo UUID; không có tài liệu ACTIVE thì trả 404, ID sai định dạng được Spring trả 400. */
    @GetMapping("/{id}")
    public DocumentDetail detail(@PathVariable UUID id) {
        var document = documents.findActiveById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        return new DocumentDetail(document.id(), document.url(), document.title(), document.contentType(),
                document.rawText(), document.publishedAt(), document.lastCrawledAt());
    }

    /** Các tên thuộc tính được giữ đúng API contract để frontend đọc JSON. */
    public record DocumentDetail(UUID id, String url, String title, String contentType,
                                 String text, Instant publishedAt, Instant lastCrawledAt) { }
}
