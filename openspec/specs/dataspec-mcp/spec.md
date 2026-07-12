# dataspec-mcp Specification

## Purpose
定义 DataSpec MCP stdio server 的资源、prompt 和工具契约，使 MCP 客户端能安全读取项目标准、调用 SQL lint、获取字段目录并使用标准化提示。
## Requirements
### Requirement: MCP Initialization
The system SHALL provide a MCP stdio server that can be initialized by MCP clients.

#### Scenario: Initialize server
- **WHEN** a client sends an `initialize` JSON-RPC request
- **THEN** the server returns protocol version, server info, and capabilities for resources, prompts, and tools

### Requirement: MCP Resources
The MCP server SHALL expose DataSpec project resources for the configured project.

#### Scenario: List project resources
- **WHEN** a client calls `resources/list`
- **THEN** the server returns field catalog, database rules, and rules YAML resource descriptors

#### Scenario: Read field catalog
- **WHEN** a client calls `resources/read` for the field catalog URI
- **THEN** the server fetches `/api/ai-context/field-catalog`
- **AND** it returns the catalog as text content

### Requirement: MCP Prompts
The MCP server SHALL expose standard prompts for AI SQL workflows.

#### Scenario: List prompts
- **WHEN** a client calls `prompts/list`
- **THEN** the server returns prompts for creating tables, reviewing SQL, and designing fields from requirements

#### Scenario: Get prompt
- **WHEN** a client calls `prompts/get`
- **THEN** the server returns MCP prompt messages with DataSpec usage guidance

### Requirement: MCP Tools
The MCP server SHALL expose executable tools backed by the DataSpec server.

#### Scenario: Lint SQL tool
- **WHEN** a client calls `tools/call` with `lint_sql` and SQL text
- **THEN** the server calls `/api/lint`
- **AND** it returns the lint result as structured content and JSON text content

#### Scenario: Field catalog tool
- **WHEN** a client calls `tools/call` with `get_field_catalog`
- **THEN** the server calls `/api/ai-context/field-catalog`
- **AND** it returns the field catalog as structured content and JSON text content when possible

### Requirement: MCP Error Handling
The MCP server SHALL return stable JSON-RPC errors for invalid requests and failed backend calls.

#### Scenario: Unknown method
- **WHEN** a client calls an unsupported method
- **THEN** the server returns JSON-RPC error code `-32601`

#### Scenario: Backend request fails
- **WHEN** the DataSpec server returns a non-success response
- **THEN** the MCP server returns a JSON-RPC error with a readable message

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

### Requirement: MCP schema registry resource
The MCP server SHALL expose the DataSpec schema registry as a resource for AI clients.

#### Scenario: List schema registry resource
- **WHEN** an MCP client calls `resources/list`
- **THEN** the response includes a `schema-registry` resource for the configured project.

#### Scenario: Read schema registry resource
- **WHEN** an MCP client reads the schema registry resource
- **THEN** the server returns JSON text containing registry metadata, contract summaries, JSON Schema refs, stable fields, deprecated fields, and compatibility policy.

#### Scenario: Prompts mention contract registry
- **WHEN** an MCP client gets a SQL or field design prompt
- **THEN** the prompt tells the agent to read the schema registry when it needs stable output field names or compatibility guidance.

### Requirement: MCP evidence package tool
The MCP server SHALL expose a tool for AI clients to retrieve evidence packages.

#### Scenario: Call evidence tool
- **WHEN** an MCP client calls `export_evidence_package` with a supported source type and id or payload
- **THEN** the server calls the DataSpec evidence package API
- **AND** returns the package as `structuredContent` and JSON text content.

#### Scenario: Evidence tool redaction
- **WHEN** the DataSpec API returns an evidence package
- **THEN** the MCP response preserves redacted fields and does not add token, password, Authorization header, or full JDBC URL to the content.

### Requirement: MCP evidence guidance
The MCP server SHALL guide AI clients to use evidence packages for handoff.

#### Scenario: Prompts mention evidence package
- **WHEN** an MCP client gets a SQL, field design, or workflow prompt
- **THEN** the prompt tells the agent to export an evidence package before handing off completed work.

