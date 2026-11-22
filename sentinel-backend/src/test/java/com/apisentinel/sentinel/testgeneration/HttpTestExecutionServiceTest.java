
package com.apisentinel.sentinel.testgeneration;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class HttpTestExecutionServiceTest {

    private HttpServer server;
    private HttpTestExecutionService executionService;

    private String receivedAuthorization;
    private String receivedPath;
    private String receivedMethod;
    private String receivedBody;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(
                new InetSocketAddress("localhost", 0), 0
        );

        server.createContext("/api/accounts/202", exchange -> {
            receivedAuthorization =
                    exchange.getRequestHeaders().getFirst("Authorization");

            receivedPath = exchange.getRequestURI().getPath();
            receivedMethod = exchange.getRequestMethod();

            receivedBody = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            byte[] response = "{\"accountId\":202}".getBytes(
                    StandardCharsets.UTF_8
            );

            exchange.getResponseHeaders().set(
                    "Content-Type", "application/json"
            );

            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        server.start();

        executionService = new HttpTestExecutionService();

        ReflectionTestUtils.setField(
                executionService,
                "targetBaseUrl",
                "http://localhost:" + server.getAddress().getPort()
        );

        ReflectionTestUtils.setField(
                executionService, "alicePassword", "alice-test-password"
        );

        ReflectionTestUtils.setField(
                executionService, "bobPassword", "bob-test-password"
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldExecuteRequestUsingAliceCredentials() {

        SecurityTestCase testCase = createTestCase(
                "alice", "/api/accounts/202", 200
        );

        TestExecutionResult result = executionService.execute(testCase);

        String expectedCredentials = Base64.getEncoder().encodeToString(
                "sentinel_alice:alice-test-password".getBytes(StandardCharsets.UTF_8)
        );

        assertEquals("Basic " + expectedCredentials, receivedAuthorization);
        assertEquals("/api/accounts/202", receivedPath);
        assertEquals(200, result.actualStatus());
        assertTrue(result.passed());
        assertEquals("{\"accountId\":202}", result.responseBody());
    }

    @Test
    void shouldExecuteRequestUsingBobCredentials() {

        SecurityTestCase testCase = createTestCase(
                "bob", "/api/accounts/202", 200
        );

        TestExecutionResult result = executionService.execute(testCase);

        String expectedCredentials = Base64.getEncoder().encodeToString(
                "sentinel_bob:bob-test-password".getBytes(StandardCharsets.UTF_8)
        );

        assertEquals("Basic " + expectedCredentials, receivedAuthorization);
        assertEquals(200, result.actualStatus());
        assertTrue(result.passed());
    }

    @Test
    void shouldMarkTestAsFailedWhenStatusDoesNotMatch() {

        SecurityTestCase testCase = createTestCase(
                "alice", "/api/accounts/202", 404
        );

        TestExecutionResult result = executionService.execute(testCase);

        assertEquals(404, result.expectedStatus());
        assertEquals(200, result.actualStatus());
        assertFalse(result.passed());
    }

    @Test
    void shouldRejectUnsupportedIdentity() {

        SecurityTestCase testCase = createTestCase(
                "unknown-user", "/api/accounts/202", 200
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> executionService.execute(testCase)
        );
    }

    @Test
    void shouldRejectAdminWhenCredentialsAreMissing() {

        SecurityTestCase testCase = createTestCase(
                "admin", "/api/accounts/202", 200
        );

        assertThrows(
                IllegalStateException.class,
                () -> executionService.execute(testCase)
        );
    }

    @Test
    void shouldRejectMissingCredentials() {

        ReflectionTestUtils.setField(
                executionService, "alicePassword", ""
        );

        SecurityTestCase testCase = createTestCase(
                "alice", "/api/accounts/202", 200
        );

        assertThrows(
                IllegalStateException.class,
                () -> executionService.execute(testCase)
        );
    }

    @Test
    void shouldRejectEndpointWithoutLeadingSlash() {

        SecurityTestCase testCase = createTestCase(
                "alice", "api/accounts/202", 200
        );

        assertThrows(
                IllegalStateException.class,
                () -> executionService.execute(testCase)
        );
    }

    @Test
    void shouldExecuteRequestUsingAdminCredentials() {

        ReflectionTestUtils.setField(
                executionService, "adminPassword", "admin-test-password"
        );

        SecurityTestCase testCase = createTestCase(
                "admin", "/api/accounts/202", 200
        );

        TestExecutionResult result = executionService.execute(testCase);

        String expectedCredentials = Base64.getEncoder().encodeToString(
                "sentinel_admin:admin-test-password".getBytes(StandardCharsets.UTF_8)
        );

        assertEquals("Basic " + expectedCredentials, receivedAuthorization);
        assertEquals(200, result.actualStatus());
        assertTrue(result.passed());
    }

    @Test
    void shouldSendJsonRequestBodyForPostRequest() {

        String requestBody = """
                {
                    "amount": 250.00,
                    "currency": "INR",
                    "beneficiaryReference": "TEST-PAYMENT",
                    "sourceAccountId": 101
                }
                """;

        SecurityTestCase testCase = new SecurityTestCase(
                "HTTP-POST-TEST",
                "BUSINESS_LOGIC",
                "alice",
                "POST",
                "/api/accounts/202",
                "account-202",
                "POST request body test",
                200,
                requestBody,
                null,
                null
        );

        TestExecutionResult result = executionService.execute(testCase);

        assertEquals("POST", receivedMethod);
        assertEquals(requestBody, receivedBody);
        assertEquals(200, result.actualStatus());
        assertTrue(result.passed());
    }

    private SecurityTestCase createTestCase(
            String identity,
            String endpoint,
            int expectedStatus
    ) {
        return new SecurityTestCase(
                "HTTP-TEST-" + identity,
                "BOLA",
                identity,
                "GET",
                endpoint,
                "account-202",
                "HTTP execution unit test",
                expectedStatus
        );
    }
}
