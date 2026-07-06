## 1. OpenSpec 与范围确认

- [x] 1.1 确认 proposal、design 和 spec delta 与 P6-90 范围一致，第一版坚持确定性本地估算、只读 planner、不调用外部 LLM、不自动导出 AI Context。
- [x] 1.2 运行 `openspec validate add-ai-context-budget-planner --strict`，修复 artifacts 格式或契约问题。

## 2. 后端预算 planner

- [x] 2.1 补失败优先测试：完整预算低风险、标准预算中风险、极简预算高风险、缺少 query 的低预算诊断、空命中降级、敏感信息不出现在响应。
- [x] 2.2 新增 request/response model，字段说明覆盖 tokenBudget、taskType/profile、query、target hints、selectedArtifacts、droppedArtifacts、qualityRisk、recommendedExportParams 和 diagnostics。
- [x] 2.3 实现只读 budget planner service：复用现有字段/规则/示例/profile/scope 语义，生成确定性 token 估算、artifact 取舍、质量风险和 fallbackSteps。
- [x] 2.4 新增 `/api/ai-context/budget/plan` API，校验 projectId/tokenBudget，错误和响应均不泄漏凭据或完整上下文内容。

## 3. CLI 与契约 fixture

- [x] 3.1 为 `context-budget plan` 补 CLI 测试：参数转发、profile/default precedence、缺少或非法 tokenBudget、服务错误脱敏、不会写入 zip/cache。
- [x] 3.2 实现 CLI `context-budget plan` 命令、help 文本、JSON/text 输出和退出码。
- [x] 3.3 更新 CLI/MCP contract fixture 和 fixture 校验测试，覆盖输出 shape、安全 metadata、unsafe 示例拒绝和 recommended next actions。

## 4. 前端预算预览

- [x] 4.1 更新前端 API/types，新增预算 planner 请求/响应类型与 API wrapper。
- [x] 4.2 在 AI Context 页面新增预算输入、计划按钮、风险摘要、selected/dropped artifacts、recommendedExportParams 展示和一键填充导出参数。
- [x] 4.3 补前端显示工具或 smoke 测试，覆盖预算预览区与风险文案不会破坏既有 AI Context 导出流程。

## 5. 文档、验证与评审

- [x] 5.1 更新 README/TODO，记录 `context-budget plan` 用法、安全边界、P6-90 完成状态和剩余限制。
- [x] 5.2 运行相关验证：OpenSpec strict/all、后端定点/全量测试、CLI 定点/全量 tools 测试、CLI/MCP fixture 检查、前端定点测试、`pnpm test`/`pnpm build`、`git diff --check`。
- [x] 5.3 在本文件追加 `Verification Evidence`，记录命令、结果、敏感词扫描和未覆盖风险。
- [x] 5.4 启动独立子 agent 做强制代码评审，处理或记录 findings，关闭子 agent。
- [x] 5.5 按项目 Git 规则核对 staged diff、敏感词扫描并创建本地 commit；不主动 push，不自动 archive OpenSpec。

## Verification Evidence

- 时间：2026-07-07。
- RED 验证：
  - `cd dataspec-server; mvn -Dtest=AiContextBudgetPlannerServiceTest test`：初始失败，预算 planner 相关类型/服务尚不存在。
  - `cd data-spec; node --test --test-name-pattern "context-budget" tools/dataspec-cli.test.mjs`：初始失败，CLI 未识别 `context-budget`。
  - `cd dataspec-web; node --test tests/aiContextBudgetPlan.test.ts`：初始失败，`src/utils/aiContextBudgetPlan.ts` 尚不存在。
  - `cd dataspec-web; node --test --test-name-pattern "DDL generation and AI Context export flows" tests/frontendSmoke.test.ts`：评审修复前失败，`AiExport.vue` watcher 未覆盖 `budgetForm.tokenBudget`。
