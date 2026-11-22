# API Sentinel — Database Schema Design

## 1. Purpose

This document defines the relational database design for API Sentinel and its independent benchmark application, Sentinel Bank.

It describes the tables, columns, relationships, constraints, indexes, and data protection rules required by the system.

**Implementation note:** This document contains both the implemented database schema verified from the current Flyway migrations and a broader proposed design for future platform capabilities. Sections describing users/projects, targets, policies, graph snapshots, scans, reports, and research tables are proposals unless explicitly listed as implemented in Section 12. The migration files are the source of truth for the schema currently created by the applications.

## 2. PostgreSQL Environment

One PostgreSQL 17 server currently hosts two independent databases.

| Database | Owner / application role | Purpose |
|---|---|---|
| api_sentinel_db | sentinel_app | Security scanner, projects, policies, scans, findings, and research results |
| sentinel_bank_db | bank_app | Synthetic banking users, accounts, and payments |

The applications use separate database connections and credentials.

There must be no direct database connection from API Sentinel to Sentinel Bank's database. The scanner communicates with Sentinel Bank through its HTTP API.

PostgreSQL foreign keys cannot enforce relationships across these two separate databases. No cross-database foreign keys will be designed.

## 3. Database Design Principles

1. Use relational tables for entities that require relationships and integrity constraints.
2. Use UUID identifiers for most API Sentinel entities.
3. Use primary keys to identify records uniquely.
4. Use foreign keys to maintain relationships between related records.
5. Use unique constraints to prevent duplicate records where necessary.
6. Use CHECK constraints to reject invalid values.
7. Use JSONB for flexible structured data such as parsed schemas, graph properties, and scan configurations.
8. Use indexes for frequently queried columns.
9. Keep sensitive credentials encrypted.
10. Preserve scan history and evidence so research results remain traceable.

## 4. Important Concepts

### 4.1 Primary Key

A primary key uniquely identifies a row in a table.

Example: Every project receives a unique project ID.

### 4.2 Foreign Key

A foreign key connects a row in one table to a row in another table.

Example: A scan belongs to a project, so `scans.project_id` references `projects.id`.

### 4.3 Unique Constraint

Prevents duplicate values where uniqueness is required.

Example: Two platform users must not register with the same email address.

### 4.4 NOT NULL Constraint

Requires a value to be present.

Example: A project must have a name.

### 4.5 CHECK Constraint

Restricts values to an allowed condition.

Example: A payment amount must be greater than zero.

## 5. API Sentinel Database

Database: `api_sentinel_db`

The tables are organized into functional groups.

### 5.1 Platform Users and Projects

#### Table: app_users

Stores people who log into the API Sentinel platform.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| email | VARCHAR(254) | Unique, not null |
| password_hash | TEXT | Not null; stores password hash |
| is_enabled | BOOLEAN | Not null, default true |
| created_at | TIMESTAMPTZ | Not null |
| updated_at | TIMESTAMPTZ | Not null |

Passwords must be stored as secure password hashes, never plaintext.

#### Table: projects

Stores security testing projects created on the platform.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| name | VARCHAR(150) | Not null |
| description | TEXT | Optional |
| created_by | UUID | Foreign key to app_users.id |
| created_at | TIMESTAMPTZ | Not null |
| updated_at | TIMESTAMPTZ | Not null |

#### Table: project_members

Connects platform users to projects.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| project_id | UUID | Foreign key to projects.id |
| user_id | UUID | Foreign key to app_users.id |
| member_role | VARCHAR(20) | OWNER, EDITOR, or VIEWER |
| created_at | TIMESTAMPTZ | Not null |

Primary key: `(project_id, user_id)`.

This allows one user to participate in multiple projects.

### 5.2 Targets and API Specifications

#### Table: targets

Stores the APIs registered for security testing.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; foreign key to projects.id |
| name | VARCHAR(150) | Not null |
| base_url | TEXT | Not null |
| authorization_confirmed | BOOLEAN | Not null, default false |
| authorization_confirmed_at | TIMESTAMPTZ | Nullable |
| max_requests_per_scan | INTEGER | Positive request limit |
| is_active | BOOLEAN | Not null, default true |
| created_at | TIMESTAMPTZ | Not null |
| updated_at | TIMESTAMPTZ | Not null |

Required constraints:

- UNIQUE (project_id, id)

Only targets for which scanning authorization has been explicitly confirmed may be scanned.


#### Table: api_specifications

Stores uploaded OpenAPI specifications.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; project identifier |
| target_id | UUID | Not null; target identifier |
| format | VARCHAR(20) | JSON or YAML |
| raw_content | TEXT | Original uploaded specification |
| content_sha256 | CHAR(64) | Specification checksum |
| parse_status | VARCHAR(20) | UPLOADED, PARSED, or FAILED |
| uploaded_by | UUID | Foreign key to app_users.id |
| created_at | TIMESTAMPTZ | Not null |
| parsed_at | TIMESTAMPTZ | Nullable |

Required constraints:

- UNIQUE (project_id, id)
- UNIQUE (project_id, target_id, id)
- FOREIGN KEY (project_id, target_id) REFERENCES targets (project_id, id)

The original specification is retained so parsing can be reproduced and audited.

#### Table: api_endpoints

Stores normalized endpoint information extracted from a specification.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; project identifier |
| specification_id | UUID | Not null; specification identifier |
| path | TEXT | Not null |
| http_method | VARCHAR(10) | GET, POST, PUT, PATCH, DELETE, etc. |
| operation_id | VARCHAR(255) | Nullable |
| summary | TEXT | Nullable |
| tags | JSONB | Parsed endpoint tags |
| parameters | JSONB | Parsed parameters |
| request_schema | JSONB | Request body schema |
| response_schema | JSONB | Response definitions |
| security_requirements | JSONB | Declared OpenAPI security requirements |

