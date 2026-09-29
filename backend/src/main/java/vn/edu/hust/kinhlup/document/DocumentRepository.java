package vn.edu.hust.kinhlup.document;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DocumentRepository {
    private static final String COLUMNS = "id, url, domain, title, content_type, raw_text, "
            + "published_at, last_crawled_at, content_hash, updated_at";
    private static final RowMapper<SourceDocument> MAPPER = (rs, row) -> {
        Timestamp published = rs.getTimestamp("published_at");
        return new SourceDocument(rs.getObject("id", UUID.class), rs.getString("url"),
                rs.getString("domain"), rs.getString("title"), rs.getString("content_type"),
                rs.getString("raw_text"), published == null ? null : published.toInstant(),
                rs.getTimestamp("last_crawled_at").toInstant(), rs.getString("content_hash"),
                rs.getTimestamp("updated_at").toInstant());
    };
    private final JdbcTemplate jdbc;

    public DocumentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<SourceDocument> findActive() {
        return jdbc.query("SELECT " + COLUMNS + " FROM documents WHERE status = 'ACTIVE' ORDER BY id", MAPPER);
    }

    public Optional<SourceDocument> findActiveById(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM documents WHERE id = ? AND status = 'ACTIVE'",
                MAPPER, id).stream().findFirst();
    }

    /** Do not acknowledge a version changed by the crawler while rebuilding the index. */
    public void markIndexed(List<SourceDocument> snapshot) {
        jdbc.batchUpdate("""
                UPDATE documents SET index_status = 'INDEXED', last_indexed_at = CURRENT_TIMESTAMP
                WHERE id = ? AND content_hash = ? AND updated_at = ? AND status = 'ACTIVE'
                """, snapshot, 200, (statement, document) -> {
            statement.setObject(1, document.id());
            statement.setString(2, document.contentHash());
            statement.setTimestamp(3, Timestamp.from(document.updatedAt()));
        });
    }
}
