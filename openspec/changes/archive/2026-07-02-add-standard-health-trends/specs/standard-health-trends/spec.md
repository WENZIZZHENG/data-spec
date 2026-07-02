## ADDED Requirements

### Requirement: Standard Health Snapshot
DataSpec SHALL allow users and AI agents to create project-level standard health snapshots without storing business data rows.

#### Scenario: Create snapshot from existing signals
- **GIVEN** a project has standard fields, quality report data, AI feedback signals, candidates, rule exemptions, and SQL check records
- **WHEN** a user creates a standard health snapshot
- **THEN** DataSpec SHALL save quality average, low-quality field count, optional coverage rate, unmanaged field count, rule issue count, candidate counts, and top actions
- **AND** the saved payload SHALL NOT include SQL text, database connection strings, passwords, tokens, or business data rows.

#### Scenario: Create snapshot with optional coverage summary
- **GIVEN** a caller has a recent coverage report summary
- **WHEN** the caller creates a health snapshot with coverage metrics
- **THEN** DataSpec SHALL store coverage rate, unmanaged count, missing comment count, possible duplicate count, and top unmanaged field names
- **AND** it SHALL mark the coverage status as collected.

#### Scenario: Create snapshot without coverage summary
- **GIVEN** no coverage report summary is supplied
- **WHEN** the caller creates a health snapshot
- **THEN** DataSpec SHALL still save quality and feedback metrics
- **AND** it SHALL mark coverage status as `not_collected`
- **AND** it SHALL include a top action recommending a coverage report.

### Requirement: Standard Health Trend
DataSpec SHALL expose recent standard health snapshots and compare the latest snapshot with week/month baselines.

#### Scenario: Return trend comparison
- **GIVEN** a project has multiple health snapshots over time
- **WHEN** a user queries the standard health trend
- **THEN** DataSpec SHALL return snapshots ordered by capture time
- **AND** it SHALL include week and month deltas for quality average, low-quality count, coverage rate, and unmanaged count where baselines exist.

#### Scenario: Empty trend
- **GIVEN** a project has no health snapshots
- **WHEN** a user queries the standard health trend
- **THEN** DataSpec SHALL return an empty trend with next actions for creating the first snapshot.

### Requirement: AI-readable Improvement Plan
DataSpec SHALL generate a stable improvement plan from the latest health snapshot.

#### Scenario: Copyable improvement plan
- **GIVEN** a latest health snapshot exists
- **WHEN** a user or AI requests the improvement plan
- **THEN** DataSpec SHALL return Markdown and structured top actions ordered by priority
- **AND** actions SHALL point to standard maintenance tasks such as filling comments, adding aliases, reviewing unmanaged fields, handling rule issues, or running coverage.

### Requirement: Standard Health Frontend
The frontend SHALL provide a project-scoped standard health trend page.

#### Scenario: View and create health snapshot
- **GIVEN** a current project is selected
- **WHEN** the user opens the standard health page
- **THEN** the page SHALL show latest snapshot summary, week/month changes, recent snapshot table, top actions, and a create-snapshot action
- **AND** after creating a snapshot it SHALL refresh the trend.

#### Scenario: Copy plan
- **GIVEN** the latest snapshot has an improvement plan
- **WHEN** the user clicks copy
- **THEN** the page SHALL copy the Markdown plan for AI use.
