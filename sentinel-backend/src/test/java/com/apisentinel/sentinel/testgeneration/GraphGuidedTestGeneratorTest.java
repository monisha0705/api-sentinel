

package com.apisentinel.sentinel.testgeneration;

import com.apisentinel.sentinel.graph.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraphGuidedTestGeneratorTest {

    private GraphGuidedTestGenerator generator;
    private SecurityGraph graph;

    @BeforeEach
    void setUp() {
        generator = new GraphGuidedTestGenerator();
        graph = new SecurityGraph();

        // Identity nodes
        GraphNode alice = node("alice", NodeType.IDENTITY, "Alice");
        GraphNode bob = node("bob", NodeType.IDENTITY, "Bob");
        GraphNode admin = node("admin", NodeType.IDENTITY, "Admin");

        // Role nodes
        GraphNode customer = node(
                "role-customer", NodeType.ROLE, "CUSTOMER"
        );

        GraphNode administrator = node(
                "role-admin", NodeType.ROLE, "ADMIN"
        );

        // Account resources
        GraphNode account203 = node(
                "account-203", NodeType.RESOURCE, "Alice Account"
        );

        GraphNode account204 = node(
                "account-204", NodeType.RESOURCE, "Bob Account"
        );

        // Account endpoint
        GraphNode accountsEndpoint = node(
                "GET:/api/accounts/{id}",
                NodeType.ENDPOINT,
                "Get account by ID"
        );

        // Payment approval endpoint
        GraphNode approvalEndpoint = node(
                "POST:/api/payments/{id}/approve",
                NodeType.ENDPOINT,
                "Approve payment"
        );

        // Identity-role relationships
        addRelationship(alice, customer, RelationshipType.HAS_ROLE);
        addRelationship(bob, customer, RelationshipType.HAS_ROLE);
        addRelationship(admin, administrator, RelationshipType.HAS_ROLE);

        // Ownership relationships
        addRelationship(alice, account203, RelationshipType.OWNS);
        addRelationship(bob, account204, RelationshipType.OWNS);

        // Account endpoint-resource relationships
        addRelationship(
                accountsEndpoint,
                account203,
                RelationshipType.ACCESSES
        );

        addRelationship(
                accountsEndpoint,
                account204,
                RelationshipType.ACCESSES
        );

        // Approval endpoint requires ADMIN
        addRelationship(
                approvalEndpoint,
                administrator,
                RelationshipType.REQUIRES_ROLE
        );
    }

    @Test
    void shouldGenerateCrossAccountTestsForCustomerIdentities() {

        List<SecurityTestCase> tests = generator.generateTests(graph);

        List<SecurityTestCase> bolaTests = tests.stream()
                .filter(test -> test.testType().equals("BOLA"))
                .toList();

        assertEquals(2, bolaTests.size());

        assertTrue(bolaTests.stream().anyMatch(test ->
                test.identity().equals("alice")
                        && test.endpoint().equals("/api/accounts/204")
                        && test.targetResource().equals("account-204")
        ));

        assertTrue(bolaTests.stream().anyMatch(test ->
                test.identity().equals("bob")
                        && test.endpoint().equals("/api/accounts/203")
                        && test.targetResource().equals("account-203")
        ));
    }

    @Test
    void shouldNotGenerateTestsForAdmin() {

        List<SecurityTestCase> tests = generator.generateTests(graph);

        assertFalse(tests.stream()
                .anyMatch(test -> test.identity().equals("admin")));
    }

    @Test
    void shouldNotGenerateTestsForOwnedResources() {

        List<SecurityTestCase> tests = generator.generateTests(graph);

        assertFalse(tests.stream().anyMatch(test ->
                test.testType().equals("BOLA")
                        && test.identity().equals("alice")
                        && test.targetResource().equals("account-203")
        ));

        assertFalse(tests.stream().anyMatch(test ->
                test.testType().equals("BOLA")
                        && test.identity().equals("bob")
                        && test.targetResource().equals("account-204")
        ));
    }

    @Test
    void shouldGenerateGetRequestsWithExpectedStatus404() {

        List<SecurityTestCase> tests = generator.generateTests(graph);

        List<SecurityTestCase> bolaTests = tests.stream()
                .filter(test -> test.testType().equals("BOLA"))
                .toList();

        assertFalse(bolaTests.isEmpty());

        assertTrue(bolaTests.stream().allMatch(test ->
                test.method().equals("GET")
                        && test.expectedStatus() == 404
        ));
    }

    @Test
    void shouldReturnNoTestsWhenGraphIsEmpty() {

        SecurityGraph emptyGraph = new SecurityGraph();

        List<SecurityTestCase> tests =
                generator.generateTests(emptyGraph);

        assertTrue(tests.isEmpty());
    }

    @Test
    void shouldGenerateBflaTestForAliceAgainstAdminEndpoint() {

        List<SecurityTestCase> tests = generator.generateTests(graph);

        assertTrue(tests.stream().anyMatch(test ->
                test.testType().equals("BFLA")
                        && test.identity().equals("alice")
                        && test.method().equals("POST")
                        && test.endpoint().equals(
                                "/api/payments/{id}/approve"
                        )
                        && test.expectedStatus() == 403
        ));
    }

    @Test
    void shouldGenerateBflaTestForBobAgainstAdminEndpoint() {

        List<SecurityTestCase> tests = generator.generateTests(graph);

        assertTrue(tests.stream().anyMatch(test ->
                test.testType().equals("BFLA")
                        && test.identity().equals("bob")
                        && test.method().equals("POST")
                        && test.endpoint().equals(
                                "/api/payments/{id}/approve"
                        )
                        && test.expectedStatus() == 403
        ));
    }

    @Test
    void shouldNotGenerateBflaTestForAdmin() {

        List<SecurityTestCase> tests = generator.generateTests(graph);

        assertFalse(tests.stream().anyMatch(test ->
                test.testType().equals("BFLA")
                        && test.identity().equals("admin")
        ));
    }

    private GraphNode node(
            String id,
            NodeType type,
            String name) {

        return new GraphNode(id, type, name, "");
    }

    private void addRelationship(
            GraphNode source,
            GraphNode target,
            RelationshipType relationship) {

        graph.addRelationship(
                source,
                target,
                new GraphEdge(relationship, "")
        );
    }
}
