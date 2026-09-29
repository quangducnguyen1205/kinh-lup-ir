package vn.edu.hust.kinhlup.lucene;

import java.util.List;

public record SearchPage(String query, long total, int page, int size, List<SearchResult> results) {
    public SearchPage { results = List.copyOf(results); }
}