### Requirement: MCP exposes capability catalog resource
The DataSpec MCP server SHALL expose the AI capability catalog as a resource that agents can read before choosing tools.

#### Scenario: Read global capability catalog resource
- **WHEN** an MCP client reads the capability catalog resource
- **THEN** the server returns JSON text and structuredContent containing the DataSpec capability catalog
- **AND** the resource metadata describes it as read-only self-description.

#### Scenario: Read project-scoped capability catalog resource
- **WHEN** the MCP server has a configured projectId
- **THEN** the capability catalog resource includes project-aware diagnostics from the DataSpec server.

#### Scenario: MCP catalog read failure
- **WHEN** the DataSpec server is unavailable or returns an error
- **THEN** the MCP response includes AI-readable DataSpec error data
- **AND** it does not fabricate a successful catalog.

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

### Requirement: MCP session bootstrap resource
The DataSpec MCP server SHALL expose a session bootstrap resource for configured projects.

#### Scenario: List bootstrap resource
- **WHEN** an MCP client calls `resources/list`
- **THEN** the response includes `dataspec://project/<id>/session-bootstrap` with JSON mime type.

#### Scenario: Read bootstrap resource
- **WHEN** an MCP client reads the session bootstrap resource
- **THEN** the server fetches `/api/bootstrap/session`
- **AND** returns JSON text and `structuredContent` containing the bootstrap package.

#### Scenario: Bootstrap resource failure
- **WHEN** the DataSpec backend rejects or fails the bootstrap request
- **THEN** the MCP response includes AI-readable DataSpec error data
- **AND** it does not fabricate a successful bootstrap package.

### Requirement: MCP session bootstrap tool
The DataSpec MCP server SHALL expose a read-only `get_session_bootstrap` tool.

#### Scenario: Call bootstrap tool
- **WHEN** an MCP client calls `get_session_bootstrap` with an optional `projectId`
- **THEN** the server calls `/api/bootstrap/session`
- **AND** returns the package as JSON text and `structuredContent`.

### Requirement: MCP session state resource
The DataSpec MCP server SHALL expose a read-only session state resource for configured projects.

#### Scenario: List session state resource
- **WHEN** an MCP client calls `resources/list` and the MCP server has a configured project id
- **THEN** the response includes `dataspec://project/<id>/session-state` with JSON mime type.
- **AND** the resource description identifies it as a local, read-only current project memory summary.

#### Scenario: Read session state resource
- **WHEN** an MCP client reads `dataspec://project/<id>/session-state`
- **THEN** the server returns JSON text and `structuredContent` containing `kind`, `schemaVersion`, `generatedAt`, `currentProject`, `currentSnapshot`, `lastTaskResult`, `toolCursor`, `safeDefaults`, `redactedMemory`, `diagnostics`, and `nextActions`.
- **AND** the server does not call DataSpec write APIs, run lint, connect to source databases, generate DDL, or mutate project state.

#### Scenario: Projectless template discovery
- **WHEN** the MCP server starts without a configured project id
- **THEN** `resources/templates/list` includes `dataspec://project/{projectId}/session-state`.

### Requirement: MCP session state tool
The DataSpec MCP server SHALL expose a read-only `get_session_state` tool.

#### Scenario: Call session state tool
- **WHEN** an MCP client calls `get_session_state` with an optional `projectId`
- **THEN** the server returns the same session state shape as JSON text and `structuredContent`.
- **AND** an explicit `projectId` overrides the configured project id for the returned state.

#### Scenario: Missing project id
- **WHEN** an MCP client calls `get_session_state` without a project id and no project id is configured
- **THEN** the server returns a valid session state with `currentProject.status` set to `BLOCKED`.
- **AND** `nextActions` tells the client to provide `projectId`, update `.dataspec/config.json`, or call `get_session_bootstrap` after selecting a project.

### Requirement: MCP session state safety
The MCP session state SHALL be safe for AI clients to read at the beginning and middle of a session.

