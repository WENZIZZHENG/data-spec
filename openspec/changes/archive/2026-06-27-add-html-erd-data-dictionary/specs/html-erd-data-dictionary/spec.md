## ADDED Requirements

### Requirement: HTML data dictionary export
The system SHALL generate a project-level HTML data dictionary that can be opened offline in a browser.

#### Scenario: Preview HTML data dictionary
- **WHEN** a user requests HTML preview for a project
- **THEN** the response includes a complete HTML document with overview, domains, fields, enum dictionaries, templates, and relationship section

#### Scenario: Download HTML data dictionary
- **WHEN** a user downloads the HTML data dictionary
- **THEN** the system returns an HTML file encoded as UTF-8 with a browser-friendly content type

### Requirement: Relationship graph export
The system SHALL generate Mermaid relationship graph text for project standards.

#### Scenario: Generate graph text
- **WHEN** a project has domains, fields, enum dictionaries, and templates
- **THEN** the graph includes nodes and edges that represent field-domain, field-enum, template-field, and template-standard-field relationships

#### Scenario: Empty relation sources
- **WHEN** a project lacks one of the relation source types
- **THEN** the graph still returns valid Mermaid text without broken references

### Requirement: Frontend dictionary preview and download
The generator frontend SHALL expose HTML and ERD data dictionary actions for the current project.

#### Scenario: View generated dictionary
- **WHEN** a user selects a project and clicks HTML or ERD preview
- **THEN** the page shows the generated content without leaving the current workflow

#### Scenario: Download generated dictionary
- **WHEN** a user clicks download for HTML or ERD
- **THEN** the browser downloads the generated file for the current project
