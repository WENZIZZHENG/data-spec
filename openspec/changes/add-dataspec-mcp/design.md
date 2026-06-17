## Context

P0 阶段目标是让 AI 优先用起来。后端已经提供 `/api/ai-context/*` 和 `/api/lint`，CLI 已经证明 HTTP-backed adapter 足够轻量。本变更继续复用后端 API，不在 MCP 进程里复制字段、规则或 SQL 解析逻辑。

## Decisions

### 使用 Node stdio adapter

- `tools/dataspec-mcp.mjs` 通过 stdin/stdout 收发 JSON-RPC 消息。
- 支持 `initialize`、`resources/list`、`resources/read`、`prompts/list`、`prompts/get`、`tools/list`、`tools/call` 和 `ping`。
- 只把协议适配和 HTTP 调用放在工具脚本中，业务能力仍由后端维护。

### 项目选择

- 启动参数 `--project <id>` 是默认项目，保证 resources/list 能列出确定 URI。
- `lint_sql` 和 `get_field_catalog` 允许传 `projectId` 覆盖默认值，方便同一 MCP server 在调试时临时访问别的项目。
- 第一版不维护多项目会话状态，也不做权限隔离。

### MCP 输出

- Resources 返回 text content，URI 使用 `dataspec://project/{projectId}/...`。
- Tools 返回 `structuredContent`，并同时提供 JSON text content，兼容只读取文本的 MCP client。
- Lint 发现 ERROR 不算工具调用失败；只有参数错误、HTTP 失败或后端业务错误才返回 JSON-RPC error。

## Risks

- MCP spec 会持续演进；本轮仅实现常见 MCP client 需要的基础方法和 JSON-RPC 结构。
- stdio adapter 默认信任本地调用者；远程鉴权、token 和多租户隔离留给后续安全专项。
