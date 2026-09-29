package vn.edu.hust.kinhlup.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.hust.kinhlup.document.DocumentRepository;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentRepository documents;

    public DocumentController(DocumentRepository documents) { this.documents = documents; }

    @GetMapping("/{id}")
    public DocumentDetail detail(@PathVariable UUID id) {
        var document = documents.findActiveById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        return new DocumentDetail(document.id(), document.url(), document.title(), document.contentType(),
                document.rawText(), document.publishedAt(), document.lastCrawledAt());
    }

    public record DocumentDetail(UUID id, String url, String title, String contentType,
                                 String text, Instant publishedAt, Instant lastCrawledAt) { }
}
