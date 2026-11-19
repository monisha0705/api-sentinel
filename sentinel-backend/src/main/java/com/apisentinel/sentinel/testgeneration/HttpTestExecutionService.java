
package com.apisentinel.sentinel.testgeneration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class HttpTestExecutionService {

    private final HttpClient httpClient;

    @Value("${sentinel.target.base-url:http://localhost:8081}")
    private String targetBaseUrl;

    @Value("${sentinel.credentials.alice:}")
    private String alicePassword;

    @Value("${sentinel.credentials.bob:}")
    private String bobPassword;

    @Value("${sentinel.credentials.admin:}")
    private String adminPassword;

    public HttpTestExecutionService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public TestExecutionResult execute(SecurityTestCase testCase) {

        String password = switch (testCase.identity().toLowerCase()) {
            case "alice" -> alicePassword;
            case "bob" -> bobPassword;
            case "admin" -> adminPassword;
            default -> throw new IllegalArgumentException(
                    "Unsupported benchmark identity: " + testCase.identity()
            );
        };

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Credentials are not configured for " + testCase.identity()
            );
        }

        try {
            String credentials = testCase.identity() + ":" + password;

            String authorization = "Basic " +
                    Base64.getEncoder().encodeToString(
                            credentials.getBytes(StandardCharsets.UTF_8)
                    );

            String endpoint = testCase.endpoint();

            if (endpoint == null || !endpoint.startsWith("/")
                    || endpoint.startsWith("//")) {
                throw new IllegalArgumentException(
                        "Endpoint must be an absolute path starting with /"
                );
            }

            URI uri = URI.create(
                    targetBaseUrl.replaceAll("/+$", "") + endpoint
            );

            HttpRequest.BodyPublisher bodyPublisher;

            if (testCase.requestBody() == null) {
                bodyPublisher = HttpRequest.BodyPublishers.noBody();
            } else {
                bodyPublisher = HttpRequest.BodyPublishers.ofString(
                        testCase.requestBody(),
                        StandardCharsets.UTF_8
                );
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", authorization)
                    .header("Accept", "application/json");

            if (testCase.requestBody() != null) {
                requestBuilder.header("Content-Type", "application/json");
            }

            HttpRequest request = requestBuilder
                    .method(testCase.method(), bodyPublisher)
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            boolean passed =
                    response.statusCode() == testCase.expectedStatus();

            return new TestExecutionResult(
                    testCase.testId(),
                    testCase.identity(),
                    endpoint,
                    testCase.expectedStatus(),
                    response.statusCode(),
                    passed,
                    response.body(),
                    Instant.now()
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "HTTP test execution was interrupted", exception
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to execute test " + testCase.testId(), exception
            );
        }
    }
}
