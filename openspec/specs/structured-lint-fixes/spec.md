# structured-lint-fixes Specification

## Purpose
Define the structured fix metadata that lint rules expose to API, CLI, MCP, AI agents, and frontends while preserving compatibility with existing lint issue fields.
## Requirements
### Requirement: Structured Lint Issue Fix Metadata
The system SHALL include structured fix metadata on lint issues when a rule can suggest a deterministic fix.

#### Scenario: Fix metadata fields
- **WHEN** a lint rule can suggest a deterministic fix
- **THEN** the issue includes `suggestion`, `replacement`, `before`, `after`, and `confidence` where applicable

### Requirement: Core Rule Fix Suggestions
Core naming and standard rules SHALL populate fix metadata.

#### Scenario: Naming rule suggestion
- **WHEN** a table or column violates snake_case
- **THEN** the issue suggests a snake_case replacement

#### Scenario: Recommended field replacement
- **WHEN** a column matches a known recommended field replacement
- **THEN** the issue includes the recommended replacement field name

#### Scenario: Required column suggestion
- **WHEN** a table misses a required column
- **THEN** the issue includes a column snippet or target name in the fix metadata

### Requirement: AI Tool Compatibility
The structured fix metadata SHALL be exposed through existing API, CLI, and MCP lint outputs.

#### Scenario: Existing lint entry points
- **WHEN** a client calls `/api/lint`, CLI `lint`, or MCP `lint_sql`
- **THEN** the new metadata is included in the returned JSON without changing existing field names

### Requirement: Fixed SQL dialect safety diagnostics
fixedSql output SHALL include dialect diagnostics describing whether deterministic fixes are safe for the inferred dialect.

#### Scenario: Fixed SQL is generated for PostgreSQL
- **WHEN** fixedSql is generated for PostgreSQL-style SQL
- **THEN** diagnostics indicate PostgreSQL as the target dialect
- **AND** no MySQL-only compatibility claim is made

#### Scenario: Fixed SQL is generated for MySQL-like input
- **WHEN** fixedSql is generated from MySQL-like SQL
- **THEN** diagnostics warn when the fixer normalizes output through PostgreSQL-style COMMENT or type rendering
- **AND** the warning includes a next action for manual review or later dialect-specific fixing

### Requirement: Fixer risk and policy metadata
Structured lint issue fix metadata SHALL identify deterministic fixer risk and policy status when available.

#### Scenario: Fixable issue includes policy metadata
- **WHEN** a lint issue can participate in deterministic fixed SQL generation
- **THEN** the issue includes `fixRiskLevel`, `fixChangeType`, `fixStatus`, and `fixExplain`
- **AND** existing `suggestion`, `replacement`, `before`, `after`, and `confidence` fields remain compatible.

#### Scenario: Suppressed issue is not applied
- **WHEN** a lint issue is suppressed by a rule exemption
- **THEN** the issue is not applied to `fixedSql`
- **AND** its fix metadata explains that the issue was skipped because it was suppressed.

### Requirement: AI tool fix metadata compatibility
The additional fix metadata SHALL be exposed through existing API, CLI, and MCP lint outputs without breaking existing clients.

#### Scenario: Existing lint consumers receive additive metadata
- **WHEN** a client calls `/api/lint`, CLI `lint`, CLI `lint-files`, or MCP `lint_sql`
- **THEN** deterministic fix metadata is present as additive optional fields
- **AND** existing field names and severity/count semantics remain unchanged.
