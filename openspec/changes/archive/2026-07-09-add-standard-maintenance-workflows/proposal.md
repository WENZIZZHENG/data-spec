## Why

覆盖率、字段质量、标准候选 Inbox、规则冲突和 AI 反馈已经能产生大量“该处理的事”，但这些信号仍散落在多个页面和 API 中。用户和 AI 需要一个稳定的维护 workflow 入口，把待处理项转成可预检、可执行、可验证、可归档的步骤，而不是在各页面手工拼接上下文。

## What Changes

- 新增标准维护 workflow 计划能力，聚合候选、低质量字段、未纳管字段、规则冲突和 AI 反馈失败项，输出 `inboxAction`、`recipeBinding`、`dryRunSteps`、`executionState`、`undoHint`、`evidenceLinks` 和安全 `nextActions`。
- 前端在标准候选、字段质量和覆盖率报告等高频维护入口提供“生成维护 workflow”动作，先生成 dry-run 计划，不自动批量采纳或写标准字段。
- CLI / MCP / AI 可读取同一维护 workflow 计划结构，并能看到需要用户确认的步骤、验证命令、失败恢复位置和证据链接。
- 扩展 AI workflow recipes 与 AI 任务推荐队列，让推荐任务能绑定到维护 workflow recipe，而不只给出页面路由或自由文本命令。
- 保持安全边界：第一版不做团队审批系统、不做后台任务调度、不绕过现有候选/字段/质量 API 的显式确认和幂等写入规则。

## Capabilities

### New Capabilities

- `standard-maintenance-workflows`: 定义标准维护 Inbox 到可执行 workflow 的统一计划契约、dry-run 步骤、执行状态、证据链接、恢复和安全边界。

### Modified Capabilities

- `standard-candidate-inbox`: 候选 Inbox 可为待处理候选生成维护 workflow action，但不自动采纳、合并或忽略候选。
- `field-quality-scoring`: 字段质量报告可把低质量字段批次作为维护 workflow 来源，并保留问题类型和建议修复动作。
- `field-coverage-report`: 覆盖率报告可把未纳管字段批次作为维护 workflow 来源，并标记 partial coverage 边界。
- `ai-workflow-recipes`: workflow recipe 目录新增或扩展标准维护 recipe，使 CLI/MCP/AI Context 能读取同一任务步骤。
- `ai-task-recommendation-queue`: 推荐任务输出可绑定维护 workflow recipe，提供可机器读取的 `recipeBinding`、`completionCheck` 和证据引用。
- `frontend-task-entrypoints`: 前端高频页面增加维护 workflow dry-run 入口，并保持项目缺失、空结果和失败状态可恢复。

## Impact

- 后端：新增或扩展标准维护 workflow controller/service/model；读取候选、字段质量、覆盖率、冲突、AI 反馈和 workflow recipe 数据；必要时新增只读计划 API，不新增数据库迁移。
- 前端：标准候选、字段质量、覆盖率报告或共享工具增加 workflow dry-run 入口、计划展示、证据链接和失败状态。
- CLI / MCP / tools：扩展 workflow recipe 或 task card 生成，使维护 workflow 可被 AI 稳定读取；更新契约 fixture 和相关测试。
- OpenSpec：新增 `standard-maintenance-workflows` 主能力 delta，并修改候选 Inbox、字段质量、覆盖率、workflow recipe、AI 推荐队列和前端任务入口相关规格。
- 验证与评审：本任务为 SDD full，涉及多模块与 CLI/MCP/AI 外部协议，归档或 commit 前必须运行相关后端、前端、tools、OpenSpec 验证，并启动独立子 agent 只读评审。
