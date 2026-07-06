## ADDED Requirements

### Requirement: Include business code reference impacts
The field impact report SHALL support business code reference impacts as a distinct impact source.

#### Scenario: Field impact report includes code reference summary
- **WHEN** business code reference summary data is available for a standard field
- **THEN** the field impact report includes a `CODE_REFERENCE` impact item.
- **AND** the item metadata includes non-sensitive reference counts, highest confidence, highest rename risk, and representative relative file paths.

#### Scenario: Field impact summary counts code references
- **WHEN** a field impact report includes one or more `CODE_REFERENCE` items
- **THEN** the summary includes `codeReferenceImpactCount`.
- **AND** total impact count includes the business code reference impact items.

### Requirement: Frontend displays business code reference impact
The frontend SHALL display business code reference impacts in the field impact dialog.

#### Scenario: View code reference impact in field library
- **WHEN** a user opens the field impact dialog and the report contains `CODE_REFERENCE` impacts
- **THEN** the frontend labels them as business code references.
- **AND** the summary area shows the code reference count without requiring the browser to scan local files.
