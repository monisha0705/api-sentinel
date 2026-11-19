
package com.apisentinel.sentinel.testgeneration;

import com.apisentinel.sentinel.graph.GraphNode;
import com.apisentinel.sentinel.graph.NodeType;
import com.apisentinel.sentinel.graph.RelationshipType;
import com.apisentinel.sentinel.graph.SecurityGraph;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GraphGuidedTestGenerator {

    public List<SecurityTestCase> generateTests(SecurityGraph graph) {

        List<SecurityTestCase> testCases = new ArrayList<>();

        if (graph == null) {
            return testCases;
        }

        Set<GraphNode> nodes = graph.getNodes();

        // Generate Broken Object Level Authorization tests.
        generateBolaTests(graph, nodes, testCases);

        // Generate Broken Function Level Authorization tests.
        generateBflaTests(graph, nodes, testCases);

        return testCases;
    }

    private void generateBolaTests(
            SecurityGraph graph,
            Set<GraphNode> nodes,
            List<SecurityTestCase> testCases) {

        for (GraphNode identity : nodes) {

            if (identity.type() != NodeType.IDENTITY
                    || !hasRole(graph, identity, "CUSTOMER")) {
                continue;
            }

            Set<String> ownedResources =
                    graph.getOutgoingRelationships(identity)
                            .stream()
                            .filter(edge ->
                                    edge.relationship() == RelationshipType.OWNS)
                            .map(edge -> graph.getTarget(edge).id())
                            .collect(Collectors.toSet());

            for (GraphNode resource : nodes) {

                if (resource.type() != NodeType.RESOURCE
                        || ownedResources.contains(resource.id())) {
                    continue;
                }

                String owner = findOwner(graph, resource);

                if (owner == null || owner.equals(identity.id())) {
                    continue;
                }

                for (GraphNode endpoint : nodes) {

                    if (endpoint.type() != NodeType.ENDPOINT
                            || !isAssociatedWith(graph, endpoint, resource)) {
                        continue;
                    }

                    if (!endpoint.id().startsWith("GET:")) {
                        continue;
                    }

                    String path = extractPath(endpoint);

                    if (!path.contains("{id}")) {
                        continue;
                    }

                    String endpointPath =
                            path.replace("{id}", resourceId(resource));

                    testCases.add(new SecurityTestCase(
                            "BOLA-" + identity.id() + "-" + resource.id(),
                            "BOLA",
                            identity.id(),
                            "GET",
                            endpointPath,
                            resource.id(),
                            identity.name()
                                    + " attempts to access a resource owned by "
                                    + owner,
                            404
                    ));
                }
            }
        }
    }

    private void generateBflaTests(
            SecurityGraph graph,
            Set<GraphNode> nodes,
            List<SecurityTestCase> testCases) {

        for (GraphNode endpoint : nodes) {

            if (endpoint.type() != NodeType.ENDPOINT) {
                continue;
            }

            // Find the role required by this endpoint.
            Set<GraphNode> requiredRoles =
                    graph.getOutgoingRelationships(endpoint)
                            .stream()
                            .filter(edge ->
                                    edge.relationship()
                                            == RelationshipType.REQUIRES_ROLE)
                            .map(edge -> graph.getTarget(edge))
                            .collect(Collectors.toSet());

            if (requiredRoles.isEmpty()) {
                continue;
            }

            String endpointId = endpoint.id();
            int separatorIndex = endpointId.indexOf(':');

            if (separatorIndex <= 0) {
                continue;
            }

            String method = endpointId.substring(0, separatorIndex);
            String path = endpointId.substring(separatorIndex + 1);

            // Identify identities that lack the required role.
            for (GraphNode identity : nodes) {

                if (identity.type() != NodeType.IDENTITY) {
                    continue;
                }

                boolean hasRequiredRole = requiredRoles.stream()
                        .anyMatch(role ->
                                hasRole(graph, identity, role.name()));

                if (hasRequiredRole) {
                    continue;
                }

                String testId = "BFLA-"
                        + identity.id() + "-"
                        + endpointId.replace(":", "-")
                                .replace("/", "-");

                String precondition = path.contains("{id}")
                        ? "Requires an existing resource ID; resolve before execution"
                        : null;

                testCases.add(new SecurityTestCase(
                        testId,
                        "BFLA",
                        identity.id(),
                        method,
                        path,
                        "authorization-policy",
                        identity.name()
                                + " attempts to access an endpoint requiring "
                                + requiredRoles.stream()
                                        .map(GraphNode::name)
                                        .collect(Collectors.joining(", ")),
                        403,
                        null,
                        precondition,
                        null
                ));
            }
        }
    }

    private String extractPath(GraphNode endpoint) {

        String endpointId = endpoint.id();

        return endpointId.substring(
                endpointId.indexOf(':') + 1
        );
    }

    private String findOwner(
            SecurityGraph graph,
            GraphNode resource) {

        for (GraphNode node : graph.getNodes()) {

            if (node.type() != NodeType.IDENTITY) {
                continue;
            }

            boolean ownsResource =
                    graph.getOutgoingRelationships(node)
                            .stream()
                            .anyMatch(edge ->
                                    edge.relationship() == RelationshipType.OWNS
                                            && graph.getTarget(edge).equals(resource)
                            );

            if (ownsResource) {
                return node.id();
            }
        }

        return null;
    }

    private boolean isAssociatedWith(
            SecurityGraph graph,
            GraphNode endpoint,
            GraphNode resource) {

        return graph.getOutgoingRelationships(endpoint)
                .stream()
                .anyMatch(edge ->
                        edge.relationship() == RelationshipType.ACCESSES
                                && graph.getTarget(edge).equals(resource)
                );
    }

    private String resourceId(GraphNode resource) {
        return resource.id().replace("account-", "");
    }

    private boolean hasRole(
            SecurityGraph graph,
            GraphNode identity,
            String roleName) {

        return graph.getOutgoingRelationships(identity)
                .stream()
                .filter(edge ->
                        edge.relationship() == RelationshipType.HAS_ROLE)
                .map(edge -> graph.getTarget(edge))
                .anyMatch(role -> role.name().equals(roleName));
    }
}

