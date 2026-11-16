
package com.apisentinel.sentinel.controller;

import com.apisentinel.sentinel.dto.EndpointSummary;
import com.apisentinel.sentinel.dto.GraphEdgeResponse;
import com.apisentinel.sentinel.dto.GraphNodeResponse;
import com.apisentinel.sentinel.dto.SecurityGraphResponse;
import com.apisentinel.sentinel.graph.GraphEdge;
import com.apisentinel.sentinel.graph.GraphNode;
import com.apisentinel.sentinel.graph.SecurityGraph;
import com.apisentinel.sentinel.service.SecurityGraphService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/graph")
public class SecurityGraphController {

    private final SecurityGraphService graphService;

    public SecurityGraphController(SecurityGraphService graphService) {
        this.graphService = graphService;
    }

    @PostMapping("/build")
    public ResponseEntity<SecurityGraphResponse> buildGraph(
            @RequestBody(required = false)
            List<EndpointSummary> endpoints) {

        SecurityGraph graph =
                graphService.buildBenchmarkGraph(endpoints);

        List<GraphNodeResponse> nodes = graph.getNodes()
                .stream()
                .map(this::toNodeResponse)
                .toList();

        List<GraphEdgeResponse> relationships =
                graph.getRelationships()
                .stream()
                .map(edge -> toEdgeResponse(graph, edge))
                .toList();

        SecurityGraphResponse response =
                new SecurityGraphResponse(
                        graph.getNodeCount(),
                        graph.getRelationshipCount(),
                        nodes,
                        relationships
                );

        return ResponseEntity.ok(response);
    }

    private GraphNodeResponse toNodeResponse(GraphNode node) {
        return new GraphNodeResponse(
                node.id(),
                node.type(),
                node.name(),
                node.description()
        );
    }

    private GraphEdgeResponse toEdgeResponse(
            SecurityGraph graph,
            GraphEdge edge) {

        return new GraphEdgeResponse(
                graph.getSource(edge).id(),
                graph.getTarget(edge).id(),
                edge.relationship(),
                edge.description()
        );
    }
}
