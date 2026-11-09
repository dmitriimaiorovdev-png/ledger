package com.example.ledger.repository;

import com.example.ledger.model.Role;
import com.example.ledger.model.Wallet;
import com.example.ledger.testsupport.RepositoryIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WalletRepositoryIntegrationTest extends RepositoryIntegrationTest {

    @Test
    void filtersOwnerAndCurrencyAndSortsIds() {
        var owner = user();
        var other = user();
        var first = wallet(owner, "CHF");
        var second = wallet(owner, "EUR");
        var unrelated = wallet(other, "CHF");
        entityManager.clear();
        assertThat(wallets.findByUserIdOrderByIdAsc(owner.getId()))
                .extracting(Wallet::getId).containsExactly(first.getId(), second.getId());
        assertThat(wallets.findByIdAndUserId(first.getId(), owner.getId())).isPresent();
        assertThat(wallets.findByIdAndUserId(unrelated.getId(), owner.getId())).isEmpty();
        assertThat(wallets.findByUserIdOrderByIdAsc(-1L)).isEmpty();
        assertThat(wallets.existsByUserIdAndCurrency(owner.getId(), "CHF")).isTrue();
        assertThat(wallets.existsByUserIdAndCurrency(owner.getId(), "USD")).isFalse();
        assertThat(wallets.existsByUserIdAndCurrency(-1L, "CHF")).isFalse();
    }

    @Test
    void findsSystemRoleInRequestedCurrency() {
        var system = user();
        system.setRole(Role.SYSTEM);
        users.saveAndFlush(system);
        var expected = wallet(system, "CHF");
        wallet(system, "EUR");
        wallet(user(), "CHF");
        entityManager.clear();
        assertThat(wallets.findSystemWallet("CHF")).get()
                .extracting("id").isEqualTo(expected.getId());
        assertThat(wallets.findSystemWallet("USD")).isEmpty();
    }

    @Test
    void missingLockedWalletReturnsEmpty() {
        assertThat(wallets.findByIdForUpdate(-1L)).isEmpty();
    }
}
