package com.example.ledger.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ExchangeRateUpdateRequest(
        @NotNull @Positive BigDecimal rate,
        @Pattern(regexp = "[A-Z]{3}") String baseCurrency,
        @Pattern(regexp = "[A-Z]{3}") String quoteCurrency) {
}
