# API Sentinel — System Architecture

**Research Title:** A Graph-Guided, Multi-Identity Framework for Automated Detection of Authorization and Business Logic Vulnerabilities in REST APIs

**Project:** API Sentinel  
**Architecture Status:** Implemented foundation with planned research extensions  
**Primary Language:** Java 21  
**Backend Framework:** Spring Boot  
**Frontend:** React + TypeScript  
**Database:** PostgreSQL

---

## 1. Project Overview

API Sentinel is an automated REST API security testing platform designed to identify authorization weaknesses and business logic vulnerabilities through identity-aware, workflow-aware, and graph-guided security testing.

The system analyzes REST API specifications, models relationships between API operations and security entities, generates security test cases, executes HTTP requests against authorized targets, and evaluates observed behavior against expected security properties.

The research focuses on three important vulnerability categories:

- **Broken Object Level Authorization (BOLA):** Unauthorized access to resources belonging to another identity.
- **Broken Function Level Authorization (BFLA):** Unauthorized access to functions or operations restricted by role or privilege.
- **Business Logic Vulnerabilities:** Violations of valid application workflows, authorization requirements, or permitted state transitions.

API Sentinel is designed as a deterministic security analysis system. Vulnerability detection relies on explicit security models, graph relationships, test generation, HTTP execution, and evidence-based validation rather than relying on an LLM to decide whether an API is vulnerable.

The implementation uses a separate benchmark application, Sentinel Bank, to provide controlled and reproducible security testing scenarios.

## 2. Research Objectives

The primary objectives are:

1. Parse REST API specifications using OpenAPI.
2. Extract and normalize API endpoints and operations.
3. Represent security-relevant entities and relationships.
4. Model identities, roles, resources, and ownership relationships.
5. Construct a typed security graph.
6. Generate security test cases using structured rules and graph relationships.
7. Execute generated tests against authorized REST API targets.
8. Validate authorization behavior and observable workflow state changes.
9. Detect BOLA, BFLA, and business logic vulnerabilities.
10. Persist findings and supporting execution evidence.
11. Develop a controlled benchmark for reproducible experiments.
12. Evaluate the effectiveness of different test generation strategies.

The research aims to investigate whether graph-guided relationships and multi-identity testing can improve the detection of authorization and business logic vulnerabilities compared with simpler rule-based testing.

## 3. System Architecture

API Sentinel consists of three independently running applications.

### 3.1 API Sentinel — Security Scanner Backend

The main application is developed using Java and Spring Boot.

It is responsible for processing API specifications, constructing security models, generating tests, executing requests, validating results, and storing findings.

**Implemented capabilities:**

- OpenAPI specification upload and parsing.
- API endpoint and operation processing.
- Security graph construction.
- Rule-based security test generation.
- HTTP request execution.
- Stateful test execution.
- Response and observable state validation.
- Security finding detection.
- Finding persistence and retrieval.

**Planned extensions:**

- Complete project and target management.
- Configurable identity and role management.
- Persistent ownership and authorization policy configuration.
- General-purpose workflow modelling.
- Advanced graph-guided test exploration and prioritization.
- Reproducible experiment orchestration.
- Automated research reporting and export.

### 3.2 Sentinel Bank — Benchmark Application

Sentinel Bank is an independent REST API application developed to act as a controlled security testing target.

It simulates selected banking operations using synthetic identities, accounts, and payments.

**Implemented capabilities:**

- Payment creation.
- Payment approval.
- Payment retrieval.
- Secure and vulnerable execution configurations.
- Synthetic benchmark users and database records.
- Stateful payment workflow testing.

The benchmark provides an environment in which the scanner can test whether unauthorized identities can perform restricted operations or cause invalid workflow transitions.

The scanner communicates with Sentinel Bank exclusively through HTTP requests.

It must not directly inspect the benchmark application's database or internal implementation to determine whether a vulnerability exists.

### 3.3 React Frontend

The frontend is developed using React and TypeScript.

It provides an interface for interacting with the scanner.

**Implemented capabilities:**

- Interaction with the scanner backend.
- Security test generation and execution.
- Display of detected findings.

**Planned extensions:**

- Project and target configuration.
- OpenAPI specification management.
- Identity and policy configuration.
- Scan history and monitoring.
- Detailed request and response evidence.
- Research metrics and report visualization.

