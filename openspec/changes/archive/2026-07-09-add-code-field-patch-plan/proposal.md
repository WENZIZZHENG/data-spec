## Why

字段标准发生重命名、类型调整或枚举变化后，现有字段引用索引只能告诉 AI 和用户“哪些文件可能受影响”，还不能形成可审查的候选修改计划。为了让 DataSpec 真正服务业务仓库改造，需要把字段变更转换成默认 dry-run 的 Patch Plan，列出候选文件、建议修改、风险、验证命令、人工确认点和回滚提示。

## What Changes

- 新增本地业务代码 Patch Plan 能力：基于字段引用索引和字段变更输入生成 `patchPlan`、`candidateEdit`、`fileRef`、`riskLevel`、`dryRunDiff`、`manualStep`、`rollbackHint` 等稳定结构。
- 新增 CLI 命令 `code-patch plan`，支持字段重命名、类型变化和枚举变化的 JSON/Markdown 输出，默认只读 dry-run，不写业务仓库文件。
- 新增可复用的本地工具 API，用于测试、CLI 和后续 MCP/HTTP API 扩展复用同一套计划生成逻辑。
- 新增 CLI/MCP 契约 fixture，记录命令、输出形状、安全元数据、成功示例、失败示例和后续动作。
- 延续现有引用索引的扫描边界、路径越界保护、敏感信息脱敏和生成目录跳过策略。
- 不新增自动应用补丁能力；不保证识别所有动态 SQL 或运行时拼接字段；不在第一版实现服务端 HTTP API，因为服务端通常无法安全访问本地业务仓库文件。

## Capabilities

### New Capabilities
- `code-field-patch-plan`: 定义字段变更到业务代码 Patch Plan 的只读计划能力、稳定输出结构、风险分级、dry-run diff、人工步骤和安全边界。

### Modified Capabilities
- `dataspec-cli`: 增加 `code-patch plan` 命令及其参数、输出格式、退出码和错误诊断契约。
- `cli-mcp-contract-fixtures`: 增加 `code-patch plan` CLI 契约 fixture 覆盖和安全示例校验。

## Impact

- 影响代码：`tools/dataspec-cli.mjs`、`tools/dataspec-code-refs.mjs` 或新增相邻工具模块、`tools/dataspec-cli.test.mjs`、`tools/dataspec-cli-mcp-contract-check*.mjs`、`tools/fixtures/cli-mcp-contracts.json`。
- 影响规格：新增 `openspec/specs/code-field-patch-plan/spec.md`，并为 `dataspec-cli`、`cli-mcp-contract-fixtures` 增加 delta specs。
- 影响用户：AI 和用户可以在业务仓库内生成字段变更候选修改计划，审查后再决定是否另开 OpenSpec 或人工改代码。
- 安全影响：能力保持只读，不读取业务数据行，不写业务文件，不调用外部 LLM，不使用网络，不输出 token、password、Authorization、API key、完整 JDBC URL、DSN 或连接串。
