# stable-standard-references Specification

## Purpose
定义标准字段、枚举、规则和快照可跨名称变化稳定引用的项目级标识契约。
## Requirements
### Requirement: Standard objects expose project-scoped stable references
DataSpec SHALL expose additive stable reference metadata for standard fields, enum code sets, rules, and standard snapshots without replacing their existing identifiers or names.

#### Scenario: Field stable reference
- **WHEN** a field is returned by a stable-reference-aware API, AI Context, search result, or evidence package
- **THEN** it SHALL include `stableRef` and `canonicalRef`
- **AND** the field reference SHALL remain tied to the same project field object when its name, display name, aliases, or lifecycle status changes.

#### Scenario: Enum rule and snapshot references
- **WHEN** enum code sets, project rules, or standard snapshots are exposed to AI-oriented consumers
- **THEN** DataSpec SHALL provide deterministic project-scoped references for those objects
- **AND** existing object IDs, codes, versions, and names SHALL remain compatible.

### Requirement: Resolve current historical and lifecycle references
DataSpec SHALL provide a project-scoped reference resolution contract for stable refs, current names, aliases, historical names, deprecated refs, and replacement refs.

#### Scenario: Resolve current or alias reference
- **WHEN** a caller resolves a current name, alias, historical name, or stableRef that uniquely identifies one standard object
- **THEN** the result SHALL include `inputRef`, `refType`, `resolutionStatus`, `stableRef`, `canonicalRef`, `objectId`, `currentName`, `matchedAlias`, `lifecycleStatus`, `confidence`, `evidenceLinks`, and `warnings`
- **AND** `resolutionStatus` SHALL identify a current match without changing project state.

#### Scenario: Resolve deprecated or replaced field
- **WHEN** a reference identifies a deprecated, disabled, merged, or replaced field
- **THEN** the result SHALL report a stale lifecycle state
- **AND** it SHALL include `replacementRef` or a readable replacement warning when available.

#### Scenario: Ambiguous or unknown reference
- **WHEN** a reference matches multiple objects or no object in the selected project
- **THEN** the result SHALL use `AMBIGUOUS` or `UNKNOWN`
- **AND** it SHALL NOT guess a canonical object.

### Requirement: Stable reference resolution remains bounded and safe
Reference resolution SHALL be read-only, project-scoped, and secret-safe.

#### Scenario: Reject cross-project reference
- **WHEN** a stableRef belongs to a different project than the requested project
- **THEN** DataSpec SHALL reject or mark the reference as cross-project
- **AND** it SHALL NOT expose the other project's object metadata.

#### Scenario: Redact unsafe reference text
- **WHEN** input refs, aliases, warnings, or evidence labels contain token, password, Authorization, JDBC URL, DSN, or connection-string-like text
- **THEN** returned JSON, CLI output, MCP output, logs, and diagnostics SHALL NOT contain the raw secret-like value.

### Requirement: Historical field references come from auditable change records
DataSpec SHALL derive historical field names and aliases from project-scoped field change records without changing the field stableRef or inventing history that cannot be traced to a stored snapshot.

#### Scenario: Resolve a renamed field by historical name
- **WHEN** a caller resolves a field name that appears in an existing change-log snapshot for a current field in the selected project
- **THEN** DataSpec returns that field's current `stableRef` and `canonicalRef`
- **AND** `matchedAlias` identifies the historical value and `evidenceLinks` includes the field and source change-log references.

#### Scenario: Historical name is ambiguous
- **WHEN** the same normalized historical name is traced to more than one current field in the selected project
- **THEN** DataSpec returns `AMBIGUOUS` without selecting a canonicalRef
- **AND** the response contains only project-scoped, secret-safe candidate evidence.

#### Scenario: Change history cannot be parsed
- **WHEN** a field change record has missing, malformed, or unsupported snapshot content
- **THEN** DataSpec ignores that record for historical-name matching
- **AND** current names, current aliases, and stableRef resolution continue to work.
