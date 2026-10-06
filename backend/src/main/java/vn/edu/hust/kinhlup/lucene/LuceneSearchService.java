package vn.edu.hust.kinhlup.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.springframework.stereotype.Service;
import vn.edu.hust.kinhlup.text.TextNormalizer;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class LuceneSearchService {
    private final TextNormalizer textNormalizer;
    private final LuceneIndexService indexService;

    public LuceneSearchService(TextNormalizer textNormalizer, LuceneIndexService indexService) {
        this.textNormalizer = textNormalizer;
        this.indexService = indexService;
    }

    public List<SearchResult> search(String inputQuery) throws IOException {
        return search(inputQuery, 0, 10).results();
    }

    public SearchPage search(String inputQuery, int page, int size) throws IOException {
        return search(inputQuery, page, size, new ClassicSimilarity());
    }

    /** Select a scoring model per search, so experiments do not change the API default. */
    public SearchPage search(String inputQuery, int page, int size, Similarity similarity) throws IOException {
        Objects.requireNonNull(similarity, "similarity");
        if (inputQuery == null || inputQuery.length() > 1000) {
            throw new IllegalArgumentException("q must contain at most 1000 characters");
        }
        if (page < 0 || size < 1 || size > 100 || ((long) page + 1) * size > 10000) {
            throw new IllegalArgumentException("page >= 0, size between 1 and 100, search window <= 10000 required");
        }
        var empty = new SearchPage(inputQuery, 0, page, size, List.of());
        if (inputQuery.isBlank()) return empty;
        String normalizedQuery = textNormalizer.normalize(inputQuery);
        try (Analyzer analyzer = new StandardAnalyzer();
             var directory = FSDirectory.open(indexService.indexPath())) {
            if (!DirectoryReader.indexExists(directory)) return empty;
            // Treat user input as plain text, including Lucene operators and punctuation.
            QueryBuilder builder = new QueryBuilder(analyzer);
            Query content = builder.createBooleanQuery("content", normalizedQuery);
            Query title = builder.createBooleanQuery("title", normalizedQuery);
            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
            if (content != null) queryBuilder.add(content, BooleanClause.Occur.SHOULD);
            if (title != null) queryBuilder.add(new BoostQuery(title, 2f), BooleanClause.Occur.SHOULD);
            Query query = queryBuilder.build();
            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                searcher.setSimilarity(similarity);
                long total = searcher.count(query);
                int offset = page * size;
                if (offset >= total) return new SearchPage(inputQuery, total, page, size, List.of());
                TopDocs topDocs = searcher.search(query, offset + size);
                var storedFields = searcher.storedFields();
                List<SearchResult> results = new ArrayList<>();
                for (int i = offset; i < topDocs.scoreDocs.length; i++) {
                    ScoreDoc hit = topDocs.scoreDocs[i];
                    var document = storedFields.document(hit.doc);
                    String rawText = document.get("rawText");
                    if (rawText == null) rawText = document.get("content");
                    String snippet = rawText.replaceAll("\\s+", " ").strip();
                    if (snippet.length() > 240) snippet = snippet.substring(0, 240) + "…";
                    String published = document.get("publishedAt");
                    results.add(new SearchResult(document.get("docId"), document.get("title"),
                            document.get("url"), snippet, hit.score,
                            published == null ? null : Instant.parse(published)));
                }
                return new SearchPage(inputQuery, total, page, size, results);
            }
        }
    }
}
