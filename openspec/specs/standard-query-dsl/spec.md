# standard-query-dsl Specification

## Purpose
定义面向标准对象元数据的项目级只读查询 DSL、校验和诊断契约。
## Requirements
### Requirement: Standard Query DSL contract
DataSpec SHALL provide a project-scoped Standard Query DSL for read-only standard object metadata queries.

#### Scenario: Valid field query
- **WHEN** a caller submits a DSL request with `projectId`, `target=FIELD`, optional `text`, supported `filters`, `limit`, and `explain`
- **THEN** DataSpec returns a normalized query, matching field results, query summary, applied filters, ignored filters, result counts, and next query hints
- **AND** the query SHALL NOT modify standards, business files, databases, AI jobs, or local context cache.

#### Scenario: Legacy parameters become DSL
- **WHEN** a caller submits legacy field search parameters such as `query`, `category`, `tag`, `status`, `sensitive`, `sourceBatchId`, and `limit`
- **THEN** DataSpec deterministically maps them to an equivalent Standard Query DSL plan before executing the search.

### Requirement: DSL validation and supported filters
Standard Query DSL SHALL validate filter fields, operators, value types, and bounds before execution.

#### Scenario: Unsupported filter in non-strict mode
- **WHEN** a DSL request includes an unsupported filter and `strict` is false or omitted
- **THEN** DataSpec ignores that filter, records it in `ignoredFilters`, and returns a next query hint that names supported fields.

#### Scenario: Unsupported filter in strict mode
- **WHEN** a DSL request includes an unsupported filter and `strict` is true
- **THEN** DataSpec returns a structured validation error before executing the query.

#### Scenario: Supported field filters
- **WHEN** a DSL request filters fields by `category`, `tag`, `status`, `sensitive`, `sourceBatchId`, `stableRef`, `canonicalRef`, `hasExample`, or `updatedSince`
- **THEN** DataSpec applies each supported filter using allowlisted operators and records the applied filter semantics in the result.

### Requirement: DSL output is bounded and secret-safe
Standard Query DSL SHALL bound query input and redact secret-like values in output, errors, CLI, MCP, and logs.

#### Scenario: Oversized query
- **WHEN** the query text, filter count, filter value count, or limit exceeds the documented bound
- **THEN** DataSpec rejects the request with a readable validation error.

#### Scenario: Secret-like query text
- **WHEN** query text, filter values, ignored filter reasons, diagnostics, or hints contain token, password, Authorization, JDBC URL, DSN, or connection-string-like text
- **THEN** DataSpec output SHALL NOT expose the raw secret-like value.
