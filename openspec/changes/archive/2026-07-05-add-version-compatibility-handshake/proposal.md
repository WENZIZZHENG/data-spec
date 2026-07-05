## Why

DataSpec 的服务端、CLI、MCP 和 AI capability catalog 会持续演进；业务仓库里保留的 CLI/MCP 脚本可能落后于当前服务端。缺少统一版本握手时，AI 和用户只能在实际命令失败后猜测是服务不可达、能力未启用、CLI 太旧还是契约漂移，影响后续自动化开发效率。

本变更属于 SDD standard：它新增 API/CLI/MCP/AI 外部协议的可观察输出，但不涉及存储迁移、权限、安全边界或破坏性兼容策略。

## What Changes

- 新增只读版本兼容握手能力，提供服务端版本、API schema hash、最小推荐 CLI 版本、支持能力列表、废弃字段和升级/降级建议。
- CLI 新增或扩展可机器读取的兼容检查入口，供 AI 在执行关键命令前判断是否继续、降级或停止。
- MCP 暴露同一份兼容信息，作为 resource 或 tool 入口，避免 AI 只通过失败工具调用推断兼容状态。
- AI capability catalog 描述该只读能力，并在推荐首步中提示先读取兼容握手。
- 保持现有命令、资源和 API 的兼容字段不变；本次只做 additive contract。

## Capabilities

### New Capabilities

- `version-compatibility-handshake`: 描述 DataSpec 服务端、CLI、MCP 与 AI agent 之间的版本兼容握手契约。

### Modified Capabilities

- `dataspec-cli`: CLI 需要提供版本兼容检查命令或等价 JSON 输出，并在关键诊断中暴露兼容建议。
- `dataspec-mcp`: MCP 需要暴露版本兼容资源或工具，返回与 API/CLI 一致的结构化握手信息。
- `ai-capability-catalog`: capability catalog 需要包含版本兼容握手能力，并引导 AI 在关键任务前读取。

## Impact

- 后端：新增只读 API endpoint 或复用能力目录服务生成版本兼容 payload；不写数据库，不读取业务数据。
- CLI：扩展 `tools/dataspec-cli.mjs` 及契约测试，提供 JSON/text 输出与稳定退出码。
- MCP：扩展 `tools/dataspec-mcp.mjs` 及契约测试，提供 resource/tool 入口。
- 文档与规格：更新 README、OpenSpec delta 和 TODO 状态；验证需覆盖 CLI/MCP/API contract、OpenSpec strict 和文档状态检查。
