# field-suggestion Specification

## Purpose
定义字段建议能力，为业务描述或候选字段返回项目内已有标准字段匹配、确定性语义推荐和新字段命名兜底，并暴露 API、CLI 和 MCP 入口。
## Requirements
### Requirement: Field Suggestion API
The system SHALL recommend standard field candidates for a business description in a project.

#### Scenario: Suggest existing field by alias or description
- **WHEN** a client requests field suggestions with `projectId` and `query`
- **THEN** the system returns ranked candidates from the project's standard fields
- **AND** each candidate includes the field, score, match reason, recommended field name, and whether it already exists

#### Scenario: Exclude disabled fields
- **WHEN** a field is marked `disabled`
- **THEN** it is not returned as an existing field suggestion

#### Scenario: Unknown query fallback
- **WHEN** no existing field is a meaningful match
- **THEN** the system returns a fallback candidate with a generated snake_case `recommendedName`
- **AND** `existing` is false

### Requirement: CLI Field Suggestion
The CLI SHALL expose field suggestion as a JSON command for AI and CI workflows.

#### Scenario: Suggest field via CLI
- **WHEN** a user runs `suggest-field <query> --project <id> --format json`
- **THEN** the CLI calls the field suggestion API and prints the JSON result

### Requirement: MCP Field Suggestion Tool
The MCP server SHALL expose field suggestion as a tool.

#### Scenario: Suggest fields via MCP
- **WHEN** an MCP client calls `suggest_fields` with `query`
- **THEN** the server calls the field suggestion API and returns structured JSON content

### Requirement: Semantic Field Suggestion Quality
The field suggestion API SHALL use deterministic semantic matching to improve recommendations for common personal and small-team database field descriptions without calling an external LLM.

#### Scenario: Recommend by Chinese synonym or pinyin abbreviation
- **WHEN** a client requests suggestions with a Chinese synonym, English alias, or pinyin abbreviation for a known semantic group
- **THEN** the system returns matching standard fields ranked ahead of less specific generic fields
- **AND** the match reason explains the semantic keyword that caused the recommendation

#### Scenario: Penalize generic-only matches
- **WHEN** a query only shares generic business words such as user, order, amount, status, time, or date with a field
- **THEN** the system gives that candidate a lower score than candidates that match a more specific semantic keyword

#### Scenario: Sensitive field explanation
- **WHEN** a recommended existing field is marked sensitive
- **THEN** its match reason includes a sensitive-field hint while keeping the existing response structure compatible

#### Scenario: Standard fallback names
- **WHEN** no existing field is a meaningful match but the query contains a known semantic group
- **THEN** the fallback candidate uses the canonical standard snake_case name for that semantic group
- **AND** the fallback remains marked as `existing=false`

### Requirement: Field suggestion naming and semantic guidance
Field suggestion SHALL use naming translation guidance and field semantic rules to improve deterministic recommendations.

#### Scenario: Suggest canonical field from translation guidance
- **WHEN** a suggestion query contains a Chinese term, English alias, or translation alias maintained on a standard field
- **THEN** DataSpec ranks the canonical standard field ahead of generic fallback names
- **AND** the match reason explains the translation guidance source.

#### Scenario: Suggest fallback avoids forbidden translation
- **WHEN** no existing field is a meaningful match but the query contains a forbidden translation
- **THEN** the fallback candidate avoids that forbidden name
- **AND** next actions explain which preferred English name or canonical field should be considered.

#### Scenario: Suggestion includes semantic caution
- **WHEN** a matching field has source-of-truth, unit conversion, enum lifecycle, or metric-boundary warnings
- **THEN** DataSpec includes a concise caution in match reason or next actions without changing the existing suggestion response shape.

### Requirement: Field suggestion can explain historical-name recall
Field suggestion SHALL use auditable project-scoped field history to recall a current enabled standard field without recommending a historical name as the new canonical name.

#### Scenario: Suggest current field from historical name
- **WHEN** a suggestion query matches a historical name for a current enabled field
- **THEN** DataSpec recommends the field's current name
- **AND** the match reason and evidence identify that the input matched field history.

#### Scenario: Historical match points to a non-enabled field
- **WHEN** a historical value only matches a draft, deprecated, or disabled field
- **THEN** existing lifecycle filtering remains in effect
- **AND** DataSpec does not promote that historical value as a safe current recommendation.

### Requirement: Field suggestion uses shared query tokens
Field suggestion SHALL normalize the query once through deterministic name tokenization and SHALL use the resulting direct tokens, resolved glossary expansions, and historical evidence consistently for scoring and fallback.

#### Scenario: Suggest from resolved abbreviation
- **WHEN** `ord_amt` contains exact abbreviations that current-project glossary entries resolve without ambiguity
- **THEN** suggestions use the configured canonical fields or terms instead of treating `ord_amt` as one unknown name
- **AND** each suggestion exposes additive query token evidence and the glossary source.

#### Scenario: Ambiguous abbreviation cannot choose a field
- **WHEN** an abbreviation token is ambiguous or disabled
- **THEN** suggestion does not promote a glossary canonical field from that token
- **AND** fallback reason and token evidence request confirmation or glossary correction.

#### Scenario: Existing direct match keeps priority
- **WHEN** an existing field current name or current alias matches directly while another field only matches an expanded token
- **THEN** the existing direct match keeps its current higher priority when other inputs are equal.
