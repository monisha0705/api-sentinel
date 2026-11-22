package com.apisentinel.sentinel.testgeneration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class HttpTestExecutionService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${sentinel.target.base-url:http://localhost:8081}")
    private String targetBaseUrl;

    @Value("${sentinel.credentials.alice:}")
    private String alicePassword;

    @Value("${sentinel.credentials.bob:}")
    private String bobPassword;

    @Value("${sentinel.credentials.admin:}")
    private String adminPassword;

    public HttpTestExecutionService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public TestExecutionResult execute(SecurityTestCase testCase) {

        String identity = testCase.identity();
        String password = getPassword(identity);
        String targetUsername = getTargetUsername(identity);

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Credentials are not configured for " + identity
            );
        }

        try {
            String endpoint = testCase.endpoint();

            if (endpoint == null || !endpoint.startsWith("/")
                    || endpoint.startsWith("//")
                    || endpoint.contains("?")
                    || endpoint.contains("#")
                    || endpoint.contains("\\")) {
                throw new IllegalArgumentException(
                        "Endpoint must be an absolute path starting with /"
                );
            }

            if (endpoint.equals("/api/payments/{id}/approve")) {

                String fixtureIdentity =
                        identity.equalsIgnoreCase("admin")
                                ? "alice"
                                : identity;

                String fixturePassword = getPassword(fixtureIdentity);
                String fixtureUsername =
                        getTargetUsername(fixtureIdentity);

                UUID paymentId = createPaymentAndResolveId(
                        fixtureIdentity,
                        fixtureUsername,
                        fixturePassword
                );

                endpoint = "/api/payments/" + paymentId + "/approve";
            }

            HttpResponse<String> response = sendRequest(
                    targetUsername,
                    testCase.method(),
                    endpoint,
                    password,
                    testCase.requestBody()
            );

            boolean passed =
                    response.statusCode() == testCase.expectedStatus();

            return new TestExecutionResult(
                    testCase.testId(),
                    identity,
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
                    "Failed to execute test " + testCase.testId(),
                    exception
            );
        }
    }

    private String getPassword(String identity) {
        return switch (identity.toLowerCase()) {
            case "alice" -> alicePassword;
            case "bob" -> bobPassword;
            case "admin" -> adminPassword;
            default -> throw new IllegalArgumentException(
                    "Unsupported benchmark identity: " + identity
            );
        };
    }

    private String getTargetUsername(String identity) {
        return switch (identity.toLowerCase()) {
            case "alice" -> "sentinel_alice";
            case "bob" -> "sentinel_bob";
            case "admin" -> "sentinel_admin";
            default -> throw new IllegalArgumentException(
                    "Unsupported benchmark identity: " + identity
            );
        };
    }

    private UUID createPaymentAndResolveId(
            String identity,
            String targetUsername,
            String password
    ) throws Exception {

        int accountId = switch (identity.toLowerCase()) {
            case "alice" -> 203;
            case "bob" -> 204;
            default -> throw new IllegalArgumentException(
                    "Payment fixture identity must be Alice or Bob"
            );
        };

        String body = """
                {
                    "amount": 250.00,
                    "currency": "INR",
                    "beneficiaryReference": "API-SENTINEL-BFLA-%s",
                    "sourceAccountId": %d
                }
                """.formatted(identity.toUpperCase(), accountId);

        HttpResponse<String> response = sendRequest(
                targetUsername,
                "POST",
                "/api/payments",
                password,
                body
        );

        if (response.statusCode() != 201) {
            throw new IllegalStateException(
                    "Could not create BFLA payment fixture. HTTP "
                            + response.statusCode()
                            + ". Response: " + response.body()
            );
        }

        JsonNode json = objectMapper.readTree(response.body());
        String paymentId = json.path("id").asText();

        try {
            return UUID.fromString(paymentId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Payment creation response contained an invalid UUID: "
                            + paymentId,
                    exception
            );
        }
    }

    private HttpResponse<String> sendRequest(
            String username,
            String method,
            String endpoint,
            String password,
            String requestBody
    ) throws Exception {

        String credentials = username + ":" + password;

        String authorization = "Basic " +
                Base64.getEncoder().encodeToString(
                        credentials.getBytes(StandardCharsets.UTF_8)
                );

        URI baseUri = URI.create(targetBaseUrl);

        if (baseUri.getHost() == null
                || !("http".equalsIgnoreCase(baseUri.getScheme())
                        || "https".equalsIgnoreCase(baseUri.getScheme()))
                || baseUri.getUserInfo() != null
                || baseUri.getQuery() != null
                || baseUri.getFragment() != null) {
            throw new IllegalStateException(
                    "Target base URL must be an HTTP(S) URL without user info, query, or fragment"
            );
        }

        URI uri = baseUri.resolve(endpoint);

        if (!baseUri.getScheme().equalsIgnoreCase(uri.getScheme())
                || !baseUri.getHost().equalsIgnoreCase(uri.getHost())
                || baseUri.getPort() != uri.getPort()) {
            throw new IllegalArgumentException(
                    "Test endpoint must remain on the configured target host"
            );
        }

        HttpRequest.BodyPublisher bodyPublisher =
                requestBody == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(
                                requestBody, StandardCharsets.UTF_8
                        );

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", authorization)
                .header("Accept", "application/json");

        if (requestBody != null) {
            builder.header("Content-Type", "application/json");
        }

        HttpRequest request = builder
                .method(method, bodyPublisher)
                .build();

        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }
}
