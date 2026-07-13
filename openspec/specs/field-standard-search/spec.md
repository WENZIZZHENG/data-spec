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

### Requirement: Field search returns auditable historical-name matches
Field standard search SHALL use project-scoped field change records as a deterministic historical-name source while keeping current names and current aliases higher priority.

#### Scenario: Search by a previous field name
- **WHEN** a query matches a historical field name or alias from an existing change-log snapshot
- **THEN** search returns the current field with its current `stableRef` and `canonicalRef`
- **AND** `matchedAlias`, match reasons, and evidence identify the historical value and source change log.

#### Scenario: Current name competes with historical name
- **WHEN** one field matches the current name or alias and another field only matches a historical value
- **THEN** the current-name or current-alias match ranks above the historical-only match when other scoring inputs are equal.

### Requirement: Field search explains deterministic query normalization
Field standard search SHALL use the same deterministic query normalization as field suggestion and SHALL expose additive bounded query token evidence in the search summary and matching field evidence.

#### Scenario: Search an acronym and number name
- **WHEN** a caller searches for `HTTPStatus2Code`
- **THEN** search evaluates the ordered `http`, `status`, `2`, and `code` tokens rather than only `httpstatus2` and `code`
- **AND** the summary explains direct, resolved, unresolved, or unit token states.

#### Scenario: Search a Chinese glossary phrase
- **WHEN** a query contains a longest current-project glossary match such as `会员手机号`
- **THEN** fields bound to that canonical glossary meaning receive deterministic glossary evidence
- **AND** shorter overlapping terms do not independently create a competing high-confidence match at the same position.

#### Scenario: Search reports token ambiguity
- **WHEN** normalization finds an ambiguous or disabled token
- **THEN** search summary hints and query token evidence expose the conservative resolution
- **AND** no field is ranked from a guessed canonical expansion.

### Requirement: Field search supports compatible pagination
Field standard search SHALL support bounded page navigation without changing existing limit-only clients.

#### Scenario: Search a requested page
- **WHEN** a caller sends `current` and/or `size` with valid field search conditions
- **THEN** the API returns only the deterministically ordered items for that page
- **AND** additive page metadata includes current, size, total, pages, hasPrevious, and hasNext
- **AND** a page after the first continues from the same score-descending, name-ascending, and field-ID-ascending order.

#### Scenario: Navigate beyond fifty matches
- **WHEN** a search matches more than 50 fields and the caller requests later pages
- **THEN** every matching field remains reachable through page navigation
- **AND** no fixed legacy limit silently makes later matches inaccessible.

#### Scenario: Preserve legacy limit behavior
- **WHEN** an existing API, CLI, or MCP caller sends `limit` without `current` or `size`
- **THEN** the response keeps the existing first-N item behavior and maximum limit
- **AND** existing result fields retain their meaning while pagination metadata remains optional.

#### Scenario: Explicitly include every lifecycle status
- **WHEN** the field library requests a filtered page while its status selector is set to all statuses
- **THEN** it explicitly requests all lifecycle statuses and receives matching draft, deprecated, disabled, and enabled fields
- **AND** callers that omit this additive option keep the existing enabled-default behavior.

### Requirement: Field library uses server-side result windows
DataSpec Web SHALL render field-library list and search results from server-side pages rather than loading the full catalog for browser pagination.

#### Scenario: Browse fields without filters
- **WHEN** a user opens or pages through the field library without search conditions
- **THEN** the page calls the existing paginated field API with current and size
- **AND** the table renders only the returned records while pagination uses the server total.

#### Scenario: Browse filtered fields
- **WHEN** a user enters a keyword or selects status, domain, category, tag, ungrouped, or source batch filters
- **THEN** the page requests the corresponding search page from the server
- **AND** changing page or page size retrieves a new server page without client-side slicing.

#### Scenario: Debounce keyword requests and ignore stale responses
- **WHEN** a user changes the keyword several times within the debounce window
- **THEN** the page submits only the final settled keyword request
- **AND** an older response cannot replace results from a newer request.

#### Scenario: Show a slow request state
- **WHEN** a field page or search request exceeds the slow-state threshold
- **THEN** the page shows a non-sensitive accessible loading status without resizing the table or pagination controls
- **AND** the status clears when the current request completes.

#### Scenario: Load full candidate options only when needed
- **WHEN** a user only browses, searches, filters, or pages the field table
- **THEN** the page does not request the full field catalog for replacement or merge options
- **AND** those options are loaded and cached only after the user opens a workflow that requires cross-page candidates.

#### Scenario: Browser regression covers more than fifty results
- **WHEN** the browser regression uses a deterministic dataset with more than 50 matching fields
- **THEN** it verifies later-page fields are reachable, earlier-page fields are not duplicated, and request parameters reflect the selected page
- **AND** it verifies continuous keyword input is debounced to the final search request.
