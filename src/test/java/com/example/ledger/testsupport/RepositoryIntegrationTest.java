package com.example.ledger.testsupport;

import com.example.ledger.model.*;
import com.example.ledger.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@SpringBootTest(classes = RepositoryTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(initializers = LedgerTestDatabaseGuard.class)
@TestPropertySource("classpath:application-repository-test.properties")
@Transactional
public abstract class RepositoryIntegrationTest {
    @Autowired protected UserRepository users;
    @Autowired protected WalletRepository wallets;
    @Autowired protected TransferRepository transfers;
    @Autowired protected PaymentTransactionRepository payments;
    @Autowired protected LedgerEntryRepository entries;
    @Autowired protected ExchangeRateRepository rates;
    @Autowired protected OutboxRepository outbox;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected PlatformTransactionManager transactionManager;
    @PersistenceContext protected EntityManager entityManager;

    protected String unique() {
        return UUID.randomUUID().toString();
    }

    protected User user() {
        return users.saveAndFlush(new User(unique() + "@example.invalid", null, "Test", "User"));
    }

    protected Wallet wallet(User owner, String currency) {
        return wallets.saveAndFlush(new Wallet(owner, currency));
    }

    protected Transfer transfer(User owner, Wallet from, Wallet to, String key, Instant createdAt) {
        Transfer transfer = new Transfer(owner, from, to, "CHF", BigDecimal.TEN, key);
        transfer.setCreatedAt(createdAt);
        return transfers.saveAndFlush(transfer);
    }

    protected PaymentTransaction payment(User owner, Wallet wallet, String key, Instant createdAt) {
        PaymentTransaction payment = new PaymentTransaction(
                owner, wallet, PaymentType.TOP_UP, BigDecimal.TEN, "CHF", key);
        payment.setCreatedAt(createdAt);
        return payments.saveAndFlush(payment);
    }

    protected LedgerEntry entry(Wallet wallet, String key, Instant createdAt) {
        LedgerEntry entry = new LedgerEntry(wallet, (Transfer) null, EntryType.CREDIT,
                BigDecimal.ONE, BigDecimal.ONE, wallet.getCurrency(), key);
        entry.setCreatedAt(createdAt);
        return entries.saveAndFlush(entry);
    }

    protected OutboxEvent event(String type, OutboxStatus status, Instant availableAt) {
        OutboxEvent event = new OutboxEvent("RepositoryTest", unique(), type, "{}");
        event.setStatus(status);
        event.setAvailableAt(availableAt);
        return outbox.saveAndFlush(event);
    }
}
