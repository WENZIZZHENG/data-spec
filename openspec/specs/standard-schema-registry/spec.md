# standard-schema-registry Specification

## Purpose
定义 AI 可消费标准契约的 Schema Registry，集中描述字段、枚举、规则、模板、快照、lint 结果、AI Context 和任务画像的版本、稳定字段与兼容策略。
## Requirements
### Requirement: Schema registry catalog
DataSpec SHALL expose a machine-readable schema registry catalog for AI-consumed standard contracts.

#### Scenario: List registry catalog
- **WHEN** a client requests the schema registry catalog
- **THEN** the response includes `kind`, `schemaVersion`, `registryVersion`, `compatibilityPolicy`, and `contracts[]`
- **AND** every contract summary includes `contractId`, `displayName`, `schemaVersion`, `stableFields`, `deprecatedFields`, `jsonSchemaRef`, and `docsRef`.

#### Scenario: Registry contains core contracts
- **WHEN** the schema registry catalog is generated
- **THEN** it contains contracts for Field, Enum, Rule, Template, StandardSnapshot, LintResult, AI Context manifest, AI Context field catalog, and AI task profile.

### Requirement: Contract detail
DataSpec SHALL expose a detail view for each registered standard contract.

#### Scenario: Show contract detail
- **WHEN** a client requests a known contract id
- **THEN** the response includes the contract metadata, JSON Schema, stable field paths, deprecated field metadata, compatibility window, and examples when available.

#### Scenario: Unknown contract
- **WHEN** a client requests an unknown contract id
- **THEN** DataSpec returns a readable failure that includes supported contract ids.

### Requirement: Compatibility policy
The registry SHALL define a compatibility policy for AI-consumed contract changes.

#### Scenario: Compatible additive field
- **WHEN** a contract gains an optional field without changing documented stable semantics
- **THEN** the registry marks this as a compatible change.

#### Scenario: Breaking contract change
- **WHEN** a stable field is removed, renamed, changes type, or changes documented semantics
- **THEN** the registry policy requires a schemaVersion update, fixture update, and migration note.

#### Scenario: Deprecated field
- **WHEN** a field is deprecated but still present
- **THEN** the contract lists the field path, replacement hint, deprecatedSince, removalAfter, and reason.

### Requirement: Registry describes stable references and post-check results
The standard schema registry SHALL describe the stable-reference and AI output post-check contracts.

#### Scenario: Registry catalog includes new contracts
- **WHEN** the registry catalog is generated
- **THEN** it SHALL include contract summaries for standard reference resolution and AI output post-check results.

#### Scenario: Contract detail describes stable fields
- **WHEN** a caller requests either new contract detail
- **THEN** the JSON Schema SHALL describe required fields, enums, array item semantics, compatibility policy, secret-safety constraints, and examples
- **AND** additive stableRef fields on existing contracts SHALL be documented.

### Requirement: Registry describes Standard Query DSL contracts
The standard schema registry SHALL describe Standard Query DSL request, result, filter, summary, and validation error contracts.

#### Scenario: Registry catalog includes DSL contracts
- **WHEN** the registry catalog is generated
- **THEN** it includes contract summaries for Standard Query DSL request and result objects.

#### Scenario: Contract detail describes DSL fields
- **WHEN** a caller requests Standard Query DSL contract detail
- **THEN** the JSON Schema describes target, text, filters, operators, limit, strict, explain, normalized query, applied filters, ignored filters, counts, hints, supported fields, bounds, and secret-safety constraints.

### Requirement: Registry contains table standard contracts
The schema registry SHALL register the contracts used by business object and table structure standards.

#### Scenario: Catalog lists table standard contracts
- **WHEN** the schema registry catalog is generated
- **THEN** it contains contract summaries for BusinessObjectStandard, TableStructureStandard, TableRelationHint, TableIndexStandard, TableForeignKeyStandard, TablePolicyStandard, and AI Context table standards
- **AND** every summary includes schema version, stable fields, JSON Schema reference, docs reference, and compatibility policy.

#### Scenario: Contract detail describes stable fields
- **WHEN** a client requests one of the table standard contract details
- **THEN** the response describes each stable field, nested object, array shape, nullability, allowed enum values, and redaction boundary
- **AND** unknown contract ids continue to return the existing readable failure with supported ids.

### Requirement: Registry compatibility for additive table standards
The schema registry SHALL mark table standard additions as compatible when existing stable fields are preserved.

#### Scenario: Add optional table-standard fields
- **WHEN** business object, table structure, DDL preview, or AI Context contracts gain optional table-standard fields
- **THEN** the registry compatibility policy marks the change as additive
- **AND** it documents that removing or renaming stable table-standard fields requires a schema version bump and migration note.

### Requirement: Registry includes field semantics contracts
The schema registry SHALL describe the AI-consumed contracts introduced for field semantics and knowledge cards.

#### Scenario: Registry lists new semantic contracts
- **WHEN** a client lists the schema registry catalog
- **THEN** it includes contracts for FieldSemanticRule, FieldKnowledgeCard, EnumValueLifecycle, MetricDefinitionMapping, and AI Context field semantics artifacts
- **AND** each contract includes schemaVersion, stable fields, docsRef, jsonSchemaRef, compatibility policy, and examples when available.

#### Scenario: Contract detail describes secret-safe fields
- **WHEN** a client shows a semantic contract detail
- **THEN** text fields that can contain user-maintained guidance include descriptions requiring secret-safe content and no sampled business rows.

#### Scenario: Additive compatibility is explicit
- **WHEN** existing Field, Enum, AI Context field catalog, or data dictionary contracts gain optional semantic fields
- **THEN** the registry marks the additions as compatible and documents stable field paths for AI clients.

### Requirement: Registry covers test data and compatibility contracts
Schema Registry SHALL register the standard test data package and consumer compatibility suite contracts for AI and local validation consumers.

#### Scenario: Catalog lists new contracts
- **WHEN** a client requests the schema registry catalog
- **THEN** the catalog includes contract summaries for `standard-test-data-package`, `consumer-compatibility-suite`, `consumer-compatibility-adapter-result`, and `consumer-compatibility-breaking-rule`
- **AND** each summary includes schema version, stable fields, deprecated fields, JSON Schema reference, docs reference, and compatibility policy.

#### Scenario: Contract details include safety descriptions
- **WHEN** a client requests a test data or compatibility contract detail
- **THEN** the detail includes JSON Schema descriptions for safety metadata, generated examples, adapter results, diagnostics, stable fields, deprecated fields, and next actions
- **AND** secret-like example values are redacted or represented as placeholders.

