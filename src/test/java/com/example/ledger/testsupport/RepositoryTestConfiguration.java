package com.example.ledger.testsupport;

import com.example.ledger.model.User;
import com.example.ledger.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(basePackageClasses = UserRepository.class)
@EnableTransactionManagement
public class RepositoryTestConfiguration {

    @Bean
    DataSource dataSource(LedgerTestDatabaseGuard.Settings settings) {
        LedgerTestDatabaseGuard.requireTestDatabase(settings.url);
        DriverManagerDataSource source = new DriverManagerDataSource() {
            @Override
            protected Connection getConnectionFromDriverManager(String url, java.util.Properties info)
                    throws SQLException {
                LedgerTestDatabaseGuard.requireTestDatabase(url);
                Connection connection = super.getConnectionFromDriverManager(url, info);
                try {
                    if (!"ledger_test".equals(connection.getCatalog())) {
                        throw new SQLException("Connected database must be ledger_test");
                    }
                    connection.setSchema(settings.schema);
                    return connection;
                } catch (SQLException | RuntimeException ex) {
                    connection.close();
                    throw ex;
                }
            }
        };
        source.setDriverClassName("org.postgresql.Driver");
        source.setUrl(settings.url);
        source.setUsername(settings.username);
        source.setPassword(settings.password);
        return source;
    }

    @Bean(initMethod = "migrate")
    Flyway flyway(DataSource dataSource, LedgerTestDatabaseGuard.Settings settings) {
        return Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration")
                .schemas(settings.schema).defaultSchema(settings.schema)
                .createSchemas(true).cleanDisabled(true).baselineOnMigrate(false)
                .load();
    }

    @Bean
    @DependsOn("flyway")
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan(User.class.getPackageName());
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of(
                "hibernate.hbm2ddl.auto", "validate",
                "hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl"));
        return factory;
    }

    @Bean
    JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
