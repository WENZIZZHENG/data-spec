## 1. 规格与迁移

- [x] 1.1 校验 proposal/design/spec/tasks 与 P6-57 范围一致。
- [x] 1.2 新增 Flyway 迁移 `ds_reverse_import_decision`，包含索引和注释。
- [x] 1.3 新增/扩展后端模型、实体、mapper、repository 和 OpenAPI 可见 DTO。

## 2. 后端实现

- [x] 2.1 预览阶段为每个表字段生成 mapping decision，候选字段带默认 match reason/confidence。
- [x] 2.2 确认导入阶段记录 imported、skipped existing 和 ignored decisions，并返回 `batchId` 与决策列表。
- [x] 2.3 新增按项目/批次查询 mapping decisions 的只读 API。
- [x] 2.4 保持数据库密码、JDBC URL 和业务数据行不进入决策记录。

## 3. 前端实现

- [x] 3.1 重新生成 OpenAPI 类型并更新前端类型引用。
- [x] 3.2 反向导入页支持候选确认理由输入，导入时提交未选候选的默认忽略理由。
- [x] 3.3 导入结果展示本批次 mapping decision 摘要。

## 4. 测试、文档与收口

- [x] 4.1 新增/更新后端测试覆盖 alias match、新候选、导入、跳过、忽略和决策查询。
- [x] 4.2 新增/更新前端测试覆盖候选理由和 ignored candidates 提交模型。
- [x] 4.3 更新 README/TODO，记录 P6-57 第一版能力与边界。
- [x] 4.4 运行必要验证：OpenSpec strict、后端相关测试、`mvn test`、`pnpm gen:api`、`pnpm check:api`、`pnpm test`、`pnpm build`、`openspec validate --all`。
- [x] 4.5 使用独立代码评审 agent 审查本次变更，修复 findings 后复跑必要验证。
- [x] 4.6 归档 OpenSpec change 并提交。

## Verification Evidence

- `openspec validate add-reverse-import-mapping-decisions --strict`：通过，change valid。
- `pnpm gen:api`：通过，`http://localhost:8090/api-docs` 重新生成 `src/api/schema.ts`。
- `mvn "-Dtest=DatabaseReverseImportServiceTest,ReverseImportServiceTest,ReverseImportSourceServiceTest,ReverseImportControllerTest" test`：通过，28 tests, 0 failures, 0 errors。
- `mvn test`：通过，386 tests, 0 failures, 0 errors。
- `pnpm check:api`：通过，OpenAPI schema.ts 已是最新。
- `pnpm test`：通过，101 tests, 0 failures。
- `pnpm build`：通过；仅保留现有 `@vueuse/core` pure annotation 与 chunk size 构建警告。
- `openspec validate --all`：通过，92 passed, 0 failed。
- `git diff --check`：通过；仅输出 CRLF 工作区换行提示。

## Review Evidence

- 独立评审 agent Ohm 发现 P1：决策 `metadata_json` 白名单里的自由文本仍可能保存 JDBC URL、password、token、Bearer。已修复为决策顶层文本和 metadata 字符串统一走 `SensitiveDataSanitizer`，并新增敏感文本回归测试。
- 独立评审 agent Ramanujan 发现 P1：V22 `created_at` 使用 `timestamp with time zone` 与项目 `LocalDateTime` 约定不一致。已改为 `timestamp without time zone DEFAULT localtimestamp`。
- 独立评审 agent Rawls 发现 P2：`ds_field_source.metadata_json` 直接序列化扩展后的 `FieldCandidate`，且 ignored candidates 可能与已处理候选重复。已修复为来源快照显式 allowlist，并按 `tableName + columnName` 跳过重复 ignored candidates，补充对应测试。
