## ADDED Requirements

### Requirement: MCP standard test data package tool
The DataSpec MCP server SHALL expose a read-only tool for AI clients to generate standard-driven test data packages.

#### Scenario: Call test data package tool
- **WHEN** an MCP client calls `generate_test_data_package` with project id and bounded generation parameters
- **THEN** the server calls the DataSpec test data package API
- **AND** returns the package as JSON text and `structuredContent`
- **AND** the tool descriptor includes input schema descriptions and read-only safety metadata.

#### Scenario: Test data package tool redaction
- **WHEN** backend output, arguments, errors, or examples contain token, password, Authorization header, API key, complete JDBC URL, DSN, connection string, private key, or source database row values
- **THEN** MCP `tools/list`, `tools/call`, JSON text, `structuredContent`, and JSON-RPC errors do not expose raw sensitive values.

### Requirement: MCP compatibility suite resource and tool
The DataSpec MCP server SHALL expose the consumer compatibility suite as a local read-only resource and check tool for AI clients.

#### Scenario: List compatibility resource and tool
- **WHEN** an MCP client calls `resources/list` or `tools/list`
- **THEN** the response includes a consumer compatibility suite resource and a `check_consumer_compatibility` tool
- **AND** descriptors identify that the check is local, read-only, does not require a DataSpec server, and does not use external network or LLM calls.

#### Scenario: Run compatibility check tool
- **WHEN** an MCP client calls `check_consumer_compatibility`
- **THEN** the server returns compatibility suite JSON text and `structuredContent` containing status, adapter results, diagnostics, and next actions
- **AND** incompatible results are returned as successful tool content with `status=BREAKING` unless the local suite itself cannot be loaded.
