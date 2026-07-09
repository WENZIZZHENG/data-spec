## 1. OpenSpec 与 TDD 红灯

- [x] 1.1 运行 `openspec validate add-database-comment-patch-plan --strict`，确认 proposal、design 和 delta specs 可校验。
- [x] 1.2 先补后端失败测试：COMMENT patch plan API、只读语义、PostgreSQL/MySQL SQL 草稿、unsupported 项、脱敏、rollbackHint、evidence 和 nextActions。
- [x] 1.3 先补 CLI/tools 失败测试：`comment-plan preview` JSON/text 输出、参数校验、fixture 覆盖、状态检查和敏感信息拒绝。
- [x] 1.4 先补前端失败测试：反向导入页可生成 COMMENT plan，展示差异、风险、dry-run SQL、blocked/unsupported 和复制/导出安全内容。
- [x] 1.5 在 `Verification Evidence` 记录 TDD 红灯命令和失败点。

## 2. 后端 COMMENT patch plan API

- [x] 2.1 新增 comment plan controller/service/model，补充 Javadoc、`@Schema` 和字段级说明：`commentPatchPlan`、`currentComment`、`targetComment`、`commentDiff`、`dryRunSql`、`dialectSupport`、`riskLevel`、`rollbackHint`、`evidence`、`safety`、`nextActions`。
- [x] 2.2 实现只读 API `/api/reverse-import/database/comment-plan`，校验 `projectId` 并复用项目访问控制、数据库 metadata 读取和 metadata cache/fingerprint。
- [x] 2.3 基于表/列当前 comment 与 DataSpec 表/字段标准生成 `NO_OP`、`MISSING`、`CHANGED`、`UNSUPPORTED` 差异项，并处理字段名/别名歧义。
- [x] 2.4 生成 PostgreSQL `COMMENT ON TABLE/COLUMN` 和安全 MySQL 表 COMMENT dry-run SQL；MySQL 列 comment metadata 不足时返回 `UNSUPPORTED`，不生成危险 SQL。
- [x] 2.5 对请求摘要、错误、SQL 文本、evidence 和 rollbackHint 做脱敏，禁止 password、token、Authorization、完整 JDBC URL、DSN、连接串和业务数据行出现在响应中。

## 3. CLI / tools 契约

- [x] 3.1 在 `tools/dataspec-cli.mjs` 新增 `comment-plan preview` 命令，支持 JSON/text 输出、数据库连接参数、表选择、metadata cache mode 和安全错误码。
- [x] 3.2 更新 CLI/MCP contract fixture、status check 或契约检查，覆盖 `comment-plan preview` 的输入、输出、安全 metadata、示例和失败样例。
- [x] 3.3 更新 tools 测试，覆盖成功 JSON、text summary、参数缺失、服务端错误脱敏和 fixture 漂移检测。

## 4. 前端 COMMENT plan 预览

- [x] 4.1 新增前端 API wrapper、类型和必要展示 helper，调用 COMMENT patch plan API。
- [x] 4.2 在 `ReverseImport.vue` 数据库直连流程新增 COMMENT plan 入口，复用当前项目、连接信息、选表和 metadata cache mode。
- [x] 4.3 展示计划摘要、风险、方言支持、metadata fingerprint、差异项、blocked/unsupported 原因、rollbackHint、nextActions 和 dry-run SQL。
- [x] 4.4 确保无项目、未选表、请求失败、无差异、unsupported 和复制/导出内容均可恢复且不展示敏感信息。

## 5. 验证、评审、归档与提交

- [x] 5.1 运行后端目标测试和必要时 `mvn test`。
- [x] 5.2 运行前端目标测试、`pnpm test`、`pnpm build` 和相关 E2E（若页面流程受影响）。
- [x] 5.3 运行 tools 验证：目标 CLI 测试、`node --test tools/*.test.mjs` 和 `node tools/dataspec-status-check.mjs --format json`。
- [x] 5.4 运行 OpenSpec 验证：`openspec validate add-database-comment-patch-plan --strict`。
- [x] 5.5 运行通用检查：`git diff --check`，存在暂存内容时运行 `git diff --cached --check`。
- [x] 5.6 启动独立子 agent 做 API/CLI/前端/安全边界只读评审，记录 agent id、用途、结论和关闭状态；修复 Critical/Important findings 或说明技术理由。
- [x] 5.7 补充 `Verification Evidence`，记录关键命令、结果、评审证据和未覆盖风险。
- [x] 5.8 完成后按 OpenSpec 归档流程归档 change，运行 `openspec validate --all`，更新 `TODO.md` 中 P6-183 状态与验证证据。
- [x] 5.9 满足门禁后按项目 Git 规则创建本地 commit，不主动 push。

## Verification Evidence

### TDD RED（2026-07-09）

