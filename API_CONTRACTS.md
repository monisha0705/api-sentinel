# API Sentinel — API Contracts

## 1. Purpose

This document defines the REST API contracts for the API Sentinel platform.

It specifies endpoint paths, HTTP methods, request structures, response structures, validation rules, authentication requirements, and error handling.

The API is the communication boundary between the React frontend and the API Sentinel Spring Boot backend.

## 2. API Standards

### 2.1 Base URL

All platform endpoints use:

`/api/v1`

Example:

`http://localhost:8080/api/v1/projects`

### 2.2 Communication Format

- Protocol: HTTP/HTTPS
- Request format: JSON, except file uploads
- Response format: JSON
- Character encoding: UTF-8
- Content type: application/json
- API versioning: URL-based versioning

### 2.3 Identifiers and Timestamps

- API Sentinel resource identifiers use UUID.
- Timestamps use ISO 8601 format.
- Timestamps are stored with timezone information.
- Pagination uses page and size parameters.

### 2.4 Authentication

Protected endpoints require a valid bearer access token.

Header:

`Authorization: Bearer <access-token>`

Authentication will be implemented using Spring Security and a token-based authentication mechanism.

The backend must validate the authenticated user and their project membership before accessing project data.

### 2.5 Project Roles

| Role | Permissions |
|---|---|
| OWNER | Full project management and scan access |
| EDITOR | Configure targets, identities, policies and scans |
| VIEWER | Read project information, scans and findings |

Only authorized project members may access project resources.

## 3. Standard Response Formats

### 3.1 Successful Response

Successful responses use a consistent data envelope.

Example:

```json
{
  "data": {
    "id": "project-uuid",
    "name": "API Security Assessment"
  },
  "meta": {}
}
```

### 3.2 Paginated Response

Collection endpoints use pagination.

Example:

```json
{
  "data": [],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3
  }
}
```

### 3.3 Error Response

All API errors use a consistent structure.

