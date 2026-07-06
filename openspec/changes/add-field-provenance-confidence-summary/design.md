## Context

DataSpec 目前已有四类可复用证据：标准字段基础信息、数据库反向导入来源记录、标准候选 Inbox 决策、字段质量评分。它们分别回答“字段是什么”“从哪里来”“是否已被人工处理”“元数据是否完整”，但没有统一出口告诉 AI 哪些字段可以作为强标准使用，哪些字段只是低置信度参考。

本变更采用 SDD standard，因为它新增只读 API 可观察契约，但不改数据库 schema、写入事务、权限模型或 AI Context 包格式。

## Goals / Non-Goals

**Goals:**

- 为项目字段生成只读来源可信度与 AI 置信度摘要。
- 将来源证据、候选证据和质量评分合并为 `VERIFIED`、`REVIEW`、`LOW`、`UNKNOWN` 四档。
- 输出可解释的 `warnings`、`recommendedUse` 和脱敏后的 `sourceRefs`，让 AI 和用户知道置信度来源。
- 保持最小实现边界：新增聚合 service/controller/model，并只为候选 Repository 增加按项目只读查询。

**Non-Goals:**

- 不新增表、字段、索引或迁移。
- 不自动采纳候选、不自动修改字段、不写入置信度结果。
- 不修改 AI Context package、CLI、MCP、前端页面或标准候选决策流程。
- 不返回 `evidenceJson`、`metadataJson`、JDBC URL、DSN、token、password、Authorization 等 raw 证据。

## Decisions

1. 新增独立 `fieldprovenance` 模块，而不是塞进 `FieldServiceImpl`。
   - 理由：聚合逻辑横跨字段、来源、候选、质量评分，独立模块能保持 `FieldService` 的核心 CRUD/检索职责清晰。
   - 备选：扩展 `/api/fields/quality`。放弃原因是质量评分关注元数据完整度，来源可信度还包含候选与来源证据，职责不同。

2. 置信度实时计算，不持久化。
   - 理由：第一版只读聚合足够支撑 AI 判断，并避免引入重算任务、缓存失效和历史兼容问题。
   - 备选：新增 `ds_field_confidence` 表。放弃原因是 P6-94 的最小闭环不需要存储语义，新增表会把变更升级为 SDD full。

3. 来源引用只输出脱敏摘要。
   - 理由：反向导入来源和候选来源可能包含连接串或外部证据文本，API 只需要解释来源，不需要复现原始证据。
   - 备选：直接返回 `metadataJson` / `evidenceJson`。放弃原因是会增加凭据泄露风险，也让 AI 上下文更噪。

4. 聚合规则以稳定、可解释优先，而不是模型分数。
   - 规则会综合字段状态、来源数量、候选决策、候选 confidence 和质量评分，输出整数 `aiConfidence` 与等级。
   - 后续如接入统计学习或使用热区，可以在不改变第一版等级语义的前提下追加证据项。

## Risks / Trade-offs

- 规则启发式可能不适用于所有团队标准 → 在 `warnings` 和 `recommendedUse` 中说明依据，第一版只作为 AI 参考信号，不自动写入或阻断。
- 实时聚合会读取项目全量字段、来源、候选和质量报告 → 先沿用现有项目规模假设；API 只读无分页，后续字段量增大时再补分页或缓存。
- 候选来源引用历史数据可能未充分脱敏 → 输出时再次走 `SensitiveDataSanitizer.redactText`，并截断过长引用。
- 新增 API 契约可能被前端或 AI 脚本依赖 → OpenSpec 记录字段语义，并用 controller/service 单测固定响应结构。

## Migration Plan

- 部署：后端新增只读 API，无数据库迁移；旧客户端不受影响。
- 回滚：删除新 controller/service/model 和候选 Repository 只读方法即可，不影响已有字段、来源、候选或质量评分数据。
- 验证：运行新增单测、相关后端测试、`openspec validate add-field-provenance-confidence-summary --strict`、`git diff --check` 和 commit 前敏感词扫描。
