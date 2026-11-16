package com.apisentinel.sentinel.dto;

import com.apisentinel.sentinel.graph.RelationshipType;

public record GraphEdgeResponse(
        String source,
        String target,
        RelationshipType relationship,
        String description
) {
}