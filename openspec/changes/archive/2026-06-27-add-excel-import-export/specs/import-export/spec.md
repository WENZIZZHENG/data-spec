## ADDED Requirements

### Requirement: Excel Field And Code Set Template

DataSpec SHALL provide an `.xlsx` template for maintaining standard fields, enum dictionaries, and enum values.

#### Scenario: Download template

- **WHEN** a user downloads the Excel template
- **THEN** the workbook contains `fields`, `enum_dicts`, and `enum_values` sheets
- **AND** each sheet contains the documented required headers.

### Requirement: Excel Import Preview

DataSpec SHALL preview Excel imports before writing data.

#### Scenario: Preview add and update operations

- **WHEN** a user uploads an `.xlsx` file for a project
- **THEN** DataSpec reports created and updated counts for fields, enum dictionaries, and enum values
- **AND** reports validation errors for missing required columns, duplicate rows, unknown domain codes, or unknown code set codes.

### Requirement: Excel Import Apply

DataSpec SHALL apply a valid Excel import by upserting project data.

#### Scenario: Apply valid workbook

- **WHEN** the workbook has no preview errors
- **THEN** DataSpec upserts enum dictionaries first
- **AND** upserts enum values
- **AND** upserts fields with aliases, category, sensitive flag, status, example value, and code set association.

### Requirement: Excel Export

DataSpec SHALL export project fields and code sets as `.xlsx`.

#### Scenario: Export project workbook

- **WHEN** a user exports a project
- **THEN** the workbook includes standard fields, enum dictionaries, and enum values in the same template-compatible sheet format.
