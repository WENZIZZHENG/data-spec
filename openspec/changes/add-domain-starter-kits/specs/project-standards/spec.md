## ADDED Requirements

### Requirement: Project creation can apply domain starter kits
The project creation flow SHALL allow a new project to be initialized with one or more domain starter kits after the project is created.

#### Scenario: Create project with starter kits
- **WHEN** a user creates a project and selects starter kits
- **THEN** DataSpec creates the project using the existing built-in standards preference
- **AND** applies the selected starter kits to the new project
- **AND** the UI reports starter kit created and skipped counts.

#### Scenario: Starter kit apply fails after project creation
- **WHEN** project creation succeeds but applying a selected starter kit fails
- **THEN** the project remains available
- **AND** the UI shows a retryable error so the user can apply the kit later from the project list.

#### Scenario: Create project without starter kits
- **WHEN** a user creates a project without selecting starter kits
- **THEN** existing project creation behavior remains unchanged.