Required constraints:

- UNIQUE (project_id, id)
- UNIQUE (specification_id, path, http_method)
- FOREIGN KEY (project_id, specification_id) REFERENCES api_specifications (project_id, id)

An endpoint is an operation identified by its HTTP method and path within a particular specification.
### 5.3 Test Identities, Roles, and Permissions

These are the identities the scanner uses when interacting with a target API.

They are different from `app_users`, who log into the API Sentinel dashboard.

#### Table: test_identities

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; foreign key to projects.id |
| display_name | VARCHAR(100) | Not null |
| identity_type | VARCHAR(30) | USER or SERVICE |
| is_enabled | BOOLEAN | Not null, default true |
| created_at | TIMESTAMPTZ | Not null |

Required constraints:

- UNIQUE (project_id, id)

Examples: Alice, Bob, and Admin.

#### Table: identity_credentials

Stores authentication material for test identities.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| identity_id | UUID | Foreign key to test_identities.id |
| auth_type | VARCHAR(30) | BEARER, BASIC, or API_KEY |
| encrypted_secret | BYTEA | Encrypted credentials |
| secret_metadata | JSONB | Non-secret configuration metadata |
| expires_at | TIMESTAMPTZ | Nullable |
| created_at | TIMESTAMPTZ | Not null |

Encryption keys must be kept outside the database and source repository. Authentication secrets must never appear in application logs.

#### Table: roles

Defines roles within a project.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; foreign key to projects.id |
| name | VARCHAR(80) | Not null |
| description | TEXT | Optional |

Required constraints:

- UNIQUE (project_id, id)

Unique constraint: `(project_id, name)`.

Examples: CUSTOMER and ADMIN.

#### Table: identity_roles

Associates test identities with roles within the same project.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| project_id | UUID | Not null; project identifier |
| identity_id | UUID | Not null; identity identifier |
| role_id | UUID | Not null; role identifier |

Primary key: `(project_id, identity_id, role_id)`.

Required foreign keys:

- FOREIGN KEY (project_id, identity_id) REFERENCES test_identities (project_id, id)
- FOREIGN KEY (project_id, role_id) REFERENCES roles (project_id, id)

This is a project-scoped many-to-many relationship.
#### Table: permissions

Stores named permissions within a project.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; foreign key to projects.id |
| code | VARCHAR(120) | Not null |
| description | TEXT | Optional |

Required constraints:

- UNIQUE (project_id, id)
- UNIQUE (project_id, code)

Example permission: PAYMENT_APPROVE.

#### Table: role_permissions

Associates roles with permissions within the same project.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| project_id | UUID | Not null; project identifier |
| role_id | UUID | Not null; role identifier |
| permission_id | UUID | Not null; permission identifier |

Primary key: `(project_id, role_id, permission_id)`.

Required foreign keys:

- FOREIGN KEY (project_id, role_id) REFERENCES roles (project_id, id)
- FOREIGN KEY (project_id, permission_id) REFERENCES permissions (project_id, id)

This is a project-scoped many-to-many relationship.

### 5.4 Resources and Ownership

#### Table: resources

Represents resources belonging to the target API, as modelled by the scanner.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; foreign key to projects.id |
| resource_type | VARCHAR(100) | Not null |
| external_key | VARCHAR(255) | Not null; target-side resource identifier |
| description | TEXT | Optional |
| created_at | TIMESTAMPTZ | Not null |

Required constraints:

- UNIQUE (project_id, id)
- UNIQUE (project_id, resource_type, external_key)

Example: Account with external key 101.

These are scanner-side models. They are not copies of records queried directly from Sentinel Bank's database.

#### Table: resource_ownership

Represents expected ownership relationships between resources and test identities within the same project.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| project_id | UUID | Not null; project identifier |
| resource_id | UUID | Not null; resource identifier |
| identity_id | UUID | Not null; identity identifier |
| relationship_type | VARCHAR(50) | Not null; example: OWNER |
| created_at | TIMESTAMPTZ | Not null |

Primary key: `(project_id, resource_id, identity_id)`.

Required foreign keys:

- FOREIGN KEY (project_id, resource_id) REFERENCES resources (project_id, id)
- FOREIGN KEY (project_id, identity_id) REFERENCES test_identities (project_id, id)

This table supports tests where one identity attempts to access another identity's resource.
### 5.5 Security Policies and Invariants

#### Table: security_policies

Stores expected authorization rules for the target API.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; project identifier |
| name | VARCHAR(150) | Not null |
| policy_type | VARCHAR(40) | ROLE_ACCESS, OWNERSHIP, or WORKFLOW |
| endpoint_id | UUID | Nullable; API endpoint identifier |
| required_role_id | UUID | Nullable; required role identifier |
| expected_decision | VARCHAR(10) | ALLOW or DENY |
| resource_type | VARCHAR(100) | Nullable |
| rule_definition | JSONB | Structured policy conditions |
| is_enabled | BOOLEAN | Not null, default true |
| created_at | TIMESTAMPTZ | Not null |

Required constraints:

- UNIQUE (project_id, id)
- FOREIGN KEY (project_id, endpoint_id) REFERENCES api_endpoints (project_id, id)
- FOREIGN KEY (project_id, required_role_id) REFERENCES roles (project_id, id)

The endpoint and required role are optional. When supplied, they must belong to the same project.

Policies describe what the target is expected to enforce. They do not prove that the target actually enforces it.
#### Table: security_invariants

