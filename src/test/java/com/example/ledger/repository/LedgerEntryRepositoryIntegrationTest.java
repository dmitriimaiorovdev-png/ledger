package com.example.ledger.repository;

import com.example.ledger.model.LedgerEntry;
import com.example.ledger.testsupport.RepositoryIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class LedgerEntryRepositoryIntegrationTest extends RepositoryIntegrationTest {

    @Test
    void filtersWalletPaginatesAndSortsByTimeThenIdDescending() {
        var wallet = wallet(user(), "CHF");
        var time = Instant.parse("2020-01-02T00:00:00Z");
        var first = entry(wallet, unique(), time);
        var second = entry(wallet, unique(), time);
        var lastId = entry(wallet, unique(), time.minusSeconds(1));
        entry(wallet(user(), "CHF"), unique(), time.plusSeconds(1));
        entityManager.clear();
        var firstPage = entries.findByWalletIdOrderByCreatedAtDescIdDesc(
                wallet.getId(), PageRequest.of(0, 2));
        assertThat(firstPage.getContent()).extracting(LedgerEntry::getId)
                .containsExactly(second.getId(), first.getId());
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        var next = entries.findByWalletIdOrderByCreatedAtDescIdDesc(
                wallet.getId(), PageRequest.of(1, 2));
        assertThat(next.getContent()).extracting(LedgerEntry::getId).containsExactly(lastId.getId());
        assertThat(entries.findByWalletIdOrderByCreatedAtDescIdDesc(-1L, PageRequest.of(0, 2))).isEmpty();
        assertThat(entries.findFirstByWalletIdOrderByIdDesc(wallet.getId())).get()
                .extracting("id").isEqualTo(lastId.getId());
        assertThat(entries.findFirstByWalletIdOrderByIdDesc(-1L)).isEmpty();
        assertThat(entries.existsByWalletIdAndOperationKey(wallet.getId(), first.getOperationKey())).isTrue();
        assertThat(entries.existsByWalletIdAndOperationKey(-1L, first.getOperationKey())).isFalse();
        assertThat(entries.existsByWalletIdAndOperationKey(wallet.getId(), unique())).isFalse();
    }

    @Test
    void operationKeyIsUniqueWithinWalletButCanBeReusedInAnotherWallet() {
        var firstWallet = wallet(user(), "CHF");
        var secondWallet = wallet(user(), "CHF");
        String key = unique();
        entry(firstWallet, key, Instant.now());
        entry(secondWallet, key, Instant.now());
        assertThatThrownBy(() -> entry(firstWallet, key, Instant.now()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("uk_ledger_entries_wallet_operation");
    }
}
