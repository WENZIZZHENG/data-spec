## ADDED Requirements

### Requirement: DDL Preview Page
The generator page SHALL let users generate DDL from table templates.

#### Scenario: Generate DDL from selected template
- **WHEN** a project is selected
- **AND** the user selects a table template and enters a table name
- **THEN** the page calls the DDL preview API
- **AND** displays the generated SQL
- **AND** displays lint self-check counts and issues from the response

#### Scenario: Review template fields
- **WHEN** the user selects a template
- **THEN** the page displays the fields belonging to that template

#### Scenario: Copy or download generated DDL
- **WHEN** generated DDL is available
- **THEN** the user can copy it to the clipboard
- **AND** download it as a `.sql` file

#### Scenario: No current project
- **WHEN** no project is selected
- **THEN** the page guides the user to create or select a project
- **AND** disables DDL generation