Stores testable security properties.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; foreign key to projects.id |
| name | VARCHAR(150) | Not null |
| category | VARCHAR(40) | OWNERSHIP, ROLE_ACCESS, STATE_TRANSITION, or IMMUTABILITY |
| description | TEXT | Not null |
| rule_definition | JSONB | Structured invariant definition |
| severity | VARCHAR(20) | LOW, MEDIUM, HIGH, or CRITICAL |
| is_enabled | BOOLEAN | Not null, default true |
| created_at | TIMESTAMPTZ | Not null |

Required constraints:

- UNIQUE (project_id, id)

Examples:

- Customers cannot access another customer's private account.
- Only administrators can approve payments.
- Completed payments cannot be modified.

### 5.6 Workflow and State Modelling

#### Table: workflows

Stores business workflow definitions.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; foreign key to projects.id |
| name | VARCHAR(150) | Not null |
| resource_type | VARCHAR(100) | Not null |
| description | TEXT | Optional |
| created_at | TIMESTAMPTZ | Not null |


Required constraints:

- UNIQUE (project_id, id)

#### Table: workflow_states

Defines possible states for a workflow.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; project identifier |
| workflow_id | UUID | Not null; workflow identifier |
| name | VARCHAR(80) | Not null |
| is_initial | BOOLEAN | Not null, default false |
| is_terminal | BOOLEAN | Not null, default false |

Required constraints:

- UNIQUE (project_id, id)
- UNIQUE (workflow_id, id)
- UNIQUE (workflow_id, name)
- FOREIGN KEY (project_id, workflow_id) REFERENCES workflows (project_id, id)

Example states: PENDING, APPROVED, CANCELLED, COMPLETED.

#### Table: workflow_transitions

Defines permitted transitions between states within a workflow and project.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; project identifier |
| workflow_id | UUID | Not null; workflow identifier |
| from_state_id | UUID | Not null; starting state |
| to_state_id | UUID | Not null; destination state |
| operation_endpoint_id | UUID | Nullable; API endpoint that triggers the transition |
| required_role_id | UUID | Nullable; role permitted to perform the transition |
| created_at | TIMESTAMPTZ | Not null |

Required constraints:

- UNIQUE (project_id, id)
- FOREIGN KEY (project_id, workflow_id) REFERENCES workflows (project_id, id)
- FOREIGN KEY (workflow_id, from_state_id) REFERENCES workflow_states (workflow_id, id)
- FOREIGN KEY (workflow_id, to_state_id) REFERENCES workflow_states (workflow_id, id)
- FOREIGN KEY (project_id, operation_endpoint_id) REFERENCES api_endpoints (project_id, id)
- FOREIGN KEY (project_id, required_role_id) REFERENCES roles (project_id, id)

The operation endpoint and required role are optional. When supplied, they must belong to the same project.

Both state references must belong to the specified workflow.

### 5.7 Graph Storage

#### Table: graph_snapshots

Stores versions of the security graph.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; project identifier |
| specification_id | UUID | Nullable; API specification identifier |
| version | INTEGER | Not null; positive snapshot version |
| graph_checksum | CHAR(64) | Graph checksum |
| created_at | TIMESTAMPTZ | Not null |

Required constraints:

- UNIQUE (project_id, id)
- UNIQUE (project_id, version)
- FOREIGN KEY (project_id, specification_id) REFERENCES api_specifications (project_id, id)
- CHECK (version > 0)

The specification reference is optional. When provided, it must belong to the same project.

Each project must have a unique version number for every graph snapshot.

#### Table: graph_nodes

Stores nodes belonging to a graph snapshot.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; project identifier |
| snapshot_id | UUID | Not null; graph snapshot identifier |
| node_type | VARCHAR(40) | IDENTITY, ROLE, RESOURCE, ENDPOINT, OPERATION, or WORKFLOW_STATE |
| external_ref | VARCHAR(255) | Reference to the modelled entity |
| label | VARCHAR(255) | Display label |
| properties | JSONB | Additional node properties |

Required constraints:

- UNIQUE (snapshot_id, id)
- UNIQUE (snapshot_id, node_type, external_ref)
- FOREIGN KEY (project_id, snapshot_id) REFERENCES graph_snapshots (project_id, id)

The external entity reference is polymorphic and must be validated by the graph-building application.

#### Table: graph_edges

Stores relationships between graph nodes belonging to the same graph snapshot.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| snapshot_id | UUID | Not null; graph snapshot identifier |
| source_node_id | UUID | Not null; source graph node |
| target_node_id | UUID | Not null; destination graph node |
| edge_type | VARCHAR(50) | HAS_ROLE, OWNS, CAN_ACCESS, INVOKES, TRANSITIONS_TO, etc. |
| properties | JSONB | Additional relationship properties |

Required constraints:

- FOREIGN KEY (snapshot_id, source_node_id) REFERENCES graph_nodes (snapshot_id, id)
- FOREIGN KEY (snapshot_id, target_node_id) REFERENCES graph_nodes (snapshot_id, id)

Both node references must belong to the same graph snapshot.

The graph-building application must validate that the edge type and node types form a valid relationship.

### 5.8 Scans and Test Generation

#### Table: scans

Stores individual security scan jobs.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; project identifier |
| target_id | UUID | Not null; target identifier |
| specification_id | UUID | Not null; API specification identifier |
| graph_snapshot_id | UUID | Nullable; graph snapshot identifier |
| mode | VARCHAR(20) | RULE_BASED, GRAPH_GUIDED, or HYBRID |
| status | VARCHAR(20) | QUEUED, RUNNING, COMPLETED, FAILED, or CANCELLED |
| scan_config | JSONB | Scan limits and enabled detectors |
| requested_by | UUID | Foreign key to app_users.id |
| started_at | TIMESTAMPTZ | Nullable |
| completed_at | TIMESTAMPTZ | Nullable |
| created_at | TIMESTAMPTZ | Not null |

