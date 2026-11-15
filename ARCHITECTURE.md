# API Sentinel — System Architecture

## 1. Project Overview

**Project:** API Sentinel

**Research title:** A Graph-Guided, Multi-Identity Framework for Automated Detection of Authorization and Business Logic Vulnerabilities in REST APIs.

API Sentinel is an automated REST API security testing platform designed to identify authorization weaknesses and business logic vulnerabilities through identity-aware, workflow-aware, graph-guided test generation.

The platform models API endpoints, identities, roles, resources, ownership relationships, workflow states, and security policies. It uses this model to generate security test cases, execute HTTP requests against an authorized target, and validate observed behavior against expected security invariants.

The system is designed as a research implementation with reproducible experiments and evidence-backed findings. It does not depend on an LLM to identify vulnerabilities.

## 2. System Objectives

The system aims to:

1. Parse REST API specifications using OpenAPI.
2. Represent test identities and their roles.
3. Model resource ownership and access policies.
4. Represent business workflows and valid state transitions.
5. Construct a typed security graph.
6. Generate test sequences using rule-based and graph-guided approaches.
7. Execute generated tests against authorized REST API targets.
8. Validate HTTP responses and observable state changes against security invariants.
9. Store execution evidence and vulnerability findings.
10. Compare detection approaches using a controlled benchmark.

## 3. High-Level System Architecture

API Sentinel consists of three independently running applications.

### 3.1 API Sentinel — Main Security Scanner

The main backend is responsible for:

- Project and target management.
- OpenAPI specification parsing.
- Endpoint classification.
- Test identity configuration.
- Authorization and ownership modelling.
- Workflow and state modelling.
- Security graph construction.
- Test generation and prioritization.
- HTTP request execution.
- Response and state validation.
- Finding and evidence management.
- Report generation.
- Research experiment execution.

### 3.2 Sentinel Bank — Benchmark Target

Sentinel Bank is a separate REST API application created specifically for controlled security testing.

It simulates a banking workflow with accounts, payments, customers, and administrative operations.

It provides configurable vulnerable and secure implementations of selected security controls.

The benchmark target is independent of the scanner. API Sentinel must interact with it exclusively through HTTP requests.

The scanner must not query Sentinel Bank's database or inspect its internal implementation to determine whether a vulnerability exists.

### 3.3 React Frontend

The frontend provides an interface for:

- Creating projects.
- Registering API targets.
- Uploading OpenAPI specifications.
- Configuring test identities.
- Defining ownership and security policies.
- Defining workflows and invariants.
- Starting and monitoring scans.
- Inspecting findings and request evidence.
- Comparing scan modes.
- Viewing research metrics and reports.

The frontend is a presentation and configuration layer. The security analysis engine remains in the backend.

## 4. Architecture Diagram

```text
                    USER
                      |
                      v
             REACT FRONTEND
                      |
                REST / JSON
                      |
                      v
              API SENTINEL
              SPRING BOOT
                      |
       +--------------+--------------+
       |              |              |
       v              v              v
  API INTELLIGENCE  SECURITY      PROJECT AND
  AND OPENAPI       MODELLING     SCAN MANAGEMENT
       |              |              |
       +--------------+--------------+
                      |
                      v
               GRAPH ENGINE
                      |
                      v
              TEST GENERATOR
                      |
                      v
              HTTP EXECUTOR
                      |
                HTTP REQUESTS
                      |
                      v
               SENTINEL BANK
                REST API
                      |
                      v
              HTTP RESPONSES
                      |
                      v
             RESPONSE VALIDATOR
                      |
                      v
              EVIDENCE ENGINE
                      |
                      v
             FINDINGS AND REPORTS
                      |
                      v
                 POSTGRESQL
```

## 5. Technology Stack

| Layer | Technology |
|---|---|
| Main backend | Java 21 |
| Backend framework | Spring Boot |
| Target application | Java 21 + Spring Boot |
| Frontend | React + TypeScript |
| Frontend tooling | Vite |
| Styling | Tailwind CSS |
| Database | PostgreSQL |
| Persistence | Spring Data JPA |
| Database migrations | Flyway |
| API specification parsing | Swagger Parser |
| Graph algorithms | JGraphT |
| HTTP execution | Java HttpClient |
| Application security | Spring Security |
| Backend testing | JUnit 5 |
| Property-based testing | jqwik |
| Containerization | Docker Compose |

## 6. Main Backend Modules

The API Sentinel backend will be organized into the following logical modules.

### 6.1 Project Management

Responsibilities:

- Create and manage security testing projects.
- Associate targets, specifications, identities, policies, and scans with projects.
- Enforce project-level data boundaries.

### 6.2 Target Management

Responsibilities:

- Register authorized REST API targets.
- Store target base URLs.
- Configure execution limits and timeouts.
- Validate target configuration.
- Enforce explicit authorization before scanning.

### 6.3 OpenAPI Intelligence

Responsibilities:

- Parse OpenAPI specifications.
- Extract paths, HTTP methods, parameters, request bodies, and response definitions.
- Identify path parameters and potential resource identifiers.
- Build a normalized endpoint representation.
- Preserve the original specification for reference.

