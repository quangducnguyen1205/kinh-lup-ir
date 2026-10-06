package vn.edu.hust.kinhlup.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    /** Kiểm tra ứng dụng trả lời HTTP; không kiểm tra kết nối DB hoặc tình trạng index. */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "service", "kinh-lup-backend"
        );
    }
}
