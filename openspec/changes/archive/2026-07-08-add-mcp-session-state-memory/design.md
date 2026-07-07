## Context

现有 MCP server 已提供 `session-bootstrap`、`capability-catalog`、`workflow-recipes`、`agent-guidance-pack`、`ai-task-runs` 和 task-card 工具；其中 `session-bootstrap` 从后端读取项目能力启动包，task-card 是本地计划快照。P6-178 需要的是更靠近 MCP 会话本身的“当前项目记忆”：AI 不需要每次都重新推断当前 project/profile/context cache，也不应把 token、JDBC URL 或业务数据行写入长期记忆。

本 change 按 SDD full 处理，因为它新增 MCP/AI 外部协议并涉及本地状态与脱敏边界。

## Goals / Non-Goals

**Goals:**
- 为 MCP 暴露稳定、只读的 `dataspec-mcp-session-state` JSON。
- 复用 `.dataspec/config.json`、`.dataspec/context/cache-metadata.json` 和当前 MCP 启动参数，避免新增后端 API。
- 输出 currentProject、currentSnapshot、lastTaskResult、toolCursor、safeDefaults、redactedMemory、diagnostics 和 nextActions。
- 所有输出都经过现有敏感信息脱敏策略，fixture 和测试覆盖 token/password/JDBC URL/DSN 不外泄。

**Non-Goals:**
- 不做云端长期记忆、不跨用户同步、不读取业务数据行。
- 不把 session state 当权限依据；真实权限仍由后端 API 和 token 校验。
- 不自动写 `.dataspec/session-state.json`；第一版只读聚合，后续如果需要可另开 change 增加显式写入/清理命令。
- 不替代 `session-bootstrap`；session-state 指向 bootstrap 作为远端能力启动包。

## Decisions

1. 使用本地只读聚合，而不是新增后端 session API。
   - 理由：P6-178 第一版主要解决 MCP 会话重复确认问题；现有 config/context/task-card 已有足够信号。新增后端 API 会扩大 Java/API/OpenAPI/前端影响面。
   - 备选：新增 `/api/mcp/session-state`。暂不采用，因为它会引入服务端状态语义和权限边界。

2. resource 和 tool 同时暴露。
   - 理由：MCP 客户端有的偏向先读 resource，有的偏向调用 tool；现有 DataSpec 已对 bootstrap 采用 resource/tool 双入口。
   - 兼容性：新增 `dataspec://project/<id>/session-state` 和 `get_session_state`，不改变现有 URI 或 tool。

3. `redactedMemory` 只保存脱敏摘要和存在性，不保存原始输入。
   - 理由：AI 需要知道“配置了 token / context cache / profile”，不需要知道 token 值或连接串。
   - 约束：字段值、notes、resumeCommand、server URL userinfo、JDBC/DSN 均走 `sanitizeSecretValue` / `sanitizeSecretText`。

4. `lastTaskResult` 第一版只引用最近 task run 和 task-card 的恢复入口，不尝试合并完整执行结果。
   - 理由：已有 `ai-task-runs` resource 和 `get_ai_task_run` tool 可读详情；session-state 只应告诉 AI 下一步去哪读，避免复制大量结果。

## Risks / Trade-offs

- [Risk] 本地 config 中可能包含 apiToken。→ Mitigation：session-state 只输出 `authMode` 和 `apiTokenPresent`，不输出 raw token。
- [Risk] AI 把 session-state 误当授权。→ Mitigation：safeDefaults 和 risks 明确“不是权限依据”，写入仍需读取 capability safety。
- [Risk] 本地状态可能过期。→ Mitigation：输出 `generatedAt`、context cache metadata 和 `diagnostics`，提示运行 `get_session_bootstrap` 或 `doctor` 刷新。
- [Risk] 新 MCP descriptor 漂移破坏客户端。→ Mitigation：更新 CLI/MCP contract fixture，并运行 fixture check 与 MCP 单测。

## Migration Plan

1. 新增测试锁定 resource/tool descriptor、输出结构、projectless 行为和脱敏。
2. 实现只读 session-state 聚合 helper，并接入 MCP resources/list、resources/templates/list、resources/read、tools/list、tools/call。
3. 更新 fixture 和文档。
4. 验证后保留 additive 协议；如需回滚，可移除新增 resource/tool 与 fixture，不影响既有 MCP 能力。

## Open Questions

- 后续是否需要显式 `session-state write/clear` 命令，由用户确认后再增加；本 change 不做。
