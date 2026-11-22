package com.apisentinel.sentinel.service;

import com.apisentinel.sentinel.dto.EndpointSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiParserServiceTest {

    private final OpenApiParserService parser = new OpenApiParserService();

    @Test
    void shouldExtractPathMethodAndOperationMetadata() {
        String specification = """
                {
                  "openapi": "3.0.3",
                  "info": {"title": "Test API", "version": "1.0.0"},
                  "paths": {
                    "/api/accounts/{id}": {
                      "get": {
                        "operationId": "getAccount",
                        "summary": "Get account",
                        "tags": ["accounts"],
                        "parameters": [
                          {
                            "name": "id",
                            "in": "path",
                            "required": true,
                            "schema": {"type": "integer"}
                          }
                        ]
                      }
                    }
                  }
                }
                """;

        List<EndpointSummary> endpoints = parser.parse(specification);

        assertEquals(1, endpoints.size());
        EndpointSummary endpoint = endpoints.get(0);
        assertEquals("/api/accounts/{id}", endpoint.path());
        assertEquals("GET", endpoint.method());
        assertEquals("getAccount", endpoint.operationId());
        assertEquals("Get account", endpoint.summary());
        assertEquals(List.of("accounts"), endpoint.tags());
        assertEquals(1, endpoint.parameterCount());
        assertFalse(endpoint.requestBodyRequired());
    }

    @Test
    void shouldRejectInvalidSpecification() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse("{ not-valid-json }")
        );
    }
}
