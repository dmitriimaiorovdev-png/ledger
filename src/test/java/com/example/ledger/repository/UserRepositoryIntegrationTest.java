package com.example.ledger.repository;

import com.example.ledger.testsupport.RepositoryIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryIntegrationTest extends RepositoryIntegrationTest {

    @Test
    void findsAndChecksEmailIgnoringCase() {
        var user = user();
        entityManager.clear();
        String upper = user.getEmail().toUpperCase(Locale.ROOT);
        assertThat(users.findByEmailIgnoreCase(upper)).get()
                .extracting("id").isEqualTo(user.getId());
        assertThat(users.existsByEmailIgnoreCase(upper)).isTrue();
        assertThat(users.findByEmailIgnoreCase(unique() + "@example.invalid")).isEmpty();
        assertThat(users.existsByEmailIgnoreCase(unique() + "@example.invalid")).isFalse();
    }
}
