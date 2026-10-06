package vn.edu.hust.kinhlup.extraction;

import java.io.IOException;

/** Lỗi bóc tách có mã nguyên nhân để caller phân biệt loại lỗi mà không phải đọc chuỗi thông báo. */
public class ExtractionException extends IOException {
    // Lần lượt: file rỗng, quá nhiều byte, quá nhiều ký tự, sai định dạng hỗ trợ, lỗi phân tích file.
    public enum Reason { EMPTY_INPUT, FILE_TOO_LARGE, TEXT_TOO_LARGE, UNSUPPORTED_TYPE, PARSE_FAILED }
    private final Reason reason;

    /** Tạo lỗi do kiểm tra đầu vào hoặc giới hạn, chưa có exception gốc. */
    public ExtractionException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    /** Giữ exception gốc để truy vết lỗi Tika/I/O cùng mã nguyên nhân của ứng dụng. */
    public ExtractionException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    /** Trả mã nguyên nhân để caller chọn cách xử lý. */
    public Reason reason() { return reason; }
}
