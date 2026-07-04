# field-format-constraints Specification

## Purpose
TBD - created by archiving change add-field-format-examples. Update Purpose after archive.
## Requirements
### Requirement: Store Field Format Constraints
DataSpec SHALL allow each standard field to store optional value-format constraints for AI-readable reuse.

#### Scenario: Create field with format constraints
- **WHEN** a user creates a field with `formatType`, `formatPattern`, `formatUnit`, `formatPrecision`, `formatTimezone`, `formatNullPolicy`, `validExamplesJson`, `invalidExamplesJson`, and `formatNotes`
- **THEN** the field API SHALL persist and return those values with the field.
- **AND** `validExamplesJson` and `invalidExamplesJson` SHALL be accepted only when they are blank or JSON arrays of strings.

#### Scenario: Update field format constraints
- **WHEN** a user updates an existing field's format constraints
- **THEN** the field API SHALL update those values without changing unrelated field metadata.
- **AND** field change logs and undo SHALL preserve the format constraint fields.

#### Scenario: Optional constraints remain optional
- **WHEN** a field has no format constraints
- **THEN** existing create, update, search, list, backup, snapshot, and reverse-import flows SHALL continue to work.

### Requirement: Frontend Field Format Editing
The frontend SHALL expose field format constraints in the standard field library.

#### Scenario: Edit field format information
- **WHEN** a user opens the create or edit field dialog
- **THEN** the dialog SHALL include controls for format type, pattern, unit, precision, timezone, null policy, valid examples, invalid examples, and notes.
- **AND** valid and invalid examples SHALL be editable as human-friendly line-separated examples while being submitted as JSON arrays.

#### Scenario: View field format summary
- **WHEN** a field has format constraints
- **THEN** the standard field table SHALL show a concise format summary so users can see value-shape metadata without opening the dialog.

