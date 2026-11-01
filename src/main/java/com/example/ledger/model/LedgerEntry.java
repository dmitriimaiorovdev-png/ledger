package com.example.ledger.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries", uniqueConstraints = @UniqueConstraint(columnNames = {"wallet_id", "operation_key"}))
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", nullable = true)
    private Transfer transfer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = true)
    private PaymentTransaction payment;

    @Enumerated(EnumType.STRING)
    private EntryType entryType;

    @Column(precision = 28, scale = 12)
    private BigDecimal amount;

    @Column(precision = 28, scale = 12)
    private BigDecimal balanceAfter;

    private String currency;

    private String description;

    private Instant createdAt;

    @Column(name = "operation_key")
    private String operationKey;

    protected LedgerEntry() {
    }

    public LedgerEntry(
            Wallet wallet,
            Transfer transfer,
            EntryType entryType,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String currency,
            String operationKey
    ) {
        this.wallet = wallet;
        this.transfer = transfer;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.currency = currency;
        this.operationKey = operationKey;
    }

    public LedgerEntry(
            Wallet wallet,
            PaymentTransaction payment,
            EntryType entryType,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String currency,
            String operationKey
    ) {
        this.wallet = wallet;
        this.payment = payment;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.currency = currency;
        this.operationKey = operationKey;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public Transfer getTransfer() {
        return transfer;
    }

    public void setTransfer(Transfer transfer) {
        this.transfer = transfer;
    }

    public PaymentTransaction getPayment() {
        return payment;
    }

    public void setPayment(PaymentTransaction payment) {
        this.payment = payment;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(EntryType entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getOperationKey() {
        return operationKey;
    }

    public void setOperationKey(String operationKey) {
        this.operationKey = operationKey;
    }

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
    }
}
