## Context

DataSpec 已有 workflow recipes、AI task run、执行证据包、AI 会话启动包和 CLI/MCP 入口。缺口不是再新增一个异步任务系统，而是给 AI 和用户一张轻量、可复制、可恢复的任务卡，明确当前目标、输入、步骤、下一条安全命令和停止条件。

P6-62 跨 CLI、MCP、前端展示和 workflow recipes，因此按 A 档处理；第一版以本地文件和结构化输出为主，不新增数据库表，不把同步接口改成异步队列。

## Goals / Non-Goals

**Goals:**

- 定义稳定 task card JSON，包含 `goal`、`inputs`、`currentStep`、`steps`、`allowedActions`、`artifacts`、`resumeCommand`、`validationCommands`、`stopConditions`、`risks` 和 `updatedAt`。
- 支持从现有 workflow recipe 生成初始任务卡。
- CLI 能创建、展示和更新本地任务卡文件，并输出 JSON 或 Markdown。
- MCP 能创建或渲染任务卡，便于 AI 客户端一跳获得当前进度与下一步。
- 前端能展示任务卡摘要和恢复信息，不需要第一版保存远端任务卡。
- 输出不包含 token、Authorization header、数据库密码、完整 JDBC URL 或业务数据行。

**Non-Goals:**

- 不新增企业审批流、多人审核、发布流或复杂权限治理。
- 不实现外部队列、后台调度或长任务编排引擎。
- 不自动执行 workflow steps，不调用外部 LLM。
- 不把所有已有接口改成 task-run 异步模式。
- 不持久化数据库连接凭据或完整业务 SQL 文件内容。

## Decisions

1. 任务卡第一版使用本地 JSON 文件，而不是后端表。
   - 原因：P6-62 主要解决 AI 会话恢复与步骤边界；本地文件更适合业务仓库和 CLI/MCP 使用。
   - 备选：新增后端 `ds_ai_task_card` 表。放弃原因是会引入权限、迁移、同步和历史清理问题，超出第一版。

2. task card 从 workflow recipe 派生，但不替代 recipe。
   - recipe 描述“通用做法”，task card 描述“本次任务做到哪里”。
   - CLI/MCP 生成任务卡时复制 recipe 的步骤、验证命令和失败恢复建议，并附加用户输入。

3. 状态流保持有限集合。
   - card status：`PLANNED`、`IN_PROGRESS`、`BLOCKED`、`READY_FOR_REVIEW`、`DONE`。
   - step status：`PENDING`、`IN_PROGRESS`、`DONE`、`SKIPPED`、`BLOCKED`。
   - 原因：让 AI 和前端可以稳定渲染，不用猜字符串。

4. 更新命令只改任务卡文件，不执行步骤。
   - `task-card update` 只设置 step/status/artifact/resumeCommand/notes，不调用 lint、reverse-import 或 DDL。
   - 真实工作仍由已有 CLI/MCP/API 明确执行。

5. Markdown 是渲染产物，JSON 是源。
   - 原因：JSON 供 AI/CLI/MCP 继续处理，Markdown 供用户阅读和粘贴到交接记录。

## Risks / Trade-offs

- [Risk] 本地任务卡可能与真实执行状态漂移。→ Mitigation：任务卡记录 `updatedAt`、`validationCommands` 和 `artifacts`，并明确它不是远端事实来源。
- [Risk] 任务卡把敏感输入写入文件。→ Mitigation：生成和更新时统一脱敏，并拒绝明显 token/password/JDBC URL 进入安全字段。
- [Risk] CLI/MCP/前端字段漂移。→ Mitigation：核心构建逻辑放在共享 Node module，前端只渲染协议字段，测试覆盖 JSON shape 和 Markdown 关键字段。
- [Risk] 用户误以为 task card 会自动执行任务。→ Mitigation：docs、CLI help 和 card `stopConditions` 明确“只描述，不执行”。

## Migration Plan

- 无数据库迁移。
- 新增 CLI/MCP/前端能力为 additive change，不影响现有 workflow、task run 或 bootstrap 命令。
- 后续如果需要远端历史，可在保持 JSON 契约兼容的前提下新增保存 API。
