package com.apisentinel.sentinel.testgeneration;

public record SecurityTestCase(
        String testId,
        String testType,
        String identity,
        String method,
        String endpoint,
        String targetResource,
        String description,
        int expectedStatus
) {
}

