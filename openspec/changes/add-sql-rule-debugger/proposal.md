## Why

DataSpec 的 SQL lint 规则、参数和豁免能力不断增加，但当前结果只面向最终问题展示，用户和 AI 无法稳定判断某条规则为什么命中、为什么没命中、使用了哪些参数和豁免状态。误报排查、规则参数调整和 AI 补标准时只能反复猜测，影响 SQL 标准治理闭环。

本变更按 SDD standard 处理，因为它新增 `/api/lint/debug` 和 CLI 调试输出这类 API / CLI 外部可观察契约，同时不改变既有 `/api/lint` 响应字段、持久化语义或写入边界。

## What Changes

- 新增只读 SQL 规则调试能力，返回每条规则的 `ruleCode`、`severity`、`enabled`、`paramsSnapshot`、`matchTrace`、`sourceRange`、`fixStrategy`、`suppressionStatus` 和 `debugNotes`。
- 后端新增 `/api/lint/debug`，复用现有 SQL 解析、规则配置、source range、fix policy 和 suppression 逻辑，默认不保存 SQL 检查记录。
- CLI 新增 `lint-debug` 命令，读取文件或 stdin，调用 `/api/lint/debug` 并输出稳定 JSON，供本地 AI agent 排查规则。
- 前端 SQL 校验页新增规则调试面板，允许用户查看规则启用状态、命中理由、参数快照、source range、修复策略和豁免建议入口。
- AI 能力清单新增或扩展 SQL 规则调试入口，说明该能力只读、适合规则排障和补标准，不执行写入。
- 不做 breaking change；既有 `/api/lint`、CLI `lint`、SQL 检查记录和历史响应兼容字段不变。

## Capabilities

### New Capabilities

- `sql-rule-debugger`: 定义 SQL 规则调试 API、调试 trace 字段、前端调试面板和 AI 可读排障输出。

### Modified Capabilities

- `dataspec-cli`: CLI 新增 `lint-debug` 命令，输出 SQL 规则调试 JSON。
- `ai-capability-catalog`: 能力清单暴露 SQL 规则调试入口和只读安全边界。

## Impact

- 后端：SQL lint service / controller、调试响应 DTO、规则配置与豁免状态映射、对应单元或集成测试。
- CLI：`tools/dataspec-cli.mjs`、CLI usage 文案和契约测试。
- 前端：`dataspec-web` 的 lint API wrapper、类型定义、`SqlLint.vue` 调试面板和 smoke 测试。
- OpenSpec / 文档：新增 `sql-rule-debugger` spec delta，更新 CLI 与 AI capability catalog delta，必要时更新 README 的 CLI 命令说明。
- 安全与兼容：调试接口只返回规则参数快照、SQL source range 和 lint 上下文，不新增写入、凭据展示或复杂 AST 编辑器；敏感字段继续按现有脱敏约束处理。
