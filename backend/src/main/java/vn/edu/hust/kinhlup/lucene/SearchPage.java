package vn.edu.hust.kinhlup.lucene;

import java.util.List;

/** Response search: total là toàn bộ số tài liệu khớp, results chỉ chứa trang yêu cầu. */
public record SearchPage(String query, long total, int page, int size, List<SearchResult> results) {
    /** Sao chép danh sách để bên ngoài không sửa được kết quả sau khi đã tạo response. */
    public SearchPage { results = List.copyOf(results); }
}
