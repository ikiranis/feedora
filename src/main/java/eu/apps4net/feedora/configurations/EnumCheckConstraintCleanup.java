package eu.apps4net.feedora.configurations;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * Hibernate adds a CHECK constraint with the allowed values to every enum column, but ddl-auto=update
 * never updates it. Adding a value to an enum (e.g. a new LogType) then makes every insert with that value fail.
 * Drops those constraints on the tables with enum columns, so the enums can grow freely.
 * Runs on every startup; once dropped, ddl-auto=update doesn't add them back.
 */
@Component
public class EnumCheckConstraintCleanup {
    // Tables with @Enumerated columns, whose only CHECK constraints are the generated enum ones
    private static final List<String> TABLES = List.of("LOG_ENTRY", "USERS");

    private final JdbcTemplate jdbcTemplate;

    // The EntityManagerFactory makes sure the schema is created/updated before this runs
    public EnumCheckConstraintCleanup(JdbcTemplate jdbcTemplate, EntityManagerFactory entityManagerFactory) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void dropEnumCheckConstraints() {
        try {
            String database = jdbcTemplate.execute((Connection connection) -> connection.getMetaData().getDatabaseProductName());
            if (database == null || !database.contains("Derby")) {
                return;
            }

            List<Map<String, Object>> constraints = jdbcTemplate.queryForList(
                    "SELECT t.TABLENAME, c.CONSTRAINTNAME FROM SYS.SYSCONSTRAINTS c " +
                    "JOIN SYS.SYSTABLES t ON c.TABLEID = t.TABLEID " +
                    "JOIN SYS.SYSSCHEMAS s ON c.SCHEMAID = s.SCHEMAID " +
                    "WHERE c.TYPE = 'C' AND s.SCHEMANAME = CURRENT SCHEMA " +
                    "AND t.TABLENAME IN (" + String.join(",", TABLES.stream().map(table -> "'" + table + "'").toList()) + ")");

            for (Map<String, Object> constraint : constraints) {
                jdbcTemplate.execute("ALTER TABLE " + constraint.get("TABLENAME") +
                        " DROP CONSTRAINT \"" + constraint.get("CONSTRAINTNAME") + "\"");
            }
        } catch (Exception e) {
            System.err.println("[Feedora] Could not drop the enum check constraints: " + e.getMessage());
        }
    }
}
