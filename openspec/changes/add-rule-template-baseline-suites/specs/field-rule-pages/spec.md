## ADDED Requirements

### Requirement: Rule baseline respects current project boundary
The rule config page SHALL require a selected current project before baseline operations are submitted.

#### Scenario: No project selected for baseline action
- **WHEN** no current project is selected
- **THEN** baseline list can be viewed if available
- **AND** apply, import, and export actions are disabled or show an actionable empty state
- **AND** no API call is submitted without `projectId`
