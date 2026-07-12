## Why

DataSpec 会把字段注释、样例、业务术语、使用示例和数据库 metadata 交给 AI 工具使用，这些文本可能包含提示注入片段、secret-like 内容或用户不希望暴露给外部 AI 的业务细节。当前 AI Context 只有敏感字段布尔值和通用脱敏工具，缺少统一的可信边界、字段可见性决策和本地安全红线配置，后续所有 AI Context、MCP resource 和证据包都会持续继承这类风险。

## What Changes

- 为 AI Context 导出增加安全摘要和不可信文本边界：`manifest.json`、`field-catalog.json`、`README.md`、`prompts.md`、`AGENTS.md.fragment` 明确区分 DataSpec 指令、工具契约和业务原文。
- 在字段目录中以 additive 字段输出 `contextSafety` 与 `exportDecision`：标记 `sourceTrustLevel`、`instructionBoundary`、`redactionReasons`、`visibility`、`maskingProfile`、`allowedTasks` 和 `warnings`。
- 导出字段、业务术语、使用示例和 prompt 输入时复用现有 `SensitiveDataSanitizer`，遇到 secret-like 文本时保留可复核的脱敏原因，不把原始 secret 写入 AI Context。
- 为 CLI/MCP 本地配置增加可选 `securityProfile`，用于表达个人安全红线：严格脱敏、敏感字段默认遮蔽、允许的 AI 工具、永不导出的文本模式和本地路径边界。
- 更新 OpenSpec、测试和验证证据；本变更保持兼容，不删除既有 AI Context 字段。

## Capabilities

### New Capabilities

- `ai-context-safety-controls`: AI Context 安全边界、字段级可见性决策、本地 security profile 和导出安全摘要。

### Modified Capabilities

- `ai-context-package`: AI Context 包新增安全摘要、字段级 contextSafety/exportDecision、业务文本不可信边界说明和兼容 schema。
- `ai-context-scoped-export`: scoped export 继续按现有 scope/query/profile 裁剪，同时把被排除或遮蔽的敏感字段计入安全摘要。
- `dataspec-local-config`: `.dataspec/config.json` 支持可选 `securityProfile`，CLI/MCP 读取后可用于 AI Context 安全策略。
- `sensitive-data-sanitization`: 既有 sanitizer 继续作为 AI Context 导出的 secret-like 文本脱敏基础。

## Impact

- 后端：修改 `AiContextExportService` 的字段目录、manifest、README、prompt 和 AGENTS fragment 生成；新增或更新 AI Context 导出单测。
- CLI/MCP：扩展 `tools/dataspec-config.mjs` 配置解析和测试，保留命令参数协议。
- OpenSpec：新增 `ai-context-safety-controls`，并补充相关既有能力的 delta spec。
- 安全边界：不新增数据库表，不保存真实 secret，不扫描业务数据行，不做组织级权限审批；第一版只在 AI Context 导出和本地配置读取链路提供确定性安全约束。
