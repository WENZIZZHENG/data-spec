## ADDED Requirements

### Requirement: CLI exposes AI capability catalog
The DataSpec CLI SHALL provide commands for AI agents to list, inspect, and validate the server capability catalog.

#### Scenario: List capabilities from CLI
- **WHEN** a user runs `dataspec capability list --format json`
- **THEN** the CLI fetches the server capability catalog and prints stable JSON
- **AND** the output includes capability ids, summaries, surfaces, preflight checks, contracts, examples, and nextActions.

#### Scenario: Show one capability from CLI
- **WHEN** a user runs `dataspec capability show <id> --format json`
- **THEN** the CLI prints the selected capability
- **AND** an unknown id exits with a parameter error and suggests running `capability list`.

#### Scenario: Capability check command
- **WHEN** a user runs `dataspec capability check --format json`
- **THEN** the CLI verifies that required core AI capabilities exist in the catalog
- **AND** reports missing ids or incompatible schemaVersion without executing those capabilities.

#### Scenario: Server unavailable
- **WHEN** the CLI cannot reach the DataSpec server while reading capabilities
- **THEN** it returns the existing AI-readable DataSpecError diagnostics
- **AND** suggests running `dataspec doctor --format json`.
