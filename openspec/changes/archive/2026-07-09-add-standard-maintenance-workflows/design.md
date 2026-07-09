## Context

DataSpec 已有标准候选 Inbox、字段质量评分、字段覆盖率报告、AI 任务推荐队列、AI workflow recipes、AI task card、任务运行失败恢复和前端任务入口。这些能力已经能指出“哪里需要维护”，但信号分散在不同页面和 API 中，AI 或用户需要手工拼出“预检 -> 执行 -> 验证 -> 归档”的步骤。

P6-181 命中 SDD full：它新增后端只读 API 契约、前端入口、tools/CLI/AI 可读 recipe，并把多个诊断来源聚合为同一种 workflow 计划结构。第一版仍服务个人/小团队本地使用，不引入审批、后台调度或自动批量写入。

## Goals / Non-Goals

**Goals:**
- 新增标准维护 workflow dry-run 计划能力，输出 `inboxAction`、`recipeBinding`、`dryRunSteps`、`executionState`、`undoHint`、`evidenceLinks` 和 `nextActions`。
- 聚合待处理候选、低质量字段、未纳管字段、规则/质量冲突和 AI 任务失败恢复信号，转成安全、可复制、可验证的步骤。
- 前端可从标准候选、字段质量和覆盖率页面发起 dry-run，并展示计划、证据、恢复位置和人工确认边界。
- tools/CLI/MCP/AI Context 可读取 `standard-maintenance` recipe，并能把推荐队列绑定到同一 recipe。
- 所有计划输出只包含脱敏摘要、计数、来源能力和安全命令模板，不返回 raw evidence、SQL 原文、AI payload、JDBC URL、DSN、token、password 或 Authorization。

**Non-Goals:**
- 不自动批量采纳、合并、忽略或修改标准字段。
- 不新增团队审批系统、后台任务调度、长生命周期 workflow runner 或数据库迁移。
- 不替代候选决策、字段编辑、覆盖率扫描、质量门禁或 task-run 现有 API。
- 不执行外部 LLM、业务仓库命令、数据库写入或源库扫描；第一版只生成 dry-run 计划和下一步说明。

## Decisions

1. 新增只读 plan API，而不是在现有候选/质量/覆盖率接口里混入执行步骤。

   方案：新增 `/api/standard-maintenance/workflows/plan`，请求提供 `projectId`、`sourceType`、`sourceIds`、可选 `issueCodes` 和 `coverageStatuses`；响应统一返回 workflow 计划。

   原因：候选、质量和覆盖率报告的职责是诊断或决策；维护 workflow 是跨来源编排层。独立只读 API 能让前端、CLI/MCP 和 AI 推荐复用同一契约，同时不改变既有写入 API 的幂等边界。

   备选：在每个来源接口各自返回 workflow 字段。放弃原因是会形成多份计划结构，AI 难以稳定消费，前端也会重复展示逻辑。

2. 第一版只生成 dry-run 计划，不持久化 workflow 实例。

   方案：`executionState` 描述 `DRY_RUN`、`WAITING_CONFIRMATION`、`BLOCKED`、`READY_FOR_REVIEW` 等计划态；计划由请求实时生成，不新增 workflow 表。

   原因：P6-181 的核心缺口是“把待处理事项转成可执行步骤”，不是运行编排平台。无持久化能降低迁移成本，并避免误导用户以为系统会后台执行步骤。

   备选：新增 workflow run 表记录计划和步骤状态。放弃原因是会扩展存储语义和清理策略，后续若需要跨会话恢复可基于 AI task runs 单独设计。

3. 复用 workflow recipe 与 task-card 概念，新增 `standard-maintenance` recipe。

   方案：tools 中扩展 `WORKFLOW_RECIPES`，后端计划响应的 `recipeBinding.recipeId` 固定指向 `standard-maintenance`，推荐队列也返回同一 binding。

   原因：AI Context、CLI/MCP resource 和 task-card 已经围绕 recipe 建立稳定入口；新增 recipe 比新增一套 AI 协议更小。

4. 计划步骤显式区分 precheck、review、execute、verify、archive。

   方案：`dryRunSteps` 每步包含 `stepId`、`phase`、`title`、`description`、`recommendedAction`、`requiresConfirmation`、`expectedEvidence` 和 `status`。执行类步骤只引用现有 API/页面动作模板，不能由 plan API 自动执行。

   原因：用户和 AI 需要知道下一步怎么做，也需要知道哪些步骤必须停下来人工确认；结构化 phase 便于前端展示和 CLI/MCP 消费。

5. evidence 只用安全链接和摘要，不携带原始 payload。

   方案：`evidenceLinks` 使用 `sourceCapability`、`label`、`targetRoute`、`summary`、`count` 等字段；所有自由文本经既有脱敏工具处理。

   原因：计划对象会被 AI 复制到任务卡或提交说明，必须默认可安全分享。

## Risks / Trade-offs

- [用户误以为 dry-run 会自动执行] -> 响应 `executionState`、`dryRunSteps.requiresConfirmation`、`undoHint` 和前端文案明确“只生成计划”，执行仍需显式调用既有 API。
- [聚合来源不完整导致计划空泛] -> 第一版优先候选、质量、覆盖率和 AI 推荐信号；没有具体来源时返回 fallback precheck 和 nextActions。
- [敏感信息泄漏] -> 后端统一脱敏 evidence、route、action 文本；前端只展示计划摘要，不显示 raw evidence。
- [OpenAPI/前端类型漂移] -> 新 DTO 补 `@Schema` 和 Javadoc，前端 API 类型同步，加入后端/前端/tools 契约测试。
- [recipe 与状态检查漂移] -> 更新 workflow recipe、AI contract/status check 相关测试，确保新增 recipe 被文档和 TODO 摘要识别。

## Migration Plan

1. 先补 OpenSpec strict 验证，确认新增和修改能力契约可归档。
2. TDD 红灯：后端计划 API/model/service/controller 测试、tools recipe/task-card 测试、前端计划入口展示测试。
3. 实现后端只读 plan API 和模型，不新增数据库迁移，不改候选/字段写入 API。
4. 扩展 tools workflow recipe、task-card/状态检查相关 fixture 或文档。
5. 更新前端 API wrapper 和候选/质量/覆盖率页面 dry-run 入口。
6. 运行后端、前端、tools、OpenSpec 和通用检查；启动独立子 agent 只读评审并处理 Critical/Important findings。
7. 补充 `Verification Evidence`，归档 OpenSpec，运行 `openspec validate --all`，更新 TODO，按 Git 规则本地 commit。

回滚策略：删除新增 plan API、前端入口和 `standard-maintenance` recipe；由于第一版不新增持久化和不自动写入标准字段或源数据库，无需数据回滚。

## Open Questions

- 第一版是否要把 AI task runs 最近失败项作为后端 plan 来源直接查询？当前先通过 AI 推荐队列和请求 sourceType 承接，避免扩大 API 扫描范围。
- 维护 workflow 是否需要持久化用户勾选的步骤状态？第一版不做，后续若需要可复用 task-card 或 task-run 能力。
