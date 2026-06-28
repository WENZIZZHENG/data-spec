## ADDED Requirements

### Requirement: Unified Request State
DataSpec Web SHALL provide a reusable front-end request state utility for page-level data loading, failure, retry, and refresh metadata.

#### Scenario: Track successful request
- **WHEN** a page runs a request through the unified request state utility and the request succeeds
- **THEN** the utility exposes loading as false, clears the previous error, records the result, and updates a last-updated timestamp.

#### Scenario: Track failed request
- **WHEN** a page request fails with a DataSpec error or network error
- **THEN** the utility exposes loading as false and stores a non-sensitive error summary that can be rendered with a retry action.

#### Scenario: Retry failed request
- **WHEN** a user activates the retry action after a failed request
- **THEN** the page reruns the same request and replaces the visible error state with the new result or new error.

### Requirement: Project Required State
Project-scoped DataSpec Web pages SHALL render a consistent project-required state when no current project is selected.

#### Scenario: Missing project
- **WHEN** a project-scoped page is opened without a current project
- **THEN** the page shows a consistent non-sensitive message and at least one action that helps the user create or select a project.

#### Scenario: Project appears
- **WHEN** a current project becomes available
- **THEN** the project-required state no longer blocks the page and the page may load its project-scoped data.

### Requirement: Recoverable State Display
DataSpec Web SHALL provide a reusable state display for empty data, failed requests, and retryable actions.

#### Scenario: Empty result
- **WHEN** a migrated page has loaded successfully but has no rows or no result
- **THEN** the page shows a consistent empty state with a next action instead of only a blank region.

#### Scenario: Failed result
- **WHEN** a migrated page request fails
- **THEN** the page shows a visible retry action and, when available, a suggested action from the DataSpec error detail.
