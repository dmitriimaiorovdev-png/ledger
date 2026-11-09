package com.example.ledger.dto;

import com.example.ledger.model.ExchangeRate;
import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeRateDto(String baseCurrency, String quoteCurrency,
                              BigDecimal rate, Instant updatedAt) {
    public static ExchangeRateDto from(ExchangeRate rate) {
        return new ExchangeRateDto(rate.getBaseCurrency(), rate.getQuoteCurrency(),
                rate.getRate(), rate.getUpdatedAt());
    }
}