Required constraints:

- UNIQUE (project_id, id)
- FOREIGN KEY (project_id, target_id) REFERENCES targets (project_id, id)
- FOREIGN KEY (project_id, target_id, specification_id) REFERENCES api_specifications (project_id, target_id, id)
- FOREIGN KEY (project_id, graph_snapshot_id) REFERENCES graph_snapshots (project_id, id)

A scan must use an API specification registered for its selected target and project.

The graph snapshot is optional. When provided, it must belong to the same project.

#### Table: test_cases

Stores generated security test definitions.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; project identifier |
| scan_id | UUID | Not null; scan identifier |
| test_key | VARCHAR(150) | Not null; stable test identifier within a scan |
| category | VARCHAR(40) | BOLA, BFLA, or BUSINESS_LOGIC |
| endpoint_id | UUID | Nullable; API endpoint identifier |
| identity_id | UUID | Nullable; test identity identifier |
| resource_id | UUID | Nullable; resource identifier |
| invariant_id | UUID | Nullable; security invariant identifier |
| request_definition | JSONB | Structured request definition |
| rationale | TEXT | Why the test was generated |
| priority | INTEGER | Test priority |
| created_at | TIMESTAMPTZ | Not null |

Required constraints:

- UNIQUE (scan_id, id)
- UNIQUE (scan_id, test_key)
- FOREIGN KEY (project_id, scan_id) REFERENCES scans (project_id, id)
- FOREIGN KEY (project_id, endpoint_id) REFERENCES api_endpoints (project_id, id)
- FOREIGN KEY (project_id, identity_id) REFERENCES test_identities (project_id, id)
- FOREIGN KEY (project_id, resource_id) REFERENCES resources (project_id, id)
- FOREIGN KEY (project_id, invariant_id) REFERENCES security_invariants (project_id, id)

Optional endpoint, identity, resource, and invariant references must belong to the same project when supplied.

The test generator creates these definitions. The HTTP executor executes them separately.
### 5.9 Execution and Evidence

#### Table: test_executions

Stores the results of executing generated security test cases.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| scan_id | UUID | Not null; parent scan identifier |
| test_case_id | UUID | Not null; executed test case |
| attempt_number | INTEGER | Not null; positive execution attempt |
| status | VARCHAR(20) | QUEUED, RUNNING, COMPLETED, or FAILED |
| outcome | VARCHAR(20) | PASS, FAIL, ERROR, or INCONCLUSIVE |
| response_status | INTEGER | Nullable HTTP status |
| duration_ms | BIGINT | Nullable execution duration |
| started_at | TIMESTAMPTZ | Nullable |
| completed_at | TIMESTAMPTZ | Nullable |

Required constraints:

- UNIQUE (scan_id, id)
- UNIQUE (test_case_id, attempt_number)
- FOREIGN KEY (scan_id, test_case_id) REFERENCES test_cases (scan_id, id)
- CHECK (attempt_number > 0)
- UNIQUE (scan_id, test_case_id, id)

A test execution must reference a test case belonging to the same scan.

One test case may have multiple execution attempts.

#### Table: request_evidence

Stores sanitized HTTP execution evidence. One test execution may produce multiple request/response records.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| scan_id | UUID | Not null; parent scan identifier |
| test_case_id | UUID | Not null; executed test case identifier |
| execution_id | UUID | Not null; execution identifier |
| sequence_number | INTEGER | Not null; positive sequence number |
| sanitized_request | JSONB | Request method, URL, and redacted content |
| sanitized_response | JSONB | Response status, headers, and redacted body |
| evidence_hash | CHAR(64) | Evidence checksum |
| captured_at | TIMESTAMPTZ | Not null |

Required constraints:

- UNIQUE (scan_id, test_case_id, id)
- UNIQUE (execution_id, sequence_number)
- FOREIGN KEY (scan_id, test_case_id, execution_id) REFERENCES test_executions (scan_id, test_case_id, id)
- CHECK (sequence_number > 0)

Each evidence record must belong to an execution of the specified test case within the specified scan.

Credentials, authorization tokens, and sensitive information must be redacted.

Multiple evidence records may belong to one test execution. Sequence numbers preserve the order of HTTP requests and responses.

### 5.10 Findings and Reports

#### Table: findings

Stores validated security findings.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; project identifier |
| scan_id | UUID | Not null; parent scan identifier |
| test_case_id | UUID | Not null; test case identifier |
| invariant_id | UUID | Nullable; security invariant identifier |
| category | VARCHAR(40) | BOLA, BFLA, or BUSINESS_LOGIC |
| status | VARCHAR(20) | CONFIRMED, SUSPECTED, or INCONCLUSIVE |
| severity | VARCHAR(20) | LOW, MEDIUM, HIGH, or CRITICAL |
| title | VARCHAR(200) | Not null |
| description | TEXT | Not null |
| observed_behavior | TEXT | Not null |
| detected_at | TIMESTAMPTZ | Not null |

Required constraints:

- UNIQUE (project_id, id)
- UNIQUE (scan_id, test_case_id, id)
- FOREIGN KEY (project_id, scan_id) REFERENCES scans (project_id, id)
- FOREIGN KEY (scan_id, test_case_id) REFERENCES test_cases (scan_id, id)
- FOREIGN KEY (project_id, invariant_id) REFERENCES security_invariants (project_id, id)

The invariant reference is optional. When supplied, it must belong to the same project.

A finding must be traceable to a test case and its supporting execution evidence.

#### Table: finding_evidence

Associates security findings with supporting HTTP evidence from the same scan and test case.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| scan_id | UUID | Not null; parent scan identifier |
| test_case_id | UUID | Not null; test case identifier |
| finding_id | UUID | Not null; finding identifier |
| evidence_id | UUID | Not null; request evidence identifier |