#### Scenario: Session state redaction
- **WHEN** local config, context metadata, task cards, commands, server URLs, or diagnostics contain tokens, passwords, Authorization headers, JDBC URLs, DSNs, connection strings, or URL userinfo
- **THEN** `resources/read`, `tools/call`, JSON text, and `structuredContent` do not reveal raw secret-like values.
- **AND** the output may only include presence flags such as `TOKEN_PRESENT`, counts, hashes, or redacted placeholders.

#### Scenario: Session state is not authorization
- **WHEN** the session state says a project id or token is configured
- **THEN** `safeDefaults` and `diagnostics` state that the snapshot is not an authorization decision.
- **AND** write-capable follow-up tools still require their existing safety metadata, user confirmation, dry-run, or idempotency checks.

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

### Requirement: MCP Task Card Safety
MCP task card tools SHALL be local planning helpers, not workflow executors.

#### Scenario: No workflow execution
- **WHEN** a client creates or renders a task card
- **THEN** the MCP server does not call DataSpec write APIs, run lint, connect to source databases, or generate DDL.

### Requirement: MCP exposes tool safety metadata
The DataSpec MCP server SHALL expose AI write safety metadata in resources or tool metadata so MCP clients can inspect risk before invoking a tool.

#### Scenario: Capability resource includes safety
- **WHEN** an MCP client reads the capability catalog resource
- **THEN** the returned JSON text and `structuredContent` preserve each capability `safety` object.

#### Scenario: Tool list includes local safety metadata
- **WHEN** an MCP client calls `tools/list`
- **THEN** each DataSpec tool descriptor includes safety metadata or a safety reference describing whether the tool is read-only, writes project state, requires dry-run, requires idempotency key, accepts sensitive inputs, and what next action to take.

#### Scenario: MCP prompts mention safety first
- **WHEN** an MCP client reads DataSpec prompts for SQL review, table creation, or field design
- **THEN** the prompt tells the AI to inspect capability safety metadata before invoking write-capable tools.

### Requirement: MCP returns structured safety diagnostics
The DataSpec MCP server SHALL propagate or create AI-readable JSON-RPC safety errors for missing required safety parameters.

#### Scenario: Backend safety error is preserved
- **WHEN** a DataSpec backend write API rejects a request with a safety diagnostic
- **THEN** the MCP JSON-RPC error includes `error.data.dataspecError` with the diagnostic code, category, missing fields, safety metadata, and next actions.

#### Scenario: Local safety validation fails
- **WHEN** an MCP tool declares a required safety parameter and the client omits it
- **THEN** the MCP server rejects the call before invoking the backend
- **AND** the JSON-RPC error describes the missing parameter and safe retry action.

#### Scenario: MCP safety output is redacted
- **WHEN** MCP tools/list, resources/read, tools/call, or errors include safety metadata
- **THEN** the output does not include raw token, password, Authorization header, API key, complete JDBC URL, DSN, connection string, or source database rows.

### Requirement: MCP exposes version compatibility resource
The DataSpec MCP server SHALL expose the version compatibility handshake as a read-only resource.

#### Scenario: List compatibility resource
- **WHEN** an MCP client calls `resources/list`
- **THEN** the response includes a `dataspec://version-compatibility` resource
- **AND** the resource description tells AI clients to read it before running version-sensitive tools.

#### Scenario: Read compatibility resource
- **WHEN** an MCP client reads `dataspec://version-compatibility`
- **THEN** the MCP server requests `/api/capabilities/version`
- **AND** it returns the compatibility payload as JSON text and `structuredContent`.

#### Scenario: Compatibility resource failure is AI-readable
- **WHEN** the DataSpec server cannot provide the compatibility payload
- **THEN** the MCP server returns a JSON-RPC error with a DataSpec diagnostic and next action
- **AND** the error does not expose tokens, passwords, Authorization headers, JDBC URLs, DSNs, or connection strings.

### Requirement: MCP descriptors have contract fixture coverage
The DataSpec repository SHALL keep contract fixture entries for high-frequency MCP resources, prompts, and tools.

#### Scenario: Fixture covers MCP resources and prompts
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies fixture entries for high-frequency MCP resources such as `dataspec://version-compatibility`, `dataspec://capability-catalog`, `session-bootstrap`, `field-catalog`, `workflow-recipes`, `ai-task-profiles`, and `schema-registry`
- **AND** it verifies fixture entries for DataSpec MCP prompts used for create-table, SQL review, and field design guidance.

