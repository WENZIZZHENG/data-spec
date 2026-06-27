## Why

P6-1 已经保存标准快照和 hash，但 AI 排查旧 SQL 检查、旧 DDL 预览或历史任务时仍只能读取当前标准，难以复现“当时为什么这么诊断”。P6-21 需要把已保存的 snapshot payload 变成可导出的历史上下文和可读回放证据。

## What Changes

- 新增只读历史快照导出能力：按 `snapshotId` 或版本导出 field catalog、rules.yaml 和 AI Context package 的标准元数据。
- 新增 SQL 检查记录回放详情：基于记录中保存的 snapshot 引用返回“当时标准 / 当前标准”的摘要、可复制导出命令和兼容提示。
- 扩展 DDL/AI Context/CLI 的参数入口，让 AI 能明确选择当前标准或历史快照。
- 保留无快照历史记录的 `unversioned` 兼容路径，不强行补历史数据。

## Capabilities

### New Capabilities
- `snapshot-replay-export`: 覆盖按历史标准快照导出 AI 上下文、SQL 检查记录回放详情和 AI 可消费 next actions。

### Modified Capabilities
- `standard-snapshot-versioning`: 标准快照从“只记录最新元数据”扩展为“可按快照读取 payload 并驱动历史导出”。
- `ai-context-package`: AI Context 导出支持指定标准快照，并在输出中声明当前/历史标准来源。
- `sql-check-records`: SQL 检查记录详情支持返回与标准快照相关的回放信息。
- `ddl-generator-tool`: DDL 生成/回放入口暴露可复用的标准快照元数据，供历史任务复现。

## Impact

- 后端：`standard`、`aicontext`、`lint`、`generator` 模块新增只读查询/导出逻辑和测试。
- 前端：SQL 校验记录详情、AI Context 页面或快照页面新增历史快照导出/回放入口。
- CLI/MCP：优先补 CLI 参数或命令，MCP 可复用现有资源参数；不引入新的外部依赖。
- 数据库：第一版不新增表，复用 `ds_standard_snapshot.payload_json` 和已有记录快照字段。
