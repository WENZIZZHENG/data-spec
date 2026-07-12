# sensitive-data-sanitization Specification

## Purpose
定义 DataSpec 在错误、诊断、AI 证据包、备份扫描和 CLI 本地交付包等可复制出口中的敏感信息脱敏边界，避免 password、token、Authorization、Bearer、完整 JDBC URL 和连接串被返回、导出或写入面向 AI 的上下文。
## Requirements
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

### Requirement: AI Context export uses shared sanitizer
DataSpec SHALL apply the shared sensitive data sanitizer to AI-consumable context outputs that may contain arbitrary business or user text.

#### Scenario: Field metadata is sanitized for AI Context
- **WHEN** field comments, aliases, default values, example values, format notes, usage contracts, or replacement guidance contain known technical secrets
- **THEN** AI Context field catalog and prompt exports SHALL contain only sanitized values
- **AND** the raw secret SHALL NOT appear in generated JSON, Markdown, YAML, prompt text, or zip package entries.

#### Scenario: Prompt inputs are sanitized
- **WHEN** create-table or fix-sql prompt generation receives business descriptions or SQL containing known technical secrets
- **THEN** the returned prompt SHALL redact those secrets
- **AND** the prompt SHALL still preserve enough non-sensitive text for local review.
