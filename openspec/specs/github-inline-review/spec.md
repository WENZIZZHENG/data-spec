# github-inline-review Specification

## Purpose
定义 CLI 发布 GitHub PR inline SQL review 的能力，将可定位 lint issue 映射到 diff 行，同时保留汇总评论、去重和 fallback 诊断。
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

### Requirement: Review PR emits a verifiable delivery envelope
`review-pr --format json` SHALL return additive delivery metadata that links deterministic findings to the reviewed commit and published comments.

#### Scenario: Local SQL matches the PR head
- **WHEN** review-pr is asked to lint local SQL files
- **THEN** review-pr loads every PR files page available through the GitHub API, and each local file uniquely maps to a GitHub PR file whose Git blob SHA matches before lint starts
- **AND** the same in-memory bytes are sent to DataSpec lint.

#### Scenario: Local SQL is not the reviewed PR content
- **WHEN** a local file is not in the PR, maps ambiguously, lacks a GitHub blob sha, or its Git blob SHA differs
- **THEN** review-pr exits with error semantics before DataSpec lint or GitHub comment writes.

#### Scenario: PR head changes during review
- **WHEN** the PR head differs after loading files, before the initial publish phase, or immediately before any inline or summary comment write
- **THEN** review-pr stops before starting another remote write and asks the caller to rerun
- **AND** comments that completed before a later head change may remain, but no summary or subsequent inline comment is published after the change is detected.

#### Scenario: Review publishes findings
- **WHEN** review-pr lints PR SQL files and publishes inline or fallback comments
- **THEN** JSON includes kind, schemaVersion, commitSha, reviewCommentUrl, inlineCommentUrls, findings, sqlCheckRecordIds, postCheck status, and evidence package entries
- **AND** existing reviewCommentAction, summary, inline, files, and exit codes remain compatible.

#### Scenario: Review has no finding
- **WHEN** no lint issue is found
- **THEN** the envelope contains the authoritative PR head SHA and summary comment result
- **AND** findings, SQL check IDs, inline URLs, and evidence package entries are empty rather than fabricated.

#### Scenario: GitHub omits a comment URL
- **WHEN** a GitHub response succeeds but omits `html_url`
- **THEN** the corresponding URL is null
- **AND** comment action, counts, marker deduplication, and exit code remain correct.

### Requirement: Review findings remain evidence-separated from delivery metadata
GitHub commit and comment URLs SHALL prove delivery location but SHALL NOT be treated as canonical DataSpec finding evidence.

#### Scenario: Finding is delivered to GitHub
- **WHEN** a finding is published as an inline or summary comment
- **THEN** its evidenceRefs contain only resolvable DataSpec evidence refs
- **AND** GitHub SHA and URLs are returned in delivery fields outside the finding evidence list.
