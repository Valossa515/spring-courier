package io.github.valossa515.spring_courier.outbox;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * {@link OutboxStore} backed by a plain JDBC {@link DataSource}.
 *
 * <p>Uses {@link JdbcTemplate}, which transparently joins any Spring-managed
 * transaction in scope — so {@link #save(OutboxMessage)} commits together with
 * the business transaction that produced the message.
 *
 * <p>The only value interpolated into a SQL string is the table name, which is
 * validated against a strict {@code [A-Za-z0-9_]+} allow-list in the
 * constructor; all user/business data is bound through parameter placeholders.
 * Hence {@code java:S2077} (dynamic SQL) is suppressed for this class.
 */
@SuppressWarnings("java:S2077")
public class JdbcOutboxStore implements OutboxStore {

    private static final Pattern SAFE_TABLE = Pattern.compile("[A-Za-z0-9_]+");

    private final JdbcTemplate jdbc;
    private final String table;

    private final RowMapper<OutboxMessage> rowMapper = (rs, rowNum) -> new OutboxMessage(
            rs.getString("id"),
            rs.getString("event_type"),
            rs.getString("payload"),
            OutboxStatus.valueOf(rs.getString("status")),
            rs.getInt("attempts"),
            rs.getString("last_error"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("processed_at")));

    public JdbcOutboxStore(DataSource dataSource, String tableName) {
        if (!SAFE_TABLE.matcher(tableName).matches()) {
            throw new IllegalArgumentException(
                    "Illegal outbox table name: " + tableName);
        }
        this.jdbc = new JdbcTemplate(dataSource);
        this.table = tableName;
    }

    @Override
    public void save(OutboxMessage m) {
        Timestamp created = Timestamp.from(m.createdAt());
        jdbc.update("INSERT INTO " + table
                        + " (id, event_type, payload, status, attempts, last_error,"
                        + " created_at, updated_at, processed_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                m.id(), m.eventType(), m.payload(), m.status().name(), m.attempts(),
                m.lastError(), created, created,
                m.processedAt() == null ? null : Timestamp.from(m.processedAt()));
    }

    @Override
    public List<OutboxMessage> fetchPending(int limit) {
        return jdbc.query("SELECT * FROM " + table
                        + " WHERE status = ? ORDER BY created_at ASC LIMIT ?",
                rowMapper, OutboxStatus.PENDING.name(), limit);
    }

    @Override
    public boolean claim(String id) {
        int updated = jdbc.update("UPDATE " + table
                        + " SET status = ?, updated_at = ? WHERE id = ? AND status = ?",
                OutboxStatus.PROCESSING.name(), Timestamp.from(Instant.now()), id,
                OutboxStatus.PENDING.name());
        return updated == 1;
    }

    @Override
    public void markProcessed(String id) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("UPDATE " + table
                        + " SET status = ?, processed_at = ?, updated_at = ? WHERE id = ?",
                OutboxStatus.PROCESSED.name(), now, now, id);
    }

    @Override
    public void markFailed(String id, String error, int maxAttempts) {
        String truncated = error == null ? null
                : error.substring(0, Math.min(error.length(), 2048));
        Timestamp now = Timestamp.from(Instant.now());
        // The CASE expressions must see the *current* attempts value. MySQL
        // evaluates SET assignments left-to-right and lets later expressions
        // observe already-updated columns, so the `attempts = attempts + 1`
        // increment is placed LAST to keep behaviour identical across H2,
        // PostgreSQL and MySQL. Once the next attempt reaches maxAttempts the
        // message is parked as FAILED, otherwise it returns to PENDING.
        jdbc.update("UPDATE " + table
                        + " SET last_error = ?, updated_at = ?,"
                        + " status = CASE WHEN attempts + 1 >= ? THEN ? ELSE ? END,"
                        + " processed_at = CASE WHEN attempts + 1 >= ? THEN ? ELSE processed_at END,"
                        + " attempts = attempts + 1"
                        + " WHERE id = ?",
                truncated, now, maxAttempts,
                OutboxStatus.FAILED.name(), OutboxStatus.PENDING.name(), maxAttempts,
                now, id);
    }

    @Override
    public int reclaimStale(Duration timeout) {
        Timestamp cutoff = Timestamp.from(Instant.now().minus(timeout));
        return jdbc.update("UPDATE " + table
                        + " SET status = ? WHERE status = ? AND updated_at < ?",
                OutboxStatus.PENDING.name(), OutboxStatus.PROCESSING.name(), cutoff);
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
