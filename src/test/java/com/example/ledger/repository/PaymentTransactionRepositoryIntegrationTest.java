package com.example.ledger.repository;

import com.example.ledger.model.PaymentTransaction;
import com.example.ledger.testsupport.RepositoryIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class PaymentTransactionRepositoryIntegrationTest extends RepositoryIntegrationTest {

    @Test
    void findsOwnedPaymentsAndIdempotencyKeyAndSortsNewestFirst() {
        var owner = user();
        var wallet = wallet(owner, "CHF");
        var older = payment(owner, wallet, unique(), Instant.parse("2020-01-01T00:00:00Z"));
        var newer = payment(owner, wallet, unique(), Instant.parse("2020-01-02T00:00:00Z"));
        var other = user();
        payment(other, wallet(other, "CHF"), unique(), Instant.parse("2020-01-03T00:00:00Z"));
        entityManager.clear();
        assertThat(payments.findByIdAndUserId(older.getId(), owner.getId())).isPresent();
        assertThat(payments.findByIdAndUserId(older.getId(), other.getId())).isEmpty();
        assertThat(payments.findByIdempotencyKey(newer.getIdempotencyKey())).get()
                .extracting("id").isEqualTo(newer.getId());
        assertThat(payments.findByIdempotencyKey(unique())).isEmpty();
        assertThat(payments.findByUserIdOrderByCreatedAtDesc(owner.getId()))
                .extracting(PaymentTransaction::getId).containsExactly(newer.getId(), older.getId());
        assertThat(payments.findByUserIdOrderByCreatedAtDesc(-1L)).isEmpty();
        assertThat(payments.findByIdForUpdate(-1L)).isEmpty();
    }

    @Test
    void rejectsDuplicateIdempotencyKey() {
        var owner = user();
        var wallet = wallet(owner, "CHF");
        String key = unique();
        payment(owner, wallet, key, Instant.now());
        assertThatThrownBy(() -> payment(owner, wallet, key, Instant.now()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("uk_payment_transactions_idempotency_key");
    }
}
