## 1. OpenSpec artifacts

- [x] 1.1 创建 `add-mcp-session-state-memory` proposal/design/spec/tasks，明确 SDD full、MCP 外部协议和脱敏边界。
- [x] 1.2 运行 `openspec validate add-mcp-session-state-memory --strict`，确认 artifacts 有效。
- [x] 1.3 运行 `node tools/dataspec-openspec-readiness.mjs --change add-mcp-session-state-memory --format json`，确认准备度缺口清空或记录原因。

## 2. 测试先行

- [x] 2.1 在 `tools/dataspec-mcp.test.mjs` 增加红灯测试，覆盖 `resources/list`、`resources/templates/list` 和 `resources/read` 的 session-state 输出。
- [x] 2.2 增加 `get_session_state` tool 红灯测试，覆盖显式 projectId 覆盖、projectless BLOCKED 状态和 structuredContent。
- [x] 2.3 增加敏感信息脱敏红灯测试，覆盖 token、password、JDBC URL、DSN、Authorization 和 URL userinfo 不出现在 session-state 输出。
- [x] 2.4 更新 `tools/dataspec-cli-mcp-contract-check.test.mjs` 或 fixture 测试，覆盖新增 resource/tool descriptor。

## 3. 实现

- [x] 3.1 在 `tools/dataspec-mcp.mjs` 新增 session-state 聚合 helper，复用现有 config、profile、context cache 和脱敏函数。
- [x] 3.2 接入 MCP `RESOURCE_DEFS`、`RESOURCE_TEMPLATE_KEYS`、`TOOL_SAFETY`、`tools/list` 和 `tools/call`，确保新能力只读且不调用后端写 API。
- [x] 3.3 更新 `tools/fixtures/cli-mcp-contracts.json`，记录 session-state resource/tool descriptor 和安全 metadata。
- [x] 3.4 更新 README 与 TODO P6-178 状态，说明第一版能力、边界和验证入口。

## 4. 验证与评审

- [x] 4.1 运行 `node --test tools/dataspec-mcp.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs`。
- [x] 4.2 运行 `node tools/dataspec-cli-mcp-contract-check.mjs --format json`。
- [x] 4.3 运行 `node --test tools/*.test.mjs`、`openspec validate add-mcp-session-state-memory --strict`、`node tools/dataspec-status-check.mjs --format json` 和 `git diff --check`。
- [x] 4.4 因新增 MCP/AI 外部协议和本地状态脱敏边界，启动独立子 agent 只读评审并关闭 agent；修复 findings 或记录技术理由。
- [x] 4.5 在本文件记录 `Verification Evidence`，包含测试、OpenSpec、契约检查、状态检查、评审和剩余风险。

## Verification Evidence

- TDD 红灯：`node --test tools/dataspec-mcp.test.mjs` 曾失败于缺少 `dataspec://project/7/session-state` 和 `get_session_state`；`node --test tools/dataspec-cli-mcp-contract-check.test.mjs` 曾失败于缺少 bundled `get_session_state` fixture；评审修复阶段新增真实 cache metadata、带引号 JSON secret、项目不匹配、resource name/mimeType drift 和 URL userinfo 检查红灯。
- 定点测试：`node --test tools/dataspec-mcp.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 68 pass、0 fail；评审修复后单独重跑 `node --test tools/dataspec-mcp.test.mjs` 44 pass、0 fail，`node --test tools/dataspec-cli-mcp-contract-check.test.mjs` 25 pass、0 fail。
- 契约检查：`node tools/dataspec-cli-mcp-contract-check.mjs --format json` 返回 `ok=true`，summary 为 20 CLI、10 MCP tools、9 MCP resources、8 resource templates、7 prompts、0 diagnostics。
- 全量 tools：`node --test tools/*.test.mjs` 362 pass、2 skipped、0 fail；skipped 均为当前平台无法创建 symlink 的既有用例。
- OpenSpec：`openspec validate add-mcp-session-state-memory --strict` valid；`node tools/dataspec-openspec-readiness.mjs --change add-mcp-session-state-memory --format json` 返回 `readinessScore=100`、`readinessLevel=READY`、`missingFacts=[]`。
- 状态检查：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，唯一 warning 为 active change `add-mcp-session-state-memory` 尚未归档；归档后需重跑。
- Diff 检查：`git diff --check` 通过，仅输出 Windows 工作区 LF/CRLF 提示，无 whitespace error。
- 独立评审：agent `019f3db6-49ae-71f2-b7a2-d5462b1f9c61` 用途为 P6-178 MCP session-state 只读评审；发现 1 个 Critical 和 3 个 Important，已修复带引号 JSON secret 脱敏、真实 `exportOptions/exportedAt` cache metadata 读取、项目不匹配缓存快照隔离、resource `name/mimeType` drift 检查和 URL userinfo secret scanner；agent 已关闭。
- 剩余风险：第一版不写 `.dataspec/session-state.json`，不做云端长期记忆，不跨用户同步，不把 session-state 当权限依据；真实权限仍由后端 API、token、capability safety、dry-run 和用户确认决定。
