package com.finwise;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest {

    @Test
    @DisplayName("Verify Flyway V1 schema, V2 seed categories, and V3 sample development data execute cleanly")
    void verifyFlywayMigrations() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:flyway_test_db;DB_CLOSE_DELAY=-1;MODE=MySQL",
                "sa",
                ""
        );

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        var migrationResult = flyway.migrate();
        assertThat(migrationResult.migrationsExecuted).isGreaterThanOrEqualTo(3);

        // Verify all expected tables exist
        Set<String> expectedTables = Set.of(
                "ROLES", "USERS", "USER_ROLES", "REFRESH_TOKENS",
                "ACCOUNTS", "CATEGORIES", "RECURRING_TRANSACTIONS", "TRANSACTIONS",
                "TRANSFERS", "BUDGETS", "BUDGET_CATEGORIES", "SAVINGS_GOALS",
                "GOAL_CONTRIBUTIONS", "NOTIFICATIONS", "AUDIT_LOGS"
        );

        Set<String> actualTables = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                actualTables.add(rs.getString("TABLE_NAME").toUpperCase());
            }
        }

        assertThat(actualTables).containsAll(expectedTables);

        // Verify sample development data from V3
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Check mock users
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(2);
            }

            // Check mock accounts
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM accounts")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(4);
            }

            // Check savings goals
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM savings_goals")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(2);
            }

            // Check recurring transactions
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM recurring_transactions")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(3);
            }

            // Check notifications
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM notifications")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(3);
            }
        }
    }
}
