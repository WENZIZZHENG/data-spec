## 1. 测试先行

- [x] 1.1 为 `context-quality check --context-dir` 增加失败测试，覆盖 JSON 输出字段、资源缺口和只读行为。
- [x] 1.2 为 `context-quality check --context-zip` 增加失败测试，覆盖 zip 输入和不安全 zip 条目。
- [x] 1.3 为 `context-quality check --budget-plan` 增加失败测试，覆盖从预算 plan 推导质量风险和下一步动作。

## 2. CLI 实现

- [x] 2.1 新增 `context-quality check` 命令分发、参数校验、单输入源约束和 text/json 输出。
- [x] 2.2 实现 AI Context 目录和 zip 的本地只读资源扫描、分类覆盖、截断识别和缺失关键资源判断。
- [x] 2.3 实现预算 plan JSON 评分、tokenBudgetBreakdown、taskFitHints 和 nextContextActions 推导。
- [x] 2.4 复用现有 CLI 脱敏和错误处理，确保失败诊断不暴露敏感信息。

## 3. 契约与文档

- [x] 3.1 更新 CLI/MCP contract fixture，加入 `context-quality check` 的稳定输出、安全说明和示例。
- [x] 3.2 更新 contract fixture 检查测试，覆盖新命令和 unsafe example 拒绝。
- [x] 3.3 按需更新 README 的 CLI 能力说明，避免把后续前端/API 能力写成已实现。

## 4. 验证与评审

- [x] 4.1 运行相关 tools 测试、CLI/MCP contract fixture 检查和 `openspec validate add-ai-context-quality-check --strict`。
- [x] 4.2 在本 change 记录 `Verification Evidence`，列明命令、结果、评审和剩余风险。
- [x] 4.3 因新增 CLI/AI 外部协议，启动独立子 agent 代码评审，修复 findings 或记录技术理由。

## Verification Evidence

- `node --test --test-name-pattern "context-quality" tools/dataspec-cli.test.mjs`：先红灯失败，3 个测试均因新命令尚未实现返回退出码 2；实现后绿灯通过。
- `node --test --test-name-pattern "context-quality|context-budget" tools/dataspec-cli.test.mjs`：10 pass。
- `node --test --test-name-pattern "context-quality|context-budget" tools/dataspec-cli-mcp-contract-check.test.mjs`：4 pass。
- `node tools/dataspec-cli-mcp-contract-check.mjs --format json`：ok true，diagnostics 0，CLI command fixture 20 条。
- `node --test tools/*.test.mjs`：347 pass，2 skipped（当前平台无法创建部分 symlink）。
- `openspec validate add-ai-context-quality-check --strict`：valid。
- `git diff --check`：通过，仅 Git LF/CRLF 提示。
- `node tools/dataspec-status-check.mjs --format json`：status warn；唯一 warning 为 `OPENSPEC_ACTIVE_CHANGE_PRESENT`，本 change 按项目约定实现完成后默认保留 active，暂不自动归档。
- 评审：独立子 agent `019f3d19-63a0-7252-b194-24ddeda1d36b` 只读评审；完成后已关闭。评审发现 2 个 Important 和 2 个 Minor：已补 budget plan 格式校验和退化 plan 负向测试，修正 fixture 的三选一输入契约和 failureExample，补 `unclassified` 覆盖 bucket 与测试，并收口本 Verification Evidence。
- 未覆盖风险：第一版评分为确定性静态启发式，不替代真实 AI 任务结果；后续若扩展前端/API 展示需另建 OpenSpec change。
