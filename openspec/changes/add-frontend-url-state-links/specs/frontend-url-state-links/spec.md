## ADDED Requirements

### Requirement: URL state protocol
DataSpec Web SHALL expose a stable, non-sensitive URL query protocol for key front-end workflow state.

#### Scenario: Restore project and filters from URL
- **WHEN** a user opens a supported DataSpec Web route with safe query parameters such as `projectId`, `keyword`, `fieldId`, `recordId`, `aiJobId`, `table`, `status`, `sourceBatchId`, `page`, or `size`
- **THEN** the page initializes the matching current project, filter controls, pagination, and detail state from those query parameters where supported.

#### Scenario: Sync state after user interaction
- **WHEN** a user changes a supported filter, pagination value, selected detail record, or current project on a supported route
- **THEN** DataSpec Web updates the route query without a full page reload
- **AND** unrelated safe query parameters are preserved.

#### Scenario: Exclude unsafe values from URL
- **WHEN** DataSpec Web builds or copies a URL state link
- **THEN** the URL MUST NOT include SQL text, database password, API token, Authorization header, full JDBC URL, connection string, or large JSON payload values.

### Requirement: Reproducible workflow links
DataSpec Web SHALL provide reproducible links for high-frequency workflow states.

#### Scenario: Field library link
- **WHEN** a field library URL includes `projectId`, `keyword`, or `fieldId`
- **THEN** the field library restores the keyword filter and opens or highlights the requested field when it is available.

#### Scenario: SQL check record link
- **WHEN** a SQL lint URL includes `projectId` and `recordId`
- **THEN** the SQL lint page loads recent records for that project and opens the matching check record detail.

#### Scenario: AI replay detail link
- **WHEN** an AI replay URL includes `projectId`, optional `jobType`, and `aiJobId`
- **THEN** the AI replay page restores the job type filter and opens the matching replay detail.

#### Scenario: Coverage filter link
- **WHEN** a field coverage URL includes `projectId`, `table`, or `status`
- **THEN** the coverage page restores those filter controls and applies them to the current report when a report exists.

#### Scenario: Reverse import source link
- **WHEN** a reverse import URL includes `projectId`, `sourceBatchId`, optional `table`, or optional `status`
- **THEN** the reverse import page restores the lightweight source/batch context that is available without exposing connection credentials.

### Requirement: Copy and recover URL state links
DataSpec Web SHALL let users copy current reproducible workflow links and recover from invalid URL parameters.

#### Scenario: Copy current link
- **WHEN** a user activates the copy link action on a supported page or detail view
- **THEN** DataSpec Web writes a sanitized absolute URL to the clipboard
- **AND** shows a success or recoverable failure message.

#### Scenario: Invalid URL parameter
- **WHEN** a supported page receives an invalid, stale, or inaccessible URL parameter
- **THEN** DataSpec Web shows a non-sensitive recovery message
- **AND** removes or ignores the invalid parameter without clearing other usable state.
