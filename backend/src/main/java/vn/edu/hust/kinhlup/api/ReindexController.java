package vn.edu.hust.kinhlup.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import vn.edu.hust.kinhlup.lucene.ReindexService;
import java.io.IOException;

/** API rebuild thủ công, chỉ được đăng ký khi kinhlup.admin.reindex-enabled=true. */
@RestController
@RequestMapping("/api/admin")
@ConditionalOnProperty(name = "kinhlup.admin.reindex-enabled", havingValue = "true")
public class ReindexController {
    private final ReindexService reindex;

    public ReindexController(ReindexService reindex) { this.reindex = reindex; }

    /** Chạy rebuild đồng bộ; chỉ trả SUCCEEDED sau khi service hoàn thành cả index và ghi nhận DB. */
    @PostMapping("/reindex")
    public ReindexResponse rebuild() throws IOException {
        return new ReindexResponse("SUCCEEDED", reindex.rebuild());
    }

    /** Số tài liệu được xử lý trong lần rebuild, không phải tổng số hàng có trong DB. */
    public record ReindexResponse(String status, int indexedCount) { }
}
