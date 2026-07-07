## Why

跨来源标准证据视图已经提供 `GET /api/standard-evidence`，但 AI 新会话通常先读取 capability catalog 或 README 来决定可用入口。若能力清单缺少该入口，AI 仍会回到零散查询字段、热区和候选接口，无法稳定发现“单字段证据包”能力。

## What Changes

- 在 AI capability catalog 中新增稳定能力 `standard-evidence`，描述跨来源标准证据视图的 API surface、输入、输出、安全边界、preflight 和 next actions。
- 更新 capability catalog 测试，确保列表、单项查询、版本兼容 supported capabilities 和 safety metadata 均包含 `standard-evidence`。
- 更新 README 的 AI 能力清单和 AI 自动化说明，让用户和 AI 知道可通过 `GET /api/standard-evidence?projectId=<id>&subjectType=FIELD&subjectId=<fieldId>` 查询证据视图。
- 不修改 `GET /api/standard-evidence` 的响应结构、聚合规则或后端证据 service。
- 不新增 CLI/MCP 命令；第一版只通过 capability catalog 公开 API discovery。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `ai-capability-catalog`: 能力清单需要描述 `standard-evidence` 只读证据视图入口和安全 metadata。

## Impact

- 后端：更新 `AiCapabilityCatalogServiceImpl` 内置能力列表和相关测试。
- 文档：更新 README 的 AI 能力清单和能力说明。
- 安全：能力条目只描述只读 API，不执行任务、不读取原始 SQL/AI payload，不返回 token、password、Authorization、JDBC URL、DSN 或业务数据行。
- 验证：运行 capability catalog 定点测试、OpenSpec strict、`git diff --check`、敏感词扫描和独立子 agent 评审。
