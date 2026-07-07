## Why

AI 新会话通常先读取 `/api/bootstrap/session`，再决定下一步调用哪些 DataSpec 能力。`standard-evidence` 已进入 capability catalog，但 session bootstrap 使用白名单摘要，导致 AI 第一跳仍看不到“单字段跨来源证据视图”。

## What Changes

- 在 AI session bootstrap 的 `availableCapabilities` 摘要中加入 `standard-evidence`。
- 补充 bootstrap service 测试，确认 `standard-evidence` 出现在 READY 启动包中，且只声明 `GET /api/standard-evidence` API surface，不声明 CLI/MCP 命令。
- 更新 README 的 AI 会话启动包说明，提示 AI 可在字段标准来源、可信度和最近使用证据问题上读取 standard evidence。
- 不修改 `GET /api/standard-evidence` 响应结构、capability catalog entry、CLI/MCP descriptor 或 session bootstrap JSON shape。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `ai-session-bootstrap`: `availableCapabilities` 需要包含 `standard-evidence`，让 AI 第一跳能发现跨来源标准证据视图。

## Impact

- 后端：更新 `AiSessionBootstrapServiceImpl` 白名单和相关测试。
- 文档：更新 README 的 AI 会话启动包说明。
- 安全：启动包仍只读，不执行证据聚合，不返回 raw SQL、AI payload、候选 raw evidence、raw source metadata、token、password、Authorization、JDBC URL、DSN 或业务数据行。
- 验证：运行 bootstrap/capability 定点测试、OpenSpec strict、`git diff --check`、敏感词扫描和独立子 agent 评审。
