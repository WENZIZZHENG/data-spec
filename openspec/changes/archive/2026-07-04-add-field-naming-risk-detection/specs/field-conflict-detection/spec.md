## MODIFIED Requirements

### Requirement: Detect duplicate aliases and semantic conflicts
The system SHALL detect common duplicate, conflicting, and SQL-unsafe standard field definitions.

#### Scenario: Alias conflict
- **WHEN** the same alias belongs to multiple fields or an alias equals another field name
- **THEN** the report includes an alias conflict group with involved fields and evidence.

#### Scenario: Semantic duplicate
- **WHEN** multiple fields belong to the same known semantic group such as user id, mobile number, amount, ID card, or order number
- **THEN** the report includes a semantic duplicate group.

#### Scenario: Attribute mismatch
- **WHEN** fields in the same conflict group have different data types, code sets, sensitive flags, or statuses
- **THEN** the report marks the group with a warning or error severity and includes mismatch evidence.

#### Scenario: Reserved or dangerous SQL name
- **WHEN** a field name or alias matches a PostgreSQL, MySQL, or common SQL reserved/dangerous word such as `order`, `user`, `type`, `group`, or `select`
- **THEN** the report includes a naming risk conflict group with dialect evidence and a suggested safer snake_case name.
- **AND** the report remains read-only and does not rename the field.

#### Scenario: Case-sensitive collision
- **WHEN** multiple fields have names or aliases that differ only by case or quoting-sensitive normalization
- **THEN** the report includes a case collision conflict group with involved fields and evidence.

#### Scenario: Ambiguous alias
- **WHEN** an alias can refer to multiple canonical field names or a canonical field name is also used as another field's alias
- **THEN** the report includes an ambiguous alias group that tells AI clients not to use the alias without disambiguation.

### Requirement: Frontend conflict visibility
The frontend SHALL expose field conflict detection from a dedicated page.

#### Scenario: View field conflicts
- **WHEN** a user opens the field conflict page for the current project
- **THEN** the frontend displays summary counts, filter controls, conflict groups, involved fields, evidence, and suggested actions.
- **AND** reserved word, dangerous name, case collision, and ambiguous alias groups use readable labels.

#### Scenario: Edit involved field
- **WHEN** a user clicks an involved field
- **THEN** the frontend routes to the field library with that field selected for editing.

## ADDED Requirements

### Requirement: Naming risk summary
The conflict report SHALL summarize naming risks that are especially relevant to AI-generated SQL and DDL.

#### Scenario: Count naming risks
- **WHEN** the report contains reserved word, dangerous name, case collision, or ambiguous alias groups
- **THEN** the summary includes those groups in total, warning, error, and affected field counts.
- **AND** existing alias conflict and semantic duplicate counts remain compatible.
