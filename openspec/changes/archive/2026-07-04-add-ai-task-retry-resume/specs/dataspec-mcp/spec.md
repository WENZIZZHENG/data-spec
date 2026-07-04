## ADDED Requirements

### Requirement: MCP task run resource
The DataSpec MCP server SHALL expose recent AI task runs as a resource for configured projects.

#### Scenario: List task run resource
- **WHEN** an MCP client calls `resources/list`
- **THEN** the response includes an AI task runs resource when a project id is configured.

#### Scenario: Read task run resource
- **WHEN** an MCP client reads the AI task runs resource
- **THEN** the server returns JSON text and structured content containing recent failed and resumable task runs.

### Requirement: MCP task run tool
The DataSpec MCP server SHALL expose a read-only tool for AI task run detail.

#### Scenario: Get task run detail
- **WHEN** an MCP client calls `get_ai_task_run` with task run id and project id
- **THEN** the server returns task run detail as structuredContent and JSON text content.

#### Scenario: Task run tool failure
- **WHEN** the backend task run API fails
- **THEN** the MCP response includes AI-readable error data and does not fabricate a successful task state.
