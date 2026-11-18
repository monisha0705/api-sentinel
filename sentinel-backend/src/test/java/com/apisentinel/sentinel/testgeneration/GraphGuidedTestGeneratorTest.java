
package com.apisentinel.sentinel.testgeneration;

import com.apisentinel.sentinel.graph.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GraphGuidedTestGeneratorTest {

    private GraphGuidedTestGenerator generator;
    private SecurityGraph graph;

    @BeforeEach
    void setUp() {
        generator = new GraphGuidedTestGenerator();
        graph = new SecurityGraph();

        GraphNode alice = node("alice", NodeType.IDENTITY, "Alice");
        GraphNode bob = node("bob", NodeType.IDENTITY, "Bob");
        GraphNode admin = node("admin", NodeType.IDENTITY, "Admin");

        GraphNode customer = node("role-customer", NodeType.ROLE, "CUSTOMER");
        GraphNode administrator = node("role-admin", NodeType.ROLE, "ADMIN");

        GraphNode account101 = node("account-101", NodeType.RESOURCE, "Alice Account");
        GraphNode account202 = node("account-202", NodeType.RESOURCE, "Bob Account");

        GraphNode accountsEndpoint = node(
                "GET:/api/accounts/{id}",
                NodeType.ENDPOINT,
                "Get account by ID"
        );

        addRelationship(alice, customer, RelationshipType.HAS_ROLE);
        addRelationship(bob, customer, RelationshipType.HAS_ROLE);
        addRelationship(admin, administrator, RelationshipType.HAS_ROLE);

        addRelationship(alice, account101, RelationshipType.OWNS);
        addRelationship(bob, account202, RelationshipType.OWNS);

        addRelationship(accountsEndpoint, account101, RelationshipType.ACCESSES);
        addRelationship(accountsEndpoint, account202, RelationshipType.ACCESSES);
    }

    @Test
    void shouldGenerateCrossAccountTestsForCustomerIdentities() {

        List<SecurityTestCase> tests = generator.generateTests(graph);

        assertEquals(2, tests.size());

        assertTrue(tests.stream().anyMatch(test ->
                test.identity().equals("alice")
                        && test.endpoint().equals("/api/accounts/202")
                        && test.targetResource().equals("account-202")
        ));

        assertTrue(tests.stream().anyMatch(test ->
                test.identity().equals("bob")
                        && test.endpoint().equals("/api/accounts/101")
                        && test.targetResource().equals("account-101")
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
                test.identity().equals("alice")
                        && test.targetResource().equals("account-101")
        ));

        assertFalse(tests.stream().anyMatch(test ->
                test.identity().equals("bob")
                        && test.targetResource().equals("account-202")
        ));
    }

    @Test
    void shouldGenerateGetRequestsWithExpectedStatus404() {

        List<SecurityTestCase> tests = generator.generateTests(graph);

        assertTrue(tests.stream().allMatch(test ->
                test.method().equals("GET")
                        && test.expectedStatus() == 404
                        && test.testType().equals("BOLA")
        ));
    }

    @Test
    void shouldReturnNoTestsWhenGraphIsEmpty() {

        SecurityGraph emptyGraph = new SecurityGraph();

        List<SecurityTestCase> tests =
                generator.generateTests(emptyGraph);

        assertTrue(tests.isEmpty());
    }

    private GraphNode node(String id, NodeType type, String name) {
        return new GraphNode(id, type, name, "");
    }

    private void addRelationship(
            GraphNode source,
            GraphNode target,
            RelationshipType relationship
    ) {
        graph.addRelationship(
                source,
                target,
                new GraphEdge(relationship, "")
        );
    }
}
