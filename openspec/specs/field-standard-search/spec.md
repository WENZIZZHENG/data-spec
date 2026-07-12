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

### Requirement: Field search returns stable reference evidence
Field standard search SHALL return stable references and explain historical-name resolution.

#### Scenario: Search result includes stable refs
- **WHEN** field search returns a match
- **THEN** the item SHALL include `stableRef`, `canonicalRef`, and lifecycle status
- **AND** existing score, matchReasons, recommendedUse, usage contract, and nextActions SHALL remain compatible.

#### Scenario: Search matches alias history
- **WHEN** a query matches an alias or historical field name
- **THEN** search SHALL identify the matched alias or historical ref
- **AND** it SHALL return the current field stableRef and replacement warning when applicable.

### Requirement: Field search accepts Standard Query DSL
Field standard search SHALL accept additive Standard Query DSL input without removing existing query and filter parameters.

#### Scenario: Search with DSL
- **WHEN** a caller searches fields with a Standard Query DSL targeting `FIELD`
- **THEN** the response contains the same field item shape as existing field search
- **AND** each returned item preserves field, score, matchReasons, recommendedUse, usageContractSummary, evidence, stableRef, canonicalRef, lifecycleStatus, and matchedAlias.

#### Scenario: Search summary includes DSL explanation
- **WHEN** a field search executes through DSL or legacy parameters mapped to DSL
- **THEN** the response summary includes querySummary, appliedFilters, ignoredFilters, resultCount, returnedCount, truncated, and nextQueryHints.

### Requirement: Field search DSL remains compatible
Existing field search clients SHALL remain compatible when DSL metadata is added.

#### Scenario: Existing GET field search still works
- **WHEN** a caller uses existing `/api/fields/search` query parameters
- **THEN** DataSpec returns the existing stable search result fields
- **AND** additive DSL explanation fields do not change previous field search semantics.

### Requirement: Field search semantic card evidence
Field standard search SHALL use semantic rules, naming translations, enum lifecycle, and knowledge card summaries when explaining search results.

#### Scenario: Search uses preferred and forbidden translations
- **WHEN** a user or AI searches with a localized term, translation alias, preferred English name, or forbidden translation
- **THEN** field search includes translation match reasons and warnings where applicable
- **AND** forbidden translations do not boost a field as a direct safe recommendation.

#### Scenario: Search result includes semantic summary
- **WHEN** a matching field has semantic rules, enum lifecycle hints, metric references, or knowledge card risk notes
- **THEN** each search item includes a concise semantic summary or next action
- **AND** existing field, score, matchReasons, recommendedUse, usageContractSummary, evidence, stableRef, canonicalRef, lifecycleStatus, and matchedAlias remain compatible.

#### Scenario: Search avoids noisy full knowledge card dumps
- **WHEN** search returns multiple fields
- **THEN** DataSpec returns bounded card summaries and links or ids for detail lookup rather than embedding every full card.
