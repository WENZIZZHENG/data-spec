## ADDED Requirements

### Requirement: CLI schema contract commands
The DataSpec CLI SHALL expose schema registry commands for AI agents and local validation.

#### Scenario: List contracts
- **WHEN** a user runs `contract list --format json`
- **THEN** the CLI fetches `/api/contracts`
- **AND** prints the registry catalog with stable `kind`, `schemaVersion`, `registryVersion`, and `contracts[]` fields.

#### Scenario: Show contract
- **WHEN** a user runs `contract show <contractId> --format json`
- **THEN** the CLI fetches `/api/contracts/{contractId}`
- **AND** prints the contract detail including JSON Schema, stable fields, deprecated fields, and compatibility.

#### Scenario: Check registry invariants
- **WHEN** a user runs `contract check --format json`
- **THEN** the CLI verifies required core contract ids, schemaVersion values, stableFields arrays, and compatibility policy are present
- **AND** exits with code `0` when the registry is usable or `2` with diagnostics when it is not.
