package vn.edu.hust.kinhlup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.edu.hust.kinhlup.lucene.LuceneIndexService;
import vn.edu.hust.kinhlup.lucene.LuceneSearchService;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class EmptyIndexTest {
    @TempDir Path directory;

    @Test
    void searchBeforeFirstRebuildReturnsAnEmptyPage() throws Exception {
        var index = new LuceneIndexService(directory.resolve("new-index").toString());
        var search = new LuceneSearchService(text -> text, index);
        var result = search.search("HUST", 0, 10);
        assertEquals(0, result.total());
        assertTrue(result.results().isEmpty());
    }
}
