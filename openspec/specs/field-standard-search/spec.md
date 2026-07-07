# field-standard-search Specification

## Purpose
定义项目级标准字段搜索能力，通过关键词、语义别名和结构化过滤返回 AI 可读的匹配理由、推荐用法和下一步动作。
## Requirements
### Requirement: Field standard search API

DataSpec SHALL provide a project-scoped field standard search API that returns AI-readable search results without modifying field standards.

#### Scenario: Search by keyword and semantic aliases

- **WHEN** a caller searches fields with a projectId and a non-empty query such as "用户手机号" or "sfzh"
- **THEN** the response contains matched standard fields ordered by deterministic score
- **AND** each item includes the field, score, matchReasons, recommendedUse, and nextActions
- **AND** aliases, display name, comment, tags, category, and built-in semantic groups can contribute to matchReasons

#### Scenario: Search by structured filters

- **WHEN** a caller searches with category, tag, status, sensitive, or sourceBatchId filters
- **THEN** the response only contains fields that match those filters
- **AND** the response includes a summary describing total candidates, returned count, applied filters, and missing or truncated hints

#### Scenario: Empty or unsafe search

- **WHEN** a caller omits query and all structured filters
- **THEN** the API returns a validation error instead of dumping the whole field catalog

### Requirement: CLI and MCP search access

DataSpec SHALL expose field standard search through CLI and MCP with stable JSON outputs.

#### Scenario: CLI searches field standards

- **WHEN** an AI agent runs a field search command with projectId, query, and optional filters
- **THEN** stdout contains the same stable search result JSON shape as the API
- **AND** stderr uses the existing DataSpecError diagnostic contract on API failure

#### Scenario: MCP tool searches field standards

- **WHEN** an MCP client calls the field search tool with projectId, query, and optional filters
- **THEN** the tool result contains structuredContent with the stable search result
- **AND** the text content is parseable JSON

### Requirement: Frontend field library search reuse

DataSpec SHALL reuse field standard search in the field library when search conditions are present.

#### Scenario: Field library displays match reasons

- **WHEN** a user searches the field library by keyword or filter
- **THEN** the page can display matched fields with matchReasons and recommended next actions
- **AND** clearing search conditions restores the existing paginated field list behavior

### Requirement: Field search usage contract evidence
Field standard search SHALL return field usage contract evidence when it helps explain why a field should or should not be used.

#### Scenario: Search result includes usage guidance
- **WHEN** a caller searches fields and a matched field has preferred use cases, avoid conditions, join hints, default filters, aggregation hints, replacement guidance, or misuse examples
- **THEN** each matching search item includes a concise usage contract summary
- **AND** existing field, score, matchReasons, recommendedUse, and nextActions remain compatible

#### Scenario: Search query matches avoid condition
- **WHEN** a search query or structured question matches a field avoid condition or misuse example
- **THEN** the search result includes a next action that requires confirmation before using that field
- **AND** the field is not described as directly safe for that scenario
