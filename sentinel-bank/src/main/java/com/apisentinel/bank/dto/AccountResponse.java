package com.apisentinel.bank.dto;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        String accountNumber,
        BigDecimal balance,
        String currency,
        String status
) {
}