Primary key: `(finding_id, evidence_id)`.

Required foreign keys:

- FOREIGN KEY (scan_id, test_case_id, finding_id) REFERENCES findings (scan_id, test_case_id, id)
- FOREIGN KEY (scan_id, test_case_id, evidence_id) REFERENCES request_evidence (scan_id, test_case_id, id)

Every finding-evidence relationship must connect a finding and evidence record belonging to the same scan and test case.

One finding may have multiple supporting evidence records.

#### Table: reports

Stores generated scan report metadata.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; project identifier |
| scan_id | UUID | Not null; scan identifier |
| format | VARCHAR(20) | JSON, PDF, or HTML |
| status | VARCHAR(20) | QUEUED, GENERATED, or FAILED |
| storage_key | TEXT | Report storage location |
| created_by | UUID | Foreign key to app_users.id |
| created_at | TIMESTAMPTZ | Not null |

Required constraints:

- FOREIGN KEY (project_id, scan_id) REFERENCES scans (project_id, id)

Every report must belong to a scan within the same project.

### 5.11 Research Experiment Storage

#### Table: research_experiments

Stores experiment definitions and configurations.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; foreign key to projects.id |
| name | VARCHAR(150) | Not null |
| description | TEXT | Optional |
| experiment_config | JSONB | Experiment conditions and budgets |
| created_by | UUID | Foreign key to app_users.id |
| created_at | TIMESTAMPTZ | Not null |

Required constraints:

- UNIQUE (project_id, id)

#### Table: research_measurements

Stores measurements generated during controlled research experiments.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| project_id | UUID | Not null; project identifier |
| experiment_id | UUID | Not null; research experiment identifier |
| scan_id | UUID | Not null; scan identifier |
| metric_name | VARCHAR(80) | Precision, recall, F1, execution_time, etc. |
| metric_value | NUMERIC(18,8) | Not null; measured value |
| metric_metadata | JSONB | Supporting calculation metadata |
| created_at | TIMESTAMPTZ | Not null |

Required constraints:

- FOREIGN KEY (project_id, experiment_id) REFERENCES research_experiments (project_id, id)
- FOREIGN KEY (project_id, scan_id) REFERENCES scans (project_id, id)

Both the experiment and scan must belong to the same project.

Ground-truth benchmark labels must remain separate from the scanner's runtime decision-making. The evaluation process must not leak expected answers into the test generator.

## 6. Sentinel Bank Database

Database: `sentinel_bank_db`

This database belongs exclusively to the independent benchmark application.

It contains synthetic data used to test the scanner.

### 6.1 Table: bank_users

Stores benchmark application users.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | BIGINT GENERATED BY DEFAULT AS IDENTITY | Primary key |
| username | VARCHAR(100) | Unique, not null |
| password_hash | TEXT | Not null |
| role | VARCHAR(20) | CUSTOMER or ADMIN |
| is_active | BOOLEAN | Not null, default true |
| created_at | TIMESTAMPTZ | Not null |

Seeded identities:

- Alice — CUSTOMER
- Bob — CUSTOMER
- Admin — ADMIN

Only synthetic accounts and credentials are permitted.

### 6.2 Table: accounts

Stores synthetic banking accounts.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | BIGINT GENERATED BY DEFAULT AS IDENTITY | Primary key |
| account_number | VARCHAR(34) | Unique, not null |
| owner_user_id | BIGINT | Foreign key to bank_users.id |
| balance | NUMERIC(18,2) | Not null, default 0 |
| currency | CHAR(3) | Not null |
| status | VARCHAR(20) | ACTIVE or BLOCKED |
| created_at | TIMESTAMPTZ | Not null |

The initial benchmark data will include:

- Alice owns account 101.
- Bob owns account 202.

### 6.3 Table: payments

Stores synthetic payment transactions.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | UUID | Primary key |
| source_account_id | BIGINT | Foreign key to accounts.id |
| created_by_user_id | BIGINT | Foreign key to bank_users.id |
| amount | NUMERIC(18,2) | Not null, must be greater than zero |
| currency | CHAR(3) | Not null |
| beneficiary_reference | VARCHAR(100) | Not null |
| status | VARCHAR(20) | PENDING, APPROVED, CANCELLED, or COMPLETED |
| approved_by_user_id | BIGINT | Nullable foreign key to bank_users.id |
| created_at | TIMESTAMPTZ | Not null |
| updated_at | TIMESTAMPTZ | Not null |

### 6.4 Table: payment_events

Stores payment state-change history for the benchmark application.

| Column | PostgreSQL type | Constraint / purpose |
|---|---|---|
| id | BIGINT GENERATED BY DEFAULT AS IDENTITY | Primary key |
| payment_id | UUID | Foreign key to payments.id |
| actor_user_id | BIGINT | Nullable foreign key to bank_users.id |
| event_type | VARCHAR(50) | Not null |
| from_status | VARCHAR(20) | Nullable |
| to_status | VARCHAR(20) | Nullable |
| details | JSONB | Optional event metadata |
| created_at | TIMESTAMPTZ | Not null |

Payment events support the target application's own audit history. API Sentinel must not read this table directly. It must validate target behavior using the permitted HTTP interface.

## 7. Important Relationships

### API Sentinel

