# standard-usage-examples Specification

## Purpose
定义项目级标准使用示例和反例的存储、管理与 AI Context 选择规则，让 AI 能复用高价值字段、规则和 SQL 场景样例并避开反模式。
## Requirements
### Requirement: Standard Usage Example Storage
DataSpec SHALL store project-scoped standard usage examples and anti-examples for AI-readable reuse.

#### Scenario: Create field usage example
- **WHEN** a caller creates an enabled `FIELD` scoped `GOOD` example with `projectId`, `fieldId`, `input`, `expectedOutput`, `reason`, `tags`, and `priority`
- **THEN** the system persists the example for that project
- **AND** the response includes the normalized `exampleType`, `scope`, `status`, `tags`, `priority`, and timestamps.

#### Scenario: Create anti-example
- **WHEN** a caller creates a `BAD` example with `antiPattern` and `reason`
- **THEN** the system persists both the anti-pattern and the reason
- **AND** AI-readable exports can distinguish it from `GOOD` examples.

#### Scenario: Reject unsafe example content
- **WHEN** a caller submits an example containing an obvious token, password, secret, or JDBC URL
- **THEN** the system rejects the request with a non-sensitive validation message
- **AND** the unsafe value is not persisted.

### Requirement: Standard Usage Example Management API
DataSpec SHALL expose project-scoped APIs to manage standard usage examples.

#### Scenario: List examples by scope
- **WHEN** a caller lists examples with `projectId`, `scope`, `exampleType`, `status`, `query`, `current`, and `size`
- **THEN** the system returns a paged result sorted by higher priority first and newest update time second
- **AND** only examples belonging to the requested project are returned.

#### Scenario: Update example
- **WHEN** a caller updates an existing example in the same project
- **THEN** the system updates editable fields without changing `projectId`, `id`, or creation time.

#### Scenario: Delete example
- **WHEN** a caller deletes an existing example in the same project
- **THEN** the system soft-deletes it
- **AND** subsequent list and AI Context export calls omit it.

### Requirement: AI Context Usage Example Selection
DataSpec SHALL provide a reusable service-level selection of usage examples for AI Context export.

#### Scenario: Select high-value examples
- **WHEN** AI Context asks for usage examples for a project
- **THEN** the system returns only enabled, non-deleted examples
- **AND** the result is ordered by higher priority first and limited to the requested maximum size.

#### Scenario: Match scoped fields
- **WHEN** AI Context export is scoped to matched standard fields
- **THEN** the selected examples include matching `FIELD` examples for those field IDs or field names
- **AND** the selected examples MAY include `GENERAL`, `RULE`, or `TEMPLATE` examples that match the query text.

### Requirement: Synthetic Usage Example Drafts
DataSpec SHALL treat generated synthetic standard examples as reviewable usage-example drafts rather than automatically persisted project examples.

#### Scenario: Synthetic cases can seed manual review
- **WHEN** a synthetic example package includes `standardQaCases`, `goodSql`, or `badSql`
- **THEN** each generated case contains enough source metadata for a user or AI workflow to convert it into a standard usage example draft after review.

#### Scenario: Synthetic generation does not persist examples
- **WHEN** a caller generates synthetic examples
- **THEN** the standard usage example library remains unchanged
- **AND** generated cases do not appear in list or AI Context export results unless a separate reviewed create flow persists them.
