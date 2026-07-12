# standard-test-data-package Specification

## Purpose
定义基于字段标准、枚举、格式约束和语义元数据生成确定性测试数据包的契约。
## Requirements
### Requirement: Standard test data package contract
DataSpec SHALL generate a deterministic, project-scoped standard test data package from field standards, enum values, format constraints, semantic metadata, and lightweight object hints.

#### Scenario: Generate package for selected fields
- **WHEN** a caller requests a test data package with `projectId`, optional field selectors, optional object scenario, and bounded generation parameters
- **THEN** the response includes stable fields `kind`, `schemaVersion`, `projectId`, `specHash`, `generationParams`, `sourceSummary`, `testDataCases`, `seedProfiles`, `mockPayloads`, `coverageReport`, `diagnostics`, `safety`, and `nextActions`
- **AND** each generated case includes deterministic `caseId`, `fieldName`, `caseType`, `value`, `expectedValidity`, `reason`, `sourceRefs`, and `requiresBusinessReview`.

#### Scenario: Spec hash is deterministic
- **WHEN** the same project standard summary, selectors, schema version, and generation parameters are used twice
- **THEN** the generated package uses the same `specHash`
- **AND** when selected fields, enum values, format constraints, semantic metadata, object hints, schema version, or generation parameters change
- **THEN** the `specHash` changes.

### Requirement: Valid invalid and boundary cases
The standard test data package SHALL include safe valid, invalid, and boundary cases for supported field categories.

#### Scenario: Generate supported field cases
- **WHEN** selected fields include phone, amount, datetime, enum, JSON, identifier, boolean, text, or sensitive markers
- **THEN** DataSpec generates valid, invalid, and boundary examples where deterministic rules are available
- **AND** unsupported or ambiguous fields are reported in `coverageReport.missingConstraints` instead of receiving fabricated business rules.

#### Scenario: Generate mock and seed drafts
- **WHEN** enough field metadata is available for a lightweight object payload
- **THEN** the package includes JSON mock payloads, CSV rows, and SQL seed draft text derived from the same test data cases
- **AND** executable status, dialect notes, and review requirements are explicit in each seed profile.

### Requirement: Test data generation safety
Standard test data generation SHALL be read-only, bounded, and safe to share with AI or commit as fixtures.

#### Scenario: Generation has no write side effects
- **WHEN** a caller generates a test data package through API, CLI, or MCP
- **THEN** DataSpec does not create, update, delete, import, export, persist project records, write business files, connect to source databases, or call external LLMs
- **AND** the `safety` object declares `readOnly=true`, `writesProject=false`, `writesBusinessRepo=false`, `containsRealBusinessRows=false`, `externalNetworkUsed=false`, and `externalLlmUsed=false`.

#### Scenario: Sensitive metadata is redacted
- **WHEN** field metadata, generation parameters, diagnostics, examples, seed drafts, or next actions contain token, password, Authorization header, API key, complete JDBC URL, DSN, connection string, or private key patterns
- **THEN** the generated package does not expose the raw sensitive value
- **AND** diagnostics identify that redaction occurred without revealing the secret.

#### Scenario: Bounds are enforced
- **WHEN** field count, case count, payload size, selector count, text length, or seed row count exceeds the documented bounds
- **THEN** DataSpec rejects or truncates the request with a structured diagnostic that names the bound and safe retry action.
