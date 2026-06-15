package io.github.valossa515.spring_courier.outbox;

import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Creates the outbox table at startup when
 * {@code spring.courier.outbox.auto-create-schema=true}.
 *
 * <p>Convenience for development only. Production deployments should manage the
 * schema with the bundled DDL through Flyway/Liquibase.
 *
 * <p>The table name is the only value interpolated into the DDL and is
 * validated against a strict {@code [A-Za-z0-9_]+} allow-list, so
 * {@code java:S2077} (dynamic SQL) is suppressed for this class.
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
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + table + " ("
                + "id VARCHAR(36) NOT NULL, "
                + "event_type VARCHAR(512) NOT NULL, "
                + "payload CLOB NOT NULL, "
                + "status VARCHAR(20) NOT NULL, "
                + "attempts INTEGER NOT NULL, "
                + "last_error VARCHAR(2048), "
                + "created_at TIMESTAMP NOT NULL, "
                + "updated_at TIMESTAMP NOT NULL, "
                + "processed_at TIMESTAMP, "
                + "CONSTRAINT pk_" + table + " PRIMARY KEY (id))");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_" + table + "_status ON "
                + table + " (status, created_at)");
        LOG.info("Ensured outbox table '{}' exists (auto-create-schema=true)", table);
    }
}