#### Scenario: Fixture covers MCP tools
- **WHEN** the fixture check reads MCP `tools/list`
- **THEN** it verifies fixture entries for high-frequency MCP tools such as `get_session_bootstrap`, `lint_sql`, `get_field_catalog`, `search_field_catalog`, `search_fields`, `suggest_fields`, `generate_table_ddl`, `get_ai_task_run`, and `export_evidence_package`
- **AND** each tool fixture preserves input schema shape, output shape, safety metadata, examples, and next actions.

### Requirement: MCP contract fixtures align with live descriptors
The DataSpec MCP server SHALL expose descriptors that can be checked against the local contract fixtures without calling backend APIs.

#### Scenario: MCP tool descriptor drifts
- **WHEN** an MCP tool name, input property, required input, or safety metadata changes without updating the fixture
- **THEN** the fixture check fails with a diagnostic naming the affected tool and contract path.

#### Scenario: MCP resource or prompt descriptor drifts
- **WHEN** an MCP resource URI, prompt name, description, or required argument changes without updating the fixture
- **THEN** the fixture check fails with a diagnostic naming the affected resource or prompt and contract path.

### Requirement: MCP first-class agent prompts
The DataSpec MCP server SHALL expose first-class prompts for common DataSpec agent workflows.

#### Scenario: List agent prompts
- **WHEN** an MCP client calls `prompts/list`
- **THEN** the response includes prompts named `create_table_with_dataspec`, `review_sql_with_dataspec`, `reverse_import_standards`, and `answer_field_standard_question`.

#### Scenario: Get agent prompt
- **WHEN** an MCP client calls `prompts/get` for a first-class agent prompt
- **THEN** the returned prompt text includes required inputs, safe defaults, recommended resource sequence, recommended tool sequence, stop conditions, and evidence requirements.

#### Scenario: Agent prompts preserve compatibility
- **WHEN** an MCP client calls existing prompt names `dataspec_create_table`, `dataspec_review_sql`, or `dataspec_design_fields`
- **THEN** those prompts remain available and continue to guide clients to DataSpec resources, safety metadata, and evidence package handoff.

### Requirement: MCP resource templates listing
The DataSpec MCP server SHALL support listing project-scoped resource templates.

#### Scenario: Resource templates list
- **WHEN** an MCP client calls `resources/templates/list`
- **THEN** the server returns `resourceTemplates[]` entries with `uriTemplate`, `name`, `description`, and `mimeType`.

#### Scenario: Projectless template discovery
- **WHEN** the MCP server starts without a configured project id
- **THEN** `resources/templates/list` still returns URI templates containing `{projectId}` placeholders rather than failing.

### Requirement: MCP resolves standard references
DataSpec MCP SHALL expose a read-only `resolve_standard_refs` tool with structured results.

#### Scenario: Resolve references through MCP
- **WHEN** an MCP client calls `resolve_standard_refs` with projectId, refType, and refs
- **THEN** structuredContent SHALL match the stable API resolution contract
- **AND** text content SHALL be parseable JSON.

### Requirement: MCP checks AI output before use
DataSpec MCP SHALL expose a read-only `check_ai_output` tool.

#### Scenario: Post-check through MCP
- **WHEN** an MCP client calls `check_ai_output` with projectId, content type, content, and optional snapshot reference
- **THEN** structuredContent SHALL include PASS/WARN/FAIL status, safeToUse, issues, resolved refs, fixes, evidence links, and next actions
- **AND** the tool descriptor SHALL state that it does not write standards, business files, or databases.

#### Scenario: MCP output is secret-safe
- **WHEN** tool input or backend diagnostics contain secret-like content
- **THEN** MCP content and JSON-RPC error data SHALL NOT expose raw token, password, Authorization, JDBC URL, DSN, or connection string values.

### Requirement: MCP runs Standard Query DSL
DataSpec MCP SHALL expose a read-only Standard Query DSL tool or additive DSL input on `search_fields`.

