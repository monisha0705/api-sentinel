package com.apisentinel.sentinel.dto;

import java.util.List;

public record SecurityGraphResponse(
        int nodeCount,
        int relationshipCount,
        List<GraphNodeResponse> nodes,
        List<GraphEdgeResponse> relationships
) {
}

