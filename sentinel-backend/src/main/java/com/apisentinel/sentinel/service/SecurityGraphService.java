
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
                "account-203", NodeType.RESOURCE, "Account 203",
                "Synthetic account owned by the isolated sentinel_alice identity"
        );

        GraphNode bobAccount = node(
                "account-204", NodeType.RESOURCE, "Account 204",
                "Synthetic account owned by the isolated sentinel_bob identity"
        );

        GraphNode payments = node(
                "payments", NodeType.RESOURCE, "Payments",
                "Payment resources"
        );

        // Payment workflow state nodes
        GraphNode pendingState = node(
                "payment-state-pending",
                NodeType.WORKFLOW_STATE,
                "PENDING",
                "Payment created and awaiting approval or cancellation"
        );

        GraphNode approvedState = node(
                "payment-state-approved",
                NodeType.WORKFLOW_STATE,
                "APPROVED",
                "Payment approved by an administrator"
        );

        GraphNode cancelledState = node(
                "payment-state-cancelled",
                NodeType.WORKFLOW_STATE,
                "CANCELLED",
                "Payment cancelled by its creator"
        );

        // Valid payment workflow transitions
        connect(
                graph,
                pendingState,
                approvedState,
                RelationshipType.TRANSITIONS_TO
        );

        connect(
                graph,
                pendingState,
                cancelledState,
                RelationshipType.TRANSITIONS_TO
        );

        // Identity-role relationships
        connect(graph, alice, customerRole, RelationshipType.HAS_ROLE);
        connect(graph, bob, customerRole, RelationshipType.HAS_ROLE);
        connect(graph, admin, adminRole, RelationshipType.HAS_ROLE);

        // Resource ownership relationships
        connect(graph, alice, aliceAccount, RelationshipType.OWNS);
        connect(graph, bob, bobAccount, RelationshipType.OWNS);

        // API endpoint nodes and relationships
        if (endpoints != null) {

            for (EndpointSummary endpoint : endpoints) {

                String method = endpoint.method().toUpperCase();
                String path = endpoint.path().toLowerCase();

                String endpointId = method + ":" + endpoint.path();

                GraphNode endpointNode = node(
                        endpointId,
                        NodeType.ENDPOINT,
                        method + " " + endpoint.path(),
                        endpoint.summary()
                );

                graph.addNode(endpointNode);

                // Account endpoint relationships
                if (path.contains("account")) {

                    connect(
                            graph,
                            endpointNode,
                            aliceAccount,
                            RelationshipType.ACCESSES
                    );

                    connect(
                            graph,
                            endpointNode,
                            bobAccount,
                            RelationshipType.ACCESSES
                    );
                }

                // Payment endpoint relationships
                if (path.contains("payment")) {

                    connect(
                            graph,
                            endpointNode,
                            payments,
                            RelationshipType.ACCESSES
                    );

                    // Payment creation modifies payment resources.
                    if (method.equals("POST")
                            && !path.endsWith("/approve")
                            && !path.endsWith("/cancel")) {

                        connect(
                                graph,
                                endpointNode,
                                payments,
                                RelationshipType.CREATES
                        );
                    }

                    // Approval requires the ADMIN role.
                    if (method.equals("POST")
                            && path.endsWith("/approve")) {

                        connect(
                                graph,
                                endpointNode,
                                adminRole,
                                RelationshipType.REQUIRES_ROLE
                        );

                        connect(
                                graph,
                                endpointNode,
                                payments,
                                RelationshipType.APPROVES
                        );
                    }

                    // Cancellation modifies payment resources.
                    if (method.equals("POST")
                            && path.endsWith("/cancel")) {

                        connect(
                                graph,
                                endpointNode,
                                payments,
                                RelationshipType.MODIFIES
                        );
                    }
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
