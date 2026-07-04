## Why

AI 批量 lint、Context 导出、覆盖率扫描、反向导入比对和证据包生成已经能产出结果，但失败后缺少统一的任务状态、失败步骤、可重试性和恢复命令。AI agent 只能从头再跑，容易重复写入、浪费上下文，也难以把“继续执行”交给 CLI/MCP/前端。

## What Changes

- 新增项目级 AI task run 轻量模型，记录 `taskType`、`status`、`stepStatus`、`inputHash`、`retryable`、`failedStep`、`resumeCommand`、`partialArtifacts`、`expiresAt` 和安全 metadata。
- 为高频 AI 任务提供统一查询入口：最近失败/运行中的任务、任务详情、可恢复动作和幂等重试提示。
- AI batch SQL lint 首批接入 task run 状态记录，失败项保留 partial artifacts，重复 retry 不产生重复 task run。
- CLI/MCP 暴露只读任务状态查询和可复制恢复命令，供 AI 自动判断继续还是提示用户。
- 前端在 AI 批量任务/工作台相关入口展示最近失败任务、失败步骤、retryable 状态和恢复动作。

## Capabilities

### New Capabilities

- `ai-task-runs`: 项目级 AI 任务运行状态、失败诊断、断点恢复和安全查询契约。

### Modified Capabilities

- `ai-batch-delivery-package`: AI batch SQL lint 创建和结果包需要关联 task run，并在失败/部分成功时提供恢复信息。
- `dataspec-cli`: CLI 增加 task run 查询/详情命令，输出 AI 可读 JSON 和恢复命令。
- `dataspec-mcp`: MCP 增加 task run resource/tool，让 AI 客户端读取最近失败任务和恢复建议。
- `frontend-task-entrypoints`: 前端任务入口展示最近失败或可恢复任务，并提供重试/复制命令动作。
- `ai-evidence-package`: 证据包来源可引用 task run，便于失败任务交接和复盘。

## Impact

- 后端：新增 Flyway 迁移、实体/mapper/repository、task run service/controller，并接入 AI batch SQL lint 首批记录。
- 前端：更新 OpenAPI 类型、API 封装、AI 批量任务/工作台入口的恢复状态展示和冒烟测试。
- CLI/MCP：新增 task run list/show 能力，复用现有 DataSpecError 和脱敏边界。
- 文档与规格：README/TODO/OpenSpec 更新，明确第一版边界是不引入外部队列、不做分布式调度、不把所有同步 CRUD 改造成异步任务。

## Verification Evidence

- `mvn test`：389 tests，0 failures，0 errors。
- `pnpm test`：101 tests，0 failures。
- `pnpm build`：通过；仅保留依赖 pure annotation 和 chunk size warning。
- `node --test tools\*.test.mjs`：133 tests，0 failures。
- `node --test tools\dataspec-cli.test.mjs tools\dataspec-mcp.test.mjs`：106 tests，0 failures，用于复核 CLI/MCP 字段对齐修复。
- `pnpm check:api`：`src/api/schema.ts` 已是最新。
- `openspec validate --all`：92 passed，0 failed。
- `git diff --check`：无空白错误，仅 LF/CRLF 换行提示。
- 独立代码评审 agent：初审发现 2 个 P2（MCP `id/taskRunId` schema、task run `partialArtifacts` 测试字段漂移），已修复；复核确认无新的阻断性问题。
