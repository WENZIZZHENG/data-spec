## ADDED Requirements

### Requirement: MCP resolves standard references
DataSpec MCP SHALL expose a read-only `resolve_standard_refs` tool with structured results.

#### Scenario: Resolve references through MCP
- **WHEN** an MCP client calls `resolve_standard_refs` with projectId, refType, and refs
- **THEN** structuredContent SHALL match the stable API resolution contract
- **AND** text content SHALL be parseable JSON.

### Requirement: MCP checks AI output before use
DataSpec MCP SHALL expose a read-only `check_ai_output` tool.

#### Scenario: Post-check through MCP
- **WHEN** an MCP client calls `check_ai_output` with projectId, content type, content, and optional snapshot reference
- **THEN** structuredContent SHALL include PASS/WARN/FAIL status, safeToUse, issues, resolved refs, fixes, evidence links, and next actions
- **AND** the tool descriptor SHALL state that it does not write standards, business files, or databases.

#### Scenario: MCP output is secret-safe
- **WHEN** tool input or backend diagnostics contain secret-like content
- **THEN** MCP content and JSON-RPC error data SHALL NOT expose raw token, password, Authorization, JDBC URL, DSN, or connection string values.
