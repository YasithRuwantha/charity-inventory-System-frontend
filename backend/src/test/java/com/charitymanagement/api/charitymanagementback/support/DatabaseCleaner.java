package com.charitymanagement.api.charitymanagementback.support;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Truncates every table between tests so counts and dashboard figures are deterministic.
 *
 * <p>Test-scope only: this lives under {@code src/test} and is wired against the in-memory H2
 * schema. It is never on the classpath of a running application.
 */
@Component
@RequiredArgsConstructor
public class DatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;

    private List<String> tables;

    @PostConstruct
    void discoverTables() {
        tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'PUBLIC' "
                        + "and table_type = 'BASE TABLE'",
                String.class);
    }

    @Transactional
    public void clean() {
        jdbcTemplate.execute("set referential_integrity false");
        try {
            for (String table : tables) {
                jdbcTemplate.execute("truncate table \"" + table + "\" restart identity");
            }
        } finally {
            jdbcTemplate.execute("set referential_integrity true");
        }
    }
}
