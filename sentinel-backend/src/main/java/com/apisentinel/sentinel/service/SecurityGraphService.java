package com.apisentinel.sentinel.service;

import com.apisentinel.sentinel.dto.EndpointSummary;
import com.apisentinel.sentinel.graph.GraphEdge;
import com.apisentinel.sentinel.graph.GraphNode;
import com.apisentinel.sentinel.graph.NodeType;
import com.apisentinel.sentinel.graph.RelationshipType;
import com.apisentinel.sentinel.graph.SecurityGraph;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecurityGraphService {

    public SecurityGraph buildBenchmarkGraph(
            List<EndpointSummary> endpoints) {

        SecurityGraph graph = new SecurityGraph();

        // Identity nodes
        GraphNode alice = node(
                "alice", NodeType.IDENTITY, "Alice",
                "Customer benchmark identity"
        );

        GraphNode bob = node(
                "bob", NodeType.IDENTITY, "Bob",
                "Customer benchmark identity"
        );

        GraphNode admin = node(
                "admin", NodeType.IDENTITY, "Admin",
                "Administrator benchmark identity"
        );

        // Role nodes
        GraphNode customerRole = node(
                "role-customer", NodeType.ROLE, "CUSTOMER",
                "Customer role"
        );

        GraphNode adminRole = node(
                "role-admin", NodeType.ROLE, "ADMIN",
                "Administrator role"
        );

        // Resource nodes
        GraphNode aliceAccount = node(
                "account-101", NodeType.RESOURCE, "Account 101",
                "Synthetic account owned by Alice"
        );

        GraphNode bobAccount = node(
                "account-202", NodeType.RESOURCE, "Account 202",
                "Synthetic account owned by Bob"
        );

        GraphNode payments = node(
                "payments", NodeType.RESOURCE, "Payments",
                "Payment resources"
        );

        // Add identity-role relationships
        connect(graph, alice, customerRole, RelationshipType.HAS_ROLE);
        connect(graph, bob, customerRole, RelationshipType.HAS_ROLE);
        connect(graph, admin, adminRole, RelationshipType.HAS_ROLE);

        // Add ownership relationships
        connect(graph, alice, aliceAccount, RelationshipType.OWNS);
        connect(graph, bob, bobAccount, RelationshipType.OWNS);

        // Add API endpoint nodes
        if (endpoints != null) {
            for (EndpointSummary endpoint : endpoints) {

                String endpointId = endpoint.method().toUpperCase()
                        + ":" + endpoint.path();

                GraphNode endpointNode = node(
                        endpointId,
                        NodeType.ENDPOINT,
                        endpoint.method() + " " + endpoint.path(),
                        endpoint.summary()
                );

                graph.addNode(endpointNode);

                // Associate endpoint with its resource category.
                String path = endpoint.path().toLowerCase();

                if (path.contains("account")) {
                    graph.addRelationship(
                            endpointNode,
                            aliceAccount,
                            new GraphEdge(
                                    RelationshipType.ACCESSES,
                                    "Endpoint references account resources"
                            )
                    );
                    graph.addRelationship(
                            endpointNode,
                            bobAccount,
                            new GraphEdge(
                                    RelationshipType.ACCESSES,
                                    "Endpoint references account resources"
                            )
                    );
                }

                if (path.contains("payment")) {
                    graph.addRelationship(
                            endpointNode,
                            payments,
                            new GraphEdge(
                                    RelationshipType.ACCESSES,
                                    "Endpoint references payment resources"
                            )
                    );
                }
            }
        }

        return graph;
    }

    private GraphNode node(
            String id,
            NodeType type,
            String name,
            String description) {

        return new GraphNode(id, type, name, description);
    }

    private void connect(
            SecurityGraph graph,
            GraphNode source,
            GraphNode target,
            RelationshipType relationship) {

        graph.addRelationship(
                source,
                target,
                new GraphEdge(relationship, relationship.name())
        );
    }
}
