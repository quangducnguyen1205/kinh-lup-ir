package vn.edu.hust.kinhlup.text;

import org.springframework.stereotype.Component;

/** Implementation tạm do Spring sử dụng khi chưa tích hợp bộ xử lý tiếng Việt. */
@Component
public class NoOpTextNormalizer implements TextNormalizer {

    /** Giữ nguyên đầu vào; không ghép từ, sửa dấu hay chuẩn hóa Unicode. Test có thể thay bằng mock. */
    @Override
    public String normalize(String text) {
        return text;
    }
}
