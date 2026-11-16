package com.apisentinel.sentinel.controller;

import com.apisentinel.sentinel.finding.FindingDetectionService;
import com.apisentinel.sentinel.finding.FindingPersistenceService;
import com.apisentinel.sentinel.finding.SecurityFinding;
import com.apisentinel.sentinel.finding.SecurityFindingEntity;
import com.apisentinel.sentinel.testgeneration.SecurityTestCase;
import com.apisentinel.sentinel.testgeneration.TestExecutionResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/findings")
public class FindingController {

    private final FindingDetectionService detectionService;
    private final FindingPersistenceService persistenceService;

    public FindingController(
            FindingDetectionService detectionService,
            FindingPersistenceService persistenceService) {
        this.detectionService = detectionService;
        this.persistenceService = persistenceService;
    }

    @PostMapping("/detect")
    public List<SecurityFinding> detectFindings(
            @RequestBody FindingDetectionRequest request) {

        List<SecurityFinding> findings =
                detectionService.detectFindings(
                        request.testCases(),
                        request.executionResults()
                );

        persistenceService.saveFindings(findings);

        return findings;
    }

    @GetMapping
    public List<SecurityFindingEntity> getAllFindings() {
        return persistenceService.getAllFindings();
    }

    public record FindingDetectionRequest(
            List<SecurityTestCase> testCases,
            List<TestExecutionResult> executionResults) {
    }
}