#### Scenario: MCP searches with DSL
- **WHEN** an MCP client calls the DSL query tool or `search_fields` with Standard Query DSL input
- **THEN** structuredContent includes normalized query, field results, applied filters, ignored filters, counts, and next query hints
- **AND** text content is parseable JSON.

#### Scenario: MCP DSL safety metadata
- **WHEN** MCP tool descriptors are listed
- **THEN** the DSL-capable tool descriptor states that it is read-only, project-scoped, does not write standards, business files, or databases, and treats `query`, `standardQuery`, and filter values as sensitive inputs.

#### Scenario: MCP DSL output is secret-safe
- **WHEN** DSL input or backend diagnostics contain secret-like content
- **THEN** MCP content and JSON-RPC error data SHALL NOT expose raw token, password, Authorization, JDBC URL, DSN, or connection string values.

### Requirement: MCP exposes table standards
The DataSpec MCP server SHALL expose table structure standards as read-only context for AI clients.

#### Scenario: List table standards resource
- **WHEN** an MCP client calls `resources/list`
- **THEN** the response includes a project table standards resource when a project id is configured
- **AND** the resource description identifies it as read-only business object, relation, template, and structure-standard context.

#### Scenario: Read table standards resource
- **WHEN** an MCP client reads the table standards resource
- **THEN** the MCP server fetches the DataSpec table standards API or AI Context table standards endpoint
- **AND** returns JSON text and `structuredContent` with business objects, templates, relations, summary, safety metadata, and next actions.

#### Scenario: Table standards tool
- **WHEN** an MCP client calls a read-only `get_table_standards` tool with optional project, template, or business object filters
- **THEN** the server returns the same table standards shape as JSON text and `structuredContent`
- **AND** it does not create templates, update standards, generate DDL, connect to databases, or mutate project state.

### Requirement: MCP create-table guidance uses table standards
MCP prompts for table creation SHALL guide AI clients to read table standards before DDL generation.

#### Scenario: Prompt resource sequence includes table standards
- **WHEN** an MCP client gets `create_table_with_dataspec` or a compatible table design prompt
- **THEN** the prompt guidance lists table standards before `generate_table_ddl`
- **AND** stop conditions mention missing or unsafe structure standards for high-risk DDL work.

#### Scenario: DDL tool preserves structure summary
- **WHEN** an MCP client calls `generate_table_ddl`
- **THEN** the structured response preserves the server `structureSummary` when present
- **AND** recommended next actions tell the client to inspect lint, dialect diagnostics, and structure evidence before handing DDL to a user.

### Requirement: MCP standard test data package tool
The DataSpec MCP server SHALL expose a read-only tool for AI clients to generate standard-driven test data packages.

#### Scenario: Call test data package tool
- **WHEN** an MCP client calls `generate_test_data_package` with project id and bounded generation parameters
- **THEN** the server calls the DataSpec test data package API
- **AND** returns the package as JSON text and `structuredContent`
- **AND** the tool descriptor includes input schema descriptions and read-only safety metadata.

#### Scenario: Test data package tool redaction
- **WHEN** backend output, arguments, errors, or examples contain token, password, Authorization header, API key, complete JDBC URL, DSN, connection string, private key, or source database row values
- **THEN** MCP `tools/list`, `tools/call`, JSON text, `structuredContent`, and JSON-RPC errors do not expose raw sensitive values.

### Requirement: MCP compatibility suite resource and tool
The DataSpec MCP server SHALL expose the consumer compatibility suite as a local read-only resource and check tool for AI clients.

#### Scenario: List compatibility resource and tool
- **WHEN** an MCP client calls `resources/list` or `tools/list`
- **THEN** the response includes a consumer compatibility suite resource and a `check_consumer_compatibility` tool
- **AND** descriptors identify that the check is local, read-only, does not require a DataSpec server, and does not use external network or LLM calls.

#### Scenario: Run compatibility check tool
- **WHEN** an MCP client calls `check_consumer_compatibility`
- **THEN** the server returns compatibility suite JSON text and `structuredContent` containing status, adapter results, diagnostics, and next actions
- **AND** incompatible results are returned as successful tool content with `status=BREAKING` unless the local suite itself cannot be loaded.
