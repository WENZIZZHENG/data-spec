## ADDED Requirements

### Requirement: scan plan 支持采集作业字段
DataSpec SHALL extend the existing database metadata scan plan response with scan job fields while preserving current pagination behavior.

#### Scenario: 旧分页字段保持兼容
- **WHEN** a client calls `/api/reverse-import/database/scan` with existing `pageSize`, `cursor`, `scanId`, and `cancel` fields
- **THEN** DataSpec still returns the existing `scanId`, `estimatedTableCount`, `tables`, `cursor`, `progress`, `partialSummary`, `resumeCommand`, `cancelled`, `metadataCache`, and `nextActions` fields.
- **AND** DataSpec also returns compatible scan job fields when available.

#### Scenario: resume command 使用作业字段
- **WHEN** DataSpec returns `resumeCommand`
- **THEN** the command or AI-readable hint references `scanJobId` and `resumeCursor` when present.
- **AND** it MUST NOT contain password, token, Authorization header, complete JDBC URL, DSN, or connection string values.

### Requirement: scan plan 返回部分结果边界
DataSpec SHALL distinguish current page tables from accumulated partial results.

#### Scenario: 当前页和 partialResult 可区分
- **WHEN** a scan job returns a page of table metadata
- **THEN** `tables` represents the current page for backward compatibility.
- **AND** `partialResult` identifies successful tables, skipped or failed tables, and whether the result is complete enough for preview or coverage.
