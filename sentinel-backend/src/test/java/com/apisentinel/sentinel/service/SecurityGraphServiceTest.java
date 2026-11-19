
package com.apisentinel.sentinel.service;

import com.apisentinel.sentinel.dto.EndpointSummary;
import com.apisentinel.sentinel.graph.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SecurityGraphServiceTest {

    private final SecurityGraphService service = new SecurityGraphService();

    @Test
    void shouldAddPaymentWorkflowStatesAndTransitions() {
        SecurityGraph graph = service.buildBenchmarkGraph(List.of());

        Set<String> workflowStates = graph.getNodes().stream()
                .filter(node -> node.type() == NodeType.WORKFLOW_STATE)
                .map(GraphNode::name)
                .collect(Collectors.toSet());

        assertEquals(
                Set.of("PENDING", "APPROVED", "CANCELLED"),
                workflowStates
        );

        Set<String> transitions = graph.getRelationships().stream()
                .filter(edge -> edge.relationship() == RelationshipType.TRANSITIONS_TO)
                .map(edge -> {
                    GraphNode source = graph.getSource(edge);
                    GraphNode target = graph.getTarget(edge);
                    return source.name() + "->" + target.name();
                })
                .collect(Collectors.toSet());

        assertEquals(
                Set.of("PENDING->APPROVED", "PENDING->CANCELLED"),
                transitions
        );
    }

    @Test
    void shouldPreserveExistingIdentityAndOwnershipRelationships() {
        SecurityGraph graph = service.buildBenchmarkGraph(List.of());

        assertTrue(graph.getRelationships().stream().anyMatch(edge ->
                edge.relationship() == RelationshipType.HAS_ROLE
        ));

        assertTrue(graph.getRelationships().stream().anyMatch(edge ->
                edge.relationship() == RelationshipType.OWNS
        ));
    }
}
