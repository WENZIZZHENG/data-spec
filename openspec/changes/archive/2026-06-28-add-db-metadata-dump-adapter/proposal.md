## Why

数据库直连反向导入、二次比对和覆盖率报告都依赖同一类表结构 metadata，但当前读取逻辑仍绑定在连接时流程里。把 JDBC metadata 统一转换为可序列化 schema dump 后，AI、用户和测试都能在不连接数据库、不读取业务数据行的情况下复现候选、覆盖率和差异结果。

## What Changes

- 新增数据库 schema dump 能力：从 PostgreSQL/MySQL 直连 metadata 导出稳定 JSON，包含数据库类型、catalog/schema、表、列、类型、nullable、default/comment 和 source metadata。
- 抽象 metadata adapter：把 JDBC metadata 读取与 `TableDef` 分析解耦，让直连、dump、fixture 都走同一份标准化结构。
- 扩展反向导入：支持从 schema dump 生成 preview 和 compare，结果与同一份直连 metadata 保持一致。
- 扩展覆盖率报告：支持从 schema dump 生成 coverage report，不需要再次连接数据库。
- 更新 README/TODO/OpenSpec，并补 PostgreSQL/MySQL fixture 与服务层测试，确保 dump 不包含密码、token、完整 JDBC URL 或业务数据行。

## Capabilities

### New Capabilities

- `db-metadata-dump`: 定义数据库 metadata adapter、schema dump JSON 契约、脱敏边界和离线复现能力。

### Modified Capabilities

- `reverse-import`: 增加从 schema dump 生成反向导入 preview/compare 的要求。
- `field-coverage-report`: 增加从 schema dump 生成覆盖率报告的要求。
- `db-reverse-import-compare`: 增加 dump 输入与直连 compare 结果一致性的要求。

## Impact

- 后端：新增 schema dump model/adapter/service；扩展 `DatabaseReverseImportService`、`ReverseImportController` 和 `FieldCoverageController`；复用现有 `ReverseImportService.previewTables/compareTables` 与 `FieldCoverageService.reportTables`。
- 测试：新增 dump fixture 和 adapter/service 单测，覆盖 PostgreSQL/MySQL metadata、dump 脱敏、preview/compare/coverage 复现。
- 前端/API：第一版优先提供后端 API 契约，前端可在后续 P6-62/P6-69 等数据库浏览和大库扫描任务中继续增强可视化。
- 文档：README/TODO 记录 schema dump 命令/接口边界和“不扫描业务数据行”的约束。

## Verification Evidence

- `mvn test`：270 tests, 0 failures, 0 errors。
- `npx.cmd openspec validate add-db-metadata-dump-adapter`：Change valid。
- `git diff --check`：exit 0，仅 CRLF 工作区换行提示。
- 本地结构化代码评审：删除旧 `DatabaseReverseImportServiceImpl` 内部 JDBC metadata 私有读取方法，避免与 `JdbcDatabaseMetadataAdapter` 形成双实现漂移；fixture 读取显式使用 UTF-8。
