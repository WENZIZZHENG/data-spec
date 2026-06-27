# sql-lint-rules Specification

## Purpose
Defines SQL lint rule behavior that is shared by backend linting, CLI output, and AI-readable rule exports.

## Requirements
### Requirement: Field Suffix Type Rules
The SQL lint engine SHALL validate common field suffix and prefix type conventions.

#### Scenario: Detect suffix type mismatch
- **WHEN** a column name ends with `_id`, `_at`, `_no`, or `_count`
- **AND** its data type does not match the configured type list
- **THEN** the lint result includes a `field_suffix_type` issue

#### Scenario: Detect boolean prefix mismatch
- **WHEN** a column name starts with `is_`
- **AND** its data type is not boolean
- **THEN** the lint result includes a `field_suffix_type` issue

#### Scenario: Override suffix rules
- **WHEN** `paramsJson` provides `suffixTypes` or `prefixTypes`
- **THEN** the rule uses those configured type lists instead of defaults

### Requirement: Lint dialect diagnostics
SQL lint results SHALL include dialect diagnostics that describe which SQL dialect and compatibility assumptions were used.

#### Scenario: Lint result includes diagnostics
- **WHEN** a client calls `/api/lint`, CLI `lint`, or MCP `lint_sql`
- **THEN** the result includes `dialectDiagnostics`
- **AND** each diagnostic is safe for AI to parse by `code`, `level`, and `capability`

#### Scenario: Unsupported feature remains non-blocking
- **WHEN** lint encounters a dialect feature with partial support
- **THEN** DataSpec continues returning normal lint issues where possible
- **AND** includes a warning diagnostic that explains the degraded capability
