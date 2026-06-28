## MODIFIED Requirements

### Requirement: Provide edit warnings
The system SHALL provide non-blocking edit warnings and what-if inputs for field changes likely to affect downstream outputs.

#### Scenario: Critical field attributes
- **WHEN** a report contains template, SQL check, import source, snapshot, or code set impacts
- **THEN** the report includes warnings for changes to field name, data type, status, code set, or sensitive flag.

#### Scenario: Field what-if preview reuses impact report
- **WHEN** DataSpec previews a field update
- **THEN** the preview uses the field impact report as the source for field impact items and warning attributes.
