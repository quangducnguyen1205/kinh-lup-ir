package vn.edu.hust.kinhlup.extraction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Text before NLP normalization, plus JSON-compatible, multi-valued Tika metadata. */
public record ExtractedDocument(String text, String contentType, String title,
                                Map<String, List<String>> metadata) {
    public ExtractedDocument {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        metadata.forEach((name, values) -> copy.put(name, List.copyOf(values)));
        metadata = java.util.Collections.unmodifiableMap(copy);
    }
}
