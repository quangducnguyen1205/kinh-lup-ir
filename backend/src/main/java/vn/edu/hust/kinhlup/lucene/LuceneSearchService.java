package vn.edu.hust.kinhlup.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;
import vn.edu.hust.kinhlup.text.TextNormalizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class LuceneSearchService {

    private final TextNormalizer textNormalizer;
    private final LuceneIndexService indexService;

    public LuceneSearchService(TextNormalizer textNormalizer, LuceneIndexService indexService) {
        this.textNormalizer = textNormalizer;
        this.indexService = indexService;
    }

    public List<SearchResult> search(String inputQuery) throws IOException, ParseException {
        String normalizedQuery = textNormalizer.normalize(inputQuery);
        try (Analyzer analyzer = new WhitespaceAnalyzer();
             Directory directory = FSDirectory.open(indexService.indexPath());
             DirectoryReader reader = DirectoryReader.open(directory)) {
            Query query = new QueryParser("content", analyzer).parse(normalizedQuery);
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new ClassicSimilarity());
            TopDocs topDocs = searcher.search(query, 10);
            var storedFields = searcher.storedFields();
            List<SearchResult> results = new ArrayList<>();
            for (ScoreDoc hit : topDocs.scoreDocs) {
                String docId = storedFields.document(hit.doc).get("docId");
                results.add(new SearchResult(docId, hit.score));
            }
            return List.copyOf(results);
        }
    }
}
