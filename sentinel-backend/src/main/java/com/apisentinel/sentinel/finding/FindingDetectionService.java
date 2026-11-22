
package com.apisentinel.sentinel.finding;

import com.apisentinel.sentinel.testgeneration.SecurityTestCase;
import com.apisentinel.sentinel.testgeneration.TestExecutionResult;
import com.apisentinel.sentinel.testgeneration.StatefulPaymentTestExecutionService;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class FindingDetectionService {

    public List<SecurityFinding> detectFindings(
            List<SecurityTestCase> testCases,
            List<TestExecutionResult> executionResults) {

        List<SecurityFinding> findings = new ArrayList<>();

        if (testCases == null || executionResults == null) {
            return findings;
        }

        for (SecurityTestCase testCase : testCases) {

            TestExecutionResult result = executionResults.stream()
                    .filter(r -> r.testId().equals(testCase.testId()))
                    .findFirst()
                    .orElse(null);

            if (result == null) {
                continue;
            }

            // BOLA: Unauthorized access to another user's resource.
            if ("BOLA".equalsIgnoreCase(testCase.testType())
                    && result.actualStatus() == 200
                    && result.expectedStatus() == 404) {

                findings.add(new SecurityFinding(
                        "FINDING-" + testCase.testId(),
                        "BOLA",
                        "HIGH",
                        testCase.identity(),
                        testCase.endpoint(),
                        testCase.targetResource(),
                        testCase.description(),
                        result.expectedStatus(),
                        result.actualStatus(),
                        result.responseBody(),
                        Instant.now()
                ));
            }

            // BFLA: An identity successfully invokes a restricted function.
            if ("BFLA".equalsIgnoreCase(testCase.testType())
                    && result.expectedStatus() == 403
                    && result.actualStatus() >= 200
                    && result.actualStatus() < 300) {

                findings.add(new SecurityFinding(
                        "FINDING-" + testCase.testId(),
                        "BFLA",
                        "HIGH",
                        testCase.identity(),
                        testCase.endpoint(),
                        testCase.targetResource(),
                        "Function-level authorization violation: identity "
                                + testCase.identity()
                                + " successfully accessed a function that requires "
                                + "authorization. Expected HTTP 403 but received HTTP "
                                + result.actualStatus() + ".",
                        result.expectedStatus(),
                        result.actualStatus(),
                        result.responseBody(),
                        Instant.now()
                ));
            }
        }

        return findings;
    }

    public List<SecurityFinding> detectStatefulFinding(
            StatefulPaymentTestExecutionService.StatefulTestExecutionResult result) {

        if (result == null || result.invariantPreserved()) {
            return List.of();
        }

        TestExecutionResult approvalStep = result.steps().stream()
                .filter(step -> "BL-001-APPROVE".equals(step.testId()))
                .findFirst()
                .orElse(null);

        if (approvalStep == null) {
            return List.of();
        }

        boolean unauthorizedOperationAccepted =
                approvalStep.actualStatus() >= 200
                        && approvalStep.actualStatus() < 300;
        boolean unexpectedStateTransition =
                !result.expectedFinalState().equals(result.finalState());

        // A server error or blocked request is not, by itself, proof of a vulnerability.
        if (!unauthorizedOperationAccepted && !unexpectedStateTransition) {
            return List.of();
        }

        String evidence = result.steps().stream()
                .map(step -> step.testId()
                        + " | Expected HTTP: " + step.expectedStatus()
                        + " | Actual HTTP: " + step.actualStatus()
                        + " | Response: " + step.responseBody())
                .reduce((first, second) -> first + "\n\n" + second)
                .orElse("");

        SecurityFinding finding = new SecurityFinding(
                "FINDING-" + result.scenarioId(),
                "BUSINESS_LOGIC",
                "HIGH",
                approvalStep.identity(),
                approvalStep.endpoint(),
                result.resourceId(),
                "Unauthorized payment approval changed payment state from "
                        + result.initialState() + " to " + result.finalState()
                        + " instead of preserving " + result.expectedFinalState(),
                approvalStep.expectedStatus(),
                approvalStep.actualStatus(),
                evidence,
                Instant.now()
        );

        return List.of(finding);
    }
}