```text
app_users
   |
   +---- projects
             |
             +---- project_members
             |
             +---- targets
             |       |
             |       +---- api_specifications
             |                  |
             |                  +---- api_endpoints
             |
             +---- test_identities
             |       |
             |       +---- identity_credentials
             |       |
             |       +---- identity_roles ---- roles
             |
             +---- resources
             |       |
             |       +---- resource_ownership
             |
             +---- workflows
             |       |
             |       +---- workflow_states
             |       |
             |       +---- workflow_transitions
             |
             +---- security_policies
             |
             +---- security_invariants
             |
             +---- graph_snapshots
             |       |
             |       +---- graph_nodes
             |       +---- graph_edges
             |
             +---- scans
                     |
                     +---- test_cases
                     |       |
                     |       +---- test_executions
                     |                |
                     |                +---- request_evidence
                     |
                     +---- findings
                     |       |
                     |       +---- finding_evidence
                     |
                     +---- reports
```

This is a conceptual relationship overview, not executable SQL.

### Sentinel Bank

```text
bank_users
    |
    +---- accounts
    |       |
    |       +---- payments
    |               |
    |               +---- payment_events
    |
    +---- payments
         (creator / approver relationships)
```

## 8. Important Constraints and Indexes

### 8.1 Constraints

The implementation must include:

- Primary keys on all tables.
- Foreign keys for defined relationships.
- Unique email addresses for platform users.
- Unique endpoint method and path within a specification.
- Unique role names within a project.
- Unique permission codes within a project.
- Unique resource identifiers within their project and type.
- Positive payment amount checks.
- Valid role, status, and scan-mode checks.
- Valid workflow state relationships.

Foreign key delete behavior must be selected deliberately. Historical scans, findings, and evidence should not disappear accidentally when configuration records are changed or removed.

### 8.2 Recommended indexes

Initial indexes should cover common lookup paths:

| Table | Index columns | Reason |
|---|---|---|
| projects | created_by | Find a user's projects |
| targets | project_id | Find project targets |
| api_specifications | target_id | Find target specifications |
| api_endpoints | specification_id | Find specification endpoints |
| test_identities | project_id | Find project identities |
| resources | project_id, resource_type | Find modeled resources |
| scans | project_id, created_at | Scan history |
| scans | target_id, status | Target scan status |
| test_cases | scan_id | Find generated tests |
| test_executions | test_case_id | Find executions |
| findings | scan_id, status | Find scan findings |
| research_measurements | experiment_id, metric_name | Analyze experiment results |
| accounts | owner_user_id | Find accounts belonging to a customer |
| payments | source_account_id | Find account payments |
| payment_events | payment_id | Find payment history |

Indexes will be added through Flyway migrations after query patterns are reviewed.

## 9. Data Security

1. Hash platform and benchmark passwords using a suitable password hashing algorithm.
2. Encrypt stored target credentials and test identity secrets.
3. Keep encryption keys and database passwords outside Git.
4. Use separate database credentials for both applications.
5. Never store authorization tokens in plaintext evidence.
6. Redact sensitive HTTP headers and response fields.
7. Restrict database permissions to the required application database.
8. Do not expose internal benchmark configuration to the scanner.
9. Validate project access in the backend before returning records.
10. Do not allow the scanner to access Sentinel Bank's PostgreSQL database.

## 10. Database Migrations

Flyway manages schema creation and evolution. Each application has its own migration directory and database connection.

### Implemented migration files

| Application database | Migration | Implemented purpose |
|---|---|---|
| `api_sentinel_db` | `V1__create_security_findings.sql` | Creates `security_findings` and indexes for severity, vulnerability type, and detection time |
| `sentinel_bank_db` | `V1__create_bank_users_and_accounts.sql` | Creates `bank_users` and `accounts`, including role/status/balance/currency constraints and an account-owner index |
| `sentinel_bank_db` | `V2__create_payments_and_payment_events.sql` | Creates `payments` and `payment_events`, with foreign keys, amount/status/currency checks, and lookup indexes |
| `sentinel_bank_db` | `V3__seed_benchmark_data.sql` | Seeds the original synthetic Alice, Bob, and Admin users and their accounts |
| `sentinel_bank_db` | `V4__create_isolated_benchmark_users.sql` | Adds dedicated `sentinel_alice`, `sentinel_bob`, and `sentinel_admin` benchmark identities and customer accounts |

The scanner and benchmark service use separate databases. API Sentinel communicates with Sentinel Bank through its HTTP API and must not connect directly to the benchmark database.

### Proposed future migration sequence

The broader platform schema described in this document—projects, targets, persisted specifications/endpoints, test identities and roles, resources, policies, workflows, graph snapshots, scans, reports, and research measurements—remains proposed unless a corresponding migration is added. Future migrations must continue from the existing version history in the relevant application; do not reuse or rewrite applied migration versions.

Existing migration files must not be silently edited after they have been applied. New schema changes should use new migration versions.

## 11. Design Decisions

1. PostgreSQL is the relational database for both applications.
2. The two applications use separate databases and credentials.
3. API Sentinel stores an abstract model of target resources; it does not copy target database records.
4. The security graph is stored as graph snapshots, nodes, and edges. A separate graph database is not required initially.
5. JSONB is used for flexible structured data, while core relationships remain relational.
6. Test generation, HTTP execution, validation, and findings persistence remain distinct responsibilities.
7. Scan evidence is retained so findings can be reproduced and reviewed.
8. Research ground truth is kept separate from runtime scanner decisions.
9. Flyway is the source of truth for actual database schema changes.

## 12. Implementation Status

**Database platform:** PostgreSQL 17 is the configured database platform. The repository configures the scanner and benchmark service to use separate databases.

**Database/role configuration:** `api_sentinel_db` / `sentinel_app` and `sentinel_bank_db` / `bank_app` are the configured database and application-role pairs. Local creation and connection testing are environment setup steps, not schema migrations.

