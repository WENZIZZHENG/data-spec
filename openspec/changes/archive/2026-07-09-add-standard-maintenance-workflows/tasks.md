## 1. OpenSpec 与 TDD 红灯

- [x] 1.1 运行 `openspec validate add-standard-maintenance-workflows --strict`，确认 proposal、design 和 delta specs 可校验。
- [x] 1.2 先补后端失败测试：标准维护 workflow plan API、只读语义、候选/质量/覆盖率来源、脱敏、recipeBinding、executionState、evidenceLinks 和 nextActions。
- [x] 1.3 先补 tools 失败测试：`standard-maintenance` recipe 出现在 workflow list/show、task-card、AI Context/workflows 文本和状态检查覆盖范围中。
- [x] 1.4 先补前端失败测试：候选、字段质量和覆盖率页面能触发 dry-run 计划并展示步骤、证据、确认边界和失败/空状态。
- [x] 1.5 在 `Verification Evidence` 记录 TDD 红灯命令和失败点。

## 2. 后端维护 workflow 计划 API

- [x] 2.1 新增 `standardmaintenanceworkflow` controller/service/model，补充 Javadoc、`@Schema` 和字段级说明：`inboxAction`、`recipeBinding`、`dryRunSteps`、`executionState`、`undoHint`、`evidenceLinks`、`nextActions`。
- [x] 2.2 实现 `/api/standard-maintenance/workflows/plan` 只读接口，校验 `projectId` 并复用项目访问控制。
- [x] 2.3 从标准候选、字段质量、字段覆盖率/请求参数和 AI 推荐来源生成 dry-run 步骤，不调用任何写入 API。
- [x] 2.4 对自由文本、证据、命令和 route 做脱敏，禁止 raw SQL、raw evidence、AI payload、JDBC URL、DSN、token、password 和 Authorization 出现在响应中。
- [x] 2.5 扩展 AI 任务推荐 item，给维护类推荐增加 `recipeBinding` 和安全 source 参数，保持兼容旧字段。

## 3. Tools / CLI / AI 契约

- [x] 3.1 在 `tools/dataspec-workflows.mjs` 新增 `standard-maintenance` recipe，描述 precheck、plan、review、execute、verify、archive 和失败恢复。
- [x] 3.2 更新 `tools/dataspec-task-card.mjs` 或相关测试，确认新 recipe 可生成任务卡且命令/输入脱敏。
- [x] 3.3 更新 AI contract/status check 所需文档或 fixture，避免 recipe id 漂移。

## 4. 前端 dry-run 入口

- [x] 4.1 新增或更新前端 API wrapper 与类型，调用维护 workflow plan API。
- [x] 4.2 在 `StandardCandidate.vue` 增加候选维护 workflow dry-run 入口和计划展示。
- [x] 4.3 在 `FieldQuality.vue` 增加低质量字段维护 workflow dry-run 入口和计划展示。
- [x] 4.4 在 `FieldCoverage.vue` 增加未纳管/疑似重复字段维护 workflow dry-run 入口和计划展示，保留 partial coverage 边界。
- [x] 4.5 确保无项目、空结果、请求失败和计划被阻塞时 UI 可恢复且不展示敏感内容。

## 5. 验证、评审与收口

- [x] 5.1 运行后端验证：受影响测试类，必要时 `mvn test`。
- [x] 5.2 运行前端验证：目标测试、`pnpm test`，必要时 `pnpm build`。
- [x] 5.3 运行 tools 验证：`node --test tools/*.test.mjs` 和状态/契约检查。
- [x] 5.4 运行 OpenSpec 验证：`openspec validate add-standard-maintenance-workflows --strict`。
- [x] 5.5 运行通用检查：`git diff --check`，存在暂存内容时运行 `git diff --cached --check`。
- [x] 5.6 启动独立子 agent 做只读代码评审，记录 agent id、用途、结论和关闭状态；修复 Critical/Important findings 或说明技术理由。
- [x] 5.7 补充 `Verification Evidence`，记录关键命令、结果、评审证据和未覆盖风险。
- [x] 5.8 完成后按 OpenSpec 归档流程归档 change，运行 `openspec validate --all`，更新 `TODO.md` 中 P6-181 状态与验证证据。
- [x] 5.9 满足门禁后按项目 Git 规则创建本地 commit，不主动 push。

## Verification Evidence

