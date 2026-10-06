package vn.edu.hust.kinhlup.lucene;

import java.time.Instant;

/** Một kết quả tìm kiếm theo API contract; score là điểm Lucene, không phải phần trăm chính xác. */
public record SearchResult(String id, String title, String url, String snippet,
                           float score, Instant publishedAt) {
    /** Tên truy cập tương thích với test corpus cũ; trả cùng giá trị với id(). */
    public String docId() { return id; }
}
