## ADDED Requirements

### Requirement: Excel dry-run row details
Excel import preview SHALL return row-level dry-run details for fields, enum dictionaries, and enum values.

#### Scenario: Preview creates row detail
- **WHEN** an Excel row is valid and represents a new item
- **THEN** the preview includes an item with action `CREATE` and status `READY`

#### Scenario: Preview updates row detail
- **WHEN** an Excel row is valid and matches an existing item
- **THEN** the preview includes an item with action `UPDATE` and status `READY`

#### Scenario: Preview conflict row detail
- **WHEN** an Excel row has a conflict such as duplicate key or unknown reference
- **THEN** the preview includes an item with action `CONFLICT`, status `BLOCKED`, and a readable reason

### Requirement: Excel dry-run field diffs
Excel import preview SHALL include field-level before/after diffs for update rows and submitted values for create rows.

#### Scenario: Field update diff
- **WHEN** a field row changes an existing field attribute
- **THEN** the preview item includes a diff entry with field name, before value, and after value

#### Scenario: Create submitted values
- **WHEN** a row creates a new field, enum dictionary, or enum value
- **THEN** the preview item includes diff entries showing submitted values as after values

### Requirement: Frontend dry-run details
The import/export frontend SHALL display dry-run details below the summary.

#### Scenario: View preview details
- **WHEN** preview returns row-level items
- **THEN** the page shows sheet, row number, key, action, status, reason, and field diffs

#### Scenario: View blocked conflicts
- **WHEN** preview contains blocked items
- **THEN** the page makes the conflict reason visible without requiring the user to inspect raw JSON
