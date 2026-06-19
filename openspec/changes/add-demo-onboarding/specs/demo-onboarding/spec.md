## ADDED Requirements

### Requirement: Demo Project Creation
DataSpec SHALL provide an API to create or reuse a demo project with seed standards for first-time use.

#### Scenario: Create demo project in an empty workspace
- **WHEN** a user requests demo project creation
- **THEN** DataSpec creates a demo project
- **AND** imports built-in fields and domains
- **AND** seeds a demo table template and core lint rule configs
- **AND** returns the project, template id, example table name, and example SQL.

#### Scenario: Reuse existing demo project
- **WHEN** a demo project with the reserved demo name already exists
- **THEN** DataSpec returns that project instead of creating another project
- **AND** ensures missing demo template or rule configs are backfilled idempotently.

### Requirement: First-Use Frontend Entry
DataSpec SHALL expose first-use entry points that create the demo project and switch the current project.

#### Scenario: No project selected on Dashboard
- **WHEN** a user opens Dashboard without an available project
- **THEN** the page offers creating a demo project
- **AND** after creation the current project becomes the demo project.

#### Scenario: User opens Project List
- **WHEN** a user opens Project List
- **THEN** the page offers creating the demo project next to ordinary project creation.

### Requirement: Demo Workflow Shortcuts
DataSpec SHALL provide lightweight shortcuts from Dashboard into existing core workflows.

#### Scenario: Use demo SQL lint shortcut
- **WHEN** a user opens SQL lint through the demo shortcut
- **THEN** the SQL editor loads a demo SQL that can produce lint findings and fixed SQL.

#### Scenario: Use demo DDL shortcut
- **WHEN** a user opens DDL generation through the demo shortcut
- **THEN** the DDL page pre-fills the demo table name and can use the seeded demo template.

#### Scenario: Use AI context export shortcut
- **WHEN** a user opens AI context export through the demo shortcut
- **THEN** the AI export page allows previewing and downloading the current project's AI context package and core context files.
