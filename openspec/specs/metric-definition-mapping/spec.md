# metric-definition-mapping Specification

## Purpose
定义业务指标与标准字段、过滤条件、聚合规则和时间粒度之间的轻量映射。
## Requirements
### Requirement: Metric Definition Mapping
DataSpec SHALL store lightweight project-scoped metric definitions that map business metrics to standard fields, filters, aggregation rules, and time grains.

#### Scenario: Create metric definition
- **WHEN** a caller creates a metric definition with `projectId`, `metricKey`, `displayName`, `definition`, measure fields, dimension fields, filter rule, aggregation rule, time grain, owner notes, example SQL, status, and evidence refs
- **THEN** DataSpec persists the definition under the project
- **AND** all referenced standard fields MUST belong to the same project
- **AND** example SQL and notes MUST be sanitized or rejected when they contain obvious secrets, credentials, JDBC URLs, DSNs, tokens, Authorization headers, or sampled business rows.

#### Scenario: List metric definitions
- **WHEN** a caller lists metric definitions by project, query, status, fieldId, or metricKey
- **THEN** DataSpec returns only definitions from the requested project
- **AND** each item includes enough field references and business definition text for AI clients to explain the metric boundary.

### Requirement: Metric Mapping AI Guidance
DataSpec SHALL expose metric definitions in standard question, AI Context, field knowledge cards, and data dictionary outputs.

#### Scenario: Field card references metrics
- **WHEN** a standard field is used by one or more metric definitions
- **THEN** its field knowledge card includes metric references with metricKey, displayName, aggregation rule, filter rule, time grain, and role as measure or dimension.

#### Scenario: AI Context exports metric definitions
- **WHEN** AI Context is generated for a project with metric definitions
- **THEN** the package includes a bounded metric mapping artifact
- **AND** the manifest records the artifact, count, schema version, and truncation status.

#### Scenario: Metric definition does not execute SQL
- **WHEN** a metric definition includes example SQL
- **THEN** DataSpec treats the SQL as explanatory text only
- **AND** it does not connect to a business database or validate result correctness.
