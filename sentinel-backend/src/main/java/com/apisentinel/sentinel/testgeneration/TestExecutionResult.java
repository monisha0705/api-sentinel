package com.apisentinel.sentinel.testgeneration;

import java.time.Instant;

public record TestExecutionResult(
        String testId,
        String identity,
        String endpoint,
        int expectedStatus,
        int actualStatus,
        boolean passed,
        String responseBody,
        Instant executedAt
) {
}

