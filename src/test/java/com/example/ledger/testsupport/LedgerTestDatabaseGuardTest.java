package com.example.ledger.testsupport;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LedgerTestDatabaseGuardTest {

    @Test
    void acceptsOnlyExplicitTestDatabaseWithoutConnectionParameters() {
        assertDoesNotThrow(() -> LedgerTestDatabaseGuard.requireTestDatabase(
                "jdbc:postgresql://localhost:5432/ledger_test"));
        assertDoesNotThrow(() -> LedgerTestDatabaseGuard.requireTestDatabase(
                "jdbc:postgresql://[::1]:5432/ledger_test"));
        for (String url : new String[] {
                "", "jdbc:postgresql:ledger_test", "jdbc:postgresql://localhost/ledger",
                "jdbc:postgresql://localhost/ledger_test_other",
                "jdbc:postgresql://localhost/ledger_test/",
                "jdbc:postgresql://localhost/ledger%5ftest",
                "jdbc:postgresql://localhost/ledger_test?PGDBNAME=ledger",
                "jdbc:postgresql://localhost/ledger_test?user=secret",
                "jdbc:postgresql://user:secret@localhost/ledger_test",
                "jdbc:postgresql://localhost/ledger_test#ledger",
                "jdbc:postgresql://localhost,other/ledger_test",
                "jdbc:h2:mem:ledger_test"
        }) {
            assertThrows(IllegalStateException.class,
                    () -> LedgerTestDatabaseGuard.requireTestDatabase(url));
        }
        assertThrows(IllegalStateException.class,
                () -> LedgerTestDatabaseGuard.requireTestDatabase(null));
    }

    @Test
    void rejectsProductionUrlBeforeContextRefreshOrBeanCreation() {
        try (var context = new GenericApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "unsafe", Map.of("spring.datasource.url", "jdbc:postgresql://localhost/ledger")));
            assertThrows(IllegalStateException.class,
                    () -> new LedgerTestDatabaseGuard().initialize(context));
            assertFalse(context.isActive());
            assertFalse(context.getBeanFactory().containsSingleton("ledgerTestDatabaseSettings"));
        }
    }

    @Test
    void rejectsSeparateUnsafeFlywayUrlBeforeContextRefresh() {
        try (var context = new GenericApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "unsafe", Map.of(
                            "spring.datasource.url", "jdbc:postgresql://localhost/ledger_test",
                            "spring.flyway.url", "jdbc:postgresql://localhost/ledger")));
            assertThrows(IllegalStateException.class,
                    () -> new LedgerTestDatabaseGuard().initialize(context));
            assertFalse(context.isActive());
        }
    }
}