- 后端验证：
  - `cd dataspec-server; mvn -Dtest=AiContextBudgetPlannerServiceTest test`：6 tests passed。
  - `cd dataspec-server; mvn -Dtest=AiContextControllerTest test`：7 tests passed。
  - `cd dataspec-server; mvn "-Dtest=AiContextBudgetPlannerServiceTest,AiContextControllerTest" test`：13 tests passed。
  - `cd dataspec-server; mvn test`：491 tests passed，BUILD SUCCESS；本地 Maven repo 存在既有 `javax.annotation-api` POM warning，不影响结果。
- CLI / tools 验证：
  - `node --test --test-name-pattern "context-budget" tools/dataspec-cli.test.mjs`：5 tests passed。
  - `node --test --test-name-pattern "context-budget|bundled CLI/MCP contract fixtures" tools/dataspec-cli-mcp-contract-check.test.mjs`：3 tests passed。
  - `node --test tools/*.test.mjs`：222 passed / 1 skipped，0 failed。
- 前端验证：
  - `cd dataspec-web; node --test tests/aiContextBudgetPlan.test.ts`：3 tests passed。
  - `cd dataspec-web; node --test --test-name-pattern "DDL generation and AI Context export flows|critical action labels" tests/frontendSmoke.test.ts`：2 tests passed。
  - `cd dataspec-web; pnpm test`：153 tests passed。
  - `cd dataspec-web; pnpm build`：通过；保留既有 Rolldown pure annotation、chunk size 和 plugin timing warning。
- OpenSpec / 文档 / 通用检查：
  - `openspec validate add-ai-context-budget-planner --strict`：Change valid。
  - `openspec validate --all`：111 passed，0 failed。
  - `node tools/dataspec-status-check.mjs --format json`：status=warn，0 errors；仅提示 active change 存在，P6-90 本次按约定保留 active 不 archive。
  - `git diff --check`：exit 0；仅 Windows CRLF 工作区提示。
- 敏感词扫描：
  - 未暂存阶段 `git diff --name-only | Select-String 'password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn'`：0 个文件名命中。
  - 未暂存阶段 `git diff -- . | Select-String 'password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn'`：107 个文本命中，人工复核均为 `tokenBudget` 字段名、README 安全说明、测试中的 unsafe secret 拒绝样例、脱敏断言或 `standardSnapshot` 中的 `dsn` 子串误报；未发现真实凭据、完整 JDBC URL、DSN、Authorization header 或 API key。
- 独立代码评审：
  - 子 agent：`019f3900-4852-7423-b497-f0ac93c15adf`，用途：P6-90 只读代码评审。
  - 关闭结果：已调用 `close_agent`，previous_status 为 completed。
  - Findings：Critical 无；Important 1 项为前端 `tokenBudget` 修改后旧 `budgetPlan` 未清空；Minor 2 项为 README 字段名误写 `estimatedTokens`、本 Evidence 尚未追加。
  - 处理结果：已将 `budgetForm.tokenBudget` 纳入 `AiExport.vue` watcher，并补前端 smoke 红绿验证；README 已改为真实 `estimation.selectedEstimatedTokens` / `estimation.totalEstimatedTokens`；本节补齐 Evidence。
- 结构化自审：
  - 需求覆盖：API/CLI/前端均支持预算计划；推荐导出参数保持显式应用。
  - 安全边界：planner 只读，不生成 zip/cache，不调用外部 LLM/tokenizer；响应和 CLI error 继续脱敏。
  - 测试覆盖：服务层、Controller、CLI 参数/错误/不写文件、fixture 安全、前端显示工具和 smoke 均已覆盖。
  - 无关改动：未修改生成的 `dataspec-web/src/api/schema.ts`，未归档 OpenSpec，未 push。
- 未覆盖风险：
  - 第一版仍是确定性字符权重估算，不代表模型精确 token。
  - 预算 planner 不生成真实 AI Context 包，也不保证极低预算覆盖复杂任务。
  - 历史 snapshot 的预算语义未单独建模；前端切换 snapshot 会清空旧预算计划，后续如需 snapshot-aware planner 需另开 change。
