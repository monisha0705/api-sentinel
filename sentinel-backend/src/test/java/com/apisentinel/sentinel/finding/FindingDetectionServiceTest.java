
package com.apisentinel.sentinel.finding;

import com.apisentinel.sentinel.testgeneration.SecurityTestCase;
import com.apisentinel.sentinel.testgeneration.TestExecutionResult;
import com.apisentinel.sentinel.testgeneration.StatefulPaymentTestExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FindingDetectionServiceTest {

    private FindingDetectionService detectionService;

    @BeforeEach
    void setUp() {
        detectionService = new FindingDetectionService();
    }

    @Test
    void shouldDetectBolaWhenUnauthorizedAccessReturns200() {

        SecurityTestCase testCase = createTestCase(
                "BOLA-alice-account-202", "BOLA"
        );

        TestExecutionResult result = createResult(
                "BOLA-alice-account-202", 404, 200
        );

        List<SecurityFinding> findings =
                detectionService.detectFindings(
                        List.of(testCase),
                        List.of(result)
                );

        assertEquals(1, findings.size());

        SecurityFinding finding = findings.get(0);

        assertEquals("FINDING-BOLA-alice-account-202",
                finding.findingId());
        assertEquals("BOLA", finding.vulnerabilityType());
        assertEquals("HIGH", finding.severity());
        assertEquals("alice", finding.identity());
        assertEquals("/api/accounts/202", finding.endpoint());
        assertEquals("account-202", finding.targetResource());
        assertEquals(404, finding.expectedStatus());
        assertEquals(200, finding.actualStatus());
        assertEquals("{\"accountId\":202}", finding.evidence());
        assertNotNull(finding.detectedAt());
    }

    @Test
    void shouldNotDetectFindingWhenAccessIsDenied() {

        SecurityTestCase testCase = createTestCase(
                "BOLA-alice-account-202", "BOLA"
        );

        TestExecutionResult result = createResult(
                "BOLA-alice-account-202", 404, 404
        );

        List<SecurityFinding> findings =
                detectionService.detectFindings(
                        List.of(testCase),
                        List.of(result)
                );

        assertTrue(findings.isEmpty());
    }

    @Test
    void shouldIgnoreNonBolaTestCases() {

        SecurityTestCase testCase = createTestCase(
                "AUTH-alice-account-202", "AUTH"
        );

        TestExecutionResult result = createResult(
                "AUTH-alice-account-202", 404, 200
        );

        List<SecurityFinding> findings =
                detectionService.detectFindings(
                        List.of(testCase),
                        List.of(result)
                );

        assertTrue(findings.isEmpty());
    }

    @Test
    void shouldIgnoreExecutionResultsWithoutMatchingTestId() {

        SecurityTestCase testCase = createTestCase(
                "BOLA-alice-account-202", "BOLA"
        );

        TestExecutionResult result = createResult(
                "BOLA-bob-account-101", 404, 200
        );

        List<SecurityFinding> findings =
                detectionService.detectFindings(
                        List.of(testCase),
                        List.of(result)
                );

        assertTrue(findings.isEmpty());
    }

    @Test
    void shouldIgnoreTestCasesWithoutExecutionResults() {

        SecurityTestCase testCase = createTestCase(
                "BOLA-alice-account-202", "BOLA"
        );

        List<SecurityFinding> findings =
                detectionService.detectFindings(
                        List.of(testCase),
                        List.of()
                );

        assertTrue(findings.isEmpty());
    }

    @Test
    void shouldDetectMultipleBolaFindings() {

        SecurityTestCase aliceTest = createTestCase(
                "BOLA-alice-account-202", "BOLA"
        );

        SecurityTestCase bobTest = new SecurityTestCase(
                "BOLA-bob-account-101",
                "BOLA",
                "bob",
                "GET",
                "/api/accounts/101",
                "account-101",
                "Bob attempts to access Alice's account",
                404
        );

        TestExecutionResult aliceResult = createResult(
                "BOLA-alice-account-202", 404, 200
        );

        TestExecutionResult bobResult = new TestExecutionResult(
                "BOLA-bob-account-101",
                "bob",
                "/api/accounts/101",
                404,
                200,
                false,
                "{\"accountId\":101}",
                Instant.now()
        );

        List<SecurityFinding> findings =
                detectionService.detectFindings(
                        List.of(aliceTest, bobTest),
                        List.of(bobResult, aliceResult)
                );

        assertEquals(2, findings.size());
        assertTrue(findings.stream().anyMatch(
                finding -> finding.identity().equals("alice")
        ));
        assertTrue(findings.stream().anyMatch(
                finding -> finding.identity().equals("bob")
        ));
    }


    @Test
    void shouldDetectAcceptedUnauthorizedApprovalEvenWhenStateDoesNotChange() {
        TestExecutionResult approvalStep = new TestExecutionResult(
                "BL-001-APPROVE",
                "alice",
                "/api/payments/test-payment/approve",
                403,
                200,
                false,
                "{\"status\":\"PENDING\"}",
                Instant.now()
        );

        StatefulPaymentTestExecutionService.StatefulTestExecutionResult result =
                new StatefulPaymentTestExecutionService.StatefulTestExecutionResult(
                        "BL-001",
                        "Unauthorized payment approval",
                        "test-payment",
                        "PENDING",
                        "PENDING",
                        "PENDING",
                        false,
                        false,
                        List.of(approvalStep),
                        Instant.now()
                );

        assertEquals(1, detectionService.detectStatefulFinding(result).size());
    }

    @Test
    void shouldNotTreatServerErrorAsConfirmedUnauthorizedApproval() {
        TestExecutionResult approvalStep = new TestExecutionResult(
                "BL-001-APPROVE",
                "alice",
                "/api/payments/test-payment/approve",
                403,
                500,
                false,
                "Internal server error",
                Instant.now()
        );

        StatefulPaymentTestExecutionService.StatefulTestExecutionResult result =
                new StatefulPaymentTestExecutionService.StatefulTestExecutionResult(
                        "BL-001",
                        "Unauthorized payment approval",
                        "test-payment",
                        "PENDING",
                        "PENDING",
                        "PENDING",
                        false,
                        false,
                        List.of(approvalStep),
                        Instant.now()
                );

        assertTrue(detectionService.detectStatefulFinding(result).isEmpty());
    }

    private SecurityTestCase createTestCase(
            String testId,
            String testType
    ) {
        return new SecurityTestCase(
                testId,
                testType,
                "alice",
                "GET",
                "/api/accounts/202",
                "account-202",
                "Alice attempts to access Bob's account",
                404
        );
    }

    private TestExecutionResult createResult(
            String testId,
            int expectedStatus,
            int actualStatus
    ) {
        return new TestExecutionResult(
                testId,
                "alice",
                "/api/accounts/202",
                expectedStatus,
                actualStatus,
                actualStatus == expectedStatus,
                "{\"accountId\":202}",
                Instant.now()
        );
    }
}
