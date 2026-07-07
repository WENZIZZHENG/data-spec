## Why

AI 通过 MCP 使用 DataSpec 时，经常需要反复确认当前项目、默认 profile、最近上下文缓存、可恢复任务入口和安全默认值。现有 `session-bootstrap` 偏向远端项目能力启动包，缺少一个本地、只读、可在 MCP resources/tools 之间共享的会话状态快照。

## What Changes

- 在 MCP server 中新增只读 `session-state` 能力，聚合本地 `.dataspec/config.json`、上下文缓存元数据和 MCP 启动参数形成稳定 JSON。
- 暴露 `dataspec://project/<id>/session-state` resource 和 `get_session_state` tool，供 AI 在连续 MCP 调用中读取 currentProject、currentSnapshot、lastTaskResult、toolCursor、safeDefaults、redactedMemory 和 nextActions。
- 对所有本地状态输出执行统一脱敏；状态中只允许出现 token/password/JDBC URL/DSN 的存在性或脱敏占位，不写入 raw secret，不读取业务数据行。
- 更新 MCP descriptor fixture、README/TODO 和 OpenSpec 记录，使新增外部协议可被本地契约检查发现漂移。

## Capabilities

### New Capabilities

### Modified Capabilities
- `dataspec-mcp`: 新增 MCP session-state resource/tool、只读输出结构和敏感信息脱敏边界。
- `cli-mcp-contract-fixtures`: 扩展 MCP fixture 覆盖新增 session-state resource/tool descriptor，防止协议漂移。

## Impact

- 代码范围：`tools/dataspec-mcp.mjs`、`tools/dataspec-mcp.test.mjs`、`tools/fixtures/cli-mcp-contracts.json` 和必要的契约检查测试。
- 文档范围：`README.md` 的 MCP/验证说明、`TODO.md` 的 P6-178 状态和本 change 的 Verification Evidence。
- 协议影响：新增 MCP resource URI 和 tool name，为向后兼容的 additive change；不删除或重命名既有 MCP resources/tools/prompts。
- 安全边界：第一版只读聚合本地状态，不新增后端 API，不自动写会话记忆文件，不跨用户同步，不把会话状态当权限依据。
