
package com.apisentinel.sentinel.testgeneration;

public record SecurityTestCase(
        String testId,
        String testType,
        String identity,
        String method,
        String endpoint,
        String targetResource,
        String description,
        int expectedStatus,
        String requestBody,
        String precondition,
        String expectedState
) {

    // Backward-compatible constructor for existing BOLA tests.
    public SecurityTestCase(
            String testId,
            String testType,
            String identity,
            String method,
            String endpoint,
            String targetResource,
            String description,
            int expectedStatus
    ) {
        this(
                testId,
                testType,
                identity,
                method,
                endpoint,
                targetResource,
                description,
                expectedStatus,
                null,
                null,
                null
        );
    }
}
