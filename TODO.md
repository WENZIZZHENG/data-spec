# DataSpec 待办路线图

本文件只保留当前行动入口、优先级视图和文档索引。当前实施主题见 [P6 精简候选池](docs/todo-p6-candidates.md)，已完成详情见 [P5/P6 完成归档](docs/archive/todo-completed-p5-p6.md)，删除 / 不做记录见 [删除候选归档](docs/archive/todo-removed-p6-candidates.md)。

## 当前状态

- `P6-189` 确定性数标命名解析与缩写治理已完成；当前高优先级转为推荐回归、字段库性能和统一评审证据。
- 2026-07-14 完成 `P6-189` 后，当前剩余 8 个实施主题：6 个进入近期队列，2 个等待业务触发；`P6-191` 继续承接第三轮功能评审建议。
- 近期优先顺序为 `P6-120`、`P6-86`、`P6-191`、`P6-137`、`P6-111`、`P6-85`，串行约 11.5-19 个工作日。
- OpenSpec 当前 active changes 为 0；`P6-189` 已同步主规格并归档。
- 已完成 P0-P4 详情见 [docs/archive/todo-completed-p0-p4.md](docs/archive/todo-completed-p0-p4.md)。
- 已完成 P5/P6 详情见 [docs/archive/todo-completed-p5-p6.md](docs/archive/todo-completed-p5-p6.md)，当前归档 137 个待办编号。
- 已删除、合并或等待触发的独立候选 29 项，详见 [docs/archive/todo-removed-p6-candidates.md](docs/archive/todo-removed-p6-candidates.md)。
- 清理前 60 项候选全文见 [docs/archive/todo-p6-candidates-2026-07-12.md](docs/archive/todo-p6-candidates-2026-07-12.md)。
- 任务卡可从 `create-table`、`review-pr-sql`、`reverse-import-standards`、`export-min-context`、`standard-evidence-review`、`standard-maintenance` workflow recipe 生成；详细能力见完成归档。
- 真实自测库授权边界：用户已授权 `localhost:5432/ai_test` 作为可写的一次性 PostgreSQL 测试库；仅限测试库，不扩展到其他库，不把密码写入仓库，操作后记录验证范围与清理结果。

## 下一步顺序

1. `P6-120`：推荐质量与 AI 场景回归基线，约 1-2 个工作日。
2. `P6-86`：字段库服务端分页、搜索防抖和大数据量性能闭环，约 2-3 个工作日。
3. `P6-191`：统一 Finding/Evidence 与 AI/PR 评审闭环，约 3-5 个工作日。
4. `P6-137`：`.dataspec/config.json` Schema 与编辑器提示，约 0.5-1 个工作日。
5. `P6-111`：先接一个可追溯候选来源，优先承接未知词和缩写歧义，约 2-3 个工作日。
6. `P6-85`：演示项目 dry-run 清理与完整重建，约 3-5 个工作日。

开工前读取候选主题的近期范围和边界，再按任务风险决定快速、常规或 OpenSpec 流程。暂缓主题只有触发条件成立时才进入队列。

## 待办入口

- [P6 精简候选池](docs/todo-p6-candidates.md)：8 个仍值得实施的主题、第一版边界和启动条件。
- [P6 候选价值评审](docs/todo-p6-candidate-review.md)：记录 60 项候选的保留、合并、完成覆盖和删除依据。
- [P6 剩余时间评估](docs/todo-p6-remaining-estimates.md)：按 8 个实施主题估算开发时间。
- [P6 候选池历史快照](docs/archive/todo-p6-candidates-2026-07-12.md)：保留清理前 60 项完整背景和验收描述。
- [P5/P6 完成归档](docs/archive/todo-completed-p5-p6.md)：保留已完成条目的验证证据、产物和后续增强。
- [P6 删除候选归档](docs/archive/todo-removed-p6-candidates.md)：记录删除、暂不做和恢复触发条件。
- [P0-P4 完成归档](docs/archive/todo-completed-p0-p4.md)：保留早期已完成能力的详细背景。

## 当前主题摘要

### 近期 6 项

`P6-120`、`P6-86`、`P6-191`、`P6-137`、`P6-111`、`P6-85`。

### 暂缓 2 项

`P6-74`、`P6-123`。`P6-104` 已并入 `P6-74`，`P6-129` 已并入 `P6-123`，`P6-101` 已并入 `P6-137`。
