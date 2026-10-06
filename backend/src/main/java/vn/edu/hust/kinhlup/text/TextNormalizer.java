package vn.edu.hust.kinhlup.text;

/** Điểm tích hợp tiền xử lý tiếng Việt; tài liệu và query phải dùng cùng chiến lược. */
public interface TextNormalizer {
    /** Nhận và trả chuỗi văn bản; việc chuẩn hóa/ghép từ phụ thuộc implementation. */
    String normalize(String text);
}
