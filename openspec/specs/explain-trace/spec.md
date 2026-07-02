# explain-trace Specification

## Purpose
为 AI 输出提供轻量、可读、可测试的引用证据结构，帮助用户复盘字段推荐、字段检索和自然语言需求草案为什么采用某个标准字段、候选或模板。
## Requirements
### Requirement: Lightweight Explain Trace Contract
AI-facing recommendation outputs SHALL expose a lightweight `evidence` array for each key suggestion.

#### Scenario: Field suggestion exposes evidence
- **WHEN** a field recommendation or field catalog search returns a matched existing field
- **THEN** the item SHALL include an evidence entry with `sourceType`, `sourceId`, `matchReason`, `confidence` and `docsRef`
- **AND** the existing `matchReason` or `matchReasons` fields SHALL remain available for compatibility.

#### Scenario: Requirement draft exposes evidence
- **WHEN** a natural-language requirement draft returns matched fields, missing candidates, ambiguous candidates or a recommended template
- **THEN** each item SHALL include evidence explaining the source, reason and confidence for that suggestion
- **AND** missing candidates SHALL use evidence that references the requirement draft rule rather than a persisted standard field.

#### Scenario: Evidence stays lightweight and safe
- **WHEN** evidence is generated
- **THEN** it SHALL NOT contain business data rows, passwords, tokens, complete JDBC URLs or approval decisions
- **AND** `snapshotVersion` and `ruleCode` MAY be null when the current source does not have a stable snapshot or rule reference.

### Requirement: Explain Trace Frontend Visibility
The requirement draft page SHALL make first-version evidence visible to users and AI browser automation.

#### Scenario: User reviews evidence in requirement draft
- **WHEN** a draft result contains evidence
- **THEN** the page SHALL show the source type, confidence and match reason near the corresponding matched field, missing candidate, ambiguous candidate or recommended template
- **AND** the display SHALL preserve the existing copy actions and result tables.
