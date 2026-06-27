# structured-lint-fixes Specification

## Purpose
TBD - created by archiving change add-structured-lint-fixes. Update Purpose after archive.
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
