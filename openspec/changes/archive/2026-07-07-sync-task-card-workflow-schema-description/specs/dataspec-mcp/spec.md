## MODIFIED Requirements

### Requirement: MCP Task Card Tools
The DataSpec MCP server SHALL expose local task card tools for AI clients.

#### Scenario: Create task card tool
- **WHEN** an MCP client calls `create_task_card` with workflow id, goal, optional project id, and non-sensitive inputs
- **THEN** the server returns task card JSON text and `structuredContent`.

#### Scenario: Task card tool schema lists supported workflows
- **WHEN** an MCP client reads the `create_task_card` tool schema
- **THEN** the `workflowId` schema description includes the current supported workflow recipe ids.

#### Scenario: Render task card tool
- **WHEN** an MCP client calls `render_task_card` with a task card object
- **THEN** the server returns Markdown text without mutating project state.

#### Scenario: Task card tool validation
- **WHEN** an MCP client provides an unknown workflow id, invalid task card, or unsafe sensitive input
- **THEN** the server returns a JSON-RPC error with AI-readable diagnostic data.
