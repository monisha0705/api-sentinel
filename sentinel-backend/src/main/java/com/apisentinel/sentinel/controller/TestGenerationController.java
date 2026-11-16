package com.apisentinel.sentinel.controller;

import com.apisentinel.sentinel.dto.EndpointSummary;
import com.apisentinel.sentinel.graph.SecurityGraph;
import com.apisentinel.sentinel.service.SecurityGraphService;
import com.apisentinel.sentinel.testgeneration.GraphGuidedTestGenerator;
import com.apisentinel.sentinel.testgeneration.HttpTestExecutionService;
import com.apisentinel.sentinel.testgeneration.SecurityTestCase;
import com.apisentinel.sentinel.testgeneration.TestExecutionResult;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tests")
public class TestGenerationController {

    private final SecurityGraphService graphService;
    private final GraphGuidedTestGenerator testGenerator;
    private final HttpTestExecutionService executionService;

    public TestGenerationController(
            SecurityGraphService graphService,
            GraphGuidedTestGenerator testGenerator,
            HttpTestExecutionService executionService) {
        this.graphService = graphService;
        this.testGenerator = testGenerator;
        this.executionService = executionService;
    }

    @PostMapping("/generate")
    public List<SecurityTestCase> generateTests(
            @RequestBody List<EndpointSummary> endpoints) {

        SecurityGraph graph = graphService.buildBenchmarkGraph(endpoints);

        return testGenerator.generateTests(graph);
    }

    @PostMapping("/execute")
    public List<TestExecutionResult> executeTests(
            @RequestBody List<SecurityTestCase> testCases) {

        return testCases.stream()
                .map(executionService::execute)
                .toList();
    }
}


