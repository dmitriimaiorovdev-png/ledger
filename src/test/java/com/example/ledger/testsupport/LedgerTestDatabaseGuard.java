package com.example.ledger.testsupport;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.util.UUID;

public final class LedgerTestDatabaseGuard
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        Environment env = context.getEnvironment();
        String url = env.getRequiredProperty("spring.datasource.url");
        requireTestDatabase(url);
        for (String key : new String[] {
                "spring.flyway.url", "spring.datasource.hikari.jdbc-url",
                "spring.jpa.properties.jakarta.persistence.jdbc.url",
                "spring.jpa.properties.hibernate.connection.url"
        }) {
            String alternate = env.getProperty(key);
            if (alternate != null && !alternate.isBlank()) {
                requireTestDatabase(alternate);
                if (!url.equals(alternate)) {
                    throw new IllegalStateException("Conflicting test database URLs: " + key);
                }
            }
        }
        String username = env.getRequiredProperty("spring.datasource.username");
        String password = env.getRequiredProperty("spring.datasource.password");
        if (username.isBlank()) {
            throw new IllegalStateException("LEDGER_TEST_DB_USERNAME must be supplied");
        }
        context.getBeanFactory().registerSingleton("ledgerTestDatabaseSettings",
                new Settings(url, username, password,
                        "repository_it_" + UUID.randomUUID().toString().replace("-", "")));
    }

    public static void requireTestDatabase(String url) {
        try {
            if (url == null || !url.startsWith("jdbc:postgresql://")) {
                throw new IllegalArgumentException();
            }
            URI uri = URI.create(url.substring("jdbc:".length()));
            if (!"postgresql".equals(uri.getScheme()) || uri.getHost() == null
                    || !"/ledger_test".equals(uri.getRawPath())
                    || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || uri.getPort() == 0 || uri.getPort() > 65535) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            // Do not include the supplied URL: it might contain credentials.
            throw new IllegalStateException(
                    "Tests require jdbc:postgresql://host[:port]/ledger_test without URL parameters");
        }
    }

    public static final class Settings {
        final String url;
        final String username;
        final String password;
        final String schema;

        Settings(String url, String username, String password, String schema) {
            this.url = url;
            this.username = username;
            this.password = password;
            this.schema = schema;
        }
    }
}