The parser must not assume that an endpoint is vulnerable merely because of its name or HTTP method.

### 6.4 Identity and Authorization Model

Responsibilities:

- Define test identities.
- Associate identities with roles.
- Configure credentials and authentication information.
- Model endpoint access requirements.
- Represent expected authorization relationships.

Example identities:

| Identity | Role | Description |
|---|---|---|
| Alice | CUSTOMER | Owns account 101 |
| Bob | CUSTOMER | Owns account 202 |
| Admin | ADMIN | Administrative test identity |

### 6.5 Resource Ownership Model

Responsibilities:

- Represent resources and their owners.
- Associate resources with identities.
- Model expected ownership restrictions.
- Generate cross-identity access tests.

Example:

- Alice requests her own account.
- Alice requests Bob's account.
- Bob requests Alice's account.

The validator compares actual behavior with the configured access policy.

### 6.6 Workflow Engine

Responsibilities:

- Define business workflows.
- Define workflow states.
- Define valid transitions.
- Define operations that trigger transitions.
- Identify invalid or unauthorized transitions.

Example payment workflow:

```text
PENDING
   |
   +----> APPROVED
   |
   +----> CANCELLED

APPROVED
   |
   +----> COMPLETED
```

The actual allowed transitions must be configurable. The diagram is an illustrative workflow, not a universal banking rule.

### 6.7 Security Invariant Engine

Responsibilities:

- Define expected security properties.
- Evaluate authorization requirements.
- Validate ownership restrictions.
- Validate role restrictions.
- Validate workflow transition rules.
- Produce structured validation results.

Example invariants:

- A customer must not access another customer's private account.
- Only an administrator may approve a payment.
- A completed payment must not be modified.
- A cancelled payment must not be approved.

### 6.8 Graph Engine

The graph engine represents the relationships between security entities.

Node types:

- Identity
- Role
- Resource
- Endpoint
- Operation
- Workflow state

Edge types:

- HAS_ROLE
- OWNS
- CAN_ACCESS
- INVOKES
- TRANSITIONS_TO
- REQUIRES_ROLE
- OPERATES_ON

Responsibilities:

- Build the typed security graph.
- Maintain graph relationships.
- Traverse graph paths.
- Perform bounded path exploration.
- Detect relevant identity-resource-endpoint combinations.
- Generate candidate test sequences.
- Prioritize tests.
- Maintain reproducible graph snapshots.

JGraphT will be used for graph representation and traversal algorithms.

### 6.9 Detection Engine

The detection engine will contain separate detectors for:

**BOLA / IDOR**
- Test access to resources owned by different identities.
- Compare expected and observed ownership enforcement.

**BFLA**
- Test access to operations restricted to specific roles.
- Compare the invoking identity's privileges with configured requirements.

**Business Logic Vulnerabilities**
- Test invalid workflow transitions.
- Test unauthorized operations.
- Test prohibited modifications to terminal states.

Each detector must use explicit policies and evidence rather than relying on endpoint naming conventions alone.

### 6.10 Test Generation Engine

The test generation engine will support three modes.

**RULE_BASED**

Generates tests using predefined authorization, ownership, and workflow rules.

**GRAPH_GUIDED**

Generates tests by exploring relationships and paths in the security graph.

**HYBRID**

Combines rule-based test generation with graph-guided exploration.

Generated tests must be structured test cases containing:

- Test identifier.
- Target endpoint.
- HTTP method.
- Test identity.
- Authentication context.
- Request parameters.
- Request body.
- Expected security invariant.
- Test rationale.
- Execution constraints.

The generator produces test definitions. It does not execute HTTP requests directly.

### 6.11 HTTP Execution Engine

Responsibilities:

- Execute generated HTTP requests.
- Apply the configured identity's authentication context.
- Handle request timeouts.
- Enforce request limits.
- Capture response status, headers, and body.
- Record execution duration.
- Handle connection failures.
- Redact sensitive information.

Only this module is responsible for sending requests to the target API.

### 6.12 Validation Engine

Responsibilities:

- Compare observed behavior with expected security invariants.
- Evaluate response content and relevant observable state changes.
- Distinguish authorization failures from infrastructure failures.
- Validate business workflow behavior.
- Produce a structured validation result.

An HTTP 200 response alone must not automatically be treated as proof of a vulnerability.

Similarly, an HTTP 403 response alone must not automatically prove that every security invariant is correctly enforced.

### 6.13 Evidence and Findings Engine

Each finding must contain:

- Finding identifier.
- Vulnerability category.
- Affected endpoint.
- Test identity.
- Resource involved, where applicable.
- Expected behavior.
- Observed behavior.
- Supporting request and response evidence.
- Validation result.
- Severity assessment.
- Timestamp.
- Scan identifier.

Finding confidence states:

- CONFIRMED
- SUSPECTED
- INCONCLUSIVE

A confirmed finding requires sufficient evidence that the expected security property was violated.

Sensitive credentials, tokens, and personal information must be redacted from stored evidence.

### 6.14 Reporting and Research Engine

Responsibilities:

