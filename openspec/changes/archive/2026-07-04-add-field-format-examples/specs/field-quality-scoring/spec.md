## MODIFIED Requirements

### Requirement: Detect AI-risk metadata gaps
The system SHALL highlight metadata gaps that can mislead AI field reuse.

#### Scenario: Sensitive-looking field not marked sensitive
- **WHEN** a field name, display name, comment, aliases, category, or tags indicate phone, email, id card, password, token, address, or similar sensitive concepts
- **THEN** the report includes a `sensitive_not_marked` issue unless the field is already marked sensitive.

#### Scenario: Status or enum field without code set
- **WHEN** a field looks like a status, type, category, kind, flag, level, or enum field
- **AND** it has no `codeSetId`
- **THEN** the report includes a `code_set_missing` issue.

#### Scenario: Deprecated field without replacement guidance
- **WHEN** a field status is `deprecated` or `disabled`
- **AND** comment, aliases, or tags do not include replacement or migration guidance
- **THEN** the report includes a `deprecated_without_replacement` issue.

#### Scenario: Format-sensitive field lacks format examples
- **WHEN** a field looks like an amount, phone, email, timestamp, date, JSON, status, enum, or code field
- **AND** the field lacks both structured format constraints and valid examples
- **THEN** the report includes a `format_examples_missing` issue with a machine-readable suggested action.
