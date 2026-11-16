package com.apisentinel.bank.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        Long sourceAccountId,
        BigDecimal amount,
        String currency,
        String beneficiaryReference,
        String status,
        Instant createdAt
) {
}
