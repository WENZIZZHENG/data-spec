## ADDED Requirements

### Requirement: Detect workflow recipe documentation drift
The system SHALL provide a deterministic local status check that compares the supported workflow recipe ids with the AI-readable documentation and project status summaries that enumerate those ids.

#### Scenario: Recipe documentation is synchronized
- **WHEN** the local status check runs with AI contract documentation and TODO status text that include every supported workflow recipe id
- **THEN** the workflow recipe contract check passes without emitting drift issues.

#### Scenario: AI contract documentation omits a recipe
- **WHEN** the local status check runs and `docs/ai-contracts.md` omits a supported workflow recipe id from the `.dataspec/workflows.md` contract summary
- **THEN** the status check fails with an issue that identifies the missing recipe id and the AI contract document.

#### Scenario: AI contract documentation keeps a removed recipe
- **WHEN** the local status check runs and `docs/ai-contracts.md` still declares a workflow recipe id that is no longer supported
- **THEN** the status check fails with an issue that identifies the extra recipe id and the AI contract document.

#### Scenario: AI contract documentation is missing
- **WHEN** the local status check runs and `docs/ai-contracts.md` is unavailable
- **THEN** the status check fails with an issue that explains the workflow recipe id list cannot be confirmed.

#### Scenario: TODO task-card summary omits a recipe
- **WHEN** the local status check runs and `TODO.md` omits a supported workflow recipe id from the task-card workflow summary
- **THEN** the status check fails with an issue that identifies the missing recipe id and the TODO file.

#### Scenario: TODO task-card summary keeps a removed recipe
- **WHEN** the local status check runs and `TODO.md` still declares a workflow recipe id that is no longer supported
- **THEN** the status check fails with an issue that identifies the extra recipe id and the TODO file.
