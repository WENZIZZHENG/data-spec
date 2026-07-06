## 任务

- [x] 1. 完成 OpenSpec proposal/design/spec/tasks，并通过 strict 校验。
- [x] 2. 后端按 TDD 新增 schema plan response model、service 方法和 `/api/reverse-import/database/schema-plan` endpoint。
- [x] 3. 后端生成 `currentSchemaHash`、`targetSpecHash`、`changeSet`、`riskLevel`、`migrationSql`、`rollbackHint`、`manualChecks`、`blockedReasons` 和 `nextActions`，并保证输出不包含 raw password、完整 JDBC URL、DSN、token 或业务数据行。
- [x] 4. CLI 按 TDD 新增只读 schema plan 命令、帮助文本和契约 fixture/test。
- [x] 5. 前端新增 API wrapper、类型和反向导入页 schema plan 预览，覆盖风险、阻塞、人工检查、changeSet 和 dry-run SQL。
- [x] 6. 更新 README/TODO，说明 P6-87 第一版能力、命令、只读边界和未覆盖风险。
- [x] 7. 运行后端、前端、tools、OpenSpec 和通用检查，补充 Verification Evidence。
- [x] 8. 启动独立子 agent 代码评审，处理或记录 findings。
- [x] 9. 满足门禁后创建本地 commit；不自动 push，不自动 archive 本 change。

## Verification Evidence

- 2026-07-07：`openspec validate add-schema-change-plan-preview --strict` 通过。
- 2026-07-07：`mvn "-Dtest=DatabaseReverseImportServiceTest#planSchemaChange_buildsReadOnlyMigrationDraftFromCompareResult+planSchemaChange_usesPostgresqlSchemaPrefixInDraftSql+planSchemaChange_usesDefaultPostgresqlSchemaPrefixWhenRequestOmitsSchema+planSchemaChange_keepsUnsafeStructureChangesAsReviewComments+planSchemaChange_keepsMultilineIdentifiersInsideReviewComments+schemaChangePlanModelsExposeOpenApiDescriptions,ReverseImportControllerTest#dumpEndpoints_delegateToDatabaseService" test` 通过，7 tests，0 failures，覆盖只读 SQL 草案、schema 前缀、恶意标准值、换行 identifier 注释逃逸和 OpenAPI description。
- 2026-07-07：`mvn "-Dtest=DatabaseReverseImportServiceTest#planSchemaChange_keepsMultilineSchemaNameInsideReviewComments+planSchemaChange_keepsMultilineIdentifiersInsideReviewComments+planSchemaChange_usesDefaultPostgresqlSchemaPrefixWhenRequestOmitsSchema" test` 通过，3 tests，0 failures，覆盖显式 schemaName/table/column 的 CR/LF 防逃逸和默认 public schema。
- 2026-07-07：`mvn test` 通过，482 tests，0 failures。Maven 本地仓库存在 `javax.annotation-api` 父 POM parse warning，但未影响构建结果。
- 2026-07-07：`node --test tools/dataspec-cli.test.mjs --test-name-pattern "schema-plan"` 通过，111 pass / 1 skip，覆盖 `schema-plan` 命令只读 JSON 输出。
- 2026-07-07：`node --test tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，14/14。
- 2026-07-07：`node --test tools/*.test.mjs` 通过，206 pass / 1 skip。
- 2026-07-07：`pnpm exec node --test tests/databaseSchemaPlan.test.ts tests/frontendSmoke.test.ts` 通过，31/31，覆盖前端 schema plan util、API wrapper、页面按钮、风险/阻塞/人工检查/changeSet/dry-run SQL 接线。
- 2026-07-07：`pnpm test` 通过，150/150。
- 2026-07-07：`pnpm build` 通过。Vite/Rolldown 输出依赖 `@vueuse/core` pure annotation 和 chunk size warning，未影响构建。
- 2026-07-07：`git diff --check` 通过，仅输出 Windows 行尾转换 warning。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`、`errors=0`；仅提示 3 个 active OpenSpec change，其中包含当前正在实施的 `add-schema-change-plan-preview`。
- 2026-07-07：敏感词扫描 `rg -n -i "password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn" ...` 命中均为脱敏边界说明、测试用 secret 样例、变量名或断言文本，未发现真实凭据。
- 2026-07-07：第一轮独立子 agent 评审 `019f38a8-e70f-7f52-b29e-867b5eb8bdde`（用途：P6-87 评审整改复评）发现 1 个 Critical、1 个 Important、1 个 Minor；已修复 SQL 注释逃逸、默认 schema 前缀和 design 口径，并关闭该 agent。
- 2026-07-07：第二轮独立子 agent 复评 `019f38b2-71ca-71b3-bdbc-8aa35e3e535b`（用途：P6-87 findings 修复复评）未发现 Critical/Important；Minor 为建议补 schemaName 换行测试，已补充并关闭该 agent。
- 2026-07-07：未单独运行 `pnpm run check:api`；该脚本需要已启动后端 `/api-docs`。本轮以 `mvn test`、前端 `schema.ts` smoke 断言、`pnpm test` 和 `pnpm build` 覆盖新增 OpenAPI/前端消费契约。
