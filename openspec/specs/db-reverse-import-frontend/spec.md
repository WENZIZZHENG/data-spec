# db-reverse-import-frontend Specification

## Purpose
定义 DataSpec 反向导入页面的数据库直连流程、预览确认、导入结果和元数据缓存状态展示，确保用户能安全理解扫描进度、候选字段和 AI 可读摘要。
## Requirements
### Requirement: 数据库直连流程步骤化
反向导入页在数据库直连模式下 SHALL 以连续步骤呈现连接信息、选择表、预览确认、导入结果，且 SHALL 根据当前操作状态高亮当前步骤。

#### Scenario: 用户进入数据库直连模式
- **WHEN** 用户在反向导入页切换到数据库直连
- **THEN** 页面展示数据库连接、选择表、预览确认、导入结果四个步骤和当前项目名称

#### Scenario: 用户完成预览
- **WHEN** 用户选择表并成功生成反向导入预览
- **THEN** 页面高亮预览确认步骤，并展示字段候选、缺注释和非标准字段摘要

### Requirement: 表选择可批量操作
反向导入页 SHALL 支持用户在已加载数据库表后搜索、全选、清空表，并展示当前已选数量。

#### Scenario: 用户搜索并选择表
- **WHEN** 数据库表已加载且用户输入搜索关键字
- **THEN** 页面只展示匹配的表，并保留当前已选表数量

#### Scenario: 用户批量选择表
- **WHEN** 数据库表已加载且用户点击全选或清空
- **THEN** 页面更新待预览表集合，并同步更新已选数量

### Requirement: 候选字段可确认导入
反向导入页 SHALL 在预览结果中按表组织字段候选，并允许用户勾选本次要导入的候选字段；确认导入时 MUST 只提交已勾选候选字段。

#### Scenario: 用户勾选部分字段导入
- **WHEN** 预览结果包含多个字段候选且用户取消勾选其中一部分
- **THEN** 确认导入只提交仍被勾选的字段候选
- **AND** 未勾选候选作为 ignored candidates 随本次确认提交默认忽略理由。

#### Scenario: 用户填写确认理由
- **WHEN** 用户在候选字段中填写确认理由
- **THEN** 前端在确认导入请求中随对应候选提交 `confirmReason`。

#### Scenario: 用户未选择候选字段
- **WHEN** 预览结果存在但用户未勾选任何候选字段
- **THEN** 确认导入操作不可用或给出明确提示

### Requirement: 导入结果可收尾
反向导入页 SHALL 在导入完成后展示新增和跳过数量，并提供查看字段库的入口。

#### Scenario: 用户完成确认导入
- **WHEN** 后端返回导入结果
- **THEN** 页面展示新增字段数、跳过字段数、字段列表摘要和查看字段库入口

#### Scenario: 用户查看映射决策摘要
- **WHEN** 后端返回本次 mapping decisions
- **THEN** 页面展示导入、跳过和忽略的字段决策摘要与理由。

### Requirement: 反向导入页展示 metadata cache 状态
反向导入页 SHALL display database metadata cache freshness, fingerprint, and refresh controls in database direct flows.

#### Scenario: 展示缓存命中和过期信息
- **WHEN** scan, browser, preview, compare, or import preparation returns metadata cache information
- **THEN** the page SHALL show whether the result came from fresh cache, stale cache, refresh, or bypass
- **AND** it SHALL show `lastSeenAt`, `expiresAt`, and `metadataFingerprint` when available.

#### Scenario: 用户手动刷新 metadata
- **WHEN** a user chooses to refresh database metadata
- **THEN** the next database metadata request SHALL send `metadataCacheMode=REFRESH`
- **AND** the UI SHALL keep existing selected table names unless the user explicitly clears them.

### Requirement: 反向导入页展示结构变化摘要
反向导入页 SHALL show schema-only metadata change summaries returned by backend refreshes.

#### Scenario: 刷新后展示字段变化
- **WHEN** a refresh response contains added, removed, or changed fields
- **THEN** the page SHALL display a bounded summary grouped by table
- **AND** it SHALL NOT display passwords, tokens, full JDBC URLs, connection strings, or source database row values.

#### Scenario: AI 摘要包含 fingerprint
- **WHEN** the user copies or views AI-readable database metadata context
- **THEN** the context SHALL include the metadata fingerprint and freshness state
- **AND** it SHALL remain sanitized for credential-like values.

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

### Requirement: 反向导入页展示 COMMENT 回写计划
The reverse import page SHALL expose a database COMMENT patch plan preview in database direct mode without executing source database writes.

#### Scenario: 生成 COMMENT 回写计划
- **WHEN** a user selects database tables in reverse import database direct mode and clicks the COMMENT plan action
- **THEN** the page calls the comment patch plan API with the current project, non-sensitive connection metadata, selected table names, and metadata cache mode
- **AND** it displays summary counts, risk level, dialect support, metadata fingerprint, blocked reasons, and next actions.

#### Scenario: 展示 dry-run SQL 和差异项
- **WHEN** the API returns COMMENT patch plan items
- **THEN** the page groups table and column comment differences by table
- **AND** shows `currentComment`, `targetComment`, `commentDiff`, item status, risk level, rollback hint, and safe `dryRunSql` when available.

#### Scenario: 前端不泄漏敏感信息
- **WHEN** the page renders plan output, errors, copied SQL, copied JSON, or evidence text
- **THEN** rendered and copied content MUST NOT include password, token, Authorization header, complete JDBC URL, DSN, connection string, URL userinfo, or source database row values.
