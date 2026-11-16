package com.apisentinel.sentinel.dto;

import com.apisentinel.sentinel.graph.NodeType;

public record GraphNodeResponse(
        String id,
        NodeType type,
        String name,
        String description
) {
}