## ADDED Requirements

### Requirement: Frontend exposes maintenance workflow dry-run entrypoints
DataSpec Web SHALL expose standard maintenance workflow dry-run actions from high-frequency maintenance pages.

#### Scenario: Candidate page opens workflow plan
- **WHEN** a user opens the standard candidate workbench with a current project
- **THEN** the page offers a dry-run maintenance workflow action for pending or postponed candidates
- **AND** the resulting plan view shows steps, evidence links, confirmation requirements, and verification guidance.

#### Scenario: Quality and coverage pages open workflow plan
- **WHEN** a user views field quality issues or field coverage unmanaged findings with a current project
- **THEN** the page offers a dry-run maintenance workflow action for the current filtered or selected findings
- **AND** partial, failed, empty, and missing-project states remain recoverable and non-sensitive.

#### Scenario: Frontend does not execute hidden writes
- **WHEN** a user generates or views a maintenance workflow plan
- **THEN** DataSpec Web calls only the plan API for dry-run generation
- **AND** write actions remain separate explicit actions through existing candidate or field maintenance flows.
