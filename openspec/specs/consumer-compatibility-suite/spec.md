# consumer-compatibility-suite Specification

## Purpose
定义消费端对 API、CLI、MCP、AI Context 和 Schema Registry 稳定契约的本地兼容验证能力。
## Requirements
### Requirement: Consumer compatibility suite contract
DataSpec SHALL provide a local consumer compatibility suite that validates DataSpec-owned API, CLI, MCP, AI Context, and schema registry contracts against checked-in golden payloads and breaking-change rules.

#### Scenario: Run compatibility check
- **WHEN** a developer or AI agent runs the consumer compatibility check command
- **THEN** the result includes stable fields `kind`, `schemaVersion`, `suiteVersion`, `checkedAt`, `minimumSupportedVersion`, `status`, `summary`, `goldenPayloads`, `breakingRules`, `adapterResults`, `diagnostics`, and `nextActions`
- **AND** the command exits with code `0` when all required adapters are compatible.

#### Scenario: Report incompatible contract
- **WHEN** a required stable field, descriptor, output shape, safety metadata, or fixture example is removed, renamed, changes type, or changes documented semantics without an allowed migration
- **THEN** the suite reports `status=BREAKING`
- **AND** diagnostics identify the affected adapter, contract path, breaking rule, and migration hint.

### Requirement: Compatibility adapters
The consumer compatibility suite SHALL cover DataSpec-owned consumers before third-party adapters.

#### Scenario: Required adapters are checked
- **WHEN** the compatibility suite runs
- **THEN** it checks adapters for Schema Registry contracts, AI Context payloads, CLI JSON commands, MCP tool/resource/prompt descriptors, CLI/MCP contract fixtures, and standard test data package payloads
- **AND** unsupported third-party consumers are reported as out of scope rather than failing the suite.

#### Scenario: Additive fields remain compatible
- **WHEN** a checked payload gains an optional field without changing stable field semantics, requiredness, safety metadata, or documented output type
- **THEN** the adapter result remains `COMPATIBLE`
- **AND** the result may include an additive-field note for downstream consumers.

### Requirement: Compatibility suite safety
The consumer compatibility suite SHALL be local, read-only, and safe to run in CI or AI sessions.

#### Scenario: Check is read-only
- **WHEN** the compatibility suite runs from CLI or tools
- **THEN** it reads local fixtures, descriptors, schemas, and documented contract files only
- **AND** it does not call external services, connect to databases, write project data, or require secrets.

#### Scenario: Fixture examples are secret-safe
- **WHEN** golden payloads, adapter diagnostics, examples, or recommended commands contain raw token, password, Authorization header, API key, complete JDBC URL, DSN, connection string, or private key patterns
- **THEN** the compatibility suite fails with a diagnostic naming the unsafe fixture path.
