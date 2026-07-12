## ADDED Requirements

### Requirement: Local security profile defaults
DataSpec CLI and MCP SHALL read optional local AI security profile defaults from `.dataspec/config.json`.

#### Scenario: Config contains security profile
- **WHEN** `.dataspec/config.json` contains a valid `securityProfile` object
- **THEN** CLI/MCP SHALL expose the normalized profile to local command handlers
- **AND** existing `projectId`, `server`, `apiToken`, `aiProfile`, `taskType`, and `defaultPaths` behavior SHALL remain compatible.

#### Scenario: Security profile contains invalid field types
- **WHEN** `securityProfile.allowedAiTools`, `securityProfile.neverExportPatterns`, or `securityProfile.localOnlyPaths` is not a string array
- **THEN** CLI/MCP SHALL return a readable configuration error
- **AND** no DataSpec HTTP API call SHALL be made.

#### Scenario: Security profile scalar policy validation
- **WHEN** `securityProfile.redactionStrictness`, `securityProfile.sensitiveFieldPolicy`, `securityProfile.samplePolicy`, or `securityProfile.credentialPolicy` is present but not a string
- **THEN** CLI/MCP SHALL return a readable configuration error
- **AND** the error message SHALL NOT include token, password, Authorization, JDBC URL, DSN, or raw secret values.
