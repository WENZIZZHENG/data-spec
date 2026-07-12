## ADDED Requirements

### Requirement: MCP runs Standard Query DSL
DataSpec MCP SHALL expose a read-only Standard Query DSL tool or additive DSL input on `search_fields`.

#### Scenario: MCP searches with DSL
- **WHEN** an MCP client calls the DSL query tool or `search_fields` with Standard Query DSL input
- **THEN** structuredContent includes normalized query, field results, applied filters, ignored filters, counts, and next query hints
- **AND** text content is parseable JSON.

#### Scenario: MCP DSL safety metadata
- **WHEN** MCP tool descriptors are listed
- **THEN** the DSL-capable tool descriptor states that it is read-only, project-scoped, does not write standards, business files, or databases, and treats `query`, `standardQuery`, and filter values as sensitive inputs.

#### Scenario: MCP DSL output is secret-safe
- **WHEN** DSL input or backend diagnostics contain secret-like content
- **THEN** MCP content and JSON-RPC error data SHALL NOT expose raw token, password, Authorization, JDBC URL, DSN, or connection string values.
