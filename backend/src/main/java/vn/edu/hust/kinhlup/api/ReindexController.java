package vn.edu.hust.kinhlup.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import vn.edu.hust.kinhlup.lucene.ReindexService;
import java.io.IOException;

@RestController
@RequestMapping("/api/admin")
@ConditionalOnProperty(name = "kinhlup.admin.reindex-enabled", havingValue = "true")
public class ReindexController {
    private final ReindexService reindex;

    public ReindexController(ReindexService reindex) { this.reindex = reindex; }

    @PostMapping("/reindex")
    public ReindexResponse rebuild() throws IOException {
        return new ReindexResponse("SUCCEEDED", reindex.rebuild());
    }

    public record ReindexResponse(String status, int indexedCount) { }
}