Example:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The request contains invalid fields.",
    "path": "/api/v1/projects",
    "timestamp": "2026-11-15T10:30:00Z",
    "traceId": "example-trace-id",
    "fieldErrors": [
      {
        "field": "name",
        "message": "Name is required."
      }
    ]
  }
}
```

The backend must not return stack traces, database credentials, tokens, or internal implementation details.

## 4. Authentication APIs

### 4.1 Login

**POST** `/auth/login`

Authenticates a platform user.

Request:

```json
{
  "email": "user@example.com",
  "password": "example-password"
}
```

Successful response: `200 OK`

On successful authentication, the server also issues a refresh token through an HTTP Set-Cookie header.

The refresh token cookie must use the following security attributes:

- HttpOnly
- Secure in production
- SameSite=Lax
- Path=/api/v1/auth

The refresh token must not be included in the JSON response.

The client should keep the short-lived access token in memory rather than persistent browser storage.

```json
{
  "data": {
    "accessToken": "<access-token>",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": "user-uuid",
      "email": "user@example.com"
    }
  }
}
```

Invalid credentials: `401 Unauthorized`.

The example credentials are placeholders, not actual project credentials.

### 4.2 Current User

**GET** `/auth/me`

Returns information about the authenticated platform user.

Successful response: `200 OK`.

### 4.3 Refresh Token

**POST** `/auth/refresh`

Issues a new access token using the refresh token stored in the HttpOnly cookie.

The server must:

- Validate the refresh token.
- Reject expired, revoked, or invalid refresh tokens.
- Rotate the refresh token after successful use.
- Issue the replacement refresh token through the Set-Cookie header.
- Invalidate the previously used refresh token.
- Apply appropriate CSRF protection and origin validation.

Successful response: `200 OK`.

```json
{
  "data": {
    "accessToken": "<new-access-token>",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

Invalid or expired refresh token: `401 Unauthorized`.

### 4.4 Logout

**POST** `/auth/logout`

Logs out the authenticated user from the current refresh-token session.

The server must:

- Revoke the applicable refresh-token session.
- Clear the refresh token cookie using the Set-Cookie header.
- Set the cookie expiration to zero.
- Clear the cookie using the same Path, Secure, and Same Site attributes used when it was issued.

Successful response: `204 No Content`.

Logout invalidates the current refresh-token session. Other active sessions remain unaffected. The current access token may remain valid until its expiration unless access-token revocation is implemented.

## 5. Project Management APIs

Base path:

`/projects`

### 5.1 Create Project

**POST** `/projects`

Request:

```json
{
  "name": "API Security Assessment",
  "description": "Authorization and business logic testing."
}
```

Successful response: `201 Created`.

Returns the newly created project.

The backend must create a corresponding membership record for the authenticated user in `project_members` with the role `OWNER`.

Project creation and owner membership creation must happen within a single database transaction. If either operation fails, the entire transaction must be rolled back.

### 5.2 List Projects

**GET** `/projects`

Returns projects accessible to the authenticated user.

Query parameters:

- `page`
- `size`
- `sort`

Successful response: `200 OK`.

The backend must return only projects for which the authenticated user has a corresponding membership record in `project_members`.

Project membership filtering must be applied before pagination and sorting to prevent unauthorized project information from appearing in the results.

### 5.3 Get Project

**GET** `/projects/{projectId}`

Returns project details.

Successful response: `200 OK`.

The backend must verify that the authenticated user is a member of the requested project before returning project details.

If the project does not exist or the user does not have access to it, the API must return `404 Not Found` to avoid exposing the existence of inaccessible projects.

### 5.4 Update Project

**PUT** `/projects/{projectId}`

Updates project information.

Request:

```json
{
  "name": "Updated Project Name",
  "description": "Updated project description."
}
```

Successful response: `200 OK`.

The backend must:

- Verify that the project exists and the authenticated user is a member.
- Allow only users with the `OWNER` or `EDITOR` role to update project information.
- Reject update requests from users with the `VIEWER` role.
- Validate the project name and description.
- Prevent updates to ownership, membership roles, or other protected fields through this endpoint.

If the project does not exist or the user does not have access, return `404 Not Found`.

If the user is a project member but has insufficient permissions, return `403 Forbidden`.

### 5.5 Delete Project

**DELETE** `/projects/{projectId}`

Deletes a project only when its data-retention and dependency rules permit deletion.

Projects containing retained scan or evidence history must not be deleted in a way that destroys research evidence.

Successful response: `204 No Content`.

Conflicting dependencies: `409 Conflict`.

The backend must:

- Allow only users with the `OWNER` role to delete a project.
- Verify that the project exists and the requesting user has access.
- Check scan history, retained evidence, findings, reports, and research experiment dependencies before deletion.
- Never cascade-delete records that must be retained for research or audit purposes.
- Perform deletion atomically when deletion is permitted.

If the project does not exist or the user does not have access, return `404 Not Found`.

If the user lacks the required permission, return `403 Forbidden`.

If retained records or dependencies prevent deletion, return `409 Conflict`. The project and its retained records must remain intact.



### 5.6 Project Members

#### 5.6.1 List Project Members

**GET** `/projects/{projectId}/members`

Lists members of the specified project.

Successful response: `200 OK`.

The backend must:

- Verify that the project exists.
- Verify that the authenticated user is a member of the requested project.
- Return only membership information belonging to the specified project.

If the project does not exist or the authenticated user does not have access, return `404 Not Found`.

#### 5.6.2 Add Project Member

**POST** `/projects/{projectId}/members`

Adds a user to the specified project.

Request:

```json
{
  "userId": "user-uuid",
  "role": "EDITOR"
}
```

Allowed roles: `EDITOR`, `VIEWER`.

Successful response: `201 Created`.

The backend must:

- Allow only users with the `OWNER` role to add project members.
- Verify that the specified project exists.
- Verify that the specified platform user exists.
- Allow only `EDITOR` and `VIEWER` roles through this endpoint.
- Prevent duplicate membership records.
- Associate the new membership with the specified project.
- Create the membership within a database transaction.

Ownership assignment or transfer must not be performed through this endpoint.

Error responses:

- `404 Not Found` — Project or specified user does not exist.
- `403 Forbidden` — Authenticated user does not have permission to add members.
- `409 Conflict` — The specified user is already a member of the project.

#### 5.6.3 Remove Project Member

**DELETE** `/projects/{projectId}/members/{userId}`

Removes a user's membership from the specified project.

Successful response: `204 No Content`.

The backend must:

- Allow only users with the `OWNER` role to remove project members.
- Verify that the specified project exists.
- Verify that the membership belongs to the specified project.
- Remove only the specified user's membership.
- Prevent removal of the last remaining project owner.
- Perform the membership removal within a database transaction.
- Prevent removal of a member whose role is `OWNER`. Ownership changes must be performed through a separate ownership-transfer operation.

Error responses:

- `404 Not Found` — Project or membership does not exist.
- `403 Forbidden` — Authenticated user does not have permission to remove members.
- `409 Conflict` — The specified member has the `OWNER` role or the removal would leave the project without an owner.
Ownership transfer must be handled through a separate operation with explicit authorization and validation.

## 6. Target Management APIs

## 6. Target Management APIs

### 6.1 Register Target

**POST** `/projects/{projectId}/targets`

Registers an API target for security testing within the specified project.

Request:

```json
{
  "name": "Sentinel Bank",
  "baseUrl": "http://localhost:8081",
  "authorizationConfirmed": true,
  "maxRequestsPerScan": 100
}
```

Successful response: `201 Created`.

```json
{
  "data": {
    "targetId": "target-uuid",
    "projectId": "project-uuid",
    "name": "Sentinel Bank",
    "baseUrl": "http://localhost:8081",
    "authorizationConfirmed": true,
    "maxRequestsPerScan": 100,
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The authenticated user must explicitly confirm that they have authorization to test the registered target.

The backend must:

- Allow only project members with the `OWNER` or `EDITOR` role to register targets.
- Verify that the specified project exists and the authenticated user has the required project permissions.
- Require `authorizationConfirmed` to be `true` during target registration.
- Record the authenticated user's identity and the server-generated confirmation timestamp.
- Associate the target with the specified project.
- Validate and normalize the target name and base URL.
- Enforce server-side limits on `maxRequestsPerScan`.
- Record target registration and authorization confirmation in the audit history.

Target URL validation must:

- Accept only `http` and `https` schemes.
- Reject malformed URLs and URLs containing embedded credentials, query parameters, or fragments.
- Resolve and validate destination IP addresses against the server-configured target network scope.
- Prevent access to cloud metadata services and other prohibited internal destinations.
- Prevent DNS rebinding by validating resolved addresses during registration and again before scan execution.
- Disable automatic redirects or validate every redirect destination before following it.

Loopback targets such as `localhost` may be permitted in explicitly configured local development or testing environments. Production access to internal networks must follow an explicit target allowlist or approved network-scope configuration.

Error responses:

- `400 Bad Request` — Invalid target configuration or authorization confirmation missing.
- `403 Forbidden` — Authenticated user does not have permission to register targets.
- `404 Not Found` — Specified project does not exist or is not accessible to the authenticated user.
- `409 Conflict` — A conflicting target registration already exists.
- `422 Unprocessable Entity` — Target URL or network scope violates the configured security restrictions.

### 6.2 Get Target

**GET** `/targets/{targetId}`

Returns the configuration and registration details of the specified target.

Successful response: `200 OK`.

```json
{
  "data": {
    "targetId": "target-uuid",
    "projectId": "project-uuid",
    "name": "Sentinel Bank",
    "baseUrl": "http://localhost:8081",
    "authorizationConfirmed": true,
    "maxRequestsPerScan": 100,
    "createdAt": "2026-11-15T10:30:00Z",
    "updatedAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Verify that the target exists.
- Identify the project associated with the target.
- Verify that the authenticated user is a member of that project.
- Return only target information belonging to the associated project.
- Never expose stored credentials, authentication secrets, or other sensitive configuration values in the response.

If the target does not exist or the authenticated user does not have access to its project, return `404 Not Found`.

### 6.3 Update Target

**PUT** `/targets/{targetId}`

Updates the configuration of an existing API target.

Request:

```json
{
  "name": "Sentinel Bank Updated",
  "baseUrl": "http://localhost:8081",
  "authorizationConfirmed": true,
  "maxRequestsPerScan": 150
}
```

Successful response: `200 OK`.

```json
{
  "data": {
    "targetId": "target-uuid",
    "projectId": "project-uuid",
    "name": "Sentinel Bank Updated",
    "baseUrl": "http://localhost:8081",
    "authorizationConfirmed": true,
    "maxRequestsPerScan": 150,
    "updatedAt": "2026-11-15T11:00:00Z"
  }
}
```

The backend must:

- Allow only project members with the `OWNER` or `EDITOR` role to update target configuration.
- Verify that the target exists and belongs to an accessible project.
- Validate the updated target name and base URL.
- Enforce server-side limits on `maxRequestsPerScan`.
- Apply the same URL and network-scope validation requirements used during target registration.
- Prevent updates to protected fields such as target ID, project ownership, and audit records.
- Record target configuration changes in the audit history.

Authorization confirmation rules:

- Changes to authorization status must be explicitly confirmed by the authenticated user.
- The backend must record the identity of the confirming user and the server-generated confirmation timestamp.
- Changing the target's base URL must invalidate the previous authorization confirmation.
- A changed base URL must not be used for scanning until authorization is explicitly confirmed again.
- Setting `authorizationConfirmed` to `false` must immediately prevent new scans from starting against the target.
- Any running scan must be safely stopped when authorization is revoked.
- The request field `authorizationConfirmed` alone must never be treated as proof of authorization without recording the confirmation event.

Successful updates must preserve the target's project association and existing scan evidence.

Error responses:

- `400 Bad Request` — Invalid target configuration.
- `403 Forbidden` — Authenticated user does not have permission to update the target.
- `404 Not Found` — Target does not exist or its project is not accessible to the authenticated user.
- `409 Conflict` — Update conflicts with the target's current state or an active operation.
- `422 Unprocessable Entity` — Updated target URL or network scope violates configured security restrictions.

### 6.4 Delete Target

**DELETE** `/targets/{targetId}`

Deletes or deactivates a target according to data-retention rules.

An existing scan history must not be silently destroyed.

## 7. OpenAPI Specification APIs

### 7.1 Upload Specification

**POST** `/targets/{targetId}/specifications`

Uploads an OpenAPI specification for a registered API target.

Content type: `multipart/form-data`.

Request field:

- `file`: OpenAPI JSON or YAML file.

Successful response: `201 Created`.

```json
{
  "data": {
    "specificationId": "specification-uuid",
    "targetId": "target-uuid",
    "format": "YAML",
    "sha256": "example-sha256-checksum",
    "parseStatus": "SUCCEEDED",
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

1. Authenticate the requesting user.
2. Verify that the target exists and belongs to an accessible project.
3. Allow only project members with the `OWNER` or `EDITOR` role to upload specifications.
4. Validate the uploaded file size against the configured server-side limit.
5. Determine the file format using its content rather than relying only on its extension or declared content type.
6. Accept supported OpenAPI JSON and YAML documents.
7. Reject unsupported formats and malformed documents.
8. Parse the specification using a compatible OpenAPI parser.
9. Calculate the SHA-256 checksum of the original uploaded bytes.
10. Preserve the original specification without modifying its contents.
11. Store normalized endpoint and operation information associated with the specification.
12. Record parsing status, parser version, checksum, and upload timestamp.

Parsing and file-handling security requirements:

- Use safe YAML parsing and reject arbitrary YAML tags or unsupported constructs.
- Restrict `$ref` resolution to references contained within the uploaded specification.
- Do not automatically fetch remote references or access arbitrary filesystem paths while parsing.
- Apply configured limits to parsing time, document size, nesting depth, and reference resolution.
- Prevent malformed or excessively complex specifications from exhausting server resources.
- Store uploaded specifications securely and restrict access to authorized project members.
- Do not execute scripts, examples, or other content included in the specification.
- Preserve the original uploaded file as immutable evidence.

The original specification and its normalized endpoint records must remain associated with the same target and project.

Error responses:

- `400 Bad Request` — Missing file or malformed upload request.
- `403 Forbidden` — User does not have permission to upload specifications.
- `404 Not Found` — Target does not exist or its project is not accessible to the authenticated user.
- `413 Payload Too Large` — Uploaded file exceeds the configured size limit.
- `415 Unsupported Media Type` — Unsupported file format.
- `422 Unprocessable Entity` — File is not a valid or supported OpenAPI specification.

### 7.2 List Specifications

**GET** `/targets/{targetId}/specifications`

Returns specifications registered for the specified target.

Query parameters:

- `page`
- `size`
- `sort`

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each specification entry should include:

- `specificationId`
- `targetId`
- `format`
- `sha256`
- `parseStatus`
- `createdAt`

The backend must:

- Verify that the target exists.
- Verify that the authenticated user is a member of the project associated with the target.
- Return only specifications belonging to the specified target.
- Apply authorization filtering before pagination and sorting.
- Never expose original specification contents or sensitive information through the listing response.

If the target does not exist or the user does not have access to its project, return `404 Not Found`.

### 7.3 Get Specification

**GET** `/specifications/{specificationId}`

Returns metadata and parsing information for the specified OpenAPI specification.

Successful response: `200 OK`.

```json
{
  "data": {
    "specificationId": "specification-uuid",
    "targetId": "target-uuid",
    "format": "YAML",
    "sha256": "example-sha256-checksum",
    "parseStatus": "SUCCEEDED",
    "parserVersion": "configured-parser-version",
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Verify that the specification exists.
- Identify its associated target and project.
- Verify that the authenticated user is a member of the associated project.
- Return metadata belonging only to the requested specification.
- Avoid exposing stored credentials, secrets, or other sensitive information.
- Preserve the original specification as immutable uploaded evidence.

If the specification does not exist or the user does not have access to its project, return `404 Not Found`.

The original uploaded file must not be exposed through this endpoint. Access to original specification content, if required, must use a separately authorized operation.

### 7.4 List Endpoints

**GET** `/targets/{targetId}/endpoints`

Returns normalized API endpoints extracted from the target's OpenAPI specifications.

Optional query parameters:

- `method` — Filter by HTTP method.
- `path` — Filter by endpoint path.
- `tag` — Filter by OpenAPI tag.
- `page`
- `size`
- `sort`

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each endpoint entry should include:

- `endpointId`
- `specificationId`
- `method`
- `path`
- `operationId`, when available.
- `summary`, when available.
- `tags`, when available.

The backend must:

- Verify that the target exists.
- Verify that the authenticated user is a member of the target's associated project.
- Return only endpoints associated with the specified target.
- Preserve the relationship between each endpoint and its source specification.
- Support filtering by HTTP method, path, and OpenAPI tag.
- Apply authorization filtering before pagination and sorting.
- Avoid exposing sensitive specification content or credentials.

Endpoint extraction must preserve the HTTP method and path combination as separate operations. For example, `GET /api/accounts` and `POST /api/accounts` must be represented as distinct operations.

If the target does not exist or the user does not have access to its project, return `404 Not Found`.

## 8. Identity and Authorization APIs

These APIs manage test identities, credentials, roles, and permissions used by the scanner to evaluate authorization behavior in the target API.

The identities and roles defined in this section represent users and authorization roles within the target application. They are separate from API Sentinel project membership roles (`OWNER`, `EDITOR`, and `VIEWER`).

Only project members with the `OWNER` or `EDITOR` role can create or modify test identities, credentials, roles, and permissions. Project members with the `VIEWER` role may access permitted read-only information.

### 8.1 Create Test Identity

**POST** `/projects/{projectId}/identities`

Creates a test identity within the specified project.

Request:

```json
{
  "displayName": "Alice",
  "identityType": "USER"
}
```

Successful response: `201 Created`.

```json
{
  "data": {
    "identityId": "identity-uuid",
    "projectId": "project-uuid",
    "displayName": "Alice",
    "identityType": "USER",
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Verify that the project exists and the authenticated user has the required project permissions.
- Allow only `OWNER` and `EDITOR` project members to create test identities.
- Validate the identity name and identity type.
- Associate the identity with the specified project.
- Prevent identity records from being associated with multiple projects.
- Generate a unique identity ID.
- Record the identity creation event.

Error responses:

- `400 Bad Request` — Invalid identity information.
- `403 Forbidden` — User does not have permission to create identities.
- `404 Not Found` — Project does not exist or is not accessible to the authenticated user.
- `409 Conflict` — A conflicting identity record already exists.

### 8.2 List Test Identities

**GET** `/projects/{projectId}/identities`

Returns test identities configured for the specified project.

Query parameters:

- `page`
- `size`
- `sort`

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each identity entry should include:

- `identityId`
- `displayName`
- `identityType`
- `createdAt`

The backend must:

- Verify that the authenticated user is a member of the specified project.
- Return only identities belonging to that project.
- Apply project-level authorization filtering before pagination and sorting.
- Never include stored credentials or secret values in the response.

If the project does not exist or the user does not have access, return `404 Not Found`.

### 8.3 Get Test Identity

**GET** `/identities/{identityId}`

Returns metadata for the specified test identity.

Successful response: `200 OK`.

```json
{
  "data": {
    "identityId": "identity-uuid",
    "projectId": "project-uuid",
    "displayName": "Alice",
    "identityType": "USER",
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Verify that the identity exists.
- Identify the project associated with the identity.
- Verify that the authenticated user is a member of that project.
- Return only identity metadata.
- Never return stored credentials, tokens, passwords, encryption keys, or other secrets.

If the identity does not exist or the authenticated user does not have access to its project, return `404 Not Found`.

### 8.4 Configure Identity Credentials

**POST** `/identities/{identityId}/credentials`

Configures authentication credentials for a test identity.

Request:

```json
{
  "authType": "BEARER",
  "secret": "<test-identity-token>"
}
```

Successful response: `201 Created`.

```json
{
  "data": {
    "credentialId": "credential-uuid",
    "identityId": "identity-uuid",
    "authType": "BEARER",
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Verify that the identity exists.
- Verify that the authenticated user has `OWNER` or `EDITOR` permissions in the identity's project.
- Validate the authentication type and credential format.
- Encrypt credentials before storing them in the database.
- Store encryption keys separately from the encrypted credential records.
- Use authenticated encryption to protect credential confidentiality and integrity.
- Restrict credential decryption to the authorized scan execution component.
- Never store credentials in plaintext.
- Never log or return secret values.
- Avoid including credentials in exception messages, scan reports, or request evidence.
- Record credential creation and modification events without recording secret values.

Credential security requirements:

- Credential configuration requests must use HTTPS in production.
- Secrets must be handled in memory only when necessary.
- Plaintext credentials must never be persisted in logs, temporary files, or generated reports.
- Credential rotation must be supported without exposing the previous or replacement secret.
- Access to encrypted credentials must be restricted and audited.

Error responses:

- `400 Bad Request` — Missing or invalid credential information.
- `403 Forbidden` — User does not have permission to configure credentials.
- `404 Not Found` — Identity does not exist or is not accessible to the authenticated user.
- `409 Conflict` — A conflicting credential configuration already exists.
- `422 Unprocessable Entity` — Unsupported authentication type or invalid credential format.

### 8.5 Create Role

**POST** `/projects/{projectId}/roles`

Creates a target-application role within the specified project.

Request:

```json
{
  "name": "CUSTOMER",
  "description": "Standard customer role."
}
```

Successful response: `201 Created`.

```json
{
  "data": {
    "roleId": "role-uuid",
    "projectId": "project-uuid",
    "name": "CUSTOMER",
    "description": "Standard customer role.",
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Allow only project members with the `OWNER` or `EDITOR` role to create roles.
- Verify that the specified project exists and is accessible.
- Validate the role name and description.
- Enforce case-insensitive uniqueness of role names within the project.
- Associate the role with the specified project.
- Generate a unique role ID.

Error responses:

- `400 Bad Request` — Invalid role information.
- `403 Forbidden` — User does not have permission to create roles.
- `404 Not Found` — Project does not exist or is not accessible.
- `409 Conflict` — A role with the same name already exists in the project.

### 8.6 List Roles

**GET** `/projects/{projectId}/roles`

Returns roles configured for the specified project.

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each role entry should include:

- `roleId`
- `name`
- `description`
- `createdAt`

The backend must:

- Verify that the authenticated user is a member of the specified project.
- Return only roles belonging to that project.
- Never expose roles belonging to another project.
- Apply project-level authorization filtering before pagination and sorting.

If the project does not exist or the user does not have access, return `404 Not Found`.

### 8.7 Assign Role

**POST** `/identities/{identityId}/roles`

Assigns an existing target-application role to a test identity.

Request:

```json
{
  "roleId": "role-uuid"
}
```

Successful response: `201 Created`.

```json
{
  "data": {
    "identityId": "identity-uuid",
    "roleId": "role-uuid",
    "assignedAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Verify that the identity exists.
- Verify that the role exists.
- Verify that the identity and role belong to the same project.
- Verify that the authenticated user has `OWNER` or `EDITOR` permissions in that project.
- Prevent duplicate identity-role assignments.
- Record the role assignment event.

An identity may have multiple roles when permitted by the configured target authorization model.

Error responses:

- `400 Bad Request` — Invalid assignment information.
- `403 Forbidden` — User does not have permission to assign roles.
- `404 Not Found` — Identity or role does not exist or is not accessible.
- `409 Conflict` — The role is already assigned to the identity.
- `422 Unprocessable Entity` — Identity and role belong to different projects.

### 8.8 Create Permission

**POST** `/projects/{projectId}/permissions`

Creates a permission within the specified project.

Request:

```json
{
  "code": "PAYMENT_APPROVE",
  "description": "Permission to approve payments."
}
```

Successful response: `201 Created`.

```json
{
  "data": {
    "permissionId": "permission-uuid",
    "projectId": "project-uuid",
    "code": "PAYMENT_APPROVE",
    "description": "Permission to approve payments.",
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Allow only project members with the `OWNER` or `EDITOR` role to create permissions.
- Verify that the specified project exists and is accessible.
- Validate permission codes and descriptions.
- Enforce case-insensitive uniqueness of permission codes within the project.
- Associate the permission with the specified project.
- Generate a unique permission ID.

Error responses:

- `400 Bad Request` — Invalid permission information.
- `403 Forbidden` — User does not have permission to create permissions.
- `404 Not Found` — Project does not exist or is not accessible.
- `409 Conflict` — A permission with the same code already exists in the project.

### 8.9 Assign Permission

**POST** `/roles/{roleId}/permissions`

Assigns an existing permission to a target-application role.

Request:

```json
{
  "permissionId": "permission-uuid"
}
```

Successful response: `201 Created`.

```json
{
  "data": {
    "roleId": "role-uuid",
    "permissionId": "permission-uuid",
    "assignedAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Verify that the role exists.
- Verify that the permission exists.
- Verify that the role and permission belong to the same project.
- Verify that the authenticated user has `OWNER` or `EDITOR` permissions in that project.
- Prevent duplicate role-permission assignments.
- Record the permission assignment event.

Error responses:

- `400 Bad Request` — Invalid assignment information.
- `403 Forbidden` — User does not have permission to assign permissions.
- `404 Not Found` — Role or permission does not exist or is not accessible.
- `409 Conflict` — Permission is already assigned to the role.
- `422 Unprocessable Entity` — Role and permission belong to different projects.

## 9. Resource Ownership APIs

These APIs manage the scanner-side representation of resources and their ownership relationships with test identities.

Resource ownership information is configuration metadata supplied by authorized project members. API Sentinel must not access the target application's database directly to retrieve or verify ownership information. The scanner must interact with targets through their HTTP interfaces only.

### 9.1 Register Resource Ownership

**POST** `/projects/{projectId}/ownership`

Creates or references a scanner-side resource model and associates it with a test identity.

Request:

```json
{
  "targetId": "target-uuid",
  "resourceType": "ACCOUNT",
  "externalKey": "101",
  "identityId": "identity-uuid",
  "relationshipType": "OWNER"
}
```

Successful response: `201 Created`.

```json
{
  "data": {
    "resourceId": "resource-uuid",
    "ownershipId": "ownership-uuid",
    "projectId": "project-uuid",
    "targetId": "target-uuid",
    "resourceType": "ACCOUNT",
    "externalKey": "101",
    "identityId": "identity-uuid",
    "relationshipType": "OWNER",
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Allow only project members with the `OWNER` or `EDITOR` role to register resource ownership.
- Verify that the specified project and target exist.
- Verify that the target belongs to the specified project.
- Verify that the specified test identity belongs to the same project.
- Validate the resource type, external key, and relationship type.
- Create a resource record if one does not already exist for the specified target, resource type, and external key.
- Reuse the existing resource record when a matching resource already exists.
- Create an ownership relationship between the resource and the specified test identity.
- Prevent duplicate ownership relationships.
- Associate all resource and ownership records with the correct project.
- Perform resource creation and ownership registration within a single database transaction.
- Record resource and ownership registration events in the audit history.

Resource identification rules:

- A resource must be uniquely identified within a target using its `targetId`, `resourceType`, and `externalKey`.
- External keys must be stored as strings to preserve identifiers such as `"001"`.
- A resource may have multiple ownership or access relationships when supported by the configured relationship model.
- Duplicate ownership relationships must not be created.

The ownership mapping represents the configured expected relationship between a test identity and a resource. It is not proof that the target application enforces the relationship correctly. The scanner will test the actual target behavior independently.

Error responses:

- `400 Bad Request` — Missing or invalid resource or ownership information.
- `403 Forbidden` — User does not have permission to register resource ownership.
- `404 Not Found` — Project, target, or identity does not exist or is not accessible.
- `409 Conflict` — The ownership relationship already exists or conflicts with an existing record.
- `422 Unprocessable Entity` — Target and identity belong to different projects, or the relationship is invalid.

### 9.2 List Resources

**GET** `/projects/{projectId}/resources`

Returns scanner-side resource models configured for the specified project.

Optional query parameters:

- `targetId` — Filter resources by target.
- `resourceType` — Filter by resource type.
- `externalKey` — Filter by external resource identifier.
- `page`
- `size`
- `sort`

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each resource entry should include:

- `resourceId`
- `targetId`
- `resourceType`
- `externalKey`
- `createdAt`
- `updatedAt`

The backend must:

- Verify that the project exists.
- Verify that the authenticated user is a member of the specified project.
- Return only resources belonging to that project.
- Apply project-level authorization filtering before pagination and sorting.
- Validate that any supplied `targetId` belongs to the specified project.
- Support the documented filters.
- Never expose resources belonging to another project.

If the project does not exist or the authenticated user does not have access, return `404 Not Found`.

If a supplied target does not belong to the project, return `404 Not Found`.

### 9.3 List Resource Ownership

**GET** `/projects/{projectId}/ownership`

Returns ownership and access relationships configured between test identities and resources within the specified project.

Optional query parameters:

- `targetId` — Filter relationships by target.
- `resourceId` — Filter relationships by resource.
- `identityId` — Filter relationships by test identity.
- `relationshipType` — Filter by relationship type.
- `page`
- `size`
- `sort`

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each ownership entry should include:

- `ownershipId`
- `resourceId`
- `targetId`
- `resourceType`
- `externalKey`
- `identityId`
- `relationshipType`
- `createdAt`

The backend must:

- Verify that the project exists.
- Verify that the authenticated user is a member of the specified project.
- Return only ownership relationships belonging to that project.
- Verify that associated resources, targets, and identities belong to the same project.
- Validate all supplied filter identifiers against the specified project.
- Apply project-level authorization filtering before pagination and sorting.
- Avoid returning unrelated project information.

If the project does not exist or the authenticated user does not have access, return `404 Not Found`.

If a supplied resource, target, or identity does not belong to the specified project, return `404 Not Found`.

## 10. Security Policy and Invariant APIs

Security policies and invariants define the expected security behavior of the target application.

Security policies describe expected authorization decisions for API operations, identities, and resources.

Security invariants describe conditions that must remain true across API requests, resource access, and business workflows.

These definitions are used by the scanner to generate security tests and evaluate observed target behavior. They must not be treated as proof that the target application actually enforces the defined rules.

Only project members with the `OWNER` or `EDITOR` role can create security policies and invariants. Project members with the `VIEWER` role may access permitted read-only information.

### 10.1 Create Security Policy

**POST** `/projects/{projectId}/policies`

Creates an authorization policy within the specified project.

Request:

```json
{
  "name": "Customer Account Ownership",
  "policyType": "OWNERSHIP",
  "endpointId": "endpoint-uuid",
  "expectedDecision": "DENY",
  "resourceType": "ACCOUNT",
  "ruleDefinition": {
    "ownerOnly": true
  }
}
```

Successful response: `201 Created`.

```json
{
  "data": {
    "policyId": "policy-uuid",
    "projectId": "project-uuid",
    "name": "Customer Account Ownership",
    "policyType": "OWNERSHIP",
    "endpointId": "endpoint-uuid",
    "expectedDecision": "DENY",
    "resourceType": "ACCOUNT",
    "ruleDefinition": {
      "ownerOnly": true
    },
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Allow only project members with the `OWNER` or `EDITOR` role to create policies.
- Verify that the project exists and the authenticated user has the required permissions.
- Verify that the specified endpoint exists and belongs to the specified project.
- Validate the policy name, policy type, and expected decision.
- Validate that the resource type is supported.
- Validate `ruleDefinition` against the supported rule schema for the selected policy type.
- Associate the policy with the specified project.
- Preserve the relationship between the policy and its endpoint.
- Prevent duplicate policy definitions where an equivalent active policy already exists.
- Record policy creation in the audit history.

Supported policy types:

- `OWNERSHIP` — Defines resource ownership and access restrictions.
- `ROLE_BASED` — Defines authorization requirements based on assigned roles.
- `PERMISSION_BASED` — Defines authorization requirements based on assigned permissions.
- `WORKFLOW` — Defines authorization expectations for workflow-related operations.

Supported expected decisions:

- `ALLOW` — The operation is expected to be permitted.
- `DENY` — The operation is expected to be rejected.

Rule definition requirements:

- Rule definitions must use validated declarative JSON structures.
- Arbitrary executable code, scripts, or expressions must not be accepted.
- Each policy type must have a documented and validated rule schema.
- Unsupported properties and invalid rule combinations must be rejected.
- The backend must not interpret untrusted rule definitions as executable instructions.

Error responses:

- `400 Bad Request` — Missing or invalid policy information.
- `403 Forbidden` — User does not have permission to create policies.
- `404 Not Found` — Project or endpoint does not exist or is not accessible.
- `409 Conflict` — An equivalent policy already exists.
- `422 Unprocessable Entity` — Rule definition or policy configuration is unsupported or invalid.

### 10.2 List Policies

**GET** `/projects/{projectId}/policies`

Returns security policies configured for the specified project.

Optional query parameters:

- `policyType` — Filter by policy type.
- `endpointId` — Filter by endpoint.
- `expectedDecision` — Filter by expected decision.
- `page`
- `size`
- `sort`

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each policy entry should include:

- `policyId`
- `name`
- `policyType`
- `endpointId`
- `expectedDecision`
- `resourceType`
- `ruleDefinition`
- `createdAt`

The backend must:

- Verify that the project exists.
- Verify that the authenticated user is a member of the specified project.
- Return only policies belonging to that project.
- Validate that any supplied endpoint filter belongs to the specified project.
- Apply project-level authorization filtering before pagination and sorting.
- Return only validated policy definitions.
- Prevent information belonging to other projects from appearing in the response.

If the project does not exist or the authenticated user does not have access, return `404 Not Found`.

### 10.3 Create Security Invariant

**POST** `/projects/{projectId}/invariants`

Creates a security invariant within the specified project.

Request:

```json
{
  "name": "Prevent Cross-Customer Account Access",
  "category": "OWNERSHIP",
  "description": "Customers must not access another customer's account.",
  "severity": "HIGH",
  "ruleDefinition": {
    "ownershipRequired": true
  }
}
```

Successful response: `201 Created`.

```json
{
  "data": {
    "invariantId": "invariant-uuid",
    "projectId": "project-uuid",
    "name": "Prevent Cross-Customer Account Access",
    "category": "OWNERSHIP",
    "description": "Customers must not access another customer's account.",
    "severity": "HIGH",
    "ruleDefinition": {
      "ownershipRequired": true
    },
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Allow only project members with the `OWNER` or `EDITOR` role to create invariants.
- Verify that the specified project exists and is accessible.
- Validate the invariant name, category, description, and severity.
- Validate `ruleDefinition` against the supported invariant rule schema.
- Associate the invariant with the specified project.
- Prevent duplicate invariant definitions where an equivalent active invariant already exists.
- Record invariant creation in the audit history.

Supported invariant categories:

- `OWNERSHIP` — Resources must only be accessible according to their configured ownership relationships.
- `AUTHORIZATION` — Operations must respect configured role and permission requirements.
- `WORKFLOW` — Workflow operations must follow permitted state transitions.
- `STATE` — Resource state changes must satisfy configured state constraints.

Supported severity levels:

- `LOW`
- `MEDIUM`
- `HIGH`
- `CRITICAL`

Rule definition requirements:

- Invariant definitions must use validated declarative JSON structures.
- Each category must have a documented and validated rule schema.
- Unsupported properties, missing required attributes, and invalid rule combinations must be rejected.
- Arbitrary scripts or executable code must not be accepted.
- Invariants must be evaluable by the scanner's validation engine.

Error responses:

- `400 Bad Request` — Missing or invalid invariant information.
- `403 Forbidden` — User does not have permission to create invariants.
- `404 Not Found` — Project does not exist or is not accessible.
- `409 Conflict` — An equivalent invariant already exists.
- `422 Unprocessable Entity` — Invariant rule definition or configuration is unsupported or invalid.

### 10.4 List Invariants

**GET** `/projects/{projectId}/invariants`

Returns security invariants configured for the specified project.

Optional query parameters:

- `category` — Filter by invariant category.
- `severity` — Filter by severity.
- `page`
- `size`
- `sort`

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each invariant entry should include:

- `invariantId`
- `name`
- `category`
- `description`
- `severity`
- `ruleDefinition`
- `createdAt`

The backend must:

- Verify that the project exists.
- Verify that the authenticated user is a member of the specified project.
- Return only invariants belonging to that project.
- Apply project-level authorization filtering before pagination and sorting.
- Return only validated invariant definitions.
- Prevent information belonging to other projects from appearing in the response.

If the project does not exist or the authenticated user does not have access, return `404 Not Found`.

## 11. Workflow APIs

These APIs define business workflows, their possible states, and the transitions permitted between those states.

Workflows are associated with a specific target and project. They are used by the scanner to model expected business processes, generate workflow test sequences, and detect invalid state transitions.

Only project members with the `OWNER` or `EDITOR` role can create workflows, states, and transitions. Project members with the `VIEWER` role may access permitted read-only workflow information.

### 11.1 Create Workflow

**POST** `/projects/{projectId}/workflows`

Creates a business workflow within the specified project and target.

Request:

```json
{
  "targetId": "target-uuid",
  "name": "Payment Workflow",
  "resourceType": "PAYMENT",
  "description": "Payment state transition model."
}
```

Successful response: `201 Created`.

```json
{
  "data": {
    "workflowId": "workflow-uuid",
    "projectId": "project-uuid",
    "targetId": "target-uuid",
    "name": "Payment Workflow",
    "resourceType": "PAYMENT",
    "description": "Payment state transition model.",
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Allow only project members with the `OWNER` or `EDITOR` role to create workflows.
- Verify that the project exists.
- Verify that the target exists and belongs to the specified project.
- Validate the workflow name, resource type, and description.
- Associate the workflow with the specified project and target.
- Prevent duplicate workflow names for the same target.
- Generate a unique workflow ID.
- Record workflow creation in the audit history.

Error responses:

- `400 Bad Request` — Missing or invalid workflow information.
- `403 Forbidden` — User does not have permission to create workflows.
- `404 Not Found` — Project or target does not exist or is not accessible.
- `409 Conflict` — A workflow with the same name already exists for the target.
- `422 Unprocessable Entity` — Unsupported resource type or invalid workflow configuration.

### 11.2 Add Workflow State

**POST** `/workflows/{workflowId}/states`

Adds a state to an existing workflow.

Request:

```json
{
  "name": "PENDING",
  "isInitial": true,
  "isTerminal": false
}
```

Successful response: `201 Created`.

```json
{
  "data": {
    "stateId": "state-uuid",
    "workflowId": "workflow-uuid",
    "name": "PENDING",
    "isInitial": true,
    "isTerminal": false,
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Verify that the workflow exists.
- Verify that the authenticated user has `OWNER` or `EDITOR` permissions in the workflow's project.
- Validate the state name.
- Prevent duplicate state names within the same workflow.
- Associate the state with the specified workflow.
- Permit only one initial state per workflow.
- Record state creation in the audit history.

Workflow state rules:

- An initial state represents the starting point of the business process.
- A terminal state represents a completed or otherwise final workflow state.
- A workflow must have exactly one initial state before it can be used for scan execution.
- Terminal states must not have outgoing transitions.
- A state must not be associated with more than one workflow.

A workflow may be configured incrementally. Validation of the complete workflow must occur before it is used for scan execution.

Error responses:

- `400 Bad Request` — Missing or invalid state information.
- `403 Forbidden` — User does not have permission to add workflow states.
- `404 Not Found` — Workflow does not exist or is not accessible.
- `409 Conflict` — Duplicate state name or another initial state already exists.
- `422 Unprocessable Entity` — Invalid state configuration.

### 11.3 Add Workflow Transition

**POST** `/workflows/{workflowId}/transitions`

Adds an allowed state transition to an existing workflow.

Request:

```json
{
  "fromStateId": "pending-state-uuid",
  "toStateId": "approved-state-uuid",
  "operationEndpointId": "endpoint-uuid",
  "requiredRoleId": "admin-role-uuid"
}
```

`operationEndpointId` and `requiredRoleId` are optional and may be omitted when the transition does not require a specific API operation or role.

Successful response: `201 Created`.

```json
{
  "data": {
    "transitionId": "transition-uuid",
    "workflowId": "workflow-uuid",
    "fromStateId": "pending-state-uuid",
    "toStateId": "approved-state-uuid",
    "operationEndpointId": "endpoint-uuid",
    "requiredRoleId": "admin-role-uuid",
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Verify that the workflow exists.
- Verify that the authenticated user has `OWNER` or `EDITOR` permissions in the workflow's project.
- Verify that both source and destination states exist.
- Verify that both states belong to the specified workflow.
- Verify that the source state is not terminal.
- If `operationEndpointId` is provided, verify that the endpoint belongs to the workflow's target and project.
- If `requiredRoleId` is provided, verify that the role belongs to the workflow's project.
- Prevent duplicate transition definitions.
- Associate the transition with the specified workflow.
- Record transition creation in the audit history.

Transition rules:

- Transitions define permitted movement between workflow states.
- Transitions originating from terminal states are not permitted.
- An optional operation endpoint identifies the API operation associated with the transition.
- An optional required role identifies the role permitted to perform the transition.
- When no required role is specified, the transition has no role restriction in this workflow model. Other configured authorization policies may still apply.
- A transition must never connect states belonging to different workflows.
- A transition must never reference an endpoint or role belonging to another project or target where applicable.

Error responses:

- `400 Bad Request` — Missing or invalid transition information.
- `403 Forbidden` — User does not have permission to add transitions.
- `404 Not Found` — Workflow, state, endpoint, or role does not exist or is not accessible.
- `409 Conflict` — An equivalent transition already exists.
- `422 Unprocessable Entity` — States belong to different workflows, the source state is terminal, or an endpoint or role belongs to an incompatible project or target.

### 11.4 Get Workflow

**GET** `/workflows/{workflowId}`

Returns workflow metadata, states, and transitions.

Successful response: `200 OK`.

```json
{
  "data": {
    "workflowId": "workflow-uuid",
    "projectId": "project-uuid",
    "targetId": "target-uuid",
    "name": "Payment Workflow",
    "resourceType": "PAYMENT",
    "description": "Payment state transition model.",
    "states": [
      {
        "stateId": "pending-state-uuid",
        "name": "PENDING",
        "isInitial": true,
        "isTerminal": false
      },
      {
        "stateId": "approved-state-uuid",
        "name": "APPROVED",
        "isInitial": false,
        "isTerminal": true
      }
    ],
    "transitions": [
      {
        "transitionId": "transition-uuid",
        "fromStateId": "pending-state-uuid",
        "toStateId": "approved-state-uuid",
        "operationEndpointId": "endpoint-uuid",
        "requiredRoleId": "admin-role-uuid"
      }
    ],
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Verify that the workflow exists.
- Identify its associated project.
- Verify that the authenticated user is a member of that project.
- Return only states and transitions belonging to the requested workflow.
- Preserve the relationships between workflow states and transitions.
- Ensure that optional endpoint and role references belong to the workflow's target and project.
- Avoid returning information belonging to other projects.

If the workflow does not exist or the authenticated user does not have access to its project, return `404 Not Found`.

## 12. Graph Snapshot APIs

Graph snapshots represent immutable versions of the security model constructed from the configured target, API specification, test identities, roles, permissions, resources, ownership relationships, security policies, invariants, and workflows.

Each graph snapshot must belong to exactly one project, target, and API specification.

### 12.1 Build Graph Snapshot

**POST** `/projects/{projectId}/graph-snapshots`

Builds a graph snapshot using the configured security model.

Request:

```json
{
  "targetId": "target-uuid",
  "specificationId": "specification-uuid"
}
```

Successful response: `201 Created`.

```json
{
  "data": {
    "snapshotId": "snapshot-uuid",
    "projectId": "project-uuid",
    "targetId": "target-uuid",
    "specificationId": "specification-uuid",
    "version": 1,
    "status": "READY",
    "nodeCount": 25,
    "edgeCount": 40,
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Allow only project members with the `OWNER` or `EDITOR` role to build graph snapshots.
- Verify that the project exists and is accessible.
- Verify that the target belongs to the specified project.
- Verify that the specification belongs to the specified target and project.
- Verify that the specification has been parsed successfully.
- Include only configured entities belonging to the selected project and target.
- Validate relationships between identities, roles, permissions, resources, endpoints, policies, and workflows.
- Generate typed graph nodes and directed edges according to the graph model.
- Assign a unique snapshot ID and version.
- Record node and edge counts.
- Associate all generated nodes and edges with the same snapshot.
- Preserve previously generated snapshots as immutable records.
- Record graph snapshot creation in the audit history.

Graph construction requirements:

- A graph snapshot must contain only entities and relationships associated with its selected target and project.
- Nodes must use supported types such as `IDENTITY`, `ROLE`, `RESOURCE`, `ENDPOINT`, `OPERATION`, and `WORKFLOW_STATE`.
- Edges must use supported relationship types such as `HAS_ROLE`, `OWNS`, `CAN_ACCESS`, `INVOKES`, `TRANSITIONS_TO`, `REQUIRES_ROLE`, and `OPERATES_ON`.
- Every edge must reference valid source and destination nodes within the same snapshot.
- Duplicate nodes and edges must be prevented.
- Graph construction must not modify the original project configuration.
- The generated snapshot must be immutable after successful creation.

Error responses:

- `400 Bad Request` — Missing or invalid graph configuration.
- `403 Forbidden` — User does not have permission to build graph snapshots.
- `404 Not Found` — Project, target, or specification does not exist or is not accessible.
- `409 Conflict` — Graph construction conflicts with an existing operation.
- `422 Unprocessable Entity` — Specification or configured graph relationships are invalid.

### 12.2 List Graph Snapshots

**GET** `/projects/{projectId}/graph-snapshots`

Returns graph snapshot versions belonging to the specified project.

Optional query parameters:

- `targetId`
- `specificationId`
- `status`
- `page`
- `size`
- `sort`

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each snapshot entry should include:

- `snapshotId`
- `targetId`
- `specificationId`
- `version`
- `status`
- `nodeCount`
- `edgeCount`
- `createdAt`

The backend must:

- Verify that the authenticated user is a member of the specified project.
- Return only snapshots belonging to that project.
- Validate that supplied target and specification filters belong to the project.
- Apply authorization filtering before pagination and sorting.
- Preserve snapshot version information.

If the project does not exist or the user does not have access, return `404 Not Found`.

### 12.3 Get Graph Snapshot

**GET** `/graph-snapshots/{snapshotId}`

Returns metadata for the specified graph snapshot.

Successful response: `200 OK`.

The response should include:

- `snapshotId`
- `projectId`
- `targetId`
- `specificationId`
- `version`
- `status`
- `nodeCount`
- `edgeCount`
- `createdAt`

The backend must:

- Verify that the snapshot exists.
- Identify its associated project.
- Verify that the authenticated user is a member of that project.
- Return metadata belonging only to the requested snapshot.
- Ensure that graph nodes and edges belong to the same snapshot.
- Never combine nodes or edges from different snapshots.

Graph snapshots must remain immutable after successful construction.

If the snapshot does not exist or the authenticated user does not have access to its project, return `404 Not Found`.

## 13. Scan Management APIs

These APIs manage security scans, including scan creation, execution status, progress tracking, cancellation, generated test cases, findings, and execution history.

Scans are processed asynchronously. A successful scan creation request places the scan in a queue for execution.

All scan records, test cases, execution records, and findings must remain associated with their originating project, target, and API specification.

Project access requirements:

- Project members with the `OWNER` or `EDITOR` role may create and cancel scans.
- All project members, including `VIEWER`, may access permitted scan information and results.
- The backend must verify project membership before returning scan-related information.
- A user must never access another project's scans, tests, executions, or findings by manipulating resource identifiers.

### 13.1 Start Scan

**POST** `/projects/{projectId}/scans`

Starts an asynchronous security scan.

Request:

```json
{
  "targetId": "target-uuid",
  "specificationId": "specification-uuid",
  "graphSnapshotId": "graph-snapshot-uuid",
  "mode": "HYBRID",
  "maxDepth": 4,
  "maxTests": 100,
  "requestTimeoutMs": 5000,
  "includeBola": true,
  "includeBfla": true,
  "includeWorkflow": true,
  "allowStateChangingTests": false
}
```

`graphSnapshotId` is optional. When omitted, the backend may use or generate an appropriate graph snapshot according to the scan workflow.

Successful response: `202 Accepted`.

```json
{
  "data": {
    "scanId": "scan-uuid",
    "status": "QUEUED",
    "mode": "HYBRID",
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The response returns a scan identifier. The client retrieves progress using the scan status endpoints.

The backend must validate:

- The target belongs to the specified project.
- The target has a recorded authorization confirmation.
- The API specification belongs to the specified target and project.
- The API specification has been parsed successfully.
- If provided, the graph snapshot belongs to the same project and corresponds to the selected target and specification.
- The requesting user has `OWNER` or `EDITOR` permissions in the project.
- The scan mode is supported.
- `maxDepth`, `maxTests`, and `requestTimeoutMs` are within configured server-side limits.
- At least one vulnerability category is enabled.
- The selected vulnerability categories are supported.
- The requested test budget does not exceed the target's configured request limits.
- The selected identities, credentials, resources, policies, invariants, and workflows are valid for the selected target and project.

State-changing tests must require explicit authorization recorded for the target and suitable target configuration. The `allowStateChangingTests` request field alone must not be treated as proof of authorization.

Additional scan safety requirements:

- State-changing tests must remain disabled by default.
- The scanner must distinguish read-only tests from state-changing tests.
- State-changing operations must be generated only when explicitly authorized and supported by the configured target.
- The scanner must enforce request limits, timeouts, and bounded test generation.
- The scanner must recheck target authorization before executing requests.
- If target authorization is revoked during execution, the scanner must stop sending further requests and safely terminate the scan.
- Credentials must be retrieved through the authorized credential-handling mechanism and must never be exposed in scan responses or logs.

Supported scan modes:

- `RULE_BASED`
- `GRAPH_GUIDED`
- `HYBRID`

Initial scan statuses:

- `QUEUED`
- `RUNNING`
- `COMPLETED`
- `FAILED`
- `CANCELLING`
- `CANCELLED`

The scan status must reflect the actual execution state. The backend must not report a scan as completed while test executions remain unfinished.

Error responses:

- `400 Bad Request` — Invalid scan configuration.
- `401 Unauthorized` — Missing or invalid authentication.
- `403 Forbidden` — User does not have permission to start scans.
- `404 Not Found` — Project, target, specification, or graph snapshot does not exist or is not accessible.
- `409 Conflict` — Scan cannot be started because of an incompatible target or scan state.
- `422 Unprocessable Entity` — Scan configuration is semantically invalid or unauthorized operations are requested.
- `429 Too Many Requests` — Scan creation or execution limits have been exceeded.

### 13.2 Get Scan

**GET** `/scans/{scanId}`

Returns scan configuration, execution status, and summary information.

Successful response: `200 OK`.

```json
{
  "data": {
    "scanId": "scan-uuid",
    "projectId": "project-uuid",
    "targetId": "target-uuid",
    "specificationId": "specification-uuid",
    "graphSnapshotId": "graph-snapshot-uuid",
    "mode": "HYBRID",
    "status": "RUNNING",
    "maxDepth": 4,
    "maxTests": 100,
    "requestTimeoutMs": 5000,
    "allowStateChangingTests": false,
    "totalTests": 100,
    "completedExecutions": 45,
    "failedExecutions": 2,
    "createdAt": "2026-11-15T10:30:00Z",
    "startedAt": "2026-11-15T10:31:00Z",
    "completedAt": null
  }
}
```

The backend must:

- Verify that the scan exists.
- Identify the project associated with the scan.
- Verify that the authenticated user is a member of that project.
- Return only information belonging to the requested scan.
- Ensure that the scan's target, specification, and graph snapshot belong to the same project.
- Return the actual scan status and execution summary.
- Never expose authentication credentials or sensitive configuration values.

If the scan does not exist or the authenticated user does not have access to its project, return `404 Not Found`.

### 13.3 Get Scan Progress

**GET** `/scans/{scanId}/progress`

Returns the execution progress of the specified scan.

Successful response: `200 OK`.

```json
{
  "data": {
    "scanId": "scan-uuid",
    "status": "RUNNING",
    "totalGeneratedTests": 100,
    "completedExecutions": 45,
    "failedExecutions": 2,
    "pendingExecutions": 53,
    "progressPercentage": 45,
    "updatedAt": "2026-11-15T10:35:00Z"
  }
}
```

The response must include:

- Current scan status.
- Total generated tests.
- Completed executions.
- Failed executions.
- Pending executions.
- Current progress percentage.
- Last progress update timestamp.

The backend must:

- Verify that the scan exists.
- Verify that the authenticated user is a member of the scan's project.
- Calculate progress from actual test generation and execution records.
- Ensure that execution counts do not exceed the generated test count.
- Avoid reporting pending tests as completed.
- Return a progress percentage consistent with the actual execution state.

Progress calculation:

`progressPercentage = (completedExecutions / totalGeneratedTests) × 100`

If no tests have been generated, progress must be reported as `0`.

The progress percentage must not be presented as proof that vulnerabilities were detected or that the target is secure.

### 13.4 Cancel Scan

**POST** `/scans/{scanId}/cancel`

Requests cancellation of a queued or running scan.

Successful response: `202 Accepted`.

```json
{
  "data": {
    "scanId": "scan-uuid",
    "status": "CANCELLING",
    "message": "Scan cancellation requested."
  }
}
```

The backend must:

- Verify that the scan exists.
- Verify that the authenticated user has `OWNER` or `EDITOR` permissions in the scan's project.
- Accept cancellation requests only for scans in `QUEUED` or `RUNNING` status.
- Stop scheduling additional test executions.
- Signal active executions to terminate safely.
- Stop sending further requests after cancellation has been processed.
- Preserve completed executions, findings, and evidence.
- Update the scan status to `CANCELLED` after safe termination.
- Record the cancellation event and requesting user.

Cancellation is asynchronous. The scan may remain in `CANCELLING` status while active executions are being safely terminated.

A cancellation request must not delete previously generated tests, completed executions, findings, or evidence.

Error responses:

- `403 Forbidden` — User does not have permission to cancel the scan.
- `404 Not Found` — Scan does not exist or is not accessible.
- `409 Conflict` — Scan is already in a terminal state or cancellation cannot be performed in its current state.

### 13.5 List Scan Tests

**GET** `/scans/{scanId}/tests`

Returns test cases generated for the specified scan.

Optional query parameters:

- `category` — Filter by vulnerability category.
- `status` — Filter by test execution status.
- `page`
- `size`
- `sort`

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each test entry should include:

- `testCaseId`
- `scanId`
- `category`
- `testName`
- `description`
- `identityId`, when applicable.
- `endpointId`, when applicable.
- `resourceId`, when applicable.
- `generationMode`
- `createdAt`

The backend must:

- Verify that the scan exists.
- Verify that the authenticated user is a member of the scan's project.
- Return only test cases associated with the specified scan.
- Apply authorization filtering before pagination and sorting.
- Preserve the relationship between each test case and its originating scan.
- Avoid returning credentials or sensitive request data.

If the scan does not exist or the user does not have access to its project, return `404 Not Found`.

### 13.6 List Scan Findings

**GET** `/scans/{scanId}/findings`

Returns security findings associated with the specified scan.

Optional query parameters:

- `category` — Filter by vulnerability category.
- `severity` — Filter by severity.
- `validationStatus` — Filter by validation status.
- `page`
- `size`
- `sort`

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each finding entry should include:

- `findingId`
- `scanId`
- `title`
- `category`
- `severity`
- `validationStatus`
- `endpointId`
- `testCaseId`
- `detectedAt`

The backend must:

- Verify that the scan exists.
- Verify that the authenticated user is a member of the scan's project.
- Return only findings associated with the specified scan.
- Apply project-level authorization filtering before pagination and sorting.
- Ensure that each finding references a valid test case from the same scan.
- Exclude secrets and unredacted sensitive evidence from the response.

If the scan does not exist or the user does not have access to its project, return `404 Not Found`.

### 13.7 Get Test Details

**GET** `/tests/{testCaseId}`

Returns the definition and configuration of a generated test case.

Successful response: `200 OK`.

```json
{
  "data": {
    "testCaseId": "test-case-uuid",
    "scanId": "scan-uuid",
    "category": "BOLA",
    "testName": "Cross-Identity Account Access",
    "description": "Tests whether one customer can access another customer's account.",
    "generationMode": "HYBRID",
    "identityId": "identity-uuid",
    "endpointId": "endpoint-uuid",
    "resourceId": "resource-uuid",
    "createdAt": "2026-11-15T10:32:00Z"
  }
}
```

The backend must:

- Verify that the test case exists.
- Identify its originating scan and project.
- Verify that the authenticated user is a member of that project.
- Return only the requested test case.
- Preserve its relationships with the originating scan, identity, endpoint, and resource.
- Never expose credentials, tokens, or unredacted sensitive request information.

If the test case does not exist or the user does not have access to its project, return `404 Not Found`.

### 13.8 List Test Executions

**GET** `/tests/{testCaseId}/executions`

Returns execution attempts associated with the specified test case.

Optional query parameters:

- `status`
- `page`
- `size`
- `sort`

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each execution entry should include:

- `executionId`
- `testCaseId`
- `scanId`
- `status`
- `startedAt`
- `completedAt`
- `httpStatusCode`, when available.
- `durationMs`, when available.
- `validationStatus`

The backend must:

- Verify that the test case exists.
- Identify its originating scan and project.
- Verify that the authenticated user is a member of that project.
- Return only executions associated with the specified test case.
- Preserve execution-to-test-case and execution-to-scan relationships.
- Apply authorization filtering before pagination and sorting.
- Never expose raw credentials or unredacted sensitive request and response data.

Execution statuses must reflect actual execution records and may include:

- `PENDING`
- `RUNNING`
- `COMPLETED`
- `FAILED`
- `CANCELLED`
- `SKIPPED`

An execution failure must not automatically be classified as a vulnerability.

If the test case does not exist or the user does not have access to its project, return `404 Not Found`.

## 14. Finding and Evidence APIs

These APIs provide access to security findings and the HTTP evidence collected during scan execution.

Every finding must be associated with an originating scan, generated test case, and one or more supporting test executions.

Findings must be based on validated observations. An HTTP response alone must not automatically be classified as proof of a vulnerability.

Project access requirements:

- All project members, including `OWNER`, `EDITOR`, and `VIEWER`, may access permitted findings and sanitized evidence.
- The backend must verify project membership before returning finding or evidence information.
- Findings and evidence must remain associated with their originating project and scan.
- Users must never access findings or evidence belonging to another project by manipulating resource identifiers.

### 14.1 Get Finding

**GET** `/findings/{findingId}`

Returns details of the specified security finding.

Successful response: `200 OK`.

```json
{
  "data": {
    "findingId": "finding-uuid",
    "scanId": "scan-uuid",
    "testCaseId": "test-case-uuid",
    "title": "Cross-Customer Account Access",
    "category": "BOLA",
    "severity": "HIGH",
    "validationStatus": "CONFIRMED",
    "affectedEndpoint": {
      "endpointId": "endpoint-uuid",
      "method": "GET",
      "path": "/api/accounts/{id}"
    },
    "testIdentityId": "identity-uuid",
    "resourceId": "resource-uuid",
    "expectedBehavior": "A customer must not access another customer's account.",
    "observedBehavior": "The test identity accessed an account belonging to another identity.",
    "evidenceCount": 2,
    "detectedAt": "2026-11-15T10:35:00Z"
  }
}
```

The backend must:

- Verify that the finding exists.
- Identify its associated scan and project.
- Verify that the authenticated user is a member of the finding's project.
- Return only information associated with the requested finding.
- Verify that the finding references a valid test case from the same scan.
- Verify that the affected endpoint, test identity, and resource belong to the appropriate project and target.
- Return the expected security behavior and observed test result.
- Return the actual validation status.
- Include the number of supporting evidence records.
- Avoid exposing authentication credentials, raw tokens, passwords, or sensitive HTTP data.

Each finding must preserve traceability to:

- Its originating project.
- The target and API specification.
- The scan that generated it.
- The generated test case.
- The test identity used during execution, where applicable.
- The affected endpoint.
- The supporting execution records.
- The associated evidence records.

Finding validation requirements:

- A finding must be supported by at least one completed test execution and corresponding evidence.
- The validation engine must evaluate observed behavior against the configured security policies and invariants.
- Network failures, timeouts, malformed requests, and other execution errors must not automatically become confirmed security findings.
- An inconclusive test result must not be reported as a confirmed vulnerability.
- The finding must preserve the actual expected behavior and observed behavior used during validation.
- Duplicate findings for the same vulnerability scenario within a scan must be handled according to the finding deduplication rules.

If the finding does not exist or the authenticated user does not have access to its project, return `404 Not Found`.

### 14.2 Get Finding Evidence

**GET** `/findings/{findingId}/evidence`

Returns sanitized HTTP evidence supporting the specified finding.

Optional query parameters:

- `executionId` — Filter evidence by test execution.
- `page` — Zero-based page number.
- `size` — Number of evidence records per page.
- `sort` — Permitted field and direction.

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each evidence entry should include:

- `evidenceId`
- `findingId`
- `scanId`
- `testCaseId`
- `executionId`
- `sequenceNumber`
- `requestMethod`
- `requestUrl`
- `requestHeaders`
- `requestBody`
- `responseStatusCode`
- `responseHeaders`
- `responseBody`
- `durationMs`
- `capturedAt`

Example response data:

```json
{
  "data": [
    {
      "evidenceId": "evidence-uuid",
      "findingId": "finding-uuid",
      "scanId": "scan-uuid",
      "testCaseId": "test-case-uuid",
      "executionId": "execution-uuid",
      "sequenceNumber": 1,
      "requestMethod": "GET",
      "requestUrl": "http://localhost:8081/api/accounts/101",
      "requestHeaders": {
        "Authorization": "[REDACTED]"
      },
      "requestBody": null,
      "responseStatusCode": 200,
      "responseHeaders": {
        "Content-Type": "application/json"
      },
      "responseBody": {
        "accountId": 101,
        "owner": "Alice",
        "balance": 5000
      },
      "durationMs": 85,
      "capturedAt": "2026-11-15T10:35:00Z"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

The backend must:

- Verify that the finding exists.
- Verify that the authenticated user is a member of the finding's project.
- Return only evidence associated with the requested finding.
- Verify that every evidence record belongs to the same scan and test case as the finding.
- Verify that each execution belongs to the originating test case and scan.
- Preserve the chronological order of evidence using `sequenceNumber` and capture timestamps.
- Support filtering by execution ID.
- Apply authorization filtering before pagination and sorting.
- Return sanitized request and response information only.
- Never expose raw authentication credentials or other sensitive values.

Evidence sanitization requirements:

- Redact `Authorization`, `Cookie`, `Set-Cookie`, and other authentication-related headers.
- Redact passwords, access tokens, refresh tokens, API keys, and client secrets.
- Remove or redact sensitive query parameters.
- Apply configured redaction rules to request and response bodies.
- Avoid exposing sensitive personal information and confidential application data.
- Do not include encryption keys or stored credential values.
- Ensure that exception messages and diagnostic fields do not reveal secrets.
- Apply sanitization before evidence is persisted or made available through the API.
- Use consistent redaction so repeated evidence records can be compared without revealing the original secret values.

Evidence integrity requirements:

- Evidence records must be immutable after successful capture.
- Every evidence record must reference its originating test execution.
- Multiple evidence records may belong to one execution.
- `sequenceNumber` must preserve the order of HTTP exchanges within an execution.
- Evidence must retain its relationship with the finding, test case, scan, and project.
- Evidence must not be fabricated or manually substituted for actual HTTP observations.
- Failed and cancelled executions must not be represented as successful evidence.

Error responses:

- `400 Bad Request` — Invalid pagination or filter parameters.
- `404 Not Found` — Finding does not exist or the authenticated user does not have access to its project.

### 14.3 Evidence and Finding Integrity

The backend must enforce the following relationships:

- Every finding must reference a valid scan.
- Every finding must reference a test case belonging to that scan.
- Every supporting execution must belong to that test case and scan.
- Every evidence record must reference its originating execution.
- Every finding-evidence association must remain within the same project and scan.
- Evidence records must preserve their execution sequence.
- Findings must not reference evidence from unrelated scans or projects.

Finding validation status must reflect the actual validation result.

A confirmed finding must have supporting execution evidence and a successful validation result.

An inconclusive or failed execution must not be classified as a confirmed vulnerability.

Sanitized evidence must remain available for reporting, debugging, and reproducible research evaluation, subject to the project's data-retention rules.

## 15. Reporting APIs

These APIs manage the generation, tracking, and retrieval of security scan reports.

Reports summarize scan configuration, execution results, validated findings, and sanitized supporting evidence.

Report generation is asynchronous and must use data associated with the selected scan.

Project access requirements:

- Project members with the `OWNER` or `EDITOR` role may request report generation.
- All project members, including `VIEWER`, may access permitted report metadata and download reports.
- The backend must verify project membership before providing report information or files.
- Reports must contain information belonging only to their originating scan and project.

### 15.1 Generate Scan Report

**POST** `/scans/{scanId}/reports`

Requests generation of a security report for the specified scan.

Request:

```json
{
  "format": "JSON"
}
```

Supported formats:

- `JSON`
- `PDF`
- `HTML`

Successful response: `202 Accepted`.

```json
{
  "data": {
    "reportId": "report-uuid",
    "scanId": "scan-uuid",
    "format": "JSON",
    "status": "QUEUED",
    "createdAt": "2026-11-15T10:40:00Z"
  }
}
```

The backend must:

- Verify that the scan exists.
- Verify that the authenticated user has `OWNER` or `EDITOR` permissions in the scan's project.
- Verify that the scan belongs to the requested project and target.
- Validate the requested report format.
- Generate the report using actual scan configuration, execution records, validated findings, and sanitized evidence.
- Associate the report with the originating scan and project.
- Assign a unique report ID.
- Process report generation asynchronously.
- Preserve the scan's original results and evidence.
- Prevent report generation from modifying scan records or findings.
- Record report generation requests in the audit history.

Report generation requirements:

- Reports must include the scan ID, target, API specification reference, scan mode, scan status, and generation timestamp.
- Reports must include a summary of generated tests and their execution results.
- Reports must include findings, categories, severity levels, and validation statuses.
- Reports must distinguish confirmed findings from inconclusive or unconfirmed results.
- Reports must include sanitized supporting evidence where applicable.
- Reports must not contain authentication tokens, passwords, API keys, encryption keys, or other unredacted sensitive values.
- Reports must clearly identify incomplete scans and partial results.
- Reports must not present execution failures as confirmed vulnerabilities.
- Report contents must be traceable to the originating scan.

A report must be generated from a consistent snapshot of the scan results. It must not combine findings or evidence from unrelated scans.

Report generation must only be allowed after a scan reaches a terminal status: `COMPLETED`, `FAILED`, or `CANCELLED`. Reports generated for failed or cancelled scans must clearly identify the incomplete execution state.

Error responses:

- `400 Bad Request` — Invalid report configuration.
- `403 Forbidden` — User does not have permission to generate reports.
- `404 Not Found` — Scan does not exist or is not accessible.
- `409 Conflict` — Report generation cannot be started because the scan is still running or another incompatible operation is in progress.
- `422 Unprocessable Entity` — Unsupported report format or invalid report configuration.

### 15.2 Get Report

**GET** `/reports/{reportId}`

Returns report metadata and generation status.

Successful response: `200 OK`.

```json
{
  "data": {
    "reportId": "report-uuid",
    "scanId": "scan-uuid",
    "format": "JSON",
    "status": "COMPLETED",
    "fileSizeBytes": 24576,
    "createdAt": "2026-11-15T10:40:00Z",
    "completedAt": "2026-11-15T10:40:05Z"
  }
}
```

Supported report statuses:

- `QUEUED`
- `GENERATING`
- `COMPLETED`
- `FAILED`

The backend must:

- Verify that the report exists.
- Identify its associated scan and project.
- Verify that the authenticated user is a member of that project.
- Return only metadata associated with the requested report.
- Return the actual generation status.
- Include the file size when report generation succeeds.
- Never expose internal storage paths, credentials, or sensitive infrastructure information.

If report generation fails, the backend must record an appropriate failure status without exposing sensitive internal exception details.

If the report does not exist or the authenticated user does not have access to its project, return `404 Not Found`.

### 15.3 Download Report

**GET** `/reports/{reportId}/download`

Downloads the generated report file.

The report must have a `COMPLETED` generation status before it can be downloaded.

Successful response: `200 OK`.

The response returns the generated file with the appropriate content type:

- JSON: `application/json`
- PDF: `application/pdf`
- HTML: `text/html`

The backend must:

- Verify that the report exists.
- Identify the report's originating project.
- Verify that the authenticated user is a member of that project.
- Verify that report generation completed successfully.
- Retrieve only the file associated with the requested report.
- Set an appropriate `Content-Type` header.
- Set a safe `Content-Disposition` header with a generated filename.
- Prevent path traversal and unauthorized access to report storage.
- Avoid exposing internal filesystem paths or storage identifiers.
- Preserve report contents after generation.
- Record report download events in the audit history.

The download endpoint must require authentication and project-level authorization. Reports must not be exposed through publicly accessible storage URLs.

Error responses:

- `403 Forbidden` — User does not have permission to download the report.
- `404 Not Found` — Report does not exist or is not accessible.
- `409 Conflict` — Report generation is not yet completed or has failed.

### 15.4 Report Retention and Integrity

The backend must:

- Associate every report with its originating scan and project.
- Preserve report files according to the project's configured data-retention rules.
- Prevent unauthorized modification of generated report files.
- Record report creation and download events.
- Preserve the relationship between report contents and their source scan results.
- Avoid deleting reports while their associated research or audit retention requirements remain active.
- Ensure that report generation and retrieval do not alter original scan evidence or findings.

## 16. Research Experiment APIs

These APIs manage controlled research experiments for evaluating the performance of different API security testing strategies.

Experiments compare the following scan generation modes:

- `RULE_BASED`
- `GRAPH_GUIDED`
- `HYBRID`

The purpose is to evaluate whether graph-guided test generation improves the detection of authorization and business logic vulnerabilities compared with conventional rule-based testing.

Each experiment must use a consistent target, API specification, test identities, authorization configuration, resource ownership model, security policies, workflows, and benchmark ground-truth dataset.

All experiment configurations, execution results, and measurements must be retained for reproducibility.

Project access requirements:

- Only project members with the `OWNER` or `EDITOR` role may create and execute experiments.
- All project members, including `VIEWER`, may access permitted experiment configurations and results.
- The backend must verify project membership before returning experiment information or measurements.
- Experiments and measurements must remain associated with their originating project.

### 16.1 Create Experiment

**POST** `/projects/{projectId}/experiments`

Creates a research experiment within the specified project.

Request:

```json
{
  "name": "Scan Mode Comparison",
  "description": "Comparison of rule-based, graph-guided and hybrid testing.",
  "targetId": "target-uuid",
  "specificationId": "specification-uuid",
  "graphSnapshotId": "graph-snapshot-uuid",
  "groundTruthSetId": "ground-truth-uuid",
  "experimentConfig": {
    "modes": [
      "RULE_BASED",
      "GRAPH_GUIDED",
      "HYBRID"
    ],
    "repetitions": 3,
    "maxTestsPerMode": 100,
    "maxHttpRequestsPerMode": 500,
    "maxDepth": 4,
    "requestTimeoutMs": 5000,
    "timeBudgetSeconds": 600,
    "randomSeed": 20261115,
    "allowStateChangingTests": false
  }
}
```

Successful response: `201 Created`.

```json
{
  "data": {
    "experimentId": "experiment-uuid",
    "projectId": "project-uuid",
    "targetId": "target-uuid",
    "specificationId": "specification-uuid",
    "graphSnapshotId": "graph-snapshot-uuid",
    "groundTruthSetId": "ground-truth-uuid",
    "status": "CREATED",
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Allow only project members with the `OWNER` or `EDITOR` role to create experiments.
- Verify that the project exists and is accessible.
- Verify that the target belongs to the specified project.
- Verify that the API specification belongs to the specified target and project.
- Verify that the specification has been parsed successfully.
- Verify that the graph snapshot belongs to the same project, target, and specification.
- Verify that the ground-truth dataset exists and is compatible with the selected target and specification.
- Require all three supported scan modes for the comparative experiment.
- Reject duplicate or unsupported scan modes.
- Validate the number of repetitions.
- Validate all configured test, HTTP request, graph depth, timeout, and execution-time limits.
- Ensure that the configured HTTP request budget does not exceed the target's configured request limit.
- Require a valid, reproducible random seed.
- Require explicit authorization and suitable target configuration for state-changing tests.
- Associate the experiment with the specified project and target.
- Preserve the original experiment configuration for future evaluation.
- Record experiment creation in the audit history.

Experiment configuration requirements:

- All three scan modes must use the same target and API specification.
- All modes must use the same configured test identities, roles, permissions, resources, ownership relationships, security policies, and workflows.
- All modes must use the same benchmark ground-truth dataset.
- All modes must use identical maximum test counts and HTTP request budgets.
- All modes must use identical graph depth, request timeouts, and execution-time limits.
- The graph snapshot used for the experiment must remain unchanged during execution.
- The random seed must be recorded to support reproducibility.
- Corresponding repetitions must use comparable deterministic seed values across all three modes.
- The experiment configuration must not change after execution has started.

An experiment must not be treated as executed merely because its configuration was created.

Error responses:

- `400 Bad Request` — Invalid experiment configuration.
- `403 Forbidden` — User does not have permission to create experiments.
- `404 Not Found` — Project, target, specification, graph snapshot, or ground-truth dataset does not exist or is not accessible.
- `409 Conflict` — An equivalent experiment configuration already exists.
- `422 Unprocessable Entity` — Experiment configuration violates the comparative evaluation requirements.

### 16.2 List Experiments

**GET** `/projects/{projectId}/experiments`

Returns research experiments configured for the specified project.

Optional query parameters:

- `status`
- `targetId`
- `page`
- `size`
- `sort`

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each experiment entry should include:

- `experimentId`
- `name`
- `targetId`
- `status`
- `createdAt`
- `updatedAt`

The backend must:

- Verify that the authenticated user is a member of the specified project.
- Return only experiments belonging to that project.
- Validate that supplied target filters belong to the specified project.
- Apply project-level authorization filtering before pagination and sorting.
- Never expose experiments belonging to another project.

If the project does not exist or the authenticated user does not have access, return `404 Not Found`.

### 16.3 Get Experiment

**GET** `/experiments/{experimentId}`

Returns experiment metadata, configuration, and current status.

Successful response: `200 OK`.

```json
{
  "data": {
    "experimentId": "experiment-uuid",
    "projectId": "project-uuid",
    "name": "Scan Mode Comparison",
    "targetId": "target-uuid",
    "specificationId": "specification-uuid",
    "graphSnapshotId": "graph-snapshot-uuid",
    "groundTruthSetId": "ground-truth-uuid",
    "status": "COMPLETED",
    "experimentConfig": {
      "modes": [
        "RULE_BASED",
        "GRAPH_GUIDED",
        "HYBRID"
      ],
      "repetitions": 3,
      "maxTestsPerMode": 100,
      "maxHttpRequestsPerMode": 500,
      "maxDepth": 4,
      "requestTimeoutMs": 5000,
      "timeBudgetSeconds": 600,
      "randomSeed": 20261115,
      "allowStateChangingTests": false
    },
    "createdAt": "2026-11-15T10:30:00Z"
  }
}
```

The backend must:

- Verify that the experiment exists.
- Identify its associated project.
- Verify that the authenticated user is a member of that project.
- Return the stored experiment configuration.
- Return the actual execution status.
- Preserve the relationship between the experiment, target, specification, graph snapshot, and ground-truth dataset.
- Never expose information belonging to another project.

If the experiment does not exist or the authenticated user does not have access, return `404 Not Found`.

### 16.4 Start Experiment Run

**POST** `/experiments/{experimentId}/runs`

Starts asynchronous execution of the configured research experiment.

Successful response: `202 Accepted`.

```json
{
  "data": {
    "runId": "experiment-run-uuid",
    "experimentId": "experiment-uuid",
    "status": "QUEUED",
    "createdAt": "2026-11-15T11:00:00Z"
  }
}
```

The backend must:

- Verify that the experiment exists.
- Verify that the authenticated user has `OWNER` or `EDITOR` permissions in the experiment's project.
- Verify that the experiment configuration is valid and complete.
- Verify that the target authorization confirmation is still valid.
- Verify that the selected target, specification, graph snapshot, and ground-truth dataset remain available.
- Create a unique experiment run identifier.
- Execute the configured scan modes using the stored experiment configuration.
- Schedule each configured repetition for all three scan modes.
- Preserve the relationship between the experiment run and its individual scan executions.
- Record execution start and completion timestamps.
- Record the scanner version and relevant benchmark configuration used during execution.
- Generate research measurements from actual scan and benchmark results.
- Preserve all generated tests, executions, findings, and evidence.

Comparative execution requirements:

- All three scan modes must use identical test budgets and HTTP request limits.
- All three modes must use identical request timeout and execution-time limits.
- All modes must operate against equivalent initial benchmark states.
- The target's data state must be restored to the documented benchmark initial state between corresponding runs using an authorized test-fixture mechanism.
- Target state preparation must not involve direct database inspection or bypass the HTTP boundary between API Sentinel and Sentinel Bank.
- Corresponding repetitions must use the same random seed across the three modes.
- Execution order must be controlled and recorded to reduce order-related bias.
- Runs must not exceed the configured request or execution budgets.
- Each mode must retain its own generated tests, execution history, findings, and evidence.
- Target authorization must be checked before each execution.
- If authorization is revoked, the experiment must stop further target requests safely.

Experiment run statuses:

- `QUEUED`
- `RUNNING`
- `COMPLETED`
- `FAILED`
- `CANCELLED`

An experiment run must be marked `COMPLETED` only after all configured scan modes and repetitions have finished successfully.

If an experiment run is interrupted or partially completed, its status and available results must accurately reflect that condition.

Error responses:

- `403 Forbidden` — User does not have permission to execute experiments.
- `404 Not Found` — Experiment does not exist or is not accessible.
- `409 Conflict` — Experiment is already running or cannot be executed in its current state.
- `422 Unprocessable Entity` — Experiment configuration or target authorization is invalid.

### 16.5 List Experiment Runs

**GET** `/experiments/{experimentId}/runs`

Returns execution runs associated with the specified experiment.

Optional query parameters:

- `status`
- `page`
- `size`
- `sort`

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each run entry should include:

- `runId`
- `experimentId`
- `status`
- `startedAt`
- `completedAt`
- `createdAt`

The backend must:

- Verify that the experiment exists.
- Verify that the authenticated user is a member of its project.
- Return only runs belonging to the requested experiment.
- Apply project-level authorization filtering before pagination and sorting.
- Preserve the relationship between each run and its originating experiment.

If the experiment does not exist or the user does not have access, return `404 Not Found`.

### 16.6 Get Experiment Run

**GET** `/experiments/{experimentId}/runs/{runId}`

Returns execution status and summary information for the specified experiment run.

Successful response: `200 OK`.

```json
{
  "data": {
    "runId": "experiment-run-uuid",
    "experimentId": "experiment-uuid",
    "status": "COMPLETED",
    "modeResults": [
      {
        "mode": "RULE_BASED",
        "completedRepetitions": 3,
        "status": "COMPLETED"
      },
      {
        "mode": "GRAPH_GUIDED",
        "completedRepetitions": 3,
        "status": "COMPLETED"
      },
      {
        "mode": "HYBRID",
        "completedRepetitions": 3,
        "status": "COMPLETED"
      }
    ],
    "startedAt": "2026-11-15T11:00:00Z",
    "completedAt": "2026-11-15T11:20:00Z"
  }
}
```

The backend must:

- Verify that the experiment and run exist.
- Verify that the run belongs to the specified experiment.
- Verify that the authenticated user is a member of the experiment's project.
- Return actual execution status for each scan mode and repetition.
- Preserve the execution history and associated scan references.
- Never combine results from different experiments or projects.

If the experiment or run does not exist, or the user does not have access, return `404 Not Found`.

### 16.7 Get Experiment Measurements

**GET** `/experiments/{experimentId}/measurements`

Returns research metrics calculated from completed experiment executions and the associated benchmark ground truth.

Optional query parameters:

- `runId` — Filter measurements by experiment run.
- `mode` — Filter by scan generation mode.
- `page`
- `size`
- `sort`

Successful response: `200 OK`.

The response must follow the standard pagination envelope defined in Section 3.

Each measurement record should include:

- `measurementId`
- `experimentId`
- `runId`
- `mode`
- `repetition`
- `truePositives`
- `falsePositives`
- `falseNegatives`
- `trueNegatives`
- `precision`
- `recall`
- `f1Score`
- `falsePositiveRate`
- `detectionRate`
- `endpointCoverage`
- `executionTimeMs`
- `generatedTestCount`
- `executedTestCount`
- `httpRequestCount`
- `createdAt`

The backend must:

- Verify that the experiment exists.
- Verify that the authenticated user is a member of the experiment's project.
- Return only measurements associated with the specified experiment.
- Validate that any supplied run ID belongs to the specified experiment.
- Validate that the selected mode is supported.
- Derive all measurements from actual scan executions and the configured ground-truth dataset.
- Preserve measurement traceability to the experiment, run, mode, and repetition.
- Prevent manually supplied or fabricated measurement values.
- Return null for metrics whose denominators are zero rather than silently reporting them as zero.

Research measurement definitions:

**Classification unit**

The classification unit must be a unique ground-truth vulnerability scenario, not an individual HTTP request, test execution, or duplicate finding.

Repeated findings for the same vulnerability scenario must be deduplicated when calculating classification metrics.

- True Positive (TP): A vulnerable ground-truth scenario correctly identified by the scanner.
- False Positive (FP): A non-vulnerable scenario incorrectly reported as a vulnerability, or a finding that cannot be matched to a valid vulnerable ground-truth scenario.
- False Negative (FN): A vulnerable ground-truth scenario that the scanner failed to identify.
- True Negative (TN): A non-vulnerable ground-truth scenario that the scanner correctly did not report as vulnerable.

**Evaluation metrics**

Precision:

`TP / (TP + FP)`

Recall:

`TP / (TP + FN)`

F1 score:

`2 × (Precision × Recall) / (Precision + Recall)`

False Positive Rate:

`FP / (FP + TN)`

Vulnerability Detection Rate:

`Correctly detected vulnerable scenarios / Total vulnerable ground-truth scenarios`

The aggregate vulnerability detection rate is equivalent to recall. Category-specific detection rates may also be reported separately.

Endpoint Coverage:

`Unique eligible endpoints exercised / Total eligible endpoints`

Execution Time:

Elapsed time between the start and completion of the corresponding scan execution, measured in milliseconds.

Measurement requirements:

- Measurements must be calculated separately for each scan mode and repetition.
- Aggregate results across repetitions must retain the individual repetition measurements.
- Where aggregate statistics are reported, the backend should provide the mean and standard deviation across completed repetitions.
- Incomplete or failed executions must not be represented as fully completed experimental measurements.
- Measurements must be reproducible from the stored experiment configuration, execution records, and ground-truth dataset.
- The ground-truth dataset version must be preserved with the experiment results.
- The measurement calculation method and version must be recorded.

If no completed measurements exist, the endpoint must return an empty collection rather than fabricated research metrics.

### 16.8 Experiment Integrity and Reproducibility

The backend must enforce the following requirements:

- Experiment configurations must be preserved after execution begins.
- Each experiment run must retain its original configuration and ground-truth version.
- Each scan mode must retain its own generated tests and execution records.
- Test and request budgets must be enforced independently for each mode.
- The same benchmark initial state must be used for comparable repetitions.
- Random seeds and execution order must be recorded.
- Measurements must be derived from actual execution and benchmark data.
- Duplicate vulnerability findings must not inflate detection metrics.
- Findings must remain traceable to their test cases, executions, and evidence.
- The scanner must not use a large language model to decide whether a vulnerability exists. Vulnerability detection and metric calculation must be performed by implemented rules, graph algorithms, and evidence-validation logic.
- Research measurements must not be manually altered after calculation.
- Incomplete experiments must retain their partial execution history without being presented as completed results.

## 17. Common HTTP Status Codes

The API must use consistent HTTP status codes across all endpoints.

| HTTP status | Meaning | Usage |
|---|---|---|
| 200 | OK | Request completed successfully. |
| 201 | Created | A new resource was created successfully. |
| 202 | Accepted | An asynchronous operation was accepted for processing. |
| 204 | No Content | Request completed successfully without a response body. |
| 400 | Bad Request | Request structure or parameters are invalid. |
| 401 | Unauthorized | Authentication is missing, invalid, or expired. |
| 403 | Forbidden | Authenticated user lacks the required permission. |
| 404 | Not Found | Resource does not exist or is not accessible to the authenticated user. |
| 409 | Conflict | Request conflicts with existing data or the resource's current state. |
| 413 | Payload Too Large | Request or uploaded file exceeds the configured size limit. |
| 415 | Unsupported Media Type | Request content type or uploaded file format is unsupported. |
| 422 | Unprocessable Entity | Request is structurally valid but violates business or validation rules. |
| 429 | Too Many Requests | Configured rate or request limits have been exceeded. |
| 500 | Internal Server Error | An unexpected server-side error occurred. |
| 503 | Service Unavailable | Required service is temporarily unavailable. |

Additional response rules:

- `401 Unauthorized` must be returned when authentication is missing or invalid.
- `403 Forbidden` must be returned when an authenticated project member attempts an operation that their project role does not permit.
- `404 Not Found` must be returned when a resource does not exist or the authenticated user does not have access to its project.
- `409 Conflict` must be used for duplicate records, invalid state transitions, and incompatible resource states.
- `422 Unprocessable Entity` must be used for semantically invalid configurations and unsupported business rules.
- `500 Internal Server Error` responses must not expose stack traces, SQL statements, credentials, or internal infrastructure details.
- `202 Accepted` must not be interpreted as confirmation that an asynchronous operation has completed.

All endpoints must use the common response envelopes defined in Section 3.

## 18. Validation and Error Handling

The backend must perform input validation, authentication, authorization, and business-rule validation before modifying persistent data or executing target requests.

### 18.1 Request Validation

The backend must:

1. Validate required request fields.
2. Validate UUID formats for path parameters and request fields.
3. Validate supported enum values.
4. Enforce string length limits.
5. Enforce numeric minimums and maximums.
6. Validate URL and network-scope restrictions.
7. Validate supported HTTP methods and endpoint references.
8. Validate uploaded file sizes and formats.
9. Validate JSON structures and nested request objects.
10. Reject unknown or prohibited fields where appropriate.
11. Enforce project ownership and resource relationships.
12. Enforce database foreign-key and uniqueness constraints.
13. Reject malformed or unsupported request content.
14. Validate that referenced resources belong to the correct project, target, and parent resource.

### 18.2 Authentication and Authorization

The backend must:

- Authenticate protected API requests.
- Verify the authenticated user's project membership.
- Enforce the project role required for each operation.
- Validate resource ownership and project associations.
- Prevent users from modifying protected fields through unrelated endpoints.
- Reject unauthorized cross-project references.
- Apply authorization checks before accessing or modifying project data.
- Never treat knowledge of a UUID as proof of authorization.

### 18.3 Error Response Structure

All API errors must follow the common error response envelope defined in Section 3.

Validation errors should identify the affected field whenever possible.

Each error response should include:

- A stable machine-readable error code.
- A human-readable error message.
- Field-level validation details, when applicable.
- A trace identifier for troubleshooting.

Error messages must not expose:

- Stack traces.
- SQL statements.
- Database credentials.
- Authentication tokens.
- Encryption keys.
- Internal filesystem paths.
- Sensitive infrastructure configuration.
- Unredacted request or response data.

### 18.4 Transaction Handling

Operations involving multiple related database changes must use database transactions.

Examples include:

- Creating a project and its OWNER membership.
- Creating a resource and its ownership relationship.
- Creating workflow states and transitions where multiple records must be updated together.
- Creating experiment records and their associated configuration.
- Recording finding-to-evidence relationships.

If a required operation fails, the transaction must be rolled back so that partially completed data is not persisted.

### 18.5 Logging and Traceability

The backend must:

- Generate or propagate a trace identifier for API requests.
- Record relevant authentication and authorization failures.
- Record important project configuration changes.
- Record target authorization confirmation and revocation events.
- Record scan and experiment lifecycle events.
- Avoid logging credentials, tokens, passwords, encryption keys, and unredacted sensitive payloads.
- Associate operational logs with trace identifiers without exposing confidential information.

## 19. Pagination and Sorting

Collection endpoints must support consistent pagination and sorting.

### 19.1 Pagination Parameters

Supported query parameters:

- `page` — Zero-based page number.
- `size` — Number of records per page.
- `sort` — Permitted field and sort direction.

Example:

`GET /api/v1/projects?page=0&size=20&sort=createdAt,desc`

Default values:

- `page`: `0`
- `size`: `20`

The maximum page size must be enforced by server-side configuration. The initial default maximum is `100` records per page.

### 19.2 Sorting Rules

Sorting must:

- Support only explicitly permitted fields for each endpoint.
- Support ascending (`asc`) and descending (`desc`) directions.
- Reject unsupported sort fields.
- Reject invalid sort directions.
- Use a stable secondary sort field, such as the resource ID, when necessary to maintain consistent ordering.
- Apply project-level authorization filtering before sorting and pagination.

### 19.3 Pagination Validation

The backend must:

- Reject negative page numbers.
- Reject zero or negative page sizes.
- Reject page sizes exceeding the configured maximum.
- Reject malformed sorting parameters.
- Avoid returning duplicate records across pages when the underlying dataset has not changed.
- Return pagination metadata using the common response envelope defined in Section 3.

Example pagination metadata:

```json
{
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

### 19.4 Applicable Endpoints

Pagination and sorting must be applied consistently to collection endpoints, including:

- Projects.
- Project members.
- Targets.
- API specifications.
- API endpoints.
- Test identities.
- Roles.
- Permissions.
- Resources.
- Ownership relationships.
- Security policies.
- Invariants.
- Workflows.
- Graph snapshots.
- Scan tests.
- Scan findings.
- Test executions.
- Finding evidence.
- Research experiments.
- Experiment runs.
- Research measurements.

## 20. Scan Safety Requirements

API Sentinel must execute security tests only against explicitly authorized and configured targets.

The scanner must enforce safety restrictions during test generation and HTTP execution.

### 20.1 Target Authorization

The scanner must:

1. Require recorded authorization confirmation before starting a scan.
2. Verify that the target belongs to the selected project.
3. Verify that the target authorization status is still valid before executing requests.
4. Recheck target authorization during execution.
5. Stop scheduling new requests when authorization is revoked.
6. Safely terminate active scan operations when authorization is revoked.
7. Record authorization-related scan interruptions.

### 20.2 Network and Target Restrictions

The scanner must:

- Validate target URLs before execution.
- Accept only configured HTTP and HTTPS target schemes.
- Enforce explicit target allowlists and approved network scopes.
- Prevent access to prohibited internal services and cloud metadata endpoints.
- Resolve and validate destination IP addresses before connecting.
- Protect against DNS rebinding.
- Validate redirect destinations before following redirects.
- Prevent redirects to unauthorized or prohibited destinations.
- Avoid using user-supplied URLs outside the registered target's approved scope.
- Disable automatic redirects unless each destination is independently validated.
- Revalidate destination restrictions during scan execution.

Loopback and local development targets may be permitted through explicit development-environment configuration. Such exceptions must not automatically apply to production environments.

### 20.3 Request and Execution Limits

The scanner must enforce:

- Maximum HTTP requests per scan.
- Maximum concurrent requests.
- Per-request timeouts.
- Maximum graph exploration depth.
- Maximum generated test count.
- Maximum total scan execution time.
- Configurable request pacing and rate limits.
- Target-specific request budgets.

All outbound HTTP requests must count toward the configured request budget, including retries and validated redirects.

The scanner must not exceed configured limits because of automatic retries, concurrency, or redirect handling.

### 20.4 State-Changing Test Restrictions

State-changing tests must:

- Be disabled by default.
- Require explicit recorded authorization for the target.
- Require explicit enablement in the scan configuration.
- Be restricted to supported and configured test operations.
- Use synthetic benchmark data where possible.
- Avoid irreversible or destructive actions.
- Preserve a record of the requests and resulting observations.
- Stop when target authorization is revoked.

The request field `allowStateChangingTests` must not be treated as independent proof of authorization.

### 20.5 Evidence and Credential Protection

The scanner must:

- Retrieve credentials only through the authorized credential-handling mechanism.
- Avoid storing plaintext credentials in logs or temporary files.
- Redact authentication tokens and other secrets from HTTP evidence.
- Prevent sensitive data from appearing in findings and generated reports.
- Preserve sanitized evidence required for validation and reproducibility.

### 20.6 Scan Cancellation

Cancellation must:

- Stop scheduling additional tests.
- Signal running executions to terminate safely.
- Prevent additional target requests after cancellation is processed.
- Preserve completed test executions, findings, and evidence.
- Record the final cancellation status.

A cancelled scan must not be reported as successfully completed.

### 20.7 API Boundary and Benchmark Isolation

API Sentinel and Sentinel Bank must remain separate applications.

API Sentinel must:

- Communicate with registered targets through HTTP.
- Never access the target application's database directly.
- Never bypass target authentication or authorization logic.
- Use only explicitly configured benchmark fixture mechanisms.
- Avoid using privileged access to obtain vulnerability ground truth during normal scanning.

Benchmark ground truth must remain separate from the scanner's vulnerability detection logic.

## 21. Project Isolation Requirements

Project isolation is a fundamental security requirement for API Sentinel.

All project-owned records and operations must enforce consistent project boundaries.

### 21.1 Project-Level Access

The backend must:

- Allow users to access only projects for which they have membership.
- Enforce project roles for every protected operation.
- Verify membership before returning project-associated information.
- Prevent access to another project's data through manipulated UUIDs or identifiers.
- Apply project-level filtering to collection endpoints before pagination and sorting.

### 21.2 Resource Ownership

The following records must remain associated with the correct project:

- Project members.
- Targets.
- API specifications.
- Normalized endpoints.
- Test identities.
- Credentials.
- Roles.
- Permissions.
- Identity-role assignments.
- Role-permission assignments.
- Resources.
- Ownership relationships.
- Security policies.
- Security invariants.
- Workflows.
- Workflow states.
- Workflow transitions.
- Graph snapshots.
- Scan records.
- Test cases.
- Test executions.
- Findings.
- Evidence records.
- Reports.
- Research experiments.
- Experiment runs.
- Research measurements.

### 21.3 Cross-Project Relationship Restrictions

The backend and database must prevent:

- Assigning an identity from one project to a role belonging to another project.
- Assigning a role to a permission belonging to another project.
- Associating a resource with an identity from another project.
- Associating an endpoint with a policy belonging to another project.
- Associating workflow states with transitions from another workflow.
- Associating graph nodes or edges with different snapshots.
- Associating a scan with a target or specification belonging to another project.
- Associating findings with test executions from unrelated scans.
- Associating evidence with findings or executions from unrelated projects.
- Associating experiment measurements with unrelated experiment runs.

### 21.4 Database Enforcement

Project isolation must be enforced at both the application and database levels.

The database design must use:

- Appropriate foreign-key constraints.
- Composite foreign keys where project consistency must be enforced.
- Unique constraints for project-scoped identifiers.
- Appropriate indexes for project-scoped lookups.
- Transactional operations for related records.

Application-level validation alone must not be relied upon to enforce cross-project data integrity.

### 21.5 Query and Response Isolation

The backend must:

- Scope database queries to the authorized project.
- Verify relationships between parent and child resources.
- Avoid returning records from other projects through joins or nested response objects.
- Apply authorization checks before returning associated records.
- Avoid exposing whether another project's protected resources exist.
- Preserve project boundaries in reports, exports, logs, and evidence retrieval.

### 21.6 Deletion and Retention

Project deletion and resource removal must respect data-retention requirements.

The backend must:

- Prevent deletion from destroying retained findings or research evidence.
- Preserve required scan and experiment history.
- Enforce configured retention rules.
- Record relevant deletion and retention events.
- Prevent cascading deletion of records that must remain available for audit or research reproducibility.

## 22. API Implementation Status

This document defines the intended API contract for API Sentinel.

The API contract is a design specification. An endpoint must not be marked implemented or complete until its application code, validation, and required tests have been created and verified.

### 22.1 API Design Status

**API endpoint definitions:** Documented.

**Request and response structures:** Documented.

**Authentication and authorization requirements:** Documented.

**Project isolation requirements:** Documented.

**Scan execution and safety requirements:** Documented.

**Research experiment requirements:** Documented.

### 22.2 Backend Implementation Status

**Spring Boot application:** Not implemented yet.

**REST controllers:** Not implemented yet.

**Service layer:** Not implemented yet.

**Request and response DTOs:** Not implemented yet.

**Bean validation:** Not implemented yet.

**Authentication and authorization:** Not implemented yet.

**Credential encryption and management:** Not implemented yet.

**Database repositories and persistence:** Not implemented yet.

**Exception handling:** Not implemented yet.

**Audit logging:** Not implemented yet.

**OpenAPI specification parsing:** Not implemented yet.

**Graph construction and snapshot management:** Not implemented yet.

**Scan execution engine:** Not implemented yet.

**Finding validation and evidence handling:** Not implemented yet.

**Report generation:** Not implemented yet.

**Research experiment execution and metric calculation:** Not implemented yet.

### 22.3 Testing Status

**Unit tests:** Not implemented yet.

**Controller and API integration tests:** Not implemented yet.

**Authentication and authorization tests:** Not implemented yet.

**Project isolation tests:** Not implemented yet.

**Database constraint tests:** Not implemented yet.

**Scan safety tests:** Not implemented yet.

**End-to-end Sentinel Bank tests:** Not implemented yet.

**Research benchmark evaluation tests:** Not implemented yet.

### 22.4 Completion Criteria

An API endpoint may be marked implemented only after:

1. Its controller and service logic are implemented.
2. Request and response validation are implemented.
3. Authentication and authorization requirements are enforced.
4. Database relationships and constraints are verified.
5. Successful and failure response cases are tested.
6. Project isolation is tested where applicable.
7. Security and data-handling requirements are verified.
8. The endpoint is documented consistently with its actual behavior.

Implementation status must reflect verified application behavior rather than intended design.