## ADDED Requirements

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
