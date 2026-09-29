package vn.edu.hust.kinhlup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import vn.edu.hust.kinhlup.document.DocumentRepository;
import vn.edu.hust.kinhlup.document.SourceDocument;
import vn.edu.hust.kinhlup.lucene.ReindexService;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"kinhlup.admin.reindex-enabled=true", "spring.config.import="})
class ApiContractTest {
    @TempDir static Path indexDirectory;
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("kinhlup.lucene.index-dir", indexDirectory::toString);
    }
    @Autowired WebApplicationContext context;
    @Autowired ReindexService reindex;
    @MockitoBean DocumentRepository documents;
    MockMvc mvc;
    List<SourceDocument> corpus;
    static final Instant DATE = Instant.parse("2026-09-20T08:00:00Z");

    @BeforeEach
    void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        corpus = IntStream.rangeClosed(1, 23).mapToObj(i -> new SourceDocument(
                new UUID(0, i), "https://hust.edu.vn/" + i, "hust.edu.vn", "HUST tuyển sinh " + i,
                "text/html", "Thông tin tuyển sinh, học bổng dành cho sinh viên. ".repeat(8),
                i == 1 ? DATE : null, DATE, "hash-" + i, DATE)).toList();
        when(documents.findActive()).thenReturn(corpus);
        when(documents.findActiveById(corpus.getFirst().id())).thenReturn(Optional.of(corpus.getFirst()));
        reindex.rebuild();
    }

    @Test
    void searchReturnsExactContractDefaultsAndRawSnippet() throws Exception {
        mvc.perform(get("/api/search").param("q", "TUYỂN SINH,"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.query").value("TUYỂN SINH,"))
                .andExpect(jsonPath("$.total").value(23))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.results", hasSize(10)))
                .andExpect(jsonPath("$.results[0].*", hasSize(6)))
                .andExpect(jsonPath("$.results[0].id").value(corpus.getFirst().id().toString()))
                .andExpect(jsonPath("$.results[0].title").value("HUST tuyển sinh 1"))
                .andExpect(jsonPath("$.results[0].url").value(corpus.getFirst().url()))
                .andExpect(jsonPath("$.results[0].snippet", startsWith("Thông tin tuyển sinh,")))
                .andExpect(jsonPath("$.results[0].score", greaterThan(0.0)))
                .andExpect(jsonPath("$.results[0].publishedAt").value(DATE.toString()));
    }

    @Test
    void paginationIncludesLastPageAndOutOfRangePageWithoutLosingTotal() throws Exception {
        mvc.perform(get("/api/search").param("q", "hust").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(23))
                .andExpect(jsonPath("$.results", hasSize(3)))
                .andExpect(jsonPath("$.results[0].id").value(corpus.get(20).id().toString()))
                .andExpect(jsonPath("$.results[0].publishedAt").value(nullValue()));
        mvc.perform(get("/api/search").param("q", "hust").param("page", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(23))
                .andExpect(jsonPath("$.results", empty()));
    }

    @Test
    void emptyMissingAndLiteralQueriesAreHandled() throws Exception {
        for (String query : List.of("", "   ", "no-such-token", "*:*", "\"[")) {
            mvc.perform(get("/api/search").param("q", query)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.results", empty()));
        }
        mvc.perform(get("/api/search")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/search").param("q", "title:hust"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void invalidPaginationAndOversizedQueriesReturn400() throws Exception {
        for (String page : List.of("-1", "2147483647", "not-a-number")) {
            mvc.perform(get("/api/search").param("q", "hust").param("page", page))
                    .andExpect(status().isBadRequest());
        }
        for (String size : List.of("0", "-1", "101")) {
            mvc.perform(get("/api/search").param("q", "hust").param("size", size))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/search").param("q", "x".repeat(1001))).andExpect(status().isBadRequest());
    }

    @Test
    void detailReadsSourceAndReturnsOnlyContractFields() throws Exception {
        mvc.perform(get("/api/documents/" + corpus.getFirst().id()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.*", hasSize(7)))
                .andExpect(jsonPath("$.id").value(corpus.getFirst().id().toString()))
                .andExpect(jsonPath("$.url").value(corpus.getFirst().url()))
                .andExpect(jsonPath("$.title").value(corpus.getFirst().title()))
                .andExpect(jsonPath("$.contentType").value("text/html"))
                .andExpect(jsonPath("$.text").value(corpus.getFirst().rawText()))
                .andExpect(jsonPath("$.publishedAt").value(DATE.toString()))
                .andExpect(jsonPath("$.lastCrawledAt").value(DATE.toString()));
        mvc.perform(get("/api/documents/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/documents/invalid-id")).andExpect(status().isBadRequest());
    }

    @Test
    void optInReindexEndpointRebuildsAndAcknowledgesDatabase() throws Exception {
        mvc.perform(post("/api/admin/reindex")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.indexedCount").value(23));
        verify(documents, times(2)).markIndexed(corpus);
    }

    @Test
    void databaseFailureReturns503WithoutInternalDetails() throws Exception {
        when(documents.findActive()).thenThrow(new DataAccessResourceFailureException("private connection details"));
        mvc.perform(post("/api/admin/reindex")).andExpect(status().isServiceUnavailable())
                .andExpect(content().string(not(containsString("private connection details"))));
        // A failed DB read must leave the previously committed index usable.
        mvc.perform(get("/api/search").param("q", "hust"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(23));
    }

    @Test
    void frontendOriginCanReadSearchButCannotReindexViaCors() throws Exception {
        mvc.perform(get("/api/search").param("q", "hust").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/admin/reindex").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(get("/api/search").param("q", "hust").header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden());
    }
}
