## ADDED Requirements

### Requirement: Built-in Standards Initialization
The system SHALL be able to initialize a project with built-in standards.

#### Scenario: Create project with default built-in standards
- **WHEN** a client creates a project without specifying an import preference
- **THEN** the system creates the project
- **AND** imports the built-in data domains into that project
- **AND** imports the built-in standard fields into that project

#### Scenario: Create blank project
- **WHEN** a client creates a project with `importBuiltInStandards` set to false
- **THEN** the system creates the project
- **AND** does not import built-in standards

#### Scenario: Idempotent standards import
- **WHEN** built-in standards are imported more than once for the same project
- **THEN** existing data domains with the same code are not duplicated
- **AND** existing standard fields with the same name are not duplicated

### Requirement: Project Creation UI Import Choice
The project creation UI SHALL let users choose whether to import built-in standards.

#### Scenario: Create project from UI
- **WHEN** the user opens the new project dialog
- **THEN** the import built-in standards option is enabled by default
- **AND** the option is sent with the create project request
