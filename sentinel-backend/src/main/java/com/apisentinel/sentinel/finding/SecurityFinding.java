package com.apisentinel.sentinel.finding;

import java.time.Instant;

public record SecurityFinding(
        String findingId,
        String vulnerabilityType,
        String severity,
        String identity,
        String endpoint,
        String targetResource,
        String description,
        int expectedStatus,
        int actualStatus,
        String evidence,
        Instant detectedAt
) {
}

