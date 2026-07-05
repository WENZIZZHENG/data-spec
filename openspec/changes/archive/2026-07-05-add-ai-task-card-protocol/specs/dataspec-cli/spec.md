## ADDED Requirements

### Requirement: CLI Task Card Commands
The DataSpec CLI SHALL expose task-card commands for local AI workflow handoff.

#### Scenario: Create task card
- **WHEN** a user runs `task-card create --workflow <id> --goal <text> --project <id> --format json`
- **THEN** the CLI prints a stable task card JSON object seeded from the workflow recipe.

#### Scenario: Write task card file
- **WHEN** a user passes `--output <path>` to task-card create or update
- **THEN** the CLI writes only the task card JSON or Markdown to that path
- **AND** it refuses to write outside the current working directory unless explicitly supported by existing safe path rules.

#### Scenario: Show task card
- **WHEN** a user runs `task-card show --file <path> --format markdown`
- **THEN** the CLI reads the task card and renders a concise Markdown summary.

#### Scenario: Update task card step
- **WHEN** a user runs `task-card update --file <path> --step <id> --status <status>`
- **THEN** the CLI updates the local task card file without executing workflow steps.

### Requirement: CLI Task Card Exit Codes
The DataSpec CLI SHALL use stable exit codes for task-card commands.

#### Scenario: Task card command succeeds
- **WHEN** a task-card command creates, shows, or updates a valid task card
- **THEN** the CLI exits with code `0`.

#### Scenario: Task card command fails validation
- **WHEN** task-card arguments, file content, workflow id, or step status are invalid
- **THEN** the CLI exits with code `2` and prints a non-sensitive error.
