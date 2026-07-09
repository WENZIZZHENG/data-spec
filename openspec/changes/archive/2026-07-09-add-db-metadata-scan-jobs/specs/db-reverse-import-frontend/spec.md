## ADDED Requirements

### Requirement: 反向导入页展示 metadata 采集作业
The reverse import page SHALL display database metadata scan job progress and recovery controls in database direct mode.

#### Scenario: 展示采集作业进度
- **WHEN** a scan response includes `scanJobId`, `status`, `progress`, `sourcePressureHint`, `failureSummary`, or `partialResult`
- **THEN** the page shows scan status, processed count, remaining estimate, effective page size, source pressure hint, failed table summary, and current batch table selection.
- **AND** the page keeps selected table names unless the user explicitly clears them.

#### Scenario: 继续和取消采集作业
- **WHEN** a scan job has `resumeCursor` and is not completed or cancelled
- **THEN** the page provides a continue action that sends `scanJobId` and `resumeCursor`.
- **AND** the page provides a cancel action that sends `cancelToken` or `cancel=true` without writing source or standard data.

#### Scenario: 失败摘要不泄漏敏感信息
- **WHEN** the page renders failure summary, source pressure hint, resume summary, or copied evidence
- **THEN** rendered text MUST NOT include password, token, Authorization header, complete JDBC URL, DSN, connection string, or source database row values.

### Requirement: 前端可复用采集作业部分结果
The reverse import page SHALL allow users to run preview or compare from successful scan job partial results.

#### Scenario: 部分结果生成预览
- **WHEN** a scan job has successful partial tables and the user selects those tables
- **THEN** preview or compare requests include only selected successful table names.
- **AND** failed, cancelled, or not-yet-scanned tables are not silently imported.