- `openspec validate add-database-comment-patch-plan --strict`：通过，确认 artifacts 可校验。
- `mvn "-Dtest=DatabaseReverseImportServiceTest,ReverseImportControllerTest" test`（`dataspec-server`）：失败，预期缺口为 `DatabaseCommentPatchPlan` / `DatabaseCommentPatchPlanItem` 模型尚未实现。
- `node --test tools/dataspec-cli.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs`：失败，预期缺口为 `comment-plan preview` 命令与 `comment-plan-preview` fixture 尚未实现。
- `node --test tests/databaseCommentPlan.test.ts tests/stableSelectors.test.ts`（`dataspec-web`）：失败，预期缺口为 `src/utils/databaseCommentPlan.ts` 与 comment plan 稳定选择器尚未实现。

### GREEN 与评审证据（2026-07-09）

- 后端目标验证：`mvn "-Dtest=DatabaseReverseImportServiceTest,SensitiveDataSanitizerTest,ReverseImportControllerTest" test`（`dataspec-server`）通过，46 tests pass。
- 后端全量验证：`mvn test`（`dataspec-server`）通过，569 tests pass；保留本地 Maven 仓库 `javax.annotation-api` POM warning 和 JDK 动态 agent warning。
- 前端目标验证：`node --test tests/databaseCommentPlan.test.ts tests/stableSelectors.test.ts`（`dataspec-web`）通过，4 tests pass。
- 前端全量验证：`pnpm test`（`dataspec-web`）通过，171 tests pass。
- 前端构建：`pnpm build`（`dataspec-web`）通过；保留既有第三方 `@vueuse/core` Rolldown `INVALID_ANNOTATION`、chunk size 和 plugin timing warning。
- 前端 E2E 契约：`pnpm exec playwright test tests/e2e/page-object-contract.spec.ts`（`dataspec-web`）通过，1 test pass；保留 Element Plus radio label deprecation 和 Monaco worker fallback warning。
- OpenAPI 同步：临时以 `mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.flyway.enabled=false"` 启动后端读取 `/api-docs`，运行 `pnpm gen:api` 重新生成 `dataspec-web/src/api/schema.ts`，随后 `pnpm check:api` 通过并输出 `OpenAPI schema.ts 已是最新: src/api/schema.ts`；临时后端已停止。未对本地 Flyway history 执行 repair 或迁移。
- CLI/tools 目标验证：`node --test tools/dataspec-cli.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，184 tests pass / 2 skipped（symlink 权限受限）。
- CLI/tools 全量验证：`node --test tools/*.test.mjs` 通过，376 tests pass / 2 skipped（symlink 权限受限）。
- 项目状态检查：`node tools/dataspec-status-check.mjs --format json` 通过并返回 `status=warn`，唯一 warning 为当前 active change `add-database-comment-patch-plan` 尚未归档。
- OpenSpec 验证：`openspec validate add-database-comment-patch-plan --strict` 通过，change valid。
- 通用检查：`git diff --check` 通过，仅输出 CRLF 工作区提示。
- 独立评审：子 agent `019f470b-0329-7982-b8c4-851e5625c8ef` 做 API/CLI/前端/安全边界只读评审，已关闭；发现的 Important findings（metadata cache 写入、MySQL literal、URL userinfo/非 Bearer Authorization 脱敏、OpenAPI schema 同步、metadata-cache-mode 校验）均已修复并重新验证。
- 独立复评：子 agent `019f4721-4b8f-7bd2-9481-6733053bf840`（用途：只读复评 findings 关闭状态和 archive/commit 门禁）已完成并关闭；结论为无 Critical/Important 行为问题，要求补齐本节证据。复评提出的 Minor（no-op 说明注释不应显示 `dryRunSql=yes`）已修复：CLI text 改为基于 `summary.executableChangeCount > 0` 判断，前端 no-op 显示“无可执行 SQL”，并补测试。
- 未覆盖风险：未执行真实数据库 COMMENT SQL，符合本变更 non-goal；MySQL 列 COMMENT 仍在 metadata 不足时返回 `UNSUPPORTED` 和人工 nextActions，不生成可能破坏列定义的 SQL。

### Archive 后验证（2026-07-09）

- `openspec archive add-database-comment-patch-plan --yes`：成功；同步 `dataspec-cli`、新增 `db-comment-patch-plan` 主规格、同步 `db-reverse-import-frontend`，归档到 `openspec/changes/archive/2026-07-09-add-database-comment-patch-plan/`。
- `openspec validate --all`：通过，124 specs passed / 0 failed。
- `TODO.md`：P6-183 已更新为完成态，并记录 archive 路径、关键验证和独立评审证据。
- `node tools/dataspec-status-check.mjs --format json`：通过，`status=pass`，active changes 为空，totalIssues=0。
