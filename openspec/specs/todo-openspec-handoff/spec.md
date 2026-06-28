# todo-openspec-handoff Specification

## Purpose
定义 DataSpec 从结构化 TODO 条目生成 OpenSpec change 草稿的本地交接能力，确保 AI 或人工开工前可以保留待办意图、验收标准、不做边界和需要确认的问题。

## Requirements
### Requirement: Parse structured TODO item
DataSpec SHALL provide a local handoff assistant that extracts one structured TODO item by id.

#### Scenario: Extract TODO fields
- **WHEN** the assistant is given a TODO id such as `P6-48`
- **THEN** it SHALL find the matching TODO heading
- **AND** it SHALL extract title, status, why, existing foundation, gap, deliverables, acceptance criteria, and boundary when present.

#### Scenario: Missing TODO item
- **WHEN** the requested TODO id is not present
- **THEN** the assistant SHALL fail with a readable error
- **AND** it SHALL NOT write a partial OpenSpec change.

### Requirement: Generate OpenSpec draft from TODO
DataSpec SHALL generate a repo-local OpenSpec change draft from the selected TODO item.

#### Scenario: Write draft artifacts
- **WHEN** a TODO item is converted without dry-run
- **THEN** the assistant SHALL write `.openspec.yaml`, `proposal.md`, `design.md`, `specs/<capability>/spec.md`, and `tasks.md`
- **AND** the generated files SHALL preserve the TODO boundary and acceptance criteria.

#### Scenario: Refuse accidental overwrite
- **WHEN** the target change directory already exists and force is not enabled
- **THEN** the assistant SHALL refuse to overwrite it
- **AND** it SHALL explain how to choose another change id or force regeneration.

#### Scenario: Dry run
- **WHEN** the assistant runs in dry-run mode
- **THEN** it SHALL return the planned change id, output files, TODO fields, and open questions
- **AND** it SHALL NOT write files.

### Requirement: AI-readable handoff output
DataSpec SHALL expose TODO handoff results in stable text and JSON formats.

#### Scenario: JSON output
- **WHEN** the assistant runs with JSON format
- **THEN** it SHALL output `kind`, `schemaVersion`, `todo`, `changeId`, `capability`, `files`, `openQuestions`, and `nextActions`.

#### Scenario: Text output
- **WHEN** the assistant runs with text format
- **THEN** it SHALL print the selected TODO, generated change id, file list, open questions, and next actions.

### Requirement: Human confirmation boundary
DataSpec SHALL keep generated OpenSpec drafts explicitly reviewable before implementation.

#### Scenario: Include open questions
- **WHEN** the assistant generates a draft
- **THEN** it SHALL include open questions asking the user or AI reviewer to confirm naming, scope, acceptance criteria, and non-goals before implementation.

#### Scenario: No automatic implementation
- **WHEN** the assistant finishes generating a draft
- **THEN** it SHALL NOT mark the TODO as complete
- **AND** it SHALL NOT run implementation, commit, or archive steps for the generated change.
