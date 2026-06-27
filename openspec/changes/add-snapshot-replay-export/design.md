## Context

当前 `ds_standard_snapshot` 已保存字段、枚举、规则的确定性 `payloadJson`，SQL 检查记录和 DDL/AI 作业记录也已保存当时的 snapshot ID、version 和 hash。但 AI Context 导出仍基于当前库状态，SQL 检查详情也只返回原始记录与 issues，导致用户和 AI 无法直接按历史标准复现旧诊断。

现有约束：

- 第一版优先个人/小团队使用，保持只读和可复制证据，不引入发布、审批或权限工作流。
- 不新增数据库表，复用 `ds_standard_snapshot.payload_json`。
- 无快照记录继续使用 `unversioned`，不能阻断旧数据查看。

## Goals / Non-Goals

**Goals:**

- 后端可按 `snapshotId` 或版本读取标准快照 payload，并返回校验后的元数据。
- AI Context field catalog、rules.yaml 和 zip package 支持指定历史快照导出，输出明确标注 `source=current|snapshot|unversioned`。
- SQL 检查记录详情返回 snapshot replay 信息，包括当时标准、当前标准、差异摘要、导出命令和 nextActions。
- CLI 至少支持按 snapshot 导出 Context，便于 AI 在业务仓库复现历史任务。

**Non-Goals:**

- 不回滚当前标准，不修改历史 SQL 检查记录，不补齐旧记录缺失的快照。
- 不重做 DDL 生成算法，不保证所有旧 DDL 可按模板完整重建。
- 不调用外部 LLM，不新增后台任务或队列。

## Decisions

1. **以保存的 payloadJson 作为历史标准源**
   - 选择：新增 `StandardSnapshotPayload` 只读模型和查询方法，导出历史 Context 时从 payload 构建字段/枚举/规则，而不是临时切换数据库查询条件。
   - 原因：payload 已是快照的权威内容，避免当前字段被编辑后污染历史导出。
   - 备选：按版本时间回放变更日志。暂不采用，因为现有变更日志不覆盖所有历史字段状态，复杂度高。

2. **在现有 AI Context API 上增加 snapshot 参数**
   - 选择：`snapshotId` / `snapshotVersion` 作为可选查询参数，未传时保持当前行为。
   - 原因：前端、CLI、MCP 已围绕这些 endpoint 集成，兼容性成本最低。
   - 备选：新增 `/snapshot-replay/context` 专用 endpoint。暂不采用，避免复制导出逻辑。

3. **SQL 检查记录详情返回 replay summary**
   - 选择：扩展 `/api/lint/records/{id}` 的 detail payload，增加 `replay` 字段，不改变原有 `record/issues`。
   - 原因：前端历史记录和 AI 回放天然从记录详情出发；新增字段兼容旧前端。
   - 备选：仅提供独立 replay endpoint。可后续追加，但第一版不让前端多跳一次。

4. **差异摘要先做轻量 hash/计数/版本对比**
   - 选择：第一版返回 snapshotId/version/hash、current snapshot、是否同一版本、字段/规则/枚举数量和 nextActions。
   - 原因：足够支撑 AI 判断“按历史标准导出”还是“用当前标准重跑”，不做昂贵的字段级 diff。

## Risks / Trade-offs

- 历史 payload 结构不完整或脏数据 → 查询时返回结构化错误，提示使用当前标准或查看原始记录。
- 指定 snapshot 不属于当前 project → 服务端必须按 projectId 校验，避免跨项目泄漏标准。
- Context 导出分支增加后可能漂移 → 增加后端单测和 CLI JSON 测试，锁定 `standard.source`、`snapshotId`、`specVersion`。
- 前端一次做太多页面改造 → 第一版只补 SQL 记录详情和 AI Context 的核心入口，复杂回放看板留给执行证据包待办。
