## ADDED Requirements

### Requirement: Unified sensitive data sanitizer
DataSpec SHALL provide a shared sanitizer for common technical secrets in backend and CLI-visible outputs.

#### Scenario: Redact common secret text
- **WHEN** text contains a password assignment, API token assignment, Authorization Bearer value, standalone Bearer value, full JDBC URL, or connection string
- **THEN** DataSpec SHALL replace the secret value with a stable redaction marker
- **AND** the original secret value SHALL NOT appear in the sanitized result.

#### Scenario: Sanitize nested payloads
- **WHEN** DataSpec summarizes Map/List/JSON/POJO payloads for evidence, diagnostics, backup checks, or CLI delivery packages
- **THEN** fields with sensitive key names such as `password`, `token`, `plainToken`, `tokenHash`, `apiKey`, `authorization`, `jdbcUrl`, or `connectionString` SHALL be redacted
- **AND** nested string values SHALL also be scanned for known secret patterns.

### Requirement: Safe export and diagnostic boundaries
DataSpec SHALL apply the sanitizer to user-copyable or AI-consumable outputs that may contain arbitrary payload text.

#### Scenario: Evidence package output
- **WHEN** an AI evidence package is generated from SQL checks, AI jobs, AI batches, or coverage payloads
- **THEN** inputs, outputs, validation summaries, artifacts, next actions, suggested commands, and diagnostics SHALL be sanitized before returning JSON or zip content.

#### Scenario: Database reverse import diagnostics
- **WHEN** database connection tests, metadata reads, or readonly diagnostics fail with vendor error messages
- **THEN** returned messages and warnings SHALL NOT expose database passwords, Bearer tokens, full JDBC URLs, or connection strings.

#### Scenario: Project backup safety scan
- **WHEN** a project backup package or restore request is inspected for unsafe content
- **THEN** DataSpec SHALL detect known sensitive key names and full JDBC URL values before accepting or exporting the package.

### Requirement: Documentation and tests
DataSpec SHALL document and test the sanitizer boundary.

#### Scenario: Sanitizer regression fixtures
- **WHEN** backend tests or CLI tests run
- **THEN** they SHALL include fixtures containing password, token, Authorization, Bearer, JDBC URL, and nested payload examples
- **AND** assertions SHALL verify that raw secrets are absent from sanitized outputs.

#### Scenario: Document allowed persistence boundary
- **WHEN** users read DataSpec documentation
- **THEN** it SHALL state which connection/config fields may be stored and which secret-bearing fields are never exported or logged by first-version sanitization.
