## ADDED Requirements

### Requirement: SQL Reverse Import Preview

DataSpec SHALL provide a read-only SQL reverse import preview.

#### Scenario: Preview SQL schema

- **WHEN** a user submits SQL DDL for a project
- **THEN** DataSpec parses tables and columns
- **AND** returns field candidates, missing comments, non-standard fields, and summary counts
- **AND** does not write to the standard field library.

### Requirement: Reverse Import Page

DataSpec SHALL expose a reverse import page for SQL text or `.sql` file input.

#### Scenario: View preview result

- **WHEN** a user submits SQL from the reverse import page
- **THEN** the page displays parsed tables, field candidates, missing comments, and non-standard fields.
