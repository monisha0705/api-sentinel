# API Sentinel — Implemented API Contracts

## 1. Purpose and scope

This document describes the HTTP contracts currently implemented by the API Sentinel scanner backend and the separate Sentinel Bank benchmark application.

It is an implementation reference, not a specification of every planned platform capability. The contracts below were checked against the Spring Boot controller and DTO/record definitions on the `main` branch.

## 2. Services and local base URLs

| Service | Default base URL | Purpose |
|---|---|---|
| API Sentinel backend | `http://localhost:8080` | Specification parsing, graph construction, test generation/execution, finding detection and retrieval |
| Sentinel Bank benchmark | `http://localhost:8081` | Target REST API exercised by the scanner |
| Frontend dev server | `http://localhost:5173` | React application; development proxy forwards `/api` and `/actuator` to the backend |

Scanner endpoints use the `/api/v1` prefix. Sentinel Bank is a separate target service and its routes do not use that scanner prefix.

## 3. Conventions

- Requests and responses use JSON unless an endpoint explicitly consumes multipart form data.
- The specification upload endpoint expects a multipart field named `file`.
- The controller methods return their DTOs/lists directly. Do not assume a universal `data` or `meta` response envelope.
- The request/response shapes below reflect the Java records and controller signatures. Validation and error behavior should be treated as endpoint-specific.
- The scanner's benchmark HTTP requests use configured benchmark identities. This is separate from authentication for users of the scanner application.

## 4. API Sentinel backend endpoints

### 4.1 Upload and parse an OpenAPI specification

**POST** `/api/v1/specifications/upload`

Content type: `multipart/form-data`

Form field:

| Name | Type | Description |
|---|---|---|
| `file` | File | OpenAPI specification content |

Successful response: `200 OK`, a JSON array of `EndpointSummary` objects.

Example response:

```json
[
  {
    "path": "/api/accounts/{accountId}",
    "method": "GET",
    "operationId": "getAccount",
    "summary": "Get account",
    "tags": ["accounts"],
    "parameterCount": 1,
    "requestBodyRequired": false
  }
]
```

The `EndpointSummary` fields are:
- `path`: endpoint path
- `method`: HTTP method
- `operationId`: OpenAPI operation ID
- `summary`: operation summary
- `tags`: operation tags
- `parameterCount`: number of parameters
- `requestBodyRequired`: whether the operation requires a request body

An empty file returns `400 Bad Request` with a text message. An invalid specification returns `400 Bad Request` with the parser error message. A file-read failure returns `500 Internal Server Error` with a text message.

### 4.2 Build the security graph

**POST** `/api/v1/graph/build`

Content type: `application/json`

Request body: a JSON array of `EndpointSummary` objects. The controller also permits an absent body; in that case the service receives `null` and builds its benchmark graph.

Successful response: `200 OK`, a `SecurityGraphResponse`.

```json
{
  "nodeCount": 2,
  "relationshipCount": 1,
  "nodes": [
    {
      "id": "node-id",
      "type": "ENDPOINT",
      "name": "Get account",
      "description": "Example graph node"
    }
  ],
  "relationships": [
    {
      "source": "node-id",
      "target": "resource-id",
      "relationship": "ACCESSES",
      "description": "Example graph relationship"
    }
  ]
}
```

The example identifiers, enum values and descriptions are illustrative. The response fields are `nodeCount`, `relationshipCount`, `nodes` and `relationships`. Each node contains `id`, `type`, `name` and `description`; each relationship contains `source`, `target`, `relationship` and `description`.

### 4.3 Generate security test cases

**POST** `/api/v1/tests/generate`

Content type: `application/json`

Request body: a JSON array of `EndpointSummary` objects (same shape as section 4.1).

Successful response: `200 OK`, a JSON array of `SecurityTestCase` objects.

Each test case contains:

| Field | Type |
|---|---|
| `testId` | string |
| `testType` | string |
| `identity` | string |
| `method` | string |
| `endpoint` | string |
| `targetResource` | string |
| `description` | string |
| `expectedStatus` | integer |
| `requestBody` | string or null |
| `precondition` | string or null |
| `expectedState` | string or null |

The first eight fields are also supported by a backward-compatible constructor that leaves the final three fields null.

### 4.4 Execute generated test cases

