
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

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(
                new InetSocketAddress("localhost", 0), 0
        );

        server.createContext("/api/accounts/202", exchange -> {
            receivedAuthorization =
                    exchange.getRequestHeaders().getFirst("Authorization");
            receivedPath = exchange.getRequestURI().getPath();

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
                "alice:alice-test-password".getBytes(StandardCharsets.UTF_8)
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
                "bob:bob-test-password".getBytes(StandardCharsets.UTF_8)
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
                "admin", "/api/accounts/202", 200
        );

        assertThrows(
                IllegalArgumentException.class,
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