The frontend is a presentation and configuration layer. Security analysis and vulnerability validation are performed by the backend.

## 4. High-Level Architecture Diagram

```text
                         USER
                           |
                           v
                  REACT FRONTEND
                     PORT 5173
                           |
                      REST / JSON
                           |
                           v
                API SENTINEL BACKEND
                  SPRING BOOT :8080
                           |
          +----------------+----------------+
          |                |                |
          v                v                v
     OPENAPI          SECURITY TEST      FINDING
     PROCESSING       GENERATION         MANAGEMENT
          |                |                |
          +----------------+----------------+
                           |
                           v
                    GRAPH ENGINE
                           |
                           v
                    HTTP EXECUTOR
                           |
                      HTTP REQUESTS
                           |
                           v
                  SENTINEL BANK API
                  SPRING BOOT :8081
                           |
                           v
                    HTTP RESPONSES
                           |
                           v
                  VALIDATION ENGINE
                           |
                           v
                   FINDING DETECTION
                           |
                           v
                    POSTGRESQL
```

**Application boundaries:**

- React frontend communicates with API Sentinel.
- API Sentinel communicates with Sentinel Bank through HTTP.
- API Sentinel maintains its own database.
- Sentinel Bank maintains a separate database.
- The scanner does not access the benchmark database directly.

## 5. Technology Stack

| Layer | Technology |
|---|---|
| Main backend language | Java 21 |
| Backend framework | Spring Boot |
| Benchmark application | Java and Spring Boot |
| Frontend | React 19 |
| Frontend language | TypeScript |
| Frontend tooling | Vite |
| Database | PostgreSQL 17 |
| Persistence | Spring Data JPA |
| Database migrations | Flyway |
| API specification | OpenAPI |
| API parsing | Swagger Parser |
| Graph processing | JGraphT |
| HTTP execution | Java HTTP Client |
| Backend testing | JUnit 5 |
| Frontend development | Node.js and npm |

The technology stack identifies the primary technologies used or specified for the implementation. Individual library usage and version compatibility should be verified against the actual dependency files before publication.

## 6. Main Backend Architecture

The backend is organized around distinct responsibilities in the security testing pipeline.

### 6.1 OpenAPI Specification Processing

**Purpose:** Convert an API specification into structured endpoint information.

Responsibilities:

- Accept OpenAPI specification uploads.
- Parse API paths and HTTP methods.
- Extract operation information.
- Process relevant request and response definitions.
- Identify path parameters.
- Produce structured API information for downstream processing.

The parser extracts API structure. It does not assume that an endpoint is vulnerable based on its name or HTTP method.

### 6.2 Security Graph Construction

**Purpose:** Represent relationships relevant to security testing.

The graph provides a structured representation of API operations and security-related entities.

Potential node categories include:

- Identity
- Role
- Resource
- Endpoint
- Operation
- Workflow state

Potential edge relationships include:

- `HAS_ROLE`
- `OWNS`
- `CAN_ACCESS`
- `INVOKES`
- `TRANSITIONS_TO`
- `REQUIRES_ROLE`
- `OPERATES_ON`

Responsibilities:

- Construct graph representations.
- Maintain typed relationships.
- Identify relevant entity combinations.
- Support graph traversal.
- Provide structured relationships for test generation.

The implemented graph construction functionality forms the foundation for more advanced graph-guided exploration.

Advanced graph traversal strategies, prioritization algorithms, and reproducible graph snapshots remain research extensions until implemented and experimentally verified.

### 6.3 Security Test Generation

**Purpose:** Generate structured security test cases.

The implemented test generation functionality supports security scenarios involving authorization weaknesses, including BOLA and BFLA.

A generated test case may contain:

- Test identifier.
- Target endpoint.
- HTTP method.
- Test identity.
- Authentication context.
- Request parameters.
- Request body.
- Expected security behavior.
- Test rationale.

The generator creates test definitions. HTTP execution is handled separately.

**Planned research extension:**

Support three independently measurable test generation strategies:

**RULE_BASED**

Generates tests from predefined security rules and configured relationships.

**GRAPH_GUIDED**

Uses graph relationships and bounded graph exploration to identify relevant identity, resource, and endpoint combinations.

