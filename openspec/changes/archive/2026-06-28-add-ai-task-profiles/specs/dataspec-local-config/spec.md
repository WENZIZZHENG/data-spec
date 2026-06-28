## ADDED Requirements

### Requirement: Local AI profile defaults
DataSpec CLI and MCP SHALL read optional AI task profile defaults from `.dataspec/config.json`.

#### Scenario: Config contains ai profile
- **WHEN** `.dataspec/config.json` contains `aiProfile` or `taskType`
- **THEN** CLI/MCP use those values as the default profile selection when explicit command options are omitted.

#### Scenario: Explicit profile overrides config
- **WHEN** a command passes an explicit profile or task type option
- **THEN** the explicit option takes precedence over `.dataspec/config.json`.

#### Scenario: Invalid profile config shape
- **WHEN** `.dataspec/config.json` contains non-string `aiProfile` or `taskType`
- **THEN** CLI/MCP return a readable configuration diagnostic before calling DataSpec HTTP APIs.
