## ADDED Requirements

### Requirement: Init generates editor-ready config schema assets
DataSpec CLI SHALL initialize a local JSON Schema and associate the generated config with the supported schema version.

#### Scenario: New repository receives schema association
- **WHEN** a user runs `init --project <id> --server <url>` in a repository without DataSpec managed files
- **THEN** DataSpec writes `.dataspec/config.schema.json`, `.dataspec/config.json`, and `.dataspec/README.md`
- **AND** config contains `$schema: "./config.schema.json"`, `configVersion: 1`, projectId, normalized server, and defaultPaths
- **AND** config does not contain an API token or another reusable credential.

#### Scenario: Existing managed files remain protected
- **WHEN** one or more of the schema, config, or README files already exist
- **THEN** init without `--force` leaves each existing file unchanged and reports it as skipped
- **AND** missing managed files are still created independently.

#### Scenario: Force refreshes schema assets
- **WHEN** a user runs `init --force`
- **THEN** DataSpec replaces the schema, config, and README with the current managed versions
- **AND** the generated config and schema declare the same supported config version.

#### Scenario: Server URL contains reusable userinfo
- **WHEN** init receives a server URL containing a username or password
- **THEN** it rejects the URL before writing schema, config, README, or AGENTS files
- **AND** the diagnostic does not echo the username or password.

#### Scenario: Existing config declares a future version
- **WHEN** an existing configVersion is greater than the current CLI supported version
- **THEN** init rejects the operation before writing any managed file even when `--force` is present
- **AND** it does not downgrade, replace, or partially pair the future config with a current schema.
