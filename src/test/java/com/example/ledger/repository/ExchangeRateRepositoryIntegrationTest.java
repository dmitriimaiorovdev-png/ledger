package com.example.ledger.repository;

import com.example.ledger.model.ExchangeRate;
import com.example.ledger.testsupport.RepositoryIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateRepositoryIntegrationTest extends RepositoryIntegrationTest {

    @Test
    void filtersCurrencyPairAndSortsQuotes() {
        rate("CHF", "USD", "1.12");
        var eur = rate("CHF", "EUR", "1.04");
        rate("USD", "EUR", "0.92");
        entityManager.clear();
        assertThat(rates.findByBaseCurrencyAndQuoteCurrency("CHF", "EUR")).get()
                .extracting("id").isEqualTo(eur.getId());
        assertThat(rates.findByBaseCurrencyAndQuoteCurrency("EUR", "CHF")).isEmpty();
        assertThat(rates.findByBaseCurrencyOrderByQuoteCurrency("CHF"))
                .extracting(ExchangeRate::getQuoteCurrency).containsExactly("EUR", "USD");
        assertThat(rates.findByBaseCurrencyOrderByQuoteCurrency("ZZZ")).isEmpty();
    }

    private ExchangeRate rate(String base, String quote, String value) {
        var rate = new ExchangeRate(base, quote);
        rate.setRate(new BigDecimal(value));
        return rates.saveAndFlush(rate);
    }
}
