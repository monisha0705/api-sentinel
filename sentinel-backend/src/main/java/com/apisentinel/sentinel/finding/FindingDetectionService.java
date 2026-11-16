package com.apisentinel.sentinel.finding;

import com.apisentinel.sentinel.testgeneration.SecurityTestCase;
import com.apisentinel.sentinel.testgeneration.TestExecutionResult;
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

        for (int i = 0; i < testCases.size(); i++) {

            SecurityTestCase testCase = testCases.get(i);
            TestExecutionResult result = executionResults.stream()
                    .filter(r -> r.testId().equals(testCase.testId()))
                    .findFirst()
                    .orElse(null);

            if (result == null) {
                continue;
            }

            if ("BOLA".equals(testCase.testType())
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
        }

        return findings;
    }
}

