package com.example.ledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record TransferRequest(Long fromWalletId, Long toWalletId,
                              @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount) {
}
