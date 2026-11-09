package com.example.ledger.dto;

import com.example.ledger.model.PaymentTransaction;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentDto(Long id, Long walletId, String type, BigDecimal amount,
                         String currency, String status, String externalPaymentId,
                         String failureReason, Instant createdAt, Instant updatedAt) {
    public static PaymentDto from(PaymentTransaction payment) {
        return new PaymentDto(payment.getId(), payment.getWallet().getId(),
                payment.getType() == null ? null : payment.getType().name(),
                payment.getAmount(), payment.getCurrency(),
                payment.getStatus() == null ? null : payment.getStatus().name(),
                payment.getExternalPaymentId(), payment.getFailureReason(),
                payment.getCreatedAt(), payment.getUpdatedAt());
    }
}
