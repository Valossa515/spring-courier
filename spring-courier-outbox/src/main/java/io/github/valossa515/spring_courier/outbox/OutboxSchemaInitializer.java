package io.github.valossa515.spring_courier.outbox;

import java.util.Locale;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Creates the outbox table at startup when
 * {@code spring.courier.outbox.auto-create-schema=true}.
 *
 * <p>Convenience for development only. Production deployments should manage the
 * schema with the bundled DDL through Flyway/Liquibase.
 *
 * <p>The DDL is adapted to the detected database (H2, PostgreSQL, MySQL/MariaDB)
 * so the dev convenience also works on those engines; unknown engines fall back
 * to portable types and a best-effort index. The table name is the only value
 * interpolated into the DDL and is validated against a strict
 * {@code [A-Za-z0-9_]+} allow-list, so {@code java:S2077} (dynamic SQL) is
 * suppressed for this class.
 */
@SuppressWarnings("java:S2077")
public class OutboxSchemaInitializer implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxSchemaInitializer.class);
    private static final Pattern SAFE_TABLE = Pattern.compile("[A-Za-z0-9_]+");

    private final JdbcTemplate jdbc;
    private final String table;

    public OutboxSchemaInitializer(DataSource dataSource, String tableName) {
        if (!SAFE_TABLE.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Illegal outbox table name: " + tableName);
        }
        this.jdbc = new JdbcTemplate(dataSource);
        this.table = tableName;
    }

    @Override
    public void afterPropertiesSet() {
        String product = detectProduct();
        String payloadType = payloadTypeFor(product);

        jdbc.execute("CREATE TABLE IF NOT EXISTS " + table + " ("
                + "id VARCHAR(36) NOT NULL, "
                + "event_type VARCHAR(512) NOT NULL, "
                + "payload " + payloadType + " NOT NULL, "
                + "status VARCHAR(20) NOT NULL, "
                + "attempts INTEGER NOT NULL, "
                + "last_error VARCHAR(2048), "
                + "created_at TIMESTAMP NOT NULL, "
                + "updated_at TIMESTAMP NOT NULL, "
                + "processed_at TIMESTAMP, "
                + "CONSTRAINT pk_" + table + " PRIMARY KEY (id))");

        createStatusIndex(product);
        LOG.info("Ensured outbox table '{}' exists for database '{}' (auto-create-schema=true)",
                table, product);
    }

    private String detectProduct() {
        String name = jdbc.execute((ConnectionCallback<String>) conn ->
                conn.getMetaData().getDatabaseProductName());
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    private static String payloadTypeFor(String product) {
        if (product.contains("postgresql")) {
            return "TEXT";
        }
        if (product.contains("mysql") || product.contains("mariadb")) {
            return "LONGTEXT";
        }
        // H2 and unknown engines: CLOB is widely understood.
        return "CLOB";
    }

    private void createStatusIndex(String product) {
        String indexName = "idx_" + table + "_status";
        // MySQL/MariaDB do not support "CREATE INDEX IF NOT EXISTS", so attempt
        // the plain statement and tolerate the "already exists" error.
        if (product.contains("mysql") || product.contains("mariadb")) {
            try {
                jdbc.execute("CREATE INDEX " + indexName + " ON " + table + " (status, created_at)");
            } catch (DataAccessException alreadyExists) {
                LOG.debug("Outbox index '{}' likely already exists", indexName, alreadyExists);
            }
        } else {
            jdbc.execute("CREATE INDEX IF NOT EXISTS " + indexName + " ON "
                    + table + " (status, created_at)");
        }
    }
}
