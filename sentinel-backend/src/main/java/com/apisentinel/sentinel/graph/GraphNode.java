package com.apisentinel.sentinel.graph;

public record GraphNode(
        String id,
        NodeType type,
        String name,
        String description
) {
}
