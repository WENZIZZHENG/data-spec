## ADDED Requirements

### Requirement: Candidate inbox can seed maintenance workflow plans
DataSpec SHALL allow pending or postponed standard candidates to seed a standard maintenance workflow plan without changing candidate status.

#### Scenario: Candidate selection creates dry-run action
- **WHEN** a caller selects one or more pending or postponed candidates for workflow planning
- **THEN** DataSpec returns an `inboxAction` that identifies candidate review, accept, merge, ignore, or postpone options
- **AND** the plan keeps all write decisions as explicit user-confirmed steps.

#### Scenario: Candidate workflow does not decide automatically
- **WHEN** a candidate workflow plan is generated
- **THEN** candidate statuses remain unchanged
- **AND** DataSpec does not create or mutate standard fields until a caller invokes the existing candidate decision API.
