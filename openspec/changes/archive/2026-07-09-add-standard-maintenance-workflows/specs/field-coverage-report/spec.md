## ADDED Requirements

### Requirement: Field coverage report can seed maintenance workflow plans
DataSpec SHALL allow unmanaged, possible duplicate, missing comment, and partial coverage findings to seed standard maintenance workflow plans.

#### Scenario: Unmanaged fields create candidate workflow
- **WHEN** a coverage report contains unmanaged or possible duplicate fields selected for maintenance planning
- **THEN** DataSpec returns dry-run steps for reviewing candidates, comparing existing standards, and verifying coverage after explicit decisions.

#### Scenario: Partial coverage boundary remains visible
- **WHEN** a maintenance workflow is generated from a partial, cancelled, or failed coverage source
- **THEN** the plan includes evidence for failed or skipped table counts
- **AND** it MUST NOT mark unscanned tables or fields as handled.
