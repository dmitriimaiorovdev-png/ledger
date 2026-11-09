package com.example.ledger.dto;

import com.example.ledger.model.Transfer;
import java.math.BigDecimal;
import java.time.Instant;

public record TransferDto(Long id, Long fromWalletId, Long toWalletId, String currency,
                          BigDecimal amount, BigDecimal convertedAmount, BigDecimal fxRate,
                          BigDecimal fee, String status, String errorMessage,
                          Instant createdAt, Instant completedAt) {
    public static TransferDto from(Transfer transfer) {
        return new TransferDto(transfer.getId(),
                transfer.getFromWallet() == null ? null : transfer.getFromWallet().getId(),
                transfer.getToWallet() == null ? null : transfer.getToWallet().getId(),
                transfer.getCurrency(), transfer.getAmount(), transfer.getConvertedAmount(),
                transfer.getFxRate(), transfer.getFee(),
                transfer.getStatus() == null ? null : transfer.getStatus().name(),
                transfer.getErrorMessage(), transfer.getCreatedAt(), transfer.getCompletedAt());
    }
}
