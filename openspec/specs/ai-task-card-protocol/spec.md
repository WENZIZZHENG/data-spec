# ai-task-card-protocol Specification

## Purpose

Defines the stable local AI task card contract used by CLI, MCP, frontend helpers, and AI agents to resume DataSpec-assisted work without relying on terminal history or prior conversation context.

## Requirements

### Requirement: AI Task Card Shape
DataSpec SHALL define a stable AI task card JSON contract for resumable AI-assisted work.

#### Scenario: Create task card
- **WHEN** a task card is created for a workflow and goal
- **THEN** the card includes `kind`, `schemaVersion`, `taskId`, `workflowId`, `projectId`, `goal`, `inputs`, `status`, `currentStep`, `steps`, `allowedActions`, `artifacts`, `resumeCommand`, `validationCommands`, `stopConditions`, `risks`, `createdAt`, and `updatedAt`
- **AND** the card can be parsed without reading terminal history or prior AI conversation.

#### Scenario: Missing required inputs
- **WHEN** a workflow requires inputs that are not provided
- **THEN** the card status is `BLOCKED`
- **AND** `stopConditions` or `nextActions` tell the caller which non-sensitive inputs are missing.

### Requirement: Task Card Lifecycle
DataSpec SHALL use a small, machine-readable status model for task cards and steps.

#### Scenario: Update current step
- **WHEN** a task card step is marked `IN_PROGRESS`, `DONE`, `SKIPPED`, or `BLOCKED`
- **THEN** the card updates `currentStep`, `updatedAt`, and next safe command without executing the step itself.

#### Scenario: Resume blocked work
- **WHEN** a task card is blocked or partially complete
- **THEN** it includes `resumeCommand` and `validationCommands` that describe how to continue or verify the work.

### Requirement: Task Card Safety
DataSpec task cards SHALL be safe to store in a business repository.

#### Scenario: Secret redaction
- **WHEN** a task card is created, rendered, or updated
- **THEN** it does not include API token values, Authorization headers, database passwords, complete JDBC URLs, or business data rows.

#### Scenario: No hidden execution
- **WHEN** a task card is generated or updated
- **THEN** DataSpec does not run lint, export AI Context, connect to source databases, generate DDL, create standards, or mutate project state.

### Requirement: Task Card Markdown Rendering
DataSpec SHALL render task cards as concise Markdown for human review.

#### Scenario: Render markdown
- **WHEN** a task card is rendered as Markdown
- **THEN** the output includes goal, status, current step, next command, validation commands, artifacts, risks, and stop conditions.
