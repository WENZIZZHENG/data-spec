# business-object-table-standards Specification

## Purpose
定义业务对象标准、表模板和字段要求之间的项目级映射与消费契约。
## Requirements
### Requirement: Business Object Standards
DataSpec SHALL allow a project to maintain lightweight business object standards that describe how a business entity maps to table templates and field requirements.

#### Scenario: Create and list business objects
- **WHEN** a user creates a business object standard with `projectId`, `objectKey`, `entityName`, `tablePattern`, optional `templateId`, `requiredFields`, `optionalFields`, `relations`, `foreignKeyHints`, `auditFields`, `commonPitfalls`, `aiUsageNotes`, and `contextExport`
- **THEN** DataSpec persists the object under the project and returns the same stable fields with timestamps and enabled status
- **AND** listing business objects for the project returns only objects visible to that project.

#### Scenario: Validate object identity
- **WHEN** a user creates or updates a business object standard
- **THEN** `objectKey` and `entityName` MUST be non-empty and unique within the project
- **AND** optional `templateId` MUST reference a table template owned by the same project.

#### Scenario: Keep relation data lightweight
- **WHEN** a business object includes `relations` or `foreignKeyHints`
- **THEN** each relation or hint SHALL use structured fields such as source object, target object, source columns, target columns, relation type, optionality, confidence, and notes
- **AND** raw SQL, database credentials, JDBC URLs, DSNs, tokens, Authorization headers, or sampled business rows MUST NOT be accepted as relation content.

### Requirement: Table Structure Standard
DataSpec SHALL allow a table template to carry table-level structure standards in addition to template fields.

#### Scenario: Save table structure standard
- **WHEN** a user updates a table template with `primaryKey`, `uniqueKeys`, `indexes`, `foreignKeys`, `checkHints`, `auditPolicy`, `softDeletePolicy`, `dialectNotes`, and `aiUsageNotes`
- **THEN** DataSpec stores those values as the template structure standard without removing existing template fields
- **AND** the template detail response includes the structure standard in additive fields.

#### Scenario: Validate table constraints
- **WHEN** a table structure standard references columns
- **THEN** every referenced column MUST match a template field name or a selected standard field name for the same template
- **AND** constraint names, index names, relation names, and column names MUST use the same safe identifier boundary as DDL preview.

#### Scenario: Preserve optional policy semantics
- **WHEN** `auditPolicy`, `softDeletePolicy`, `dialectNotes`, or `checkHints` are configured
- **THEN** DataSpec SHALL expose them as AI/lint guidance unless they can be safely represented as structured DDL
- **AND** DataSpec SHALL NOT execute a database migration or rewrite existing database tables.

### Requirement: Business Object Relation Summary
DataSpec SHALL provide a read-only relation summary for business objects and table templates.

#### Scenario: Show relation graph summary
- **WHEN** a caller requests the business object relation summary for a project
- **THEN** DataSpec returns object nodes, template nodes, field nodes, and relation edges derived from business objects, table template fields, and foreign key hints
- **AND** each edge includes a source, target, relation kind, optional confidence, and evidence text.

#### Scenario: Summary remains optional
- **WHEN** a project has no business object standards or no relation hints
- **THEN** DataSpec returns an empty valid relation summary
- **AND** existing DDL preview, field catalog, and template list calls continue to work.

### Requirement: Table Standards Read API
DataSpec SHALL provide a read-only table standards API for CLI, MCP, and AI clients.

#### Scenario: Read table standards as JSON
- **WHEN** a caller requests `GET /api/table-standards?projectId=<id>`
- **THEN** DataSpec returns the same stable shape as AI Context `.dataspec/table-standards.json`
- **AND** the response includes business objects, templates, relation edges, summary, safety metadata, and next actions.

#### Scenario: Scope table standards by template or business object
- **WHEN** a caller passes `templateId` or `businessObject`
- **THEN** DataSpec applies `scope=table-template` or `scope=business-object` semantics to the returned table standards JSON
- **AND** the API remains read-only and does not generate DDL, connect to a business database, or write project state.

### Requirement: Frontend Maintenance Flow
The frontend SHALL expose a minimal maintenance flow for business object and table structure standards.

#### Scenario: Maintain structure standard from template page
- **WHEN** a user opens the template management page for a project
- **THEN** the page allows viewing or editing the business object association, primary key, unique keys, indexes, foreign key hints, audit policy, soft delete policy, dialect notes, and AI usage notes for a template
- **AND** the page keeps existing template field list and field selection behavior available.

#### Scenario: Preview relation and DDL evidence
- **WHEN** a user previews DDL for a template with table structure standards
- **THEN** the page displays generated DDL, lint summary, structure standard summary, and a simple relation graph or edge list
- **AND** the page does not present generated DDL as already applied to any database.
