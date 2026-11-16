package com.apisentinel.bank.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AccountRequest(
        @NotNull
        @Positive
        BigDecimal initialBalance
) {
}