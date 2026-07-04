## Context

DataSpec 已经有 AI job replay、AI batch delivery package、执行证据包、项目活动时间线和单机写入幂等保护。它们分别能记录结果、交付包、证据和重复写保护，但还缺少一层统一的“任务运行状态”：失败发生在哪一步、是否可重试、用什么命令恢复、哪些 artifact 已经产生、AI 是否应该继续。

P6-58 第一版优先解决个人/小团队本地使用场景，避免引入队列系统或后台调度。任务仍由现有同步 API/CLI 流程触发，task run 只负责记录状态、恢复建议和可查询的断点信息。

## Goals / Non-Goals

**Goals:**

- 提供项目级 AI task run 持久化模型，覆盖状态、步骤、输入 hash、失败诊断、恢复命令和 partial artifacts。
- 首批把 AI batch SQL lint 接入 task run，让部分失败和重复重试有统一记录。
- 暴露 API、CLI、MCP 和前端入口，让 AI 能查询最近失败任务并判断继续或提示用户。
- 所有 task run metadata 和 artifacts 摘要必须复用敏感信息脱敏边界。

**Non-Goals:**

- 不引入外部任务队列、分布式调度、定时任务或后台 worker。
- 不把所有同步 CRUD 改造成异步任务。
- 不自动重新执行外部副作用操作；第一版只提供可复制/可调用的恢复命令和幂等 key。
- 不保证服务重启后恢复内存中的幂等结果；持久 task run 只保存诊断和恢复信息。

## Decisions

### 新增 `ds_ai_task_run`

使用独立表而不是继续扩展 `ds_ai_job_record` 或 `ds_ai_batch_run`。AI job 更偏“AI 输入输出回放”，AI batch 更偏“SQL lint delivery package”，task run 则是跨任务类型的运行状态与恢复索引。独立表可以避免把每个业务表都塞入恢复字段。

关键字段：

- `project_id`、`task_type`、`source_type`、`source_id`：绑定项目和来源对象。
- `status`：`RUNNING`、`SUCCEEDED`、`PARTIAL_FAILED`、`FAILED`、`CANCELLED`、`EXPIRED`。
- `input_hash`、`idempotency_key`：帮助 AI 和 CLI 判断重复 retry。
- `step_status_json`：小型步骤状态数组，包含 step、status、message、artifactRef。
- `retryable`、`failed_step`、`resume_command`、`next_action`：恢复判断核心字段。
- `partial_artifacts_json`、`metadata_json`：只保存脱敏摘要，不保存 token、密码、完整 JDBC URL 或业务数据行。
- `started_at`、`finished_at`、`expires_at`：支持最近失败任务和过期清理提示。

### 先接入 AI batch SQL lint

AI batch 已有批量运行、部分失败 item、delivery package 和幂等保护，最适合验证 task run 模型。Context 导出、覆盖率扫描、反向导入比对和 evidence package 在第一版先通过通用 API/CLI 查询模型承接，后续逐步接入具体记录点。

### 重试语义是“恢复建议”，不是自动调度

`retryable=true` 表示系统认为用户或 AI 可以用 `resumeCommand`/`idempotencyKey` 安全重试。后端不自动重放任务，避免隐藏副作用和长任务调度复杂度。CLI/MCP 第一版可以读取任务和复制命令，后续再做自动 retry 执行器。

### API 只做项目级只读查询和记录辅助

新增只读 API：

- `GET /api/ai-task-runs?projectId=&status=&taskType=&current=&size=`
- `GET /api/ai-task-runs/recent-failures?projectId=&limit=`
- `GET /api/ai-task-runs/{id}?projectId=`

记录由服务内部完成，不开放任意创建接口给前端，避免用户伪造任务历史。

## Risks / Trade-offs

- [Risk] task run 与具体业务记录可能出现不一致。→ Mitigation：只把 task run 作为诊断索引，sourceId 指向 AI batch/evidence 等权威记录。
- [Risk] metadata 泄露敏感输入。→ Mitigation：写入前统一使用 `SensitiveDataSanitizer`，测试覆盖 token/password/JDBC URL。
- [Risk] 过早抽象成通用任务平台。→ Mitigation：第一版只做持久模型、查询、AI batch 接入和恢复命令，不做调度器。
- [Risk] CLI/MCP 自动执行 retry 可能产生副作用。→ Mitigation：第一版输出命令和 task run JSON，由调用方显式执行。

## Migration Plan

1. 新增 Flyway 迁移创建 `ds_ai_task_run` 和必要索引。
2. 实现 entity/mapper/repository/service/controller。
3. AI batch 创建流程写入 task run：开始、成功、部分失败或失败。
4. 更新 OpenAPI 类型、前端 API 和页面入口。
5. CLI/MCP 增加只读任务查询能力。
6. 更新 README/TODO，归档 OpenSpec。

Rollback：删除新表和新入口不会影响已有 AI batch、AI job、SQL lint 主流程；若 task run 记录失败，主流程仍返回原结果。
