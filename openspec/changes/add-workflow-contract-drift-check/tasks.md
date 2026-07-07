## 1. OpenSpec Artifacts

- [x] 1.1 完成 proposal、design 和 `ai-workflow-recipes` spec delta，明确本次只新增本地确定性漂移检查。
- [x] 1.2 运行 `openspec validate add-workflow-contract-drift-check --strict`，确认 artifacts 格式有效。

## 2. Test-First Coverage

- [x] 2.1 在 `tools/dataspec-status-check.test.mjs` 增加红灯用例，覆盖 AI contract 文档缺失 workflow recipe id。
- [x] 2.2 在同一测试中覆盖 TODO 任务卡 summary 缺失 workflow recipe id。

## 3. Implementation

- [x] 3.1 扩展 `tools/dataspec-status-check.mjs`，读取 `docs/ai-contracts.md` 并注入 `supportedWorkflowRecipeIds()`。
- [x] 3.2 增加 workflow recipe contract check，输出稳定 issue code、file、line 和修复建议。
- [x] 3.3 更新 README 对 status-check 的说明，记录新增检查范围。

## 4. Verification And Review

- [x] 4.1 运行 Node 单测和 CLI 状态检查，确认新规则通过真实仓库。
- [x] 4.2 运行 OpenSpec strict、`git diff --check` 和必要的敏感词扫描。
- [x] 4.3 启动独立只读子 agent 评审并关闭，处理或记录 findings。
- [x] 4.4 记录 Verification Evidence，并在满足门禁后创建本地 commit。

## Verification Evidence

- 2026-07-07：`openspec validate add-workflow-contract-drift-check --strict` 通过。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs` 红灯符合预期，新增用例因缺少 `AI_CONTRACT_WORKFLOW_RECIPES_DRIFT` / `TODO_WORKFLOW_RECIPES_DRIFT` 检查而失败。
- 2026-07-07：实现后 `node --test tools/dataspec-status-check.test.mjs` 通过，6 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`、0 errors；`workflow-recipes` check 为 pass，仅保留既有 active change warning。
- 2026-07-07：`node --test tools/*.test.mjs` 通过，244 pass、2 skip（Windows symlink 权限）、0 fail。
- 2026-07-07：`openspec validate add-workflow-contract-drift-check --strict` 再次通过。
- 2026-07-07：`git diff --check` 通过，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：敏感词扫描新增 diff 行无命中；全文件扫描命中均为 README 既有脱敏/安全说明和示例。
- 2026-07-07：独立只读评审子 agent `019f3a49-65ed-7eb3-b369-824d1b0d2a96`（Plato）发现 2 个 Important：未检测旧 recipe id 残留、`docs/ai-contracts.md` 缺失时提示语义不稳定；已调用 `close_agent` 关闭。
- 2026-07-07：按评审补红灯测试后，`node --test tools/dataspec-status-check.test.mjs` 先失败 2 项，原因分别是旧 recipe id 残留未触发 fail、缺失 AI contract 文档未输出“无法确认”语义。
- 2026-07-07：修复双向集合对比和缺失文档提示后，`node --test tools/dataspec-status-check.test.mjs` 通过，8 pass、0 fail。
- 2026-07-07：修复评审 findings 后，`node --test tools/*.test.mjs` 通过，246 pass、2 skip（Windows symlink 权限）、0 fail。
- 2026-07-07：修复评审 findings 后，`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`、0 errors；`workflow-recipes` check 为 pass，仅保留既有 active change warning。
- 2026-07-07：修复评审 findings 后，`openspec validate add-workflow-contract-drift-check --strict` 通过。
- 2026-07-07：修复评审 findings 后，`git diff --check` 通过，仅输出 Windows LF/CRLF 提示。
