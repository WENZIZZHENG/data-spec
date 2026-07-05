## Why

DataSpec 已经通过 CLI、MCP、API 和前端向 AI 开放越来越多标准维护入口，但写入类能力目前缺少统一、机器可读的安全协议。AI 在批量导入、标准复用、恢复 apply、反向导入确认或 AI 回放等操作前，无法稳定判断哪些操作只读、哪些必须 dry-run、哪些需要幂等 key、哪些包含敏感输入或可撤销证据。

本变更按 SDD full 处理，因为它会调整 CLI/MCP/AI 外部协议、安全边界和高风险写入前置约束；目标是用产品内建约束降低误写风险，而不是引入组织审批、复杂 RBAC 或多人审核流。

## What Changes

- 定义统一 AI 写入安全协议 `ai-write-safety-protocol`，覆盖 `readOnly`、`writesProject`、`requiresDryRun`、`supportsUndo`、`requiresIdempotencyKey`、`sensitiveInputs` 和 `nextActions` 等 machine-readable 字段。
- 扩展能力清单，让高风险写入能力在 API/CLI/MCP/前端入口中暴露统一 `safety` metadata，并能让 AI 先枚举安全等级再执行动作。
- 扩展 CLI 行为，让写入相关命令能输出或校验安全协议；缺少必需 dry-run 或幂等 key 时返回结构化错误，且不输出 token/password/Authorization/JDBC URL 等敏感值。
- 扩展 MCP tool/resource 描述和调用保护，让 MCP 客户端在工具列表或调用结果中读取安全 metadata，并在缺少必要安全参数时得到 AI-readable error。
- 将已有幂等写保护与协议对齐：高风险批量写入默认要求 dry-run 或显式 apply 证据，缺少必需 idempotency key 时给出可恢复 nextActions。
- 前端批量写入、标准复用/恢复/反向导入等入口在确认 apply 前展示 dry-run 摘要，并复用同一安全协议字段说明风险。
- 不做 breaking change；新增字段保持兼容，既有只读命令、低风险单条个人 CRUD 和已有 HTTP 状态语义不被阻断。

## Capabilities

### New Capabilities

- `ai-write-safety-protocol`: 定义 AI/CLI/MCP/API/前端共享的写入安全 metadata、dry-run 要求、幂等 key 要求、敏感输入声明和结构化 nextActions。

### Modified Capabilities

- `ai-capability-catalog`: 能力清单条目新增统一 safety metadata，并继续保持只读自描述边界。
- `dataspec-cli`: CLI 写入类命令输出并校验 safety metadata、dry-run 和 idempotency key 要求。
- `dataspec-mcp`: MCP resources/tools 暴露并校验同一安全协议。
- `idempotent-write-guards`: 高风险写入缺少必要幂等参数时返回可恢复、机器可读的安全诊断。

## Impact

- 后端：能力清单 DTO/service/controller、幂等写保护错误诊断、高风险写操作的安全 metadata 来源和测试 fixture。
- CLI：`tools/dataspec-cli.mjs` 及对应契约测试，尤其是 capability、lint-files、restore/reuse/apply 类写入入口和 `Idempotency-Key` 透传。
- MCP：`tools/dataspec-mcp.mjs` 及工具/resource 契约测试，暴露 tool safety metadata 并校验调用参数。
- 前端：批量写入或 apply 前的 dry-run 摘要展示、API 类型与最小展示 helper。
- 文档与契约：README、AI contract 文档、OpenSpec delta、相关 golden/fixture 测试。
- 安全：所有日志、错误、测试快照和可复制输出必须保持敏感信息脱敏；协议只描述敏感字段类别，不返回 raw secret。