**POST** `/api/v1/tests/execute`

Content type: `application/json`

Request body: a JSON array of `SecurityTestCase` objects as described in section 4.3.

Successful response: `200 OK`, a JSON array of `TestExecutionResult` objects.

Each result contains:

| Field | Type |
|---|---|
| `testId` | string |
| `identity` | string |
| `endpoint` | string |
| `expectedStatus` | integer |
| `actualStatus` | integer |
| `passed` | boolean |
| `responseBody` | string |
| `executedAt` | ISO-8601 timestamp |

### 4.5 Execute the stateful payment authorization scenario

**POST** `/api/v1/tests/execute/stateful`

Request body: none.

Successful response: `200 OK`, a `StatefulTestExecutionResult` object. The controller runs the unauthorized payment-approval workflow and returns the scenario outcome.

Response fields:

| Field | Type |
|---|---|
| `scenarioId` | string |
| `scenarioName` | string |
| `resourceId` | string |
| `initialState` | string |
| `finalState` | string |
| `expectedFinalState` | string |
| `invariantPreserved` | boolean |
| `passed` | boolean |
| `steps` | array of `TestExecutionResult` |
| `executedAt` | ISO-8601 timestamp |

The implemented scenario creates a payment, attempts an approval using the customer identity, then retrieves the payment to verify its state. The expected final state is `PENDING`.

### 4.6 Detect findings from test execution

**POST** `/api/v1/findings/detect`

Content type: `application/json`

Request body:

```json
{
  "testCases": [],
  "executionResults": []
}
```

- `testCases`: array of `SecurityTestCase` objects (section 4.3)
- `executionResults`: array of `TestExecutionResult` objects (section 4.4)

Successful response: `200 OK`, a JSON array of detected `SecurityFinding` objects. The controller persists the returned findings before responding.

### 4.7 Detect findings from a stateful scenario

**POST** `/api/v1/findings/detect/stateful`

Content type: `application/json`

Request body: a `StatefulTestExecutionResult` object as described in section 4.5.

Successful response: `200 OK`, a JSON array of detected `SecurityFinding` objects. The controller persists the returned findings before responding.

### 4.8 Retrieve persisted findings

**GET** `/api/v1/findings`

Request body: none.

Successful response: `200 OK`, a JSON array of persisted `SecurityFindingEntity` objects.

## 5. Sentinel Bank benchmark API

Sentinel Bank is the target application that the scanner exercises. Its routes are not scanner-management endpoints. The stateful workflow currently uses these target operations:

| Method | Path | Role in the workflow |
|---|---|---|
| `POST` | `/api/payments` | Create a payment fixture |
| `POST` | `/api/payments/{paymentId}/approve` | Attempt payment approval |
| `GET` | `/api/payments/{paymentId}` | Read payment state after the approval attempt |

The stateful test sends a payment creation body containing `amount`, `currency`, `beneficiaryReference` and `sourceAccountId`. It expects creation to return HTTP `201`, the initial payment state to be `PENDING`, and an unauthorized customer approval to be rejected with HTTP `403`. It then verifies that the payment remains `PENDING`.

The benchmark application's authorization mode is configurable. Run security tests only against an instance you own or are explicitly authorized to assess.

## 6. Authentication and authorization boundaries

The scanner backend's controller contracts listed above do not define the planned platform-user login, refresh-token, logout, project-membership or role-management API described in earlier drafts of this document. Those routes and their bearer-token behavior are not documented here as implemented features.

The benchmark target has its own test identities and authorization behavior. Do not confuse benchmark identity credentials with scanner-platform user authentication.

## 7. Error handling and status codes

Only the status behavior explicitly visible in the controller implementations is stated above. The specification upload controller returns:
- `400` for an empty upload or an invalid specification
- `500` when the uploaded file cannot be read

The other controllers return their successful DTO/list values directly. This document does not assert a global error envelope, trace ID format, pagination contract, or uniform validation behavior because those are not established by the controller contracts reviewed for this document.

## 8. Implementation status

This document is intentionally limited to the implemented controller surface described above. Features such as platform-user authentication, project and membership management, refresh-token rotation, generic pagination, and a universal response envelope must be documented as planned only if and when they are implemented and tested.

When an endpoint changes, update this document alongside its controller/DTO changes and corresponding tests.
