package com.apisentinel.sentinel.service;

import com.apisentinel.sentinel.dto.EndpointSummary;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OpenApiParserService {

    public List<EndpointSummary> parse(String specification) {

        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result =
                new OpenAPIV3Parser().readContents(
                        specification, null, options
                );

        OpenAPI openAPI = result.getOpenAPI();

        if (openAPI == null || openAPI.getPaths() == null) {
            throw new IllegalArgumentException(
                    "Invalid OpenAPI specification: "
                    + result.getMessages()
            );
        }

        List<EndpointSummary> endpoints = new ArrayList<>();

        for (Map.Entry<String, PathItem> pathEntry
                : openAPI.getPaths().entrySet()) {

            String path = pathEntry.getKey();
            PathItem pathItem = pathEntry.getValue();

            Map<PathItem.HttpMethod, Operation> operations =
                    pathItem.readOperationsMap();

            for (Map.Entry<PathItem.HttpMethod, Operation> operationEntry
                    : operations.entrySet()) {

                PathItem.HttpMethod method = operationEntry.getKey();
                Operation operation = operationEntry.getValue();

                int parameterCount = operation.getParameters() == null
                        ? 0
                        : operation.getParameters().size();

                boolean requestBodyRequired =
                        operation.getRequestBody() != null
                        && Boolean.TRUE.equals(
                                operation.getRequestBody().getRequired()
                        );

                List<String> tags = operation.getTags() == null
                        ? List.of()
                        : operation.getTags();

                EndpointSummary endpoint = new EndpointSummary(
                        path,
                        method.name(),
                        operation.getOperationId(),
                        operation.getSummary(),
                        tags,
                        parameterCount,
                        requestBodyRequired
                );

                endpoints.add(endpoint);
            }
        }

        return endpoints;
    }
}
