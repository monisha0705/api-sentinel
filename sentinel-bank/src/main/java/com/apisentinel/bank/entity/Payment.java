
package com.apisentinel.bank.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private BankUser createdBy;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "beneficiary_reference", nullable = false, length = 100)
    private String beneficiaryReference;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private BankUser approvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(
            Account sourceAccount,
            BankUser createdBy,
            BigDecimal amount,
            String currency,
            String beneficiaryReference) {

        this.sourceAccount = sourceAccount;
        this.createdBy = createdBy;
        this.amount = amount;
        this.currency = currency;
        this.beneficiaryReference = beneficiaryReference;
        this.status = "PENDING";
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    public BankUser getCreatedBy() {
        return createdBy;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getBeneficiaryReference() {
        return beneficiaryReference;
    }

    public String getStatus() {
        return status;
    }

    public BankUser getApprovedBy() {
        return approvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void approve(BankUser admin) {
    if (!"PENDING".equals(this.status)) {
        throw new IllegalStateException(
                "Only pending payments can be approved");
    }

    this.status = "APPROVED";
    this.approvedBy = admin;
}

public void cancel() {
    if (!"PENDING".equals(this.status)) {
        throw new IllegalStateException(
                "Only pending payments can be cancelled");
    }

    this.status = "CANCELLED";
}
}
