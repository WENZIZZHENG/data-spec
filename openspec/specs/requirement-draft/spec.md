## Purpose

描述 DataSpec 如何把自然语言建表需求转换为只读标准候选草案，帮助用户和 AI 在生成 DDL 或写入标准候选前先完成字段、模板和歧义点确认。

## Requirements

### Requirement: Requirement draft API
DataSpec SHALL provide a project-scoped API that converts a natural language table requirement into a deterministic standard candidate draft.

#### Scenario: Draft from natural language requirement
- **WHEN** a caller submits `projectId`, `description`, `targetTableName`, and optional `groupHint`
- **THEN** DataSpec SHALL return `matchedFields`, `missingCandidates`, `ambiguousTerms`, `recommendedTemplate`, `nextActions`, and `copyablePrompt`
- **AND** the result SHALL be generated without calling an external LLM.

#### Scenario: Draft remains read-only
- **WHEN** a caller generates a requirement draft
- **THEN** DataSpec SHALL NOT create or update standard fields, standard candidates, table templates, or DDL records automatically
- **AND** missing candidates SHALL include enough payload hints for the existing standard candidate Inbox flow to use later.

#### Scenario: Ambiguous terms are explicit
- **WHEN** one requirement token matches multiple plausible standard fields or only generic terms
- **THEN** DataSpec SHALL return an `ambiguousTerms` entry with the term, reason, and candidate fields instead of silently choosing one.

### Requirement: Requirement draft frontend
DataSpec web SHALL provide a project-aware page for drafting table standards from natural language requirements.

#### Scenario: User generates and consumes a draft
- **WHEN** a user enters a business description and target table name in the page
- **THEN** the page SHALL show matched standard fields, missing candidates, ambiguous terms, the recommended template, next actions, and a copyable prompt
- **AND** the page SHALL provide navigation to DDL preview and the standard candidate Inbox without pretending the draft has been persisted.
