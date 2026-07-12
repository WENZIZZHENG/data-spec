## 1. OpenSpec 与实施准备

- [x] 1.1 创建 `add-ai-context-safety-controls` proposal/design/spec delta，并通过 `openspec validate add-ai-context-safety-controls --strict`。
- [x] 1.2 读取 apply instructions 和相关上下文文件，确认任务进度与实现边界。

## 2. 后端 AI Context 安全导出

- [x] 2.1 为 `AiContextExportServiceTest` 增加失败用例，覆盖 manifest safety summary、字段 `contextSafety`/`exportDecision`、敏感字段 example 脱敏和 prompt 输入脱敏。
- [x] 2.2 在 `AiContextExportService` 中实现字段级安全决策、业务文本脱敏、prompt 注入 warning、manifest `contextSafetySummary` 和指导文本边界说明。
- [x] 2.3 更新 `field-catalog.schema.json` 生成内容，补充 `contextSafety` 与 `exportDecision` 字段说明和枚举约束。

## 3. CLI/MCP 本地安全红线配置

- [x] 3.1 为 `tools/dataspec-config.test.mjs` 增加失败用例，覆盖合法 `securityProfile` 解析和非法字段类型诊断。
- [x] 3.2 扩展 `tools/dataspec-config.mjs`，规范化可选 `securityProfile` 并保持既有配置行为兼容。
- [x] 3.3 如 MCP session-state 需要展示本地策略，只输出 profile presence、policy names 和计数，不输出 raw pattern 或 secret-like 值。

## 4. 验证、评审与收口

- [x] 4.1 运行后端 AI Context 相关单测和 CLI config 测试。
- [x] 4.2 运行 OpenSpec strict、项目状态检查、`git diff --check` 和必要 secrets scan。
- [x] 4.3 启动独立子 agent 做安全/契约/代码质量评审，修复或记录所有 findings。
- [x] 4.4 更新 TODO/候选池/完成归档或 Verification Evidence，提交本地 commit。

## Verification Evidence

- 后端目标测试：`mvn "-Dtest=AiContextExportServiceTest,AiContextControllerTest,AiContextBudgetPlannerServiceTest,SensitiveDataSanitizerTest" test`，48 tests / 0 failures / 0 errors，BUILD SUCCESS；保留本地 Maven `javax.annotation-api` POM warning。
- Tools 目标测试：`node --test tools\dataspec-config.test.mjs tools\dataspec-mcp.test.mjs`，52 pass。
- CLI/MCP 契约扩展测试：`node --test tools\dataspec-cli.test.mjs tools\dataspec-cli-mcp-contract-check.test.mjs`，184 pass，2 个 symlink skip（当前平台无法创建 symlink）。
- OpenSpec：`openspec validate add-ai-context-safety-controls --strict` 通过；`openspec validate --all` 125 passed。
- 状态检查：`node tools\dataspec-status-check.mjs --format json` 仅 `OPENSPEC_ACTIVE_CHANGE_PRESENT` warning，符合本 change 暂保留 active 的项目约定。
- 空白检查：`git diff --check` 通过，仅 LF/CRLF warning。
- Secrets scan：`rg "(?i)(password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn)"` 命中均为脱敏测试假值、断言、历史安全说明或 OpenSpec 安全边界说明；回归测试已覆盖 live、snapshot、scoped、glossary、enum、usage example、prompt、standard metadata、rule metadata、rule exemption、matchReasons 和 MCP session-state 不输出 raw secret-like 值。
- 独立评审：子 agent `019f4764-7724-7232-a90b-b3a29f132333` 只读评审多轮发现并跟踪 Critical/Important 泄漏面，包括敏感 format examples、snapshot field catalog、scoped matchReasons/query、glossary/enum raw text、规则 metadata、scope metadata、rule exemption ruleCode、status matchReasons 和 standard metadata。已全部补回归测试或防御性脱敏；最终复评结论为 Critical 无、Important 无、Minor 无，`Ready to merge: Yes`。子 agent 已关闭。
