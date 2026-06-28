## Why

AI 完成 SQL 修复、覆盖率分析、反向导入或批量检查后，用户需要一份可交付、可复盘、可继续传给下游 AI 的证据包，而不是只依赖页面结果或聊天摘要。P6-38 第一版要把已有记录、快照、命令、输出摘要和验证建议整理成只读归档物，并明确脱敏边界。

## What Changes

- 新增 AI 执行证据包导出能力，支持按 AI job、SQL check、coverage report 和 AI batch run 生成 JSON 证据包。
- 新增 zip 导出，至少包含 `evidence.json`、`summary.md` 和 `README.md`，用于人工交付和 AI 续跑。
- 证据包包含 source、standardSnapshot、inputsSummary、outputsSummary、validationSummary、artifacts、nextActions、suggestedCommands 和脱敏后的 diagnostics。
- 后端提供只读 API，前端在高频结果页提供“复制证据 JSON / 下载证据包”的最小入口。
- CLI/MCP 新增读取 evidence package 的命令/tool，保持机器可读输出。
- 更新 AI 契约文档和 fixtures，确保 evidence package 稳定字段、脱敏约束和非审计边界可测试。
- 不新增长期对象存储，不上传第三方，不把证据包变成企业审计、审批或权限系统。

## Capabilities

### New Capabilities

- `ai-evidence-package`: AI 执行证据包结构、导出 API、zip 内容、脱敏和稳定字段。

### Modified Capabilities

- `dataspec-cli`: CLI 增加 evidence 导出命令，支持 JSON/zip 输出。
- `dataspec-mcp`: MCP 增加 evidence package tool/resource，供 AI 客户端读取。
- `ai-contract-fixtures`: 契约测试覆盖 evidence package 稳定字段和敏感信息剔除。
- `frontend-task-entrypoints`: 前端高频结果页增加复制/下载证据包入口。

## Impact

- 后端：新增 evidence package DTO/service/controller；复用 AI job、SQL check record、coverage report、AI batch run 和标准快照数据；新增脱敏工具或复用现有脱敏逻辑。
- 前端：新增 evidence API wrapper、类型导出和最小按钮/菜单入口，优先接 SQL 校验记录与覆盖率报告。
- CLI/MCP：新增命令和工具，输出稳定 JSON；zip 输出只写用户指定路径。
- 文档/规范：更新 README、docs/ai-contracts.md、TODO 和 OpenSpec specs。
- 测试：后端单测覆盖四类 source、zip 内容和脱敏；前端 smoke 覆盖入口；Node 测试覆盖 CLI/MCP；`mvn test`、`pnpm test/build`、`node --test` 和 `openspec validate --all` 作为验证入口。
