# mcp-agent-guidance-pack Specification

## Purpose
定义 MCP Agent guidance pack 和资源模板，让 AI 客户端在本地、只读、脱敏的前提下获取常见工作流的输入要求、工具顺序、停止条件和证据要求。
## Requirements
### Requirement: MCP agent guidance pack
DataSpec SHALL expose a local MCP agent guidance pack resource for common AI workflows.

#### Scenario: List guidance pack resource
- **WHEN** an MCP client calls `resources/list` with a configured project
- **THEN** the response includes `dataspec://project/<id>/agent-guidance-pack` with JSON mime type.

#### Scenario: Read guidance pack resource
- **WHEN** an MCP client reads the agent guidance pack resource
- **THEN** the server returns JSON text and `structuredContent` containing guidance templates with required inputs, safe defaults, resource sequence, tool sequence, stop conditions, evidence requirements, and next actions.

#### Scenario: Guidance pack is local and safe
- **WHEN** the guidance pack is listed or read
- **THEN** the MCP server does not call backend APIs, execute MCP tools, connect to source databases, or include raw token, password, Authorization header, JDBC URL, DSN, connection string, or source database rows.

### Requirement: MCP resource templates
DataSpec SHALL expose MCP resource template descriptors for project-scoped AI guidance resources.

#### Scenario: List resource templates
- **WHEN** an MCP client calls `resources/templates/list`
- **THEN** the response includes resource templates for session bootstrap, capability catalog, schema registry, field catalog, workflow recipes, AI task profiles, and the agent guidance pack.

#### Scenario: Resource templates are contract checked
- **WHEN** a resource template URI, description, or mime type drifts without updating contract fixtures
- **THEN** the local CLI/MCP contract fixture check fails with a diagnostic naming the affected resource template.
