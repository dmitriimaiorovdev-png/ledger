package com.example.ledger.repository;

import com.example.ledger.model.OutboxEvent;
import com.example.ledger.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    @Transactional(propagation = Propagation.MANDATORY)
    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE status = 'PENDING' AND available_at <= :now
            ORDER BY available_at, id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch(@Param("now") Instant now, @Param("limit") int limit);

    long countByStatus(OutboxStatus status);

    Optional<OutboxEvent> findFirstByEventTypeOrderByIdDesc(String eventType);
}
