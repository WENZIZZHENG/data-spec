## ADDED Requirements

### Requirement: Data Dictionary Semantic Sections
The Markdown data dictionary SHALL include concise field semantic, enum lifecycle, knowledge card, naming translation, and metric mapping summaries.

#### Scenario: Field rows include semantic summary
- **WHEN** standard fields have semantic rules, naming guidance, or knowledge card risk notes
- **THEN** the data dictionary includes concise semantic summary columns or subsections for those fields
- **AND** it avoids dumping oversized card content into compact tables.

#### Scenario: Enum dictionary includes lifecycle
- **WHEN** enum values have status, aliases, replacement values, validity windows, or mapping hints
- **THEN** each enum section includes that lifecycle information in a human-readable way.

#### Scenario: Metric definitions are documented
- **WHEN** a project has metric definitions
- **THEN** the data dictionary includes metricKey, displayName, definition, measure fields, dimensions, filter rule, aggregation rule, time grain, and example SQL summary
- **AND** example SQL is clearly labeled as explanatory guidance only.
