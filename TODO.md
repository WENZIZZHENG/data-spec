# DataSpec 待办路线图

本文件只保留当前行动入口、优先级视图和文档索引。当前实施主题见 [P6 精简候选池](docs/todo-p6-candidates.md)，已完成详情见 [P5/P6 完成归档](docs/archive/todo-completed-p5-p6.md)，删除 / 不做记录见 [删除候选归档](docs/archive/todo-removed-p6-candidates.md)。

## 当前状态

- 高优先级已清零；`P6-84` 前端可访问性与键盘操作基线已完成。
- 2026-07-12 已完成第二轮候选收束：60 个原候选中，36 个编号合并为 10 个实施主题，7 个由现有能力覆盖，17 个删除或等待外部触发。
- 近期队列共 6 个主题，建议从 `P6-137` 配置 Schema 开始，再依次考虑 `P6-85`、`P6-86`、`P6-101`、`P6-111`、`P6-120`。
- OpenSpec 当前 active changes：`add-standard-test-data-compat-suite`、`add-ai-context-safety-controls`、`add-stable-standard-refs-and-ai-output-checks`、`add-standard-query-dsl`、`add-business-object-table-standards` 与 `add-field-semantics-knowledge-cards` 均已完成实现、验证和独立评审；按项目约定暂保留 open，不自动 archive。
- 已完成 P0-P4 详情见 [docs/archive/todo-completed-p0-p4.md](docs/archive/todo-completed-p0-p4.md)。
- 已完成 P5/P6 详情见 [docs/archive/todo-completed-p5-p6.md](docs/archive/todo-completed-p5-p6.md)，当前归档 135 个原待办编号。
- 已删除 / 不做或等待触发的独立候选 26 项，详见 [docs/archive/todo-removed-p6-candidates.md](docs/archive/todo-removed-p6-candidates.md)。
- 清理前 60 项候选全文见 [docs/archive/todo-p6-candidates-2026-07-12.md](docs/archive/todo-p6-candidates-2026-07-12.md)。
- 任务卡可从 `create-table`、`review-pr-sql`、`reverse-import-standards`、`export-min-context`、`standard-evidence-review`、`standard-maintenance` workflow recipe 生成；详细能力见完成归档。
- 真实自测库授权边界：用户已授权 `localhost:5432/ai_test` 作为可写的一次性 PostgreSQL 测试库；仅限测试库，不扩展到其他库，不把密码写入仓库，操作后记录验证范围与清理结果。

## 下一步顺序

1. `P6-137`：`.dataspec/config.json` Schema 与编辑器提示，约 0.5-1 个工作日。
2. `P6-85`：本地数据清理、重置与演示项目重建，约 1-2 个工作日。
3. `P6-86`：前端性能体验与字段库密集操作，约 2-4 个工作日。
4. `P6-101`：环境运行指纹与漂移诊断，约 2-3 个工作日。
5. `P6-111`：标准候选来源管道，约 3-5 个工作日。
6. `P6-120`：AI 场景回放与规则回归基线，约 3-5 个工作日。

开工前读取候选主题的近期范围和边界，再按任务风险决定快速、常规或 OpenSpec 流程。暂缓主题只有触发条件成立时才进入队列。

## 待办入口

- [P6 精简候选池](docs/todo-p6-candidates.md)：10 个仍值得实施的主题、第一版边界和启动条件。
- [P6 候选价值评审](docs/todo-p6-candidate-review.md)：记录 60 项候选的保留、合并、完成覆盖和删除依据。
- [P6 剩余时间评估](docs/todo-p6-remaining-estimates.md)：按 10 个实施主题估算开发时间。
- [P6 候选池历史快照](docs/archive/todo-p6-candidates-2026-07-12.md)：保留清理前 60 项完整背景和验收描述。
- [P5/P6 完成归档](docs/archive/todo-completed-p5-p6.md)：保留已完成条目的验证证据、产物和后续增强。
- [P6 删除候选归档](docs/archive/todo-removed-p6-candidates.md)：记录删除、暂不做和恢复触发条件。
- [P0-P4 完成归档](docs/archive/todo-completed-p0-p4.md)：保留早期已完成能力的详细背景。

## 当前主题摘要

### 近期 6 项

`P6-137`、`P6-85`、`P6-86`、`P6-101`、`P6-111`、`P6-120`。

### 暂缓 4 项

`P6-74`、`P6-104`、`P6-123`、`P6-129`。这些编号是合并主题锚点，不代表按编号线性开发。
