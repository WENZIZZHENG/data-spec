## Context

数据库直连能力目前集中在 `DatabaseReverseImportServiceImpl`：连接数据库、读取表、读取列、生成 `TableDef`，再调用 `ReverseImportService.previewTables/compareTables` 或 `FieldCoverageService.reportTables`。这条链路已经证明可用，但 JDBC 读取、方言差异和标准分析耦合在同一个连接流程里，导致离线复现、fixture 测试和后续多方言扩展都不够稳。

P6-30 第一版不重写分析逻辑，而是把“数据库 metadata -> 标准化 schema dump -> TableDef”作为中间层。直连仍然只读读取 metadata；dump 只保存结构信息和脱敏 source metadata；preview/compare/coverage 继续复用已有服务。

## Goals / Non-Goals

**Goals:**

- 定义 DataSpec schema dump JSON，稳定表达 databaseType、catalog/schema、tables、columns、nullable、default/comment 和 warnings。
- 抽出 metadata adapter，把 PostgreSQL/MySQL 的 JDBC metadata 读取转换为同一份 dump 结构。
- 支持从 dump 运行反向导入 preview、数据库 compare 和字段覆盖率报告。
- 补 fixture 与单测，证明同一份 dump 可离线复现核心结果，并且不包含密码、token、完整 JDBC URL 或业务数据行。

**Non-Goals:**

- 不保存 schema dump 到数据库表；第一版只做即时导出和请求体输入。
- 不扫描业务数据行，不读取样本值，不执行写 SQL。
- 不新增第三方数据库客户端依赖，不一次性支持 Oracle/SQL Server 等方言。
- 不在本轮重做前端数据库浏览器或大库分页扫描；后续待办继续承接。

## Decisions

### 1. Dump 作为后端模型，不作为持久化实体

第一版新增 `DatabaseSchemaDump` 及子结构模型，接口返回/接收 JSON 即可。这样能快速服务 AI/测试/离线分析，不引入存储迁移，也避免用户误以为 DataSpec 会长期保存数据库结构。

备选方案是新增 `ds_database_schema_snapshot` 表持久化历史 dump。它更适合增量缓存和变更扫描，但会扩大数据治理边界，留给 P6-57/P6-70。

### 2. Adapter 输出 dump，再由 dump 转 `TableDef`

`ReverseImportService` 和 `FieldCoverageService` 已经以 `List<TableDef>` 为核心分析输入。Adapter 只负责读取与规范化 metadata，dump converter 负责转换为 `TableDef`，分析逻辑不重复实现。

备选方案是让每个功能各自支持 dump 解析。那会在 preview、compare、coverage 中复制字段映射，后续方言差异也更难收口。

### 3. API 使用显式 dump 输入端点

新增端点建议为：

- `POST /api/reverse-import/database/dump`：直连导出 dump。
- `POST /api/reverse-import/dump/preview`：从 dump 生成 preview。
- `POST /api/reverse-import/dump/compare`：从 dump 生成 compare。
- `POST /api/coverage/dump`：从 dump 生成 coverage report。

保留现有 `/database/preview`、`/database/compare`、`/coverage/database`，它们内部可改为先构建 dump 再复用 dump 分析路径，保证兼容。

### 4. Dump metadata 做保守脱敏

Dump 允许记录 databaseType、databaseName、schemaName、selectedTables、productName/productVersion、generatedAt 和 warnings；禁止 password、token、完整 JDBC URL、username password 组合串和业务数据行。错误信息沿用现有连接错误脱敏策略。

## Risks / Trade-offs

- [Risk] dump 模型和现有 `TableDef` 字段语义不完全一致。→ 明确 converter 单测，先覆盖 name/dataType/nullable/default/comment/schema/tableType。
- [Risk] PostgreSQL/MySQL metadata remarks 行为不同。→ warnings 保留方言限制，测试 fixture 固定差异。
- [Risk] 用户上传很大的 dump 导致请求过重。→ 第一版限制必须有表和列，保留后续 P6 大库扫描任务做分页/缓存；本轮不做后台任务。
- [Risk] “离线 dump”被误解为数据备份。→ README/API 文档强调只包含结构 metadata，不包含业务数据行。

## Migration Plan

1. 新增 dump 模型、adapter 和 converter，不改变现有接口签名。
2. 将现有直连 preview/compare/coverage 内部改为 `exportDump(req)` 后复用 dump 分析。
3. 新增 dump 输入端点和测试。
4. 更新 README/TODO/OpenSpec，验证 `mvn test` 和 OpenSpec。
