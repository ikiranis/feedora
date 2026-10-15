package eu.apps4net.feedora.configurations;

import eu.apps4net.feedora.models.LogLevel;
import eu.apps4net.feedora.models.LogType;
import eu.apps4net.feedora.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnumCheckConstraintCleanupTest extends ApiTestBase {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EnumCheckConstraintCleanup cleanup;

    private int checkConstraintCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SYS.SYSCONSTRAINTS c JOIN SYS.SYSTABLES t ON c.TABLEID = t.TABLEID " +
                "WHERE c.TYPE = 'C' AND t.TABLENAME IN ('LOG_ENTRY', 'USERS')", Integer.class);
    }

    @Test
    void theGeneratedConstraintsAreDroppedOnStartup() {
        assertThat(checkConstraintCount()).isZero();
    }

    @Test
    void aStaleEnumConstraintNoLongerBlocksNewValues() {
        // Like a database created before FEED_PARSING existed
        jdbcTemplate.execute("ALTER TABLE LOG_ENTRY ADD CONSTRAINT STALE_TYPE CHECK (type IN ('FEED_FETCH','FEED_PARSE'))");
        assertThatThrownBy(() -> saveLog(LogType.FEED_PARSING)).isNotNull();

        cleanup.dropEnumCheckConstraints();

        assertThat(checkConstraintCount()).isZero();
        saveLog(LogType.FEED_PARSING);
        assertThat(logEntryRepository.findAll()).extracting(entry -> entry.getType()).contains(LogType.FEED_PARSING);
    }

    // createLog saves a FEED_FETCH entry, which the stale constraint allows, then it's changed to the given type
    private void saveLog(LogType type) {
        var entry = createLog(user, null, null, LogLevel.INFO, LocalDateTime.now());
        entry.setType(type);
        logEntryRepository.save(entry);
    }
}
