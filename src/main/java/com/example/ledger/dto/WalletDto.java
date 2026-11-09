package com.example.ledger.dto;

import com.example.ledger.model.Wallet;
import java.math.BigDecimal;
import java.time.Instant;

public record WalletDto(Long id, String currency, String status, BigDecimal balance, Instant createdAt) {
    public static WalletDto of(Wallet wallet, BigDecimal balance) {
        return new WalletDto(wallet.getId(), wallet.getCurrency(),
                wallet.getStatus() == null ? null : wallet.getStatus().name(),
                balance, wallet.getCreatedAt());
    }
}
