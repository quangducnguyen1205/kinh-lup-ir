package vn.edu.hust.kinhlup.extraction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Text chưa qua NLP và metadata nhiều giá trị, có cấu trúc phù hợp để chuyển thành JSON. */
public record ExtractedDocument(String text, String contentType, String title,
                                Map<String, List<String>> metadata) {
    /** Sao chép map và từng list để caller không thể sửa metadata của kết quả đã tạo. */
    public ExtractedDocument {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        metadata.forEach((name, values) -> copy.put(name, List.copyOf(values)));
        metadata = java.util.Collections.unmodifiableMap(copy);
    }
}
