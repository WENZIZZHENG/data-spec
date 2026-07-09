# db-metadata-scan-jobs Specification

## Purpose
定义数据库 schema-only metadata 采集作业的分页、恢复、取消、限速、失败摘要和只读 evidence 契约，帮助反向导入、覆盖率和元数据浏览在大库或部分失败场景下安全复用成功结果。
## Requirements
### Requirement: 数据库 metadata 采集作业状态
DataSpec SHALL expose a request-driven database metadata scan job contract for large schema-only scans.

#### Scenario: 启动采集作业
- **WHEN** a user submits a database metadata scan request without `scanJobId`
- **THEN** DataSpec returns `scanJobId`, `status=RUNNING|PARTIAL|COMPLETED`, `resumeCursor`, `cancelToken`, `pageSize`, `progress`, `partialResult`, `sourcePressureHint`, `retryPolicy`, `failureSummary`, `evidence`, and `nextActions`.
- **AND** the response MUST NOT contain password, token, Authorization header, complete JDBC URL, DSN, connection string, or source database row values.

#### Scenario: 恢复采集作业
- **WHEN** a user submits the previous `scanJobId` and `resumeCursor`
- **THEN** DataSpec reads the next metadata page using the same non-sensitive scan scope.
- **AND** DataSpec returns updated progress, partial result, failure summary, and next resume cursor.

#### Scenario: 完成采集作业
- **WHEN** there are no remaining tables after the current page
- **THEN** DataSpec returns `status=COMPLETED`.
- **AND** `resumeCursor` is empty or absent.

### Requirement: 采集作业取消与失败摘要
DataSpec SHALL let users cancel metadata scan jobs and continue using successful partial schema-only results.

#### Scenario: 取消采集作业
- **WHEN** a user submits `cancelToken` or `cancel=true` for an active scan job
- **THEN** DataSpec returns `status=CANCELLED`.
- **AND** DataSpec does not scan more tables in that response.
- **AND** DataSpec does not write the source database or DataSpec standard field library.

#### Scenario: 单表失败不丢弃部分结果
- **WHEN** one or more tables fail because of permission, timeout, unsupported metadata, or transient connection errors
- **THEN** DataSpec returns successful table metadata in `partialResult`.
- **AND** `failureSummary` lists bounded failed table examples, failure categories, retryable status, and safe next actions.
- **AND** failed table details MUST NOT include credentials or source row values.

### Requirement: 采集作业限速与源库压力提示
DataSpec SHALL bound metadata scan jobs and explain source pressure decisions.

#### Scenario: 服务端限制 page size
- **WHEN** a request asks for a page size above the service limit
- **THEN** DataSpec uses the configured maximum page size.
- **AND** the response records the effective `pageSize` and a `sourcePressureHint` explaining the limit.

#### Scenario: 返回重试策略
- **WHEN** a scan job is partial, retryable, cancelled, or failed
- **THEN** DataSpec returns `retryPolicy` with safe retry delay guidance, maximum retry suggestion, and whether to lower page size or use metadata cache.

### Requirement: 采集作业只读 evidence
DataSpec SHALL produce AI-readable evidence for metadata scan jobs without creating source writes.

#### Scenario: Evidence describes scan boundary
- **WHEN** DataSpec returns a scan job result
- **THEN** `evidence` includes scan job id, status, processed count, failed count, selected schema/table scope, metadata fingerprint when available, safety flags, and validation next actions.
- **AND** evidence MUST be safe to copy to AI and MUST NOT include credentials, full connection strings, or source database row values.
