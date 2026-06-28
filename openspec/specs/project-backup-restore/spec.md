# project-backup-restore Specification

## Purpose
DataSpec provides project backup and restore for personal or small-team migration, allowing standard assets to move between local or lightweight environments without carrying secrets or source database rows.
## Requirements
### Requirement: Export project backup package
DataSpec SHALL export a project backup package containing restorable standard assets without sensitive credentials or source database rows.

#### Scenario: Export project backup
- **WHEN** a user exports a backup for a project they can access
- **THEN** DataSpec returns a JSON package with schemaVersion, exportedAt, sourceProject, asset counts, packageHash, and standard assets
- **AND** the package includes project metadata, domains, fields, enum dictionaries and values, rule configs, rule baseline metadata, templates, standard snapshots, reverse import source summaries, and necessary change log summaries

#### Scenario: Exclude sensitive data
- **WHEN** a backup package is generated
- **THEN** it MUST NOT include API token plaintext, token hashes, database passwords, complete JDBC URLs, browser local storage content, or source database data rows
- **AND** it includes a sanitization summary describing removed sensitive field categories

### Requirement: Preview project backup restore
DataSpec SHALL support a dry-run restore preview before any project backup package is written to the database.

#### Scenario: Dry-run into new project
- **WHEN** a user previews restoring a valid backup package without targetProjectId
- **THEN** DataSpec returns a restore plan for creating a new project
- **AND** the plan includes compatibility status, target project name, created/skipped/updated/conflict counts, item-level actions, and warnings
- **AND** no project assets are written

#### Scenario: Dry-run into existing project
- **WHEN** a user previews restoring a valid backup package into an existing targetProjectId
- **THEN** DataSpec compares package assets with the target project
- **AND** existing same-name or same-code assets are marked as SKIP, UPDATE, or CONFLICT according to overwrite mode
- **AND** no project assets are written

#### Scenario: Reject incompatible or unsafe package
- **WHEN** a backup package has an unsupported schemaVersion, invalid packageHash, missing required assets, or suspected secret fields
- **THEN** DataSpec rejects the preview with a readable validation error
- **AND** no project assets are written

### Requirement: Apply project backup restore
DataSpec SHALL restore project backup packages only after an explicit apply request and using the same plan semantics as dry-run.

#### Scenario: Restore without overwrite
- **WHEN** a user applies a valid restore with overwrite=false
- **THEN** DataSpec creates missing assets in dependency order
- **AND** existing same-name or same-code assets are skipped or reported as conflicts
- **AND** it does not delete target project assets

#### Scenario: Restore with overwrite
- **WHEN** a user applies a valid restore with overwrite=true
- **THEN** DataSpec updates supported existing assets from the package
- **AND** unsupported destructive changes remain blocked or reported as conflicts

#### Scenario: Record restore summary
- **WHEN** a restore apply completes
- **THEN** DataSpec stores a restore summary with packageHash, source project, target project, overwrite mode, counts, warnings, operator, and created time
- **AND** it does not store the full backup package or sensitive values in the restore record

### Requirement: Frontend backup restore workflow
The frontend SHALL provide a project-scoped backup and restore workflow for personal migration tasks.

#### Scenario: Export backup from selected project
- **WHEN** a user opens the backup/restore page with a current project selected
- **THEN** the user can export the current project backup package
- **AND** the UI clearly states that passwords, API tokens, and source database rows are excluded

#### Scenario: Preview and apply restore
- **WHEN** a user pastes or uploads a backup package
- **THEN** the UI can run dry-run preview, display conflicts and warnings, and require an explicit apply action before restore
- **AND** after apply it displays the restore summary counts and target project

#### Scenario: No project selected
- **WHEN** no current project is selected
- **THEN** export is disabled or shows an actionable empty state
- **AND** restore preview can still target a new project if the package is valid

### Requirement: Restore apply write idempotency
Project restore apply SHALL use the project-scoped write guard for target project mutations.

#### Scenario: Retry restore apply with same key
- **WHEN** a caller retries the same restore apply request with the same target project and idempotency key
- **THEN** DataSpec returns the original restore result without applying the restore twice.

#### Scenario: Concurrent restore apply
- **WHEN** a restore apply is already mutating the same target project
- **THEN** DataSpec returns a retryable conflict diagnostic for another restore apply on that target project.
