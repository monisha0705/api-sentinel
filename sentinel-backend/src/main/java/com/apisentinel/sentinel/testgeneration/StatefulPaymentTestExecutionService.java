
package com.apisentinel.sentinel.testgeneration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class StatefulPaymentTestExecutionService {

    private final HttpTestExecutionService httpExecutor;
    private final ObjectMapper objectMapper;

    public StatefulPaymentTestExecutionService(
            HttpTestExecutionService httpExecutor) {
        this.httpExecutor = httpExecutor;
        this.objectMapper = new ObjectMapper();
    }

    public StatefulTestExecutionResult executeUnauthorizedApprovalScenario() {

        // Step 1: Alice creates a fresh payment.
        SecurityTestCase createPayment = new SecurityTestCase(
                "BL-001-CREATE",
                "BUSINESS_LOGIC",
                "alice",
                "POST",
                "/api/payments",
                "payments",
                "Create a payment fixture for the workflow test",
                201,
                """
                {
                    "amount": 250.00,
                    "currency": "INR",
                    "beneficiaryReference": "API-SENTINEL-BL-001",
                    "sourceAccountId": 203
                }
                """,
                null,
                "PENDING"
        );

        TestExecutionResult creationResult =
                httpExecutor.execute(createPayment);

        if (creationResult.actualStatus() != 201) {
            throw new IllegalStateException(
                    "Payment fixture creation failed. Expected HTTP 201 but received "
                            + creationResult.actualStatus()
                            + ". Response: "
                            + creationResult.responseBody()
            );
        }

        try {
            JsonNode creationResponse =
                    objectMapper.readTree(creationResult.responseBody());

            String paymentId = creationResponse.path("id").asText();
            String initialState =
                    creationResponse.path("status").asText();

            if (paymentId.isBlank()) {
                throw new IllegalStateException(
                        "Payment creation response did not contain an ID"
                );
            }

            if (!"PENDING".equals(initialState)) {
                throw new IllegalStateException(
                        "New payment was expected to be PENDING but was "
                                + initialState
                );
            }

            // Step 2: Alice attempts an administrator-only operation.
            SecurityTestCase approvePayment = new SecurityTestCase(
                    "BL-001-APPROVE",
                    "BUSINESS_LOGIC",
                    "alice",
                    "POST",
                    "/api/payments/" + paymentId + "/approve",
                    paymentId,
                    "Customer attempts to approve a payment",
                    403,
                    null,
                    "Payment must exist and be PENDING",
                    "PENDING"
            );

            TestExecutionResult approvalResult =
                    httpExecutor.execute(approvePayment);

            // Step 3: Alice retrieves the payment to inspect its state.
            SecurityTestCase verifyPayment = new SecurityTestCase(
                    "BL-001-VERIFY",
                    "STATE_VERIFICATION",
                    "alice",
                    "GET",
                    "/api/payments/" + paymentId,
                    paymentId,
                    "Verify payment state after unauthorized approval",
                    200,
                    null,
                    "Payment must exist",
                    "PENDING"
            );

            TestExecutionResult verificationResult =
                    httpExecutor.execute(verifyPayment);

            if (verificationResult.actualStatus() != 200) {
                throw new IllegalStateException(
                        "Payment state verification failed. HTTP "
                                + verificationResult.actualStatus()
                                + ". Response: "
                                + verificationResult.responseBody()
                );
            }

            JsonNode verificationResponse =
                    objectMapper.readTree(
                            verificationResult.responseBody()
                    );

            String finalState =
                    verificationResponse.path("status").asText();

            boolean invariantPreserved =
                    "PENDING".equals(initialState)
                            && "PENDING".equals(finalState);

            List<TestExecutionResult> steps = List.of(
                    creationResult,
                    approvalResult,
                    verificationResult
            );

            boolean passed =
                    approvalResult.passed()
                            && invariantPreserved;

            return new StatefulTestExecutionResult(
                    "BL-001",
                    "Unauthorized payment approval",
                    paymentId,
                    initialState,
                    finalState,
                    "PENDING",
                    invariantPreserved,
                    passed,
                    steps,
                    Instant.now()
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to execute unauthorized approval scenario",
                    exception
            );
        }
    }

    public record StatefulTestExecutionResult(
            String scenarioId,
            String scenarioName,
            String resourceId,
            String initialState,
            String finalState,
            String expectedFinalState,
            boolean invariantPreserved,
            boolean passed,
            List<TestExecutionResult> steps,
            Instant executedAt
    ) {}
}
