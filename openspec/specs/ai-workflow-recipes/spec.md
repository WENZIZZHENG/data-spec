# ai-workflow-recipes Specification

## Purpose
定义面向 AI 和用户的 DataSpec 工作流 recipe，用稳定目录描述常见任务的输入、预检、步骤、命令、预期产物、失败处理和后续动作。
## Requirements
### Requirement: Publish AI workflow recipes
The system SHALL provide workflow recipes that describe common AI/DataSpec tasks with inputs, ordered steps, commands, expected outputs, failure handling, and next actions.

#### Scenario: List supported recipes
- **WHEN** an AI agent or user asks for workflow recipes through CLI or MCP
- **THEN** the system returns a stable catalog that includes at least create-table, review-pr-sql, reverse-import-standards, export-min-context, and standard-evidence-review recipes.

#### Scenario: Inspect a recipe
- **WHEN** an AI agent requests a specific recipe
- **THEN** the system returns the recipe id, title, goal, required inputs, prechecks, ordered steps, expected artifacts, failure handling, and next actions.

#### Scenario: Inspect standard evidence review recipe
- **WHEN** an AI agent requests the standard-evidence-review recipe
- **THEN** the recipe describes a read-only, plan-only workflow that uses projectId, subjectType, and subjectId to read `GET /api/standard-evidence`
- **AND** it does not claim a dedicated CLI command, MCP tool, or MCP resource for standard evidence.

### Requirement: Provide machine-readable CLI recipe output
The CLI SHALL expose workflow recipe list/show commands with JSON output suitable for AI agents.

#### Scenario: CLI lists recipes as JSON
- **WHEN** a user runs the workflow list command with JSON output
- **THEN** the CLI returns a machine-readable object containing recipe ids, titles, goals, and required inputs.

#### Scenario: CLI shows one recipe as JSON
- **WHEN** a user runs the workflow show command for a known recipe id with JSON output
- **THEN** the CLI returns the complete recipe and exits successfully.

#### Scenario: CLI rejects unknown recipe id
- **WHEN** a user asks for an unknown recipe id
- **THEN** the CLI exits with parameter-error semantics and returns a clear message that includes supported recipe ids.

### Requirement: Expose recipes to MCP clients
The MCP server SHALL expose workflow recipes as an MCP resource so agent clients can read task plans before calling tools.

#### Scenario: MCP reads workflow recipes
- **WHEN** an MCP client reads the workflow recipes resource
- **THEN** the server returns the recipe catalog in JSON or markdown content without requiring external services.

### Requirement: Include recipes in AI Context package
The AI Context package SHALL include workflow recipe guidance for offline coding agents.

#### Scenario: Context package contains workflows file
- **WHEN** a user downloads an AI Context zip
- **THEN** the zip contains `.dataspec/workflows.md`
- **AND** the manifest and `.dataspec/README.md` reference the workflow file.

#### Scenario: Context workflows include standard evidence review
- **WHEN** a user downloads an AI Context zip after standard evidence review is supported
- **THEN** `.dataspec/workflows.md` includes the standard-evidence-review recipe
- **AND** the recipe describes `GET /api/standard-evidence` as a read-only API step without claiming a dedicated CLI or MCP surface.

### Requirement: Avoid hidden side effects
Workflow recipe commands MUST NOT automatically modify business repositories or invoke external LLMs.

#### Scenario: Recipe command returns plan only
- **WHEN** a user views a workflow recipe
- **THEN** the system returns commands and steps for the user or agent to execute explicitly
- **AND** it does not run the workflow steps automatically.

### Requirement: Workflow Recipes Seed Task Cards
Workflow recipes SHALL provide enough structured metadata to seed an AI task card.

#### Scenario: Generate card from recipe
- **WHEN** a caller creates a task card from a known workflow recipe
- **THEN** DataSpec maps recipe inputs, ordered steps, expected artifacts, failure handling, and next actions into the task card.

#### Scenario: Unknown workflow recipe
- **WHEN** a caller asks for a task card from an unknown workflow recipe
- **THEN** DataSpec returns a parameter error that includes supported recipe ids.

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
