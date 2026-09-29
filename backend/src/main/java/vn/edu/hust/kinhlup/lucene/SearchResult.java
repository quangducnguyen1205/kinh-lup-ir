package vn.edu.hust.kinhlup.lucene;

import java.time.Instant;

public record SearchResult(String id, String title, String url, String snippet,
                           float score, Instant publishedAt) {
    /** Compatibility with the original TF-IDF corpus tests. */
    public String docId() { return id; }
}
