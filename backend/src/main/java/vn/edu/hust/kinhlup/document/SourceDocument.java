package vn.edu.hust.kinhlup.document;

import java.time.Instant;
import java.util.UUID;

/** Snapshot of the source fields needed by indexing and the document API. */
public record SourceDocument(UUID id, String url, String domain, String title,
        String contentType, String rawText, Instant publishedAt, Instant lastCrawledAt,
        String contentHash, Instant updatedAt) {
}
