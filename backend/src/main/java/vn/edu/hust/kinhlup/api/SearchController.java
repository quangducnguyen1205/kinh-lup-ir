package vn.edu.hust.kinhlup.api;

import org.springframework.web.bind.annotation.*;
import vn.edu.hust.kinhlup.lucene.LuceneSearchService;
import vn.edu.hust.kinhlup.lucene.SearchPage;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class SearchController {
    private final LuceneSearchService search;

    public SearchController(LuceneSearchService search) { this.search = search; }

    @GetMapping("/search")
    public SearchPage search(@RequestParam String q,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size) throws IOException {
        return search.search(q, page, size);
    }
}
