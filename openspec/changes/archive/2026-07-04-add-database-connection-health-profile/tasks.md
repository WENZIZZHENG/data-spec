## 1. 规格与测试

- [x] 1.1 校验 OpenSpec proposal/design/spec/tasks 可实施。
- [x] 1.2 新增后端失败测试：成功连接返回 health capability，失败连接返回分类、retryable、nextActions 且脱敏。
- [x] 1.3 新增前端 smoke/展示测试：反向导入与覆盖率页展示连接健康、方言能力和 nextActions。

## 2. 后端实现

- [x] 2.1 新增 `DatabaseConnectionHealthDiagnostic` 与 `DatabaseDialectCapability` 模型。
- [x] 2.2 扩展 `DatabaseConnectionResult`，保留 `security` 并新增 `health`。
- [x] 2.3 扩展 `DatabaseReverseImportServiceImpl.testConnection`，统计 latency、成功 capability、失败分类和脱敏 nextActions。
- [x] 2.4 确认 PostgreSQL/MySQL 诊断不执行写探测，不保存凭据。

## 3. 前端实现

- [x] 3.1 更新 OpenAPI schema 与前端类型。
- [x] 3.2 扩展 `databaseSecurityDiagnostic` 展示工具，增加 health/capability label。
- [x] 3.3 更新 `ReverseImport.vue` 与 `FieldCoverage.vue` 的诊断卡片。
- [x] 3.4 更新 README/TODO，记录第一版能力和边界。

## 4. 验证与收口

- [x] 4.1 运行 `openspec validate add-database-connection-health-profile --strict`。
- [x] 4.2 运行 `mvn -Dtest=DatabaseReverseImportServiceTest test` 和必要前端测试/build。
- [x] 4.3 启动独立代码评审 agent，修复 findings 并复跑验证。
- [x] 4.4 归档 OpenSpec change 并提交。

## Verification Evidence

- `openspec validate add-database-connection-health-profile --strict`：通过。
- `mvn -Dtest=DatabaseReverseImportServiceTest test`：13 tests, 0 failures, 0 errors。
- `mvn test`：374 tests, 0 failures, 0 errors。
- `pnpm gen:api`：从 `http://localhost:8090/api-docs` 重新生成 `dataspec-web/src/api/schema.ts`。
- `pnpm check:api`：`OpenAPI schema.ts 已是最新`。
- `pnpm test`：99 tests, 0 failures。
- `pnpm build`：通过，保留既有 Rolldown pure annotation 和 chunk size warning。
- `git diff --check`：通过，仅输出 CRLF 工作区提示。
- 独立代码评审 agent：发现任务状态不真实、失败分类测试覆盖不足、成功态 retryable 语义误导和 `.security-section` 缺少样式；已修复并复跑 `mvn -Dtest=DatabaseReverseImportServiceTest test`、`mvn test`、`pnpm test`、`pnpm build` 和 OpenSpec 校验。
