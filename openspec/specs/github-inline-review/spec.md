# github-inline-review Specification

## Purpose
TBD - created by archiving change add-github-inline-review. Update Purpose after archive.
## Requirements
### Requirement: Publish inline SQL review comments
The CLI SHALL publish GitHub inline review comments for SQL lint issues that can be mapped to Pull Request diff lines.

#### Scenario: Issue maps to changed line
- **WHEN** `review-pr` lints a SQL file and an issue line is present in the PR diff
- **THEN** the CLI creates a GitHub inline review comment on that file line.

#### Scenario: Issue does not map to changed line
- **WHEN** `review-pr` finds an issue whose line is not present in the PR diff
- **THEN** the issue remains in the summary comment with a fallback reason.

### Requirement: Preserve summary review behavior
The CLI MUST continue creating or updating the existing DataSpec summary PR comment.

#### Scenario: Inline comments are created
- **WHEN** inline comments are created successfully
- **THEN** the summary comment still reports overall totals and inline/fallback counts.

#### Scenario: No inline comments are possible
- **WHEN** no issue can be mapped to a diff line
- **THEN** the CLI updates the summary comment and explains that all issues used fallback.

### Requirement: Avoid duplicate inline comments
The CLI SHALL avoid creating duplicate inline comments for the same file, line, and rule code on repeated runs.

#### Scenario: Existing inline marker found
- **WHEN** a matching DataSpec inline marker already exists on the PR
- **THEN** the CLI does not create another identical inline comment.

### Requirement: Report inline diagnostics
The CLI SHALL report inline review diagnostics in its result summary.

#### Scenario: Mixed inline and fallback result
- **WHEN** some issues are posted inline and others fallback to summary
- **THEN** the CLI result includes `inlineCommentsCreated`, `inlineCommentsSkipped`, `fallbackIssues`, and readable fallback reasons.

#### Scenario: GitHub permission error
- **WHEN** GitHub rejects inline or summary comment API calls due to authentication or authorization
- **THEN** the CLI exits with parameter/error semantics and returns a message mentioning token, repo, pr, or permission diagnosis.