- OpenSpec artifacts：`openspec validate add-standard-maintenance-workflows --strict`，结果 `Change 'add-standard-maintenance-workflows' is valid`。
- TDD 红灯（后端 workflow plan）：`mvn "-Dtest=StandardMaintenanceWorkflowServiceImplTest,StandardMaintenanceWorkflowControllerTest" test`（目录 `dataspec-server`），失败点为 `com.dataspec.standardmaintenanceworkflow` controller/model/service 包尚不存在。
- TDD 红灯（后端推荐绑定）：`mvn "-Dtest=AiTaskRecommendationServiceImplTest,AiTaskRecommendationControllerTest" test`（目录 `dataspec-server`），失败点为 `AiTaskRecommendationItem` 尚无 `recipeBinding` 字段和构造参数。
- TDD 红灯（tools recipe）：`node --test tools/dataspec-task-card.test.mjs tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs tools/dataspec-status-check.test.mjs`（目录仓库根），失败点为 `standard-maintenance` 尚未进入 workflow recipe catalog；同次宽跑的 status-check CLI 用例受当前真实 active change 影响产生额外失败，后续改用目标 pattern 和最终全量命令收口。
- TDD 红灯（前端）：`node --test tests/standardMaintenanceWorkflowDisplay.test.ts`（目录 `dataspec-web`），失败点为 `src/api/standardMaintenanceWorkflow.ts`、`StandardMaintenanceWorkflowPlanPanel.vue` 和三处页面接线尚不存在。
- 后端目标验证：`mvn "-Dtest=StandardMaintenanceWorkflowServiceImplTest,StandardMaintenanceWorkflowControllerTest,AiTaskRecommendationServiceImplTest,AiTaskRecommendationControllerTest" test`（目录 `dataspec-server`），10 tests / 10 pass。
- Tools 目标验证：`node --test --test-name-pattern "workflow|task cards from all|self-consistent TODO" tools/dataspec-task-card.test.mjs tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs tools/dataspec-status-check.test.mjs`，18 tests / 18 pass。
- 前端目标验证：`node --test tests/standardMaintenanceWorkflowDisplay.test.ts`（目录 `dataspec-web`），3 tests / 3 pass。
- 首轮后端全量验证：`mvn test`（目录 `dataspec-server`）先在并行验证负载下出现 `PerformanceBaselineTest.largeFieldLibraryBaseline_reportsRepeatableMetrics` 波动失败，`field.suggest` 10711ms 超过基准阈值；随后单跑 `mvn "-Dtest=PerformanceBaselineTest" test` 通过，2 tests / 2 pass，低负载重跑 `mvn test` 通过，563 tests / 563 pass。
- 独立评审：子 agent `019f46a3-3645-7370-96d3-2d02099e7c49`（用途：P6-181 SDD full / API、前端、tools、OpenSpec 和安全边界只读评审）结论为 With fixes，未修改文件。发现 2 个 Critical 和 2 个 Important：健康 action 生成的覆盖率/候选维护推荐缺 `recipeBinding`，字段质量计划未按前端 `sourceIds` 收窄，partial coverage failed/skipped table counts 未进入 workflow evidence，tasks/evidence 未收口。已全部修复并补测试；`close_agent` 返回 `not found`，记录为系统已清理或无法再次关闭该 agent，本会话未保留可用子 agent 句柄。
- 评审整改 RED：新增回归测试后运行 `mvn "-Dtest=StandardMaintenanceWorkflowServiceImplTest,AiTaskRecommendationServiceImplTest" test`，失败于 `StandardMaintenanceWorkflowPlanReq` 缺少 `setFailedTableCount` / `setSkippedTableCount`；运行 `node --test tests/standardMaintenanceWorkflowDisplay.test.ts`（目录 `dataspec-web`）失败于 `FieldCoverage` 未传 `failedTableCount` / `skippedTableCount`。
- 评审整改 GREEN：`mvn "-Dtest=StandardMaintenanceWorkflowServiceImplTest,AiTaskRecommendationServiceImplTest" test`（目录 `dataspec-server`），9 tests / 9 pass；`node --test tests/standardMaintenanceWorkflowDisplay.test.ts`（目录 `dataspec-web`），3 tests / 3 pass。
- 后端全量验证：`mvn test`（目录 `dataspec-server`），564 tests / 564 pass。
- 前端全量验证：`pnpm test`（目录 `dataspec-web`），167 tests / 167 pass。
- 前端构建验证：`pnpm build`（目录 `dataspec-web`）通过；保留既有 `@vueuse/core` pure annotation、chunk size 和 plugin timing warnings。
- Tools 全量验证：`node --test tools/*.test.mjs`，371 tests total，369 pass / 2 skipped，0 fail；2 个 skipped 均为当前平台无法创建 symlink 的既有场景。
- OpenSpec 验证：`openspec validate add-standard-maintenance-workflows --strict`，结果 `Change 'add-standard-maintenance-workflows' is valid`。
- 状态检查：`node tools/dataspec-status-check.mjs --format json`，`status=warn`，唯一 warning 为 active change `add-standard-maintenance-workflows` 尚未归档，归档后需重跑确认清零。
- 通用检查：`git diff --check` 退出码 0；仅提示若干工作区文件未来会 LF/CRLF 转换。提交前仍需执行 `git diff --cached --check`、staged diff/stat 和敏感词扫描。
- 归档：`openspec archive add-standard-maintenance-workflows --yes` 已同步 `ai-task-recommendation-queue`、`ai-workflow-recipes`、`field-coverage-report`、`field-quality-scoring`、`frontend-task-entrypoints`、`standard-candidate-inbox` 主规格，并新增 `standard-maintenance-workflows` 主规格；change 已归档到 `openspec/changes/archive/2026-07-09-add-standard-maintenance-workflows/`。
- 归档后修正：状态检查发现新增主规格 Purpose 为归档默认占位；已替换为稳定中文目的说明，未改 Requirements/Scenario 语义。
- 归档后验证：`openspec validate --all`，123 passed / 0 failed；`node tools/dataspec-status-check.mjs --format json`，`status=pass`，0 errors / 0 warnings。
- 剩余风险：第一版仍是 dry-run 计划，不持久化 workflow instance，不自动执行候选采纳/合并/忽略、字段编辑或覆盖率扫描；后续如需要跨会话步骤状态，需另起 OpenSpec 设计。
