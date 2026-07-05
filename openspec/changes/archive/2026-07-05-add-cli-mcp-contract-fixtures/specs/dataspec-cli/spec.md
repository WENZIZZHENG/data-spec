## ADDED Requirements

### Requirement: CLI commands have contract fixture coverage
The DataSpec repository SHALL keep contract fixture entries for high-frequency AI-facing CLI commands.

#### Scenario: Fixture covers core CLI commands
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies fixture entries for high-frequency CLI commands such as `doctor`, `compat check`, `capability list/show/check`, `contract list/show/check`, `lint`, `export-context`, `search-fields`, and `generate-ddl`
- **AND** each entry documents required options, optional options, output shape, exit code semantics, safety metadata, examples, and next actions.

#### Scenario: CLI fixture output shape drifts
- **WHEN** a fixture entry removes a stable output field, exit code, diagnostic shape, or safety note required by the documented CLI contract
- **THEN** the fixture check fails with a readable diagnostic.

### Requirement: CLI contract fixture check is locally runnable
The DataSpec repository SHALL expose a local Node command to validate CLI contract fixtures.

#### Scenario: Run CLI fixture check without service
- **WHEN** a developer runs the CLI/MCP contract fixture check command from the repository root
- **THEN** it validates CLI fixture entries without requiring a running backend, API token, source database, or external network access.

#### Scenario: CLI fixture check joins standard validation
- **WHEN** project Node tests are run through the existing tools test entrypoint
- **THEN** the CLI/MCP contract fixture check is executed or covered by tests so contract fixture drift is caught in normal local validation.
