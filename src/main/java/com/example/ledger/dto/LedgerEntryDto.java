package com.example.ledger.dto;

import com.example.ledger.model.LedgerEntry;
import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryDto(Long id, Long walletId, Long transferId, Long paymentId,
                             String entryType, BigDecimal amount, BigDecimal balanceAfter,
                             String currency, String description, String operationKey,
                             Instant createdAt) {
    public static LedgerEntryDto from(LedgerEntry entry) {
        return new LedgerEntryDto(entry.getId(), entry.getWallet().getId(),
                entry.getTransfer() == null ? null : entry.getTransfer().getId(),
                entry.getPayment() == null ? null : entry.getPayment().getId(),
                entry.getEntryType() == null ? null : entry.getEntryType().name(),
                entry.getAmount(), entry.getBalanceAfter(), entry.getCurrency(),
                entry.getDescription(), entry.getOperationKey(), entry.getCreatedAt());
    }
}
