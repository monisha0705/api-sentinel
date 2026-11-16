package com.apisentinel.sentinel.dto;

import java.util.List;

public record EndpointSummary(
        String path,
        String method,
        String operationId,
        String summary,
        List<String> tags,
        int parameterCount,
        boolean requestBodyRequired
) {
}