**HYBRID**

Combines rule-based generation with graph-guided exploration.

These strategies must be implemented and evaluated separately before comparative research claims are made.

### 6.4 HTTP Execution Engine

**Purpose:** Execute generated test cases against authorized API targets.

Responsibilities:

- Construct HTTP requests.
- Apply the relevant test identity context.
- Execute requests against the target API.
- Capture response status codes.
- Capture response information.
- Handle execution failures.
- Record execution evidence.

The execution engine is responsible for communication with the target application.

### 6.5 Stateful Workflow Testing

**Purpose:** Evaluate security behavior across multiple related API operations.

Single-request testing may not reveal business logic vulnerabilities because the security impact can depend on the state created by an earlier request.

API Sentinel therefore includes stateful testing for payment operations.

The implemented benchmark workflow includes:

1. Creating a payment.
2. Attempting payment approval.
3. Retrieving the payment.
4. Checking the resulting state.

This makes it possible to evaluate not only the HTTP response but also the observable effect of an operation on application state.

### 6.6 Validation Engine

**Purpose:** Determine whether observed API behavior violates expected security properties.

Responsibilities:

- Evaluate HTTP responses.
- Compare observed behavior with expected security behavior.
- Evaluate observable state changes.
- Detect unauthorized operations.
- Identify invalid workflow transitions.
- Distinguish unsuccessful execution from a confirmed security violation.

An HTTP 200 response alone is not sufficient evidence of a vulnerability.

Similarly, an HTTP 403 response alone does not prove that every relevant security property is correctly enforced.

Validation must consider the expected operation, identity, resource, and relevant observable application state.

### 6.7 Finding Detection and Persistence

**Purpose:** Record security issues supported by execution evidence.

Finding information includes:

- Finding identifier.
- Vulnerability category.
- Affected endpoint.
- Expected behavior.
- Observed behavior.
- Supporting execution information.
- Detection timestamp.

The current implementation persists security findings in the API Sentinel database.

The implemented finding categories include:

- BOLA
- BFLA
- BUSINESS_LOGIC

Further extensions should include richer evidence records, confidence classifications, severity assessment, scan identifiers, and exportable reports.

### 6.8 Research Evaluation Engine

The research evaluation component is intended to compare test generation strategies under controlled conditions.

Planned evaluation dimensions include:

- Vulnerability detection rate.
- Precision.
- Recall.
- F1 score.
- False positive rate.
- Endpoint coverage.
- Test generation count.
- Execution time.
- Resource consumption.

The comparison between RULE_BASED, GRAPH_GUIDED, and HYBRID approaches must use equivalent benchmark scenarios and execution budgets.

Research conclusions must be based on actual experimental results rather than assumed improvements.

## 7. Sentinel Bank Benchmark Architecture

Sentinel Bank is a separate Spring Boot application.

It acts as the target application used to evaluate API Sentinel.

### 7.1 Implemented API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/payments` | Create a payment |
| POST | `/api/payments/{paymentId}/approve` | Approve a payment |
| GET | `/api/payments/{paymentId}` | Retrieve a payment |

These are the payment workflow endpoints verified in the current implementation.

Additional account and administrative endpoints may be introduced as the benchmark expands.

### 7.2 Benchmark Identities

The benchmark database includes synthetic test identities and associated account records.

The security testing environment includes customer identities and an administrative identity.

The benchmark uses synthetic data rather than real banking information.

### 7.3 Secure and Vulnerable Configurations

Sentinel Bank supports controlled secure and vulnerable configurations for testing selected authorization behaviors.

The payment approval scenario is particularly important.

**Secure behavior:**

- An unauthorized customer attempts to approve a payment.
- The API rejects the operation.
- The payment remains in its expected state.

**Vulnerable behavior:**

- An unauthorized customer attempts to approve a payment.
- The API accepts the unauthorized operation.
- The payment state changes unexpectedly.

The scanner evaluates the observable HTTP behavior and payment state to identify the security violation.

### 7.4 Ground Truth

Each benchmark scenario should have:

- A documented vulnerability category.
- A secure expected outcome.
- A vulnerable expected outcome.
- A reproducible execution procedure.
- A ground-truth classification.

Ground-truth labels are necessary for calculating detection effectiveness and false positive rates.

## 8. Database Architecture

