# dashboard Specification

## Purpose
TBD - created by archiving change add-personal-dashboard. Update Purpose after archive.
## Requirements
### Requirement: Project Dashboard Summary

DataSpec SHALL provide a project-level dashboard summary for personal use.

#### Scenario: Query dashboard summary

- **WHEN** a user queries dashboard summary by project
- **THEN** DataSpec returns standard field count, enum dictionary count, rule count, forbidden term count, recent SQL check count, field hit rate, recent checks, and issue trend.

### Requirement: Dashboard Page

DataSpec SHALL expose a Dashboard page as the default entry.

#### Scenario: Open app root

- **WHEN** a user opens the app root route
- **THEN** the app navigates to Dashboard
- **AND** Dashboard refreshes when the current project changes.
