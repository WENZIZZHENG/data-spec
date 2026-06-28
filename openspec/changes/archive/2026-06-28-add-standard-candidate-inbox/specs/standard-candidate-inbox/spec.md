## ADDED Requirements

### Requirement: Project standard candidate inbox
DataSpec SHALL provide a project-scoped standard candidate inbox for fields that may need to become or enrich standard fields.

#### Scenario: List candidate fields
- **WHEN** a caller requests candidates for a project
- **THEN** DataSpec returns a paginated list filtered by project, status, source type, and keyword
- **AND** each candidate includes field metadata, source, confidence, status, evidence, decision fields, and timestamps.

#### Scenario: Candidate records remain project scoped
- **WHEN** candidate APIs are called with a projectId or candidate id
- **THEN** DataSpec MUST enforce project access checks
- **AND** DataSpec MUST NOT expose or mutate candidates from another project.

### Requirement: Candidate decisions
DataSpec SHALL support explicit human decisions for candidate fields.

#### Scenario: Accept candidate as new field
- **WHEN** a user accepts a pending or postponed candidate as a new standard field
- **THEN** DataSpec creates a project standard field from the candidate metadata
- **AND** the candidate status becomes ACCEPTED with the created field id and decision reason.

#### Scenario: Merge candidate into existing field
- **WHEN** a user merges a candidate into an existing project field
- **THEN** DataSpec records the target field id and decision reason
- **AND** the candidate status becomes MERGED
- **AND** DataSpec MUST NOT silently rewrite the target field aliases, comments, or data type.

#### Scenario: Ignore or postpone candidate
- **WHEN** a user ignores or postpones a candidate
- **THEN** DataSpec records the decision reason
- **AND** the candidate status becomes IGNORED or POSTPONED.

### Requirement: Candidate safety and deduplication
DataSpec SHALL keep the candidate inbox safe for AI-assisted use.

#### Scenario: Duplicate candidate names are controlled
- **WHEN** a caller creates or accepts a candidate whose field name already exists in the project
- **THEN** DataSpec rejects duplicate creation or acceptance with an actionable error
- **AND** suggests merging into the existing field instead.

#### Scenario: Candidate evidence is safe to display
- **WHEN** candidate evidence is saved or displayed
- **THEN** DataSpec MUST NOT include API tokens, database passwords, bearer tokens, full JDBC URLs, or source database row data.

### Requirement: Frontend candidate workbench
DataSpec Web SHALL provide a project-scoped standard candidate workbench.

#### Scenario: View and filter candidates
- **WHEN** a user opens the candidate inbox page with a current project
- **THEN** the page displays candidates with status, source, confidence, field metadata, evidence, and decision actions
- **AND** the user can filter by status, source type, and keyword.

#### Scenario: Perform candidate decisions
- **WHEN** a user accepts, merges, ignores, or postpones a candidate from the page
- **THEN** the page calls the corresponding candidate decision API
- **AND** refreshes the list after success.

#### Scenario: No project selected
- **WHEN** no project is selected
- **THEN** the page shows an empty state
- **AND** it does not call project-scoped candidate APIs.