API Sentinel and Sentinel Bank use separate PostgreSQL databases.

### 8.1 API Sentinel Database

**Database:** `api_sentinel_db`

The implemented database migration creates the `security_findings` table and associated indexes.

The findings table supports persistence of detected security issues.

The current implementation includes indexes associated with:

- Severity.
- Vulnerability type.
- Detection timestamp.

The complete research-oriented data model is broader than the currently implemented database schema.

Potential future entities include:

- Projects.
- Targets.
- API specifications.
- API endpoints.
- Test identities.
- Roles.
- Resources.
- Ownership relationships.
- Security policies.
- Workflow definitions.
- Graph snapshots.
- Scans.
- Generated test cases.
- Execution records.
- Evidence records.
- Findings.
- Research experiments.

These additional entities must be introduced through database migrations before they can be considered implemented persistence functionality.

### 8.2 Sentinel Bank Database

**Database:** `sentinel_bank_db`

The benchmark application maintains its own database.

Implemented migrations include tables for:

- `bank_users`
- `accounts`
- `payments`
- `payment_events`

The database contains synthetic benchmark identities and account records used during testing.

### 8.3 Database Isolation

The two applications must remain logically separate.

API Sentinel stores scanner information and detected findings.

Sentinel Bank stores benchmark application data and payment state.

Communication between the applications takes place through HTTP.

The scanner must not query the Sentinel Bank database directly.

## 9. Security and Safety Requirements

API Sentinel is designed for authorized security testing.

The following requirements guide implementation and future development:

1. Scan only explicitly authorized targets.
2. Use the local benchmark environment during development.
3. Require explicit configuration before executing state-changing tests against external targets.
4. Enforce request timeouts and execution limits.
5. Avoid uncontrolled concurrent requests.
6. Protect stored authentication credentials.
7. Never log plaintext passwords or authentication tokens.
8. Redact sensitive information from execution evidence.
9. Prevent requests from reaching unintended hosts.
10. Record relevant scan configuration and execution history.
11. Support safe handling of failed or interrupted executions.
12. Distinguish confirmed findings from suspected or inconclusive results.

Stateful testing must be performed with controlled test data and predictable cleanup procedures.

## 10. Deployment Architecture

During development, the applications run as separate local services.

| Component | Development Port |
|---|---:|
| React frontend | 5173 |
| API Sentinel backend | 8080 |
| Sentinel Bank backend | 8081 |
| PostgreSQL | 5432 |

The two application databases are logically separated.

The frontend communicates with the scanner backend.

The scanner backend communicates with the benchmark application through its REST API.

Docker Compose is a planned deployment mechanism for defining and running the complete environment. Its implementation and configuration should be tracked separately from the current local development setup.

## 11. Research Evaluation Methodology

The central research question is whether graph-guided, multi-identity testing can improve automated detection of authorization and business logic vulnerabilities in REST APIs.

The proposed evaluation compares:

1. RULE_BASED
2. GRAPH_GUIDED
3. HYBRID

### 11.1 Experimental Environment

The experiments should use:

- A controlled benchmark application.
- Documented vulnerability scenarios.
- Synthetic identities.
- Known expected outcomes.
- Equivalent execution budgets.
- Reproducible test configurations.

### 11.2 Evaluation Metrics

**True Positive (TP):** A vulnerable scenario correctly identified.

**False Positive (FP):** A secure scenario incorrectly reported as vulnerable.

**False Negative (FN):** A vulnerable scenario not detected.

**Precision:**

`TP / (TP + FP)`

**Recall:**

`TP / (TP + FN)`

**F1 Score:**

`2 × Precision × Recall / (Precision + Recall)`

**False Positive Rate:**

`FP / (FP + TN)`

Additional metrics:

- Vulnerability detection rate.
- Endpoint coverage.
- Security model coverage.
- Number of generated test cases.
- Test execution time.
- Resource consumption.

### 11.3 Experimental Fairness

All approaches should be evaluated using equivalent:

- Benchmark targets.
- Ground-truth vulnerability scenarios.
- Test identities.
- Request budgets.
- Execution time limits.
- Network conditions.

The classification unit should be a unique ground-truth vulnerability scenario rather than an individual HTTP request.

Experiments should record configurations, seeds where applicable, and execution results.

