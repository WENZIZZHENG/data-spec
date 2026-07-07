# field-model Specification

## Purpose
Defines the standard field metadata contract used by DataSpec for personal and small-team field standards.

## Requirements
### Requirement: Standard Field Metadata
The system SHALL store personal-use metadata and optional usage contract guidance for each standard field.

#### Scenario: Create field with AI metadata
- **WHEN** a user creates a standard field with aliases, sensitivity, status, code set, category, example value, and optional usage contract values
- **THEN** the system persists those metadata fields with the field

#### Scenario: Default metadata
- **WHEN** a user creates a field without sensitivity, status, or usage contract values
- **THEN** the field defaults to `sensitive=false`
- **AND** the field defaults to `status=enabled`
- **AND** usage contract fields remain empty and compatible with existing clients

### Requirement: Field Catalog AI Export
The system SHALL include standard field metadata and usage contract guidance in AI field catalog exports.

#### Scenario: Export field aliases and governance hints
- **WHEN** AI context field catalog is generated
- **THEN** each field includes aliases as an array when aliases exist
- **AND** it includes `sensitive`, `status`, `codeSetId`, and `example` when available
- **AND** it includes an additive `usageContract` object when at least one usage contract value is present
