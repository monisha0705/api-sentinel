package com.apisentinel.sentinel.testgeneration;

import com.apisentinel.sentinel.graph.GraphEdge;
import com.apisentinel.sentinel.graph.GraphNode;
import com.apisentinel.sentinel.graph.NodeType;
import com.apisentinel.sentinel.graph.RelationshipType;
import com.apisentinel.sentinel.graph.SecurityGraph;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class GraphGuidedTestGenerator {

    public List<SecurityTestCase> generateTests(SecurityGraph graph) {

        List<SecurityTestCase> testCases = new ArrayList<>();

        Set<GraphNode> nodes = graph.getNodes();

        for (GraphNode identity : nodes) {

           if (identity.type() != NodeType.IDENTITY
        || !hasRole(graph, identity, "CUSTOMER")) {
    continue;
}

            Set<String> ownedResources = graph.getOutgoingRelationships(identity)
                    .stream()
                    .filter(edge -> edge.relationship() == RelationshipType.OWNS)
                    .map(edge -> graph.getTarget(edge).id())
                    .collect(java.util.stream.Collectors.toSet());

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

                    String path = endpoint.id().substring(
                            endpoint.id().indexOf(':') + 1
                    );

                    if (!path.contains("{id}")) {
                        continue;
                    }

                    String endpointPath = path.replace("{id}", resourceId(resource));

                    testCases.add(new SecurityTestCase(
                            "BOLA-" + identity.id() + "-" + resource.id(),
                            "BOLA",
                            identity.id(),
                            "GET",
                            endpointPath,
                            resource.id(),
                            identity.name() + " attempts to access a resource owned by " + owner,
                            404
                    ));
                }
            }
        }

        return testCases;
    }

    private String findOwner(SecurityGraph graph, GraphNode resource) {

        for (GraphNode node : graph.getNodes()) {

            if (node.type() != NodeType.IDENTITY) {
                continue;
            }

            boolean ownsResource = graph.getOutgoingRelationships(node)
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
            .filter(edge -> edge.relationship() == RelationshipType.HAS_ROLE)
            .map(edge -> graph.getTarget(edge))
            .anyMatch(role -> role.name().equals(roleName));
}
}

