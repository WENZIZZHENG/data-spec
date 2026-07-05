## ADDED Requirements

### Requirement: Frontend Task Card Display
DataSpec Web SHALL provide a lightweight task card display for project task entrypoints and AI handoff views.

#### Scenario: Show task card summary
- **WHEN** the frontend receives a valid task card object
- **THEN** it displays goal, status, current step, next command, validation commands, artifacts, risks, and stop conditions.

#### Scenario: Missing or invalid task card
- **WHEN** the task card object is missing or invalid
- **THEN** the frontend shows a non-sensitive empty or invalid state instead of rendering raw JSON errors.

#### Scenario: Copy task card markdown
- **WHEN** a user copies a task card for handoff
- **THEN** the frontend provides Markdown containing the same non-sensitive summary fields.
