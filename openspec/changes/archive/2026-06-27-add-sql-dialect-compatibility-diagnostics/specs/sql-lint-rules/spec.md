## ADDED Requirements

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
