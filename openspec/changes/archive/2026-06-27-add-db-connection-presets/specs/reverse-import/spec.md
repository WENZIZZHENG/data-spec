## ADDED Requirements

### Requirement: Reuse database connection presets in reverse import
The reverse import page SHALL allow users to reuse project database connection presets without restoring credentials.

#### Scenario: Load preset into direct connection form
- **WHEN** a user selects a database connection preset on the reverse import page
- **THEN** the page fills databaseType, host, port, databaseName, schemaName, and tableNames from the preset
- **AND** the password field remains empty or unchanged by the preset.

#### Scenario: Save current direct connection as preset
- **WHEN** a user saves the current direct connection form as a preset
- **THEN** the page submits only non-sensitive fields to the preset API
- **AND** password, token, JDBC URL, and full connection string are not submitted.

#### Scenario: Continue existing direct import workflow
- **WHEN** a user loads a preset and then enters the required password
- **THEN** the existing test connection, table loading, metadata preview, compare, and confirm import actions continue to work through the existing reverse import APIs.
