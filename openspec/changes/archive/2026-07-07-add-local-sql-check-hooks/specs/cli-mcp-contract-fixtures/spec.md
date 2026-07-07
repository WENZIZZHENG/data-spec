## ADDED Requirements

### Requirement: CLI install-hook fixture coverage
The DataSpec repository SHALL keep contract fixture coverage for the `install-hook` command.

#### Scenario: Fixture covers install-hook command
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it SHALL verify a fixture entry for `install-hook`.
- **AND** the fixture SHALL document required options, optional options, generated artifacts, output shape, exit code semantics, safety metadata, examples, and recommended next actions.

#### Scenario: Fixture rejects unsafe hook examples
- **WHEN** the `install-hook` fixture includes raw token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values
- **THEN** the fixture check SHALL fail with a readable diagnostic.