### 11.4 Research Limitations

The evaluation must document:

- Benchmark coverage limitations.
- API specification limitations.
- Authentication and identity modelling assumptions.
- State observability limitations.
- Test generation constraints.
- False positive and false negative cases.

The research must not claim that graph-guided testing is superior without supporting experimental evidence.

## 12. Implementation Status

The project distinguishes between implemented functionality and planned research extensions.

| Component | Current status |
|---|---|
| Spring Boot scanner foundation | Implemented |
| OpenAPI upload and parsing | Implemented |
| Security graph construction | Implemented |
| Rule-based BOLA/BFLA test generation | Implemented |
| HTTP request execution | Implemented |
| Stateful payment test execution | Implemented |
| Response and state validation | Implemented |
| Finding detection | Implemented |
| Finding persistence | Implemented |
| Findings retrieval | Implemented |
| Sentinel Bank payment workflow | Implemented |
| Secure/vulnerable benchmark configuration | Implemented |
| React frontend | Implemented foundation |
| Advanced graph-guided generation | Planned extension |
| RULE_BASED / GRAPH_GUIDED / HYBRID comparison | Planned research extension |
| Complete project and target management | Planned extension |
| General-purpose identity and policy modelling | Planned extension |
| Research experiment orchestration | Planned extension |
| Automated research reporting | Planned extension |

**Status note:** Implemented means the functionality has been developed and verified in the current project. Planned means it is part of the intended architecture but is not yet established as implemented functionality.

This table should be updated as features are developed and tested.

## 13. Engineering Principles

1. Deterministic security analysis takes priority over AI-generated conclusions.
2. Every confirmed finding must be supported by execution evidence.
3. The scanner and benchmark target must remain independent.
4. Security policies must be explicit and testable.
5. Graph exploration must be bounded and reproducible.
6. Modules must have clearly defined responsibilities.
7. Security test generation and HTTP execution must remain separate concerns.
8. Stateful tests must evaluate observable application behavior.
9. Every major feature must have automated tests.
10. No feature is considered complete until it is implemented and verified.
11. Research results must be based on actual benchmark executions.
12. Architectural documentation must reflect the current implementation.

## 14. Future Development Roadmap

The development roadmap is organized into the following stages.

### Stage 1 — Core Scanner Foundation

- Backend project foundation.
- PostgreSQL integration.
- OpenAPI specification processing.
- Security graph construction.
- Rule-based test generation.
- HTTP execution.
- Finding detection and persistence.

### Stage 2 — Benchmark Application

- Synthetic identity configuration.
- Payment workflow.
- Secure and vulnerable execution modes.
- Reproducible benchmark scenarios.
- Ground-truth documentation.

### Stage 3 — Advanced Security Modelling

- General identity and role modelling.
- Resource ownership relationships.
- Configurable authorization policies.
- Workflow state modelling.
- Security invariant evaluation.

### Stage 4 — Graph-Guided Testing

- Advanced graph traversal.
- Bounded path exploration.
- Identity-resource-operation relationship analysis.
- Graph-guided test generation.
- Test prioritization.

### Stage 5 — Research Evaluation

- Independent rule-based implementation.
- Graph-guided implementation.
- Hybrid implementation.
- Controlled benchmark experiments.
- Precision, recall, F1, and coverage analysis.
- Reproducibility and statistical analysis.

### Stage 6 — Frontend and Reporting Extensions

- Complete project and target management.
- Scan history and monitoring.
- Detailed execution evidence.
- Research metric visualization.
- Report generation and export.

The roadmap describes intended development work. Completion of a stage requires implementation, testing, and documentation.

## 15. Conclusion

API Sentinel provides an implementation foundation for automated REST API security testing using structured security models, graph construction, multi-identity test generation, HTTP execution, and evidence-based validation.

Its separate benchmark application enables controlled testing of authorization weaknesses and stateful business logic vulnerabilities.

The research direction extends this foundation toward advanced graph-guided test generation and comparative evaluation against rule-based and hybrid approaches.

The architecture is intended to evolve alongside the implementation. All research claims and implementation status statements must be supported by verifiable code and experimental evidence.

---

**Architecture Status:** Core security testing pipeline implemented; advanced graph-guided research capabilities and broader platform functionality remain under development.
