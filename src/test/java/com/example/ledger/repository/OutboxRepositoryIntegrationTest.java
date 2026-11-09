package com.example.ledger.repository;

import com.example.ledger.model.OutboxEvent;
import com.example.ledger.model.OutboxStatus;
import com.example.ledger.testsupport.RepositoryIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class OutboxRepositoryIntegrationTest extends RepositoryIntegrationTest {

    @Test
    void selectsOnlyAvailablePendingEventsInOrderWithLimit() {
        Instant now = Instant.parse("2020-01-02T00:00:00Z");
        String type = unique();
        var first = event(type, OutboxStatus.PENDING, now.minusSeconds(1));
        var second = event(type, OutboxStatus.PENDING, now);
        var third = event(type, OutboxStatus.PENDING, now);
        event(type, OutboxStatus.PENDING, now.plusNanos(1000));
        event(type, OutboxStatus.PUBLISHED, now.minusSeconds(2));
        event(type, OutboxStatus.FAILED, now.minusSeconds(2));
        entityManager.clear();
        assertThat(outbox.lockNextBatch(now, 2)).extracting(OutboxEvent::getId)
                .containsExactly(first.getId(), second.getId());
        assertThat(outbox.lockNextBatch(now, 10)).extracting(OutboxEvent::getId)
                .containsExactly(first.getId(), second.getId(), third.getId());
        assertThat(outbox.lockNextBatch(now, 0)).isEmpty();
        assertThat(outbox.lockNextBatch(now.minusSeconds(10), 10)).isEmpty();
    }

    @Test
    void countsEachStatusAndFindsLatestEventOfRequestedType() {
        String type = unique();
        var first = event(type, OutboxStatus.PENDING, Instant.now());
        var last = event(type, OutboxStatus.PUBLISHED, Instant.now());
        event(unique(), OutboxStatus.FAILED, Instant.now());
        entityManager.clear();
        assertThat(outbox.countByStatus(OutboxStatus.PENDING)).isEqualTo(1);
        assertThat(outbox.countByStatus(OutboxStatus.PUBLISHED)).isEqualTo(1);
        assertThat(outbox.countByStatus(OutboxStatus.FAILED)).isEqualTo(1);
        assertThat(outbox.findFirstByEventTypeOrderByIdDesc(type)).get()
                .extracting("id").isEqualTo(last.getId());
        assertThat(outbox.findFirstByEventTypeOrderByIdDesc(unique())).isEmpty();
        assertThat(last.getId()).isGreaterThan(first.getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void refusesBatchLockOutsideTransaction() {
        assertThatThrownBy(() -> outbox.lockNextBatch(Instant.now(), 1))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