**Implemented Flyway schema — API Sentinel:** `V1__create_security_findings.sql` creates:
- `security_findings`: BIGSERIAL primary key; unique `finding_id`; vulnerability type, severity, identity, endpoint, target resource, description, expected/actual status, evidence, and detection timestamp.
- Indexes on `severity`, `vulnerability_type`, and `detected_at`.

**Implemented Flyway schema — Sentinel Bank:**
- `bank_users`: identity primary key, unique username, password hash, role, active flag, and creation timestamp. Role is constrained to `CUSTOMER` or `ADMIN`.
- `accounts`: identity primary key, unique account number, owner foreign key to `bank_users`, balance, currency, status, and creation timestamp. Includes balance, currency, and status checks plus an owner index.
- `payments`: UUID primary key, source-account and creator/approver foreign keys, amount, currency, beneficiary reference, status, and timestamps. Includes positive-amount, currency, and status checks plus lookup indexes.
- `payment_events`: identity primary key, payment and optional actor foreign keys, event type, from/to status, JSONB details, and creation timestamp. Includes status checks and lookup indexes.
- V3 seeds the original synthetic Alice, Bob, and Admin users and accounts. V4 adds the isolated `sentinel_alice`, `sentinel_bob`, and `sentinel_admin` benchmark identities and their customer accounts.

**Broader schema design:** The additional tables and relationships in Sections 5 and 14 (including projects, targets, persisted API specifications/endpoints, test identities/roles, resources, policies, workflows, graph snapshots, scans, reports, and research measurements) are proposed design, not represented by the checked-in migrations listed above.

**Persistence integration:** The backend has a persisted findings path corresponding to `security_findings`. Sentinel Bank persists users, accounts, payments, and payment events through its own schema. Do not infer that every proposed table or relationship has been implemented.

A schema feature must not be marked implemented until its migration exists and its behavior has been tested against the intended database.

## 13. Cross-Project Referential Integrity Overview

API Sentinel supports multiple independent projects. Database relationships must prevent records belonging to one project from referencing records belonging to another project.

PostgreSQL composite unique constraints and composite foreign keys will enforce project-level referential integrity wherever possible.

Application-level validation will cover polymorphic references and cross-record rules that cannot be enforced through ordinary relational constraints.

The detailed table definitions, composite keys, foreign keys, nullable relationships, and migration requirements are specified in Section 14.

Sentinel Bank remains an independent benchmark application with its own database and application-level authorization and workflow rules.

## 14. Composite Keys and Cross-Project Integrity Design

### 14.1 Objective

API Sentinel supports multiple projects. Records belonging to one project must not accidentally reference records belonging to another project.

A normal foreign key checks whether a referenced record exists. It does not necessarily ensure that both records belong to the same project.

Composite foreign keys will enforce project-level referential integrity.

### 14.2 Parent Table Requirements

The following tables must expose composite unique keys wherever they are referenced through project-scoped relationships:

| Parent table | Required unique key |
|---|---|
| targets | (project_id, id) |
| api_specifications | (project_id, id) |
| api_endpoints | (project_id, id) |
| test_identities | (project_id, id) |
| roles | (project_id, id) |
| permissions | (project_id, id) |
| resources | (project_id, id) |
| security_invariants | (project_id, id) |
| graph_snapshots | (project_id, id) |
| scans | (project_id, id) |
| test_cases | (scan_id, id) |
| research_experiments | (project_id, id) |

These constraints supplement the existing primary keys.

Workflow states require a separate composite key:

UNIQUE (workflow_id, id)

### 14.3 Identity and Role Relationships

The identity_roles table must include project_id.

Required columns:

- project_id
- identity_id
- role_id

Required primary key:

PRIMARY KEY (project_id, identity_id, role_id)

Required foreign keys:

FOREIGN KEY (project_id, identity_id)
REFERENCES test_identities (project_id, id)

FOREIGN KEY (project_id, role_id)
REFERENCES roles (project_id, id)

The role_permissions table must also include project_id.

Required primary key:

PRIMARY KEY (project_id, role_id, permission_id)

Required foreign keys:

FOREIGN KEY (project_id, role_id)
REFERENCES roles (project_id, id)

FOREIGN KEY (project_id, permission_id)
REFERENCES permissions (project_id, id)

### 14.4 Resource Ownership

The resource_ownership table must include project_id.

Required primary key:

PRIMARY KEY (project_id, resource_id, identity_id)

Required foreign keys:

FOREIGN KEY (project_id, resource_id)
REFERENCES resources (project_id, id)

FOREIGN KEY (project_id, identity_id)
REFERENCES test_identities (project_id, id)

### 14.5 API Specifications and Endpoints

The api_specifications table must include project_id in addition to target_id.

Its target relationship must guarantee that the specification belongs to the same project as its target.

The api_endpoints table must include project_id in addition to specification_id.

Its specification relationship must guarantee that the endpoint belongs to the correct project.

A scan must not combine a target with a specification registered under a different target.

### 14.6 Policies and Workflow Relationships

Security policies must reference endpoints and roles belonging to their own project.

Workflow transitions must reference states belonging to the same workflow.

Required workflow state relationship:

FOREIGN KEY (workflow_id, from_state_id)
REFERENCES workflow_states (workflow_id, id)

FOREIGN KEY (workflow_id, to_state_id)
REFERENCES workflow_states (workflow_id, id)

Required foreign-key relationships must also be defined for the transition's operation endpoint and required role.

### 14.7 Graph Relationships

Graph nodes and edges must remain associated with one graph snapshot.

The graph_nodes table must expose a unique key for:

(snapshot_id, id)

The graph_edges table must identify:

- snapshot_id
- source_node_id
- target_node_id

Both source and target node references must use composite foreign keys referencing nodes belonging to the same snapshot.

### 14.8 Scan and Test Relationships

