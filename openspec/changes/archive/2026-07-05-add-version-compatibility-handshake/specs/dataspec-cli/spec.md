## ADDED Requirements

### Requirement: CLI version compatibility check
The DataSpec CLI SHALL provide a machine-readable command for checking compatibility with the configured DataSpec server.

#### Scenario: CLI prints compatibility JSON
- **WHEN** a user runs `compat check --format json`
- **THEN** the CLI requests `/api/capabilities/version`
- **AND** it prints the returned compatibility payload as JSON with the local CLI version included.

#### Scenario: CLI reports incompatible server response
- **WHEN** the compatibility payload reports `compatibility.compatible=false`
- **THEN** the CLI exits with code `1`
- **AND** the JSON output includes reasons and next actions for upgrading, downgrading, or stopping.

#### Scenario: CLI compatibility request fails
- **WHEN** the compatibility request cannot reach the DataSpec server or returns an error
- **THEN** the CLI exits with code `2`
- **AND** it prints a readable diagnostic that does not expose tokens, passwords, Authorization headers, JDBC URLs, DSNs, or connection strings.

### Requirement: Doctor includes compatibility summary
The DataSpec CLI doctor command SHALL include version compatibility status in its JSON output.

#### Scenario: Doctor reports compatibility check
- **WHEN** a user runs `doctor --format json`
- **THEN** the result includes a compatibility check with server version, API schema hash, minimum CLI version, status, and next actions when available.

#### Scenario: Doctor remains usable when compatibility check fails
- **WHEN** the compatibility endpoint is unavailable but the server check still returns a reachable response
- **THEN** doctor reports the compatibility check as failed or warning
- **AND** it keeps the existing server, auth, project, default path, and OpenAPI checks in the output.
