## ADDED Requirements

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
