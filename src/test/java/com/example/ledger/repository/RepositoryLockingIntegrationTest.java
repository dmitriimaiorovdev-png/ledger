package com.example.ledger.repository;

import com.example.ledger.model.OutboxEvent;
import com.example.ledger.model.OutboxStatus;
import com.example.ledger.model.Role;
import com.example.ledger.testsupport.RepositoryIntegrationTest;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RepositoryLockingIntegrationTest extends RepositoryIntegrationTest {

    @Test
    void walletFindByIdHoldsPessimisticWriteLock() {
        withFixture(f -> verifyLock("wallets", f.walletId(),
                () -> wallets.findByIdForUpdate(f.walletId()).orElseThrow()));
    }

    @Test
    void systemWalletLookupHoldsPessimisticWriteLock() {
        withFixture(f -> verifyLock("wallets", f.walletId(),
                () -> wallets.findSystemWallet("CHF").orElseThrow()));
    }

    @Test
    void transferFindByIdHoldsPessimisticWriteLock() {
        withFixture(f -> verifyLock("transfers", f.transferId(),
                () -> transfers.findByIdForUpdate(f.transferId()).orElseThrow()));
    }

    @Test
    void paymentFindByIdHoldsPessimisticWriteLock() {
        withFixture(f -> verifyLock("payment_transactions", f.paymentId(),
                () -> payments.findByIdForUpdate(f.paymentId()).orElseThrow()));
    }

    @Test
    void skipLockedUsesIndependentTransactionsAndReleasesLocks() {
        Instant now = Instant.parse("2020-01-02T00:00:00Z");
        List<Long> ids = newTransaction().execute(status -> List.of(
                event(unique(), OutboxStatus.PENDING, now).getId(),
                event(unique(), OutboxStatus.PENDING, now).getId()));
        assertThat(ids).hasSize(2);
        try {
            newTransaction().executeWithoutResult(holder -> {
                setTimeouts();
                assertThat(outbox.lockNextBatch(now, 1)).extracting(OutboxEvent::getId)
                        .containsExactly(ids.get(0));
                newTransaction().executeWithoutResult(worker -> {
                    setTimeouts();
                    assertThat(outbox.lockNextBatch(now, 2)).extracting(OutboxEvent::getId)
                            .containsExactly(ids.get(1));
                    assertRowLocked("outbox_events", ids.get(0));
                    assertRowLocked("outbox_events", ids.get(1));
                    worker.setRollbackOnly();
                });
                holder.setRollbackOnly();
            });
            newTransaction().executeWithoutResult(status -> {
                setTimeouts();
                assertThat(outbox.lockNextBatch(now, 2)).extracting(OutboxEvent::getId)
                        .containsExactlyElementsOf(ids);
                status.setRollbackOnly();
            });
        } finally {
            newTransaction().executeWithoutResult(status ->
                    ids.forEach(id -> jdbc.update("DELETE FROM outbox_events WHERE id = ?", id)));
        }
    }

    private void verifyLock(String table, Long id, Supplier<Object> lookup) {
        newTransaction().executeWithoutResult(holder -> {
            setTimeouts();
            Object entity = lookup.get();
            assertThat(entityManager.getLockMode(entity)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
            assertRowLocked(table, id);
            holder.setRollbackOnly();
        });
        newTransaction().executeWithoutResult(status -> {
            setTimeouts();
            assertThat(jdbc.queryForObject(
                    "SELECT id FROM " + table + " WHERE id = ? FOR UPDATE NOWAIT", Long.class, id))
                    .isEqualTo(id);
            status.setRollbackOnly();
        });
    }

    private void assertRowLocked(String table, Long id) {
        // REQUIRES_NEW suspends the holder and uses a separate connection, without timing or sleeps.
        Throwable failure = catchThrowable(() -> newTransaction().executeWithoutResult(status -> {
            setTimeouts();
            jdbc.queryForObject("SELECT id FROM " + table + " WHERE id = ? FOR UPDATE NOWAIT",
                    Long.class, id);
            status.setRollbackOnly();
        }));
        // JdbcTemplate's default translator does not classify PostgreSQL SQLSTATE 55P03.
        assertThat(failure).isInstanceOf(UncategorizedSQLException.class);
        Throwable cause = ((UncategorizedSQLException) failure).getSQLException();
        assertThat(cause).isInstanceOf(SQLException.class);
        assertThat(((SQLException) cause).getSQLState()).isEqualTo("55P03");
    }

    private void withFixture(java.util.function.Consumer<Fixture> test) {
        Fixture fixture = newTransaction().execute(status -> {
            var owner = user();
            owner.setRole(Role.SYSTEM);
            users.saveAndFlush(owner);
            var from = wallet(owner, "CHF");
            var to = wallet(owner, "EUR");
            var transfer = transfer(owner, from, to, unique(), Instant.now());
            var payment = payment(owner, from, unique(), Instant.now());
            return new Fixture(owner.getId(), from.getId(), to.getId(),
                    transfer.getId(), payment.getId());
        });
        assertThat(fixture).isNotNull();
        try {
            test.accept(fixture);
        } finally {
            newTransaction().executeWithoutResult(status -> {
                jdbc.update("DELETE FROM payment_transactions WHERE id = ?", fixture.paymentId());
                jdbc.update("DELETE FROM transfers WHERE id = ?", fixture.transferId());
                jdbc.update("DELETE FROM wallets WHERE id IN (?, ?)",
                        fixture.walletId(), fixture.otherWalletId());
                jdbc.update("DELETE FROM users WHERE id = ?", fixture.userId());
            });
        }
    }

    private TransactionTemplate newTransaction() {
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transaction.setTimeout(5);
        return transaction;
    }

    private void setTimeouts() {
        jdbc.execute("SET LOCAL statement_timeout = '3s'");
        jdbc.execute("SET LOCAL lock_timeout = '500ms'");
    }

    private record Fixture(Long userId, Long walletId, Long otherWalletId,
                           Long transferId, Long paymentId) {
    }
}
