## ADDED Requirements

### Requirement: List built-in domain starter kits
DataSpec SHALL expose built-in domain starter kits that describe reusable project initialization bundles.

#### Scenario: List available kits
- **WHEN** a caller requests the starter kit catalog
- **THEN** DataSpec returns kits with key, name, version, description, domains, field count, enum count, template count, tags, and recommended use cases
- **AND** no project data is modified.

#### Scenario: Catalog is stable for AI consumers
- **WHEN** a kit is returned from the catalog
- **THEN** it includes machine-readable field, enum, and template summaries without database IDs
- **AND** the key and version can be used later to apply the kit.

### Requirement: Apply starter kit to a project
DataSpec SHALL apply a selected starter kit to a project by creating missing fields, enums, and templates without overwriting user changes.

#### Scenario: Apply kit to existing project
- **WHEN** a caller applies a valid starter kit key and version to a project
- **THEN** DataSpec creates missing fields, enums, enum values, and templates for that project
- **AND** created standard fields include a starter kit source marker containing the kit key and version.

#### Scenario: Repeat apply is idempotent
- **WHEN** the same starter kit is applied again to the same project
- **THEN** existing fields, enums, and templates are skipped
- **AND** DataSpec returns created and skipped counts without duplicating project assets.

#### Scenario: Unknown kit is rejected
- **WHEN** a caller applies an unknown starter kit key or incompatible version
- **THEN** DataSpec rejects the request with a clear validation error
- **AND** no project assets are written.

### Requirement: Record starter kit installation summary
DataSpec SHALL record a project-scoped starter kit installation summary when a kit is applied.

#### Scenario: Record apply result
- **WHEN** a starter kit apply request completes
- **THEN** DataSpec stores projectId, kitKey, kitVersion, created counts, skipped counts, warnings, operator, and appliedAt
- **AND** the latest install summaries can be returned for the project.

#### Scenario: Installation record is project scoped
- **WHEN** a caller queries starter kit installations for a project
- **THEN** DataSpec returns only records belonging to that project.

### Requirement: Frontend starter kit application
DataSpec Web SHALL let users apply starter kits from project workflows.

#### Scenario: Apply kit to selected project
- **WHEN** a user opens the project list with a current project
- **THEN** the page can list starter kits and apply a selected kit to that project
- **AND** the page shows created and skipped counts after apply.

#### Scenario: No project selected
- **WHEN** no current project is selected
- **THEN** the starter kit apply action is disabled or guides the user to create or select a project.