- Generate scan reports.
- Summarize detected scenarios.
- Calculate precision, recall, and F1 score.
- Calculate false positive rate.
- Measure endpoint and model coverage.
- Measure execution time.
- Compare the three scan modes.
- Export experiment results for analysis.

## 7. Sentinel Bank Benchmark Design

Sentinel Bank will provide a controlled REST API with seeded test data.

### 7.1 Initial Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | /api/accounts | List accounts |
| GET | /api/accounts/{id} | Retrieve an account |
| POST | /api/payments | Create a payment |
| GET | /api/payments/{id} | Retrieve a payment |
| PUT | /api/payments/{id} | Modify a payment |
| POST | /api/payments/{id}/approve | Approve a payment |
| POST | /api/payments/{id}/cancel | Cancel a payment |
| GET | /api/admin/payments | Administrative payment listing |

### 7.2 Seeded Data

- Alice is a CUSTOMER and owns account 101.
- Bob is a CUSTOMER and owns account 202.
- Admin has the ADMIN role.

The benchmark must use synthetic data only.

### 7.3 Controlled Security Scenarios

| Scenario | Vulnerable configuration | Secure configuration |
|---|---|---|
| Account ownership | Missing ownership validation | Ownership enforced |
| Payment ownership | Missing owner validation | Owner validation enforced |
| Payment approval | Customer can approve | Admin-only approval |
| Completed payment | Modification permitted | Modification rejected |
| Invalid transition | Invalid transition accepted | Invalid transition rejected |

Each scenario must have a documented ground-truth label and reproducible test procedure.

## 8. Data and Application Boundaries

API Sentinel and Sentinel Bank must use separate databases.

**API Sentinel database**

Stores:

- Projects
- Targets
- API specifications
- Test identities and credentials
- Roles and permissions
- Resources and ownership models
- Policies and invariants
- Workflows and transitions
- Graph snapshots
- Scans and test cases
- Execution evidence
- Findings and reports

**Sentinel Bank database**

Stores:

- Synthetic users
- Accounts
- Payments
- Payment states
- Benchmark configuration

The scanner must not read or write the Sentinel Bank database.

Communication between the applications must occur through the target's HTTP interface.

## 9. Security and Safety Requirements

- Scan only targets explicitly authorized by their owners.
- Default to local benchmark testing during development.
- Require explicit configuration before state-changing tests.
- Enforce request limits and timeouts.
- Avoid uncontrolled concurrent requests.
- Never store plaintext passwords in the database.
- Do not log authentication tokens or passwords.
- Redact sensitive request and response data.
- Keep target credentials separate from ordinary application configuration.
- Prevent scans from accessing unintended hosts.
- Record scan configuration and execution history.
- Provide scan cancellation.
- Clearly distinguish confirmed findings from suspected findings.

## 10. Deployment Architecture

During development, the applications will run locally.

Planned services:

- API Sentinel backend
- Sentinel Bank backend
- React frontend
- PostgreSQL database for API Sentinel
- PostgreSQL database for Sentinel Bank

Docker Compose will be used to define and run the complete environment.

The two databases must remain logically and operationally separate.

## 11. Research Evaluation Architecture

The research engine will compare:

1. RULE_BASED
2. GRAPH_GUIDED
3. HYBRID

All approaches must be evaluated using equivalent:

- Benchmark targets.
- Ground-truth scenarios.
- Test identities.
- Request budgets.
- Execution time limits.
- Network conditions.

Primary metrics:

- True positives (TP)
- False positives (FP)
- False negatives (FN)
- Precision
- Recall
- F1 score
- False positive rate
- Vulnerability detection rate
- Endpoint/model coverage
- Execution time

The classification unit will be a unique ground-truth vulnerability scenario, not an individual HTTP request.

Experiments must use documented configurations and reproducible seeds where applicable.

The research must report limitations and must not claim superiority without experimental evidence.

## 12. Engineering Principles

1. Deterministic security analysis takes priority over AI-generated conclusions.
2. Every finding must be supported by execution evidence.
3. The scanner and benchmark target must remain independent.
4. Security policies must be explicit and configurable.
5. Graph exploration must be bounded and reproducible.
6. Modules must have clear responsibilities.
7. Every major module must have automated tests.
8. No feature is considered complete until it is implemented and tested.
9. Research results must be based on actual benchmark executions.
10. Architectural decisions and implementation progress must be documented.

## 13. Initial Implementation Order

The implementation will follow this order:

1. Repository and project foundation.
2. Spring Boot backend foundations.
3. PostgreSQL integration and Flyway migrations.
4. Sentinel Bank REST API.
5. OpenAPI parsing and endpoint modelling.
6. Identity, ownership, and authorization modelling.
7. Workflow and invariant modelling.
8. Typed security graph construction.
9. Rule-based test generation.
10. Graph-guided test generation.
11. HTTP execution engine.
12. Response and state validation.
13. Evidence and findings persistence.
14. Research evaluation engine.
15. React frontend.
16. Integration, benchmark experiments, and reporting.

---

**Architecture status:** Design documented. Implementation and verification are tracked separately.
