package com.example.ledger.repository;

import com.example.ledger.model.Transfer;
import com.example.ledger.testsupport.RepositoryIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class TransferRepositoryIntegrationTest extends RepositoryIntegrationTest {

    @Test
    void findsOwnedTransfersAndIdempotencyKeyAndSortsNewestFirst() {
        var owner = user();
        var from = wallet(owner, "CHF");
        var to = wallet(user(), "CHF");
        var older = transfer(owner, from, to, unique(), Instant.parse("2020-01-01T00:00:00Z"));
        var newer = transfer(owner, from, to, unique(), Instant.parse("2020-01-02T00:00:00Z"));
        var other = user();
        transfer(other, to, from, unique(), Instant.parse("2020-01-03T00:00:00Z"));
        entityManager.clear();
        assertThat(transfers.findByIdAndUserId(older.getId(), owner.getId())).isPresent();
        assertThat(transfers.findByIdAndUserId(older.getId(), other.getId())).isEmpty();
        assertThat(transfers.findByIdempotencyKey(newer.getIdempotencyKey())).get()
                .extracting("id").isEqualTo(newer.getId());
        assertThat(transfers.findByIdempotencyKey(unique())).isEmpty();
        assertThat(transfers.findByUserIdOrderByCreatedAtDesc(owner.getId()))
                .extracting(Transfer::getId).containsExactly(newer.getId(), older.getId());
        assertThat(transfers.findByUserIdOrderByCreatedAtDesc(-1L)).isEmpty();
        assertThat(transfers.findByIdForUpdate(-1L)).isEmpty();
    }

    @Test
    void rejectsDuplicateIdempotencyKey() {
        var owner = user();
        var from = wallet(owner, "CHF");
        var to = wallet(user(), "CHF");
        String key = unique();
        transfer(owner, from, to, key, Instant.now());
        assertThatThrownBy(() -> transfer(owner, from, to, key, Instant.now()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("uk_transfers_idempotency_key");
    }
}
