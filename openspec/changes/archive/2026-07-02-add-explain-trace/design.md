## Context

P6-50 的目标是让 AI 采用字段或生成候选时能解释“依据来自哪里”。当前字段推荐、字段检索和自然语言需求草案已经返回 `matchReason` / `matchReasons`，但这些只是自由文本，前端、CLI/MCP 和后续回放无法稳定读取来源类型、来源对象、置信度和文档引用。

第一版选择接入字段推荐、字段标准检索和自然语言需求草案，因为它们是 AI 建表和补标准时最常用、风险最低且已有原因数据的入口。SQL lint/fixedSql、DDL、Prompt、Context 和 AI 回放保留为后续扩展，不在本轮一次性铺开。

## Goals / Non-Goals

**Goals:**
- 定义轻量 explain trace 契约；核心输出附带 evidence 数组，包含 sourceType、sourceId、snapshotVersion、matchReason、confidence、ruleCode 和 docsRef；前端详情页展示证据来源。
- 字段推荐、字段检索和自然语言需求草案中的关键字段都有可读证据；缺失候选能说明来源和置信度；契约有测试防漂移。

**Non-Goals:**
- 不引入完整分布式 tracing 平台，不记录业务数据行，不把 evidence 作为强审批依据。
- 不新增持久化表，不记录业务数据行，不把 evidence 用作审批、权限或强一致审计依据。
- 不在本轮改造 SQL lint/fixedSql、DDL、Prompt、Context 和 AI 回放的全部输出结构。

## Decisions

1. **使用轻量 record 而不是 tracing 框架。**
   - change id: `add-explain-trace`
   - capability: `explain-trace`
   - 原因：本项目优先服务个人/小团队和 AI 本地调用，当前只需要可读证据对象，不需要分布式链路追踪。

2. **首批接入已有原因文本最完整的入口。**
   - 字段推荐和字段检索使用命中的标准字段 ID、原因文本和匹配分数生成 evidence。
   - 自然语言需求草案复用推荐/检索 evidence；缺失候选使用需求解析规则、候选名和置信度生成 evidence。
   - 原因：最小改动即可让 AI 看到稳定来源，避免在本轮跨越太多模块。

3. **`sourceType` 先用字符串。**
   - 原因：证据来源会逐步扩展到 `FIELD`、`REQUIREMENT_DRAFT`、`TEMPLATE`、`RULE`、`SNAPSHOT` 等；第一版避免为了少量来源引入过早枚举和迁移成本。

## Risks / Trade-offs

- [Risk] 只覆盖字段推荐/检索/需求草案，不能立即解释 fixedSql 或 DDL。→ Mitigation：README 和 TODO 明确第一版范围，后续逐步接入。
- [Risk] evidence 与既有 `matchReason` 重复。→ Mitigation：保留旧字段兼容前端和 CLI，新增 evidence 作为结构化读取入口。
- [Risk] 文本原因可能包含敏感内容。→ Mitigation：本轮只使用字段元数据、候选名、模板名和规则说明，不记录业务数据行、密码、token 或 JDBC URL。

## Open Questions

- 已确认：change id 使用 `add-explain-trace`，capability 使用 `explain-trace`。
- 已收敛：第一版不覆盖全部 AI 输出，只覆盖字段推荐、字段检索和自然语言需求草案。