A scan must reference:

- A target belonging to its project.
- An API specification belonging to that target and project.
- A graph snapshot belonging to the same project, when supplied.

A test case must reference entities belonging to the scan's project.

This includes:

- Endpoint
- Test identity
- Resource
- Security invariant

The test_executions table must retain the parent scan relationship where needed to enforce that its test case belongs to the same scan.

A finding must reference a test case belonging to its own scan.

Research measurements must reference experiments and scans belonging to the same project.

### 14.9 Nullable Composite Foreign Keys

Some relationships are optional.

For example, a security policy may not always specify a required role.

The database implementation must use appropriate nullable composite foreign-key behavior. PostgreSQL's default MATCH SIMPLE behavior allows a composite foreign key to be skipped when one or more referencing columns are null.

Required relationships must still be declared NOT NULL.

### 14.10 Constraint Verification

Automated database tests must verify:

1. An identity can be assigned a role in its own project.
2. An identity cannot be assigned a role belonging to another project.
3. A resource cannot be owned by an identity from another project.
4. A specification cannot be associated with a target from another project.
5. A workflow transition cannot reference a state from another workflow.
6. A graph edge cannot reference a node from another snapshot.
7. A test case cannot reference an identity from another project.
8. A finding cannot reference a test case from another scan.

All invalid relationships must be rejected by PostgreSQL where a database constraint can enforce them.

Application-level validation must cover any remaining cross-record rules.

This section is an implementation contract. Exact SQL constraints and migration ordering must be finalized before applying the migrations.

### 14.11 Required Table Definition Amendments

The following changes are mandatory to make the composite relationship design implementable.

| Table | Required amendment |
|---|---|
| targets | Add UNIQUE (project_id, id) |
| api_specifications | Add project_id, UNIQUE (project_id, id), UNIQUE (project_id, target_id, id) |
| api_endpoints | Add project_id and UNIQUE (project_id, id) |
| workflows | Add UNIQUE (project_id, id) |
| workflow_states | Add project_id, UNIQUE (project_id, id), UNIQUE (workflow_id, id) |
| workflow_transitions | Add project_id |
| test_identities | Add UNIQUE (project_id, id) |
| roles | Add UNIQUE (project_id, id) |
| permissions | Add UNIQUE (project_id, id) |
| resources | Add UNIQUE (project_id, id) |
| security_invariants | Add UNIQUE (project_id, id) |
| graph_snapshots | Add UNIQUE (project_id, id) |
| graph_nodes | Add project_id, UNIQUE (snapshot_id, id) |
| scans | Add UNIQUE (project_id, id) |
| test_cases | Add project_id, UNIQUE (scan_id, id) |
| test_executions | Add scan_id and UNIQUE (scan_id, id) |
| findings | Add project_id |
| research_experiments | Add UNIQUE (project_id, id) |
| research_measurements | Add project_id |
| reports | Add project_id |

### 14.12 Required Composite Relationships

The final migration definitions must implement these relationships.

**API specification relationships**

- api_specifications (project_id, target_id) references targets (project_id, id).
- api_endpoints (project_id, specification_id) references api_specifications (project_id, id).
- api_specifications (project_id, target_id, id) supports validation that a scan uses a specification belonging to its selected target.

**Workflow relationships**

- workflow_states (project_id, workflow_id) references workflows (project_id, id).
- workflow_transitions (project_id, workflow_id) references workflows (project_id, id).
- Workflow transition state references use (workflow_id, state_id) composite keys.
- Optional endpoint and role references must remain within the same project.

**Graph relationships**

- graph_nodes (project_id, snapshot_id) references graph_snapshots (project_id, id).
- graph_edges (snapshot_id, source_node_id) references graph_nodes (snapshot_id, id).
- graph_edges (snapshot_id, target_node_id) references graph_nodes (snapshot_id, id).

The graph node's polymorphic external entity reference must additionally be validated by the graph-building application.

**Scan relationships**

- scans (project_id, target_id) references targets (project_id, id).
- scans (project_id, target_id, specification_id) references api_specifications (project_id, target_id, id).
- scans (project_id, graph_snapshot_id) references graph_snapshots (project_id, id), when a graph snapshot is supplied.

**Test case relationships**

- test_cases (project_id, scan_id) references scans (project_id, id).
- Test case endpoint, identity, resource, and invariant references must use project-scoped composite foreign keys.
- test_executions (scan_id, test_case_id) references test_cases (scan_id, id).

**Finding and reporting relationships**

- findings (scan_id, test_case_id) references test_cases (scan_id, id).
- Findings must reference security invariants belonging to their project when an invariant is supplied.
- reports (project_id, scan_id) references scans (project_id, id).

**Research relationships**

- research_measurements (project_id, experiment_id) references research_experiments (project_id, id).
- research_measurements (project_id, scan_id) references scans (project_id, id).

### 14.13 Additional Implementation Rules

1. Every composite foreign key must have a matching primary key or unique constraint on the referenced columns.
2. Every required relationship column must be NOT NULL.
3. Nullable composite foreign keys must use appropriate PostgreSQL MATCH behavior.
4. Composite foreign keys must not be replaced by application validation when database enforcement is possible.
5. Polymorphic references must be validated in application code because a single ordinary foreign key cannot reference multiple entity tables.
6. Migration tests must verify both valid relationships and invalid cross-project relationships.

### 14.14 Schema Approval Condition

The database schema is ready for implementation only when:

- Original table definitions include all required columns.
- All composite foreign keys have matching referenced unique keys.
- Optional relationships have correct nullability.
- Referential integrity tests are defined.
- Migration ordering is documented.
- Both application databases remain independent.

This section supplements the earlier table definitions and must be incorporated into the final SQL migration design.
