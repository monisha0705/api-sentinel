
package com.apisentinel.sentinel.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.Collections;
import java.util.Set;

public class SecurityGraph {

    private final Graph<GraphNode, GraphEdge> graph =
            new DefaultDirectedGraph<>(GraphEdge.class);

    public void addNode(GraphNode node) {
        graph.addVertex(node);
    }

    public void addRelationship(
            GraphNode source,
            GraphNode target,
            GraphEdge edge
    ) {
        graph.addVertex(source);
        graph.addVertex(target);
        graph.addEdge(source, target, edge);
    }

    public Set<GraphNode> getNodes() {
        return Collections.unmodifiableSet(graph.vertexSet());
    }

    public Set<GraphEdge> getRelationships() {
        return Collections.unmodifiableSet(graph.edgeSet());
    }

    public GraphNode getSource(GraphEdge edge) {
        return graph.getEdgeSource(edge);
    }

    public GraphNode getTarget(GraphEdge edge) {
        return graph.getEdgeTarget(edge);
    }

    public Set<GraphEdge> getOutgoingRelationships(GraphNode node) {
        return Collections.unmodifiableSet(graph.outgoingEdgesOf(node));
    }

    public int getNodeCount() {
        return graph.vertexSet().size();
    }

    public int getRelationshipCount() {
        return graph.edgeSet().size();
    }
}

