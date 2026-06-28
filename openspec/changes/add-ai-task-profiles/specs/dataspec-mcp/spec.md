## ADDED Requirements

### Requirement: MCP AI profile resource
The MCP server SHALL expose the configured DataSpec AI task profiles as resources for AI clients.

#### Scenario: List profile resources
- **WHEN** an MCP client calls `resources/list`
- **THEN** the response includes an AI task profiles resource for the configured project.

#### Scenario: Read profile resource
- **WHEN** an MCP client reads the AI task profiles resource
- **THEN** the server returns JSON text containing profiles, selected defaults, diagnostics, and recommended commands.

### Requirement: MCP profile guidance
The MCP server SHALL include AI task profile guidance in prompts.

#### Scenario: Prompt includes profile instruction
- **WHEN** an MCP client gets a workflow prompt
- **THEN** the prompt tells the agent to read the profile resource before choosing context scope, fixed SQL mode, or output format.

#### Scenario: Tool accepts profile hint
- **WHEN** an MCP lint or context tool receives a profile hint
- **THEN** it forwards the profile selection or derived defaults to the DataSpec API while preserving explicit tool arguments.
