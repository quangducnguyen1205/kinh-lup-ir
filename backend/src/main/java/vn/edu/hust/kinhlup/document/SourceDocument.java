package vn.edu.hust.kinhlup.document;

import java.time.Instant;
import java.util.UUID;

/** Bản dữ liệu nguồn tại thời điểm đọc DB, dùng cho lập index và API chi tiết tài liệu. */
public record SourceDocument(UUID id, String url, String domain, String title,
        String contentType, String rawText, Instant publishedAt, Instant lastCrawledAt,
        String contentHash, Instant updatedAt) {
}
