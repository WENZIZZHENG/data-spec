# data-dictionary Specification

## Purpose
TBD - created by archiving change enhance-data-dictionary-markdown. Update Purpose after archive.
## Requirements
### Requirement: Enhanced Markdown Data Dictionary
The system SHALL generate a richer Markdown data dictionary from project standards.

#### Scenario: Include project overview
- **WHEN** a client previews or downloads the Markdown data dictionary
- **THEN** the document includes counts for data domains, standard fields, enum dictionaries, and table templates

#### Scenario: Include field metadata and domain relation
- **WHEN** standard fields are present
- **THEN** each field row includes its domain relation and personal metadata such as aliases, category, sensitivity, status, code set, and example value

#### Scenario: Include enum value type
- **WHEN** enum dictionaries are present
- **THEN** each enum section includes its value type and values

#### Scenario: Include table templates
- **WHEN** table templates are present
- **THEN** the document includes each template and its fields with required, nullable, default value, sort order, and linked standard field information
