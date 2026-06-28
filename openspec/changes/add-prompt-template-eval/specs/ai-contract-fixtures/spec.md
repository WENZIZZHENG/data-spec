## ADDED Requirements

### Requirement: Prompt template contracts have fixture coverage
The system SHALL verify prompt template registry and prompt output constraints through existing backend validation.

#### Scenario: Prompt template metadata changes
- **WHEN** a prompt template key, version, required section, required phrase, or output format changes
- **THEN** backend tests detect incompatible or incomplete registry metadata.

#### Scenario: Prompt output contract changes
- **WHEN** generated create-table or fix-sql prompt output loses a required section, required phrase, or promptVersion marker
- **THEN** backend tests fail with a readable assertion.

#### Scenario: Golden prompt output changes
- **WHEN** generated prompt text changes from the checked-in golden fixture
- **THEN** backend tests fail and report a readable diff for review.
