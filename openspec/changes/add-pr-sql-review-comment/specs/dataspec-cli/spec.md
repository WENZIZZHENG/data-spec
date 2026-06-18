## ADDED Requirements

### Requirement: Pull Request SQL Review Comment

The CLI SHALL create or update a GitHub Pull Request comment containing DataSpec SQL lint results.

#### Scenario: Create review comment

- **WHEN** a user runs `review-pr <path...> --project <id> --repo <owner/name> --pr <number> --token <token>`
- **AND** the target pull request has no existing DataSpec review comment
- **THEN** the CLI batch lints the SQL files
- **AND** it creates a GitHub Pull Request comment containing summary, per-file counts, and issue details

#### Scenario: Update existing review comment

- **WHEN** the target pull request already has a comment containing the DataSpec review marker
- **THEN** the CLI updates that existing comment instead of creating a duplicate comment

### Requirement: Pull Request Review Exit Codes

The PR review command SHALL preserve CI-friendly exit codes after publishing feedback.

#### Scenario: Review has error issues

- **WHEN** any lint result has `errorCount` greater than zero
- **THEN** the CLI publishes or updates the PR comment
- **AND** it exits with code `1`

#### Scenario: Review has no error issues

- **WHEN** every lint result has zero errors
- **THEN** the CLI publishes or updates the PR comment
- **AND** it exits with code `0`

#### Scenario: GitHub request fails

- **WHEN** GitHub API comment creation or update fails
- **THEN** the CLI exits with code `2`
- **AND** it prints a readable error message to stderr
