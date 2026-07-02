## Why

AI 采用某个字段、规则或 fixedSql 修复时，用户需要知道依据来自哪个标准字段、规则、快照、术语或质量诊断；没有证据链时，AI 输出很难复盘和信任。

## What Changes

- 定义轻量 `ExplainTrace` 契约，稳定包含 `sourceType`、`sourceId`、`snapshotVersion`、`matchReason`、`confidence`、`ruleCode` 和 `docsRef`。
- 第一版先接入字段推荐、字段标准检索和自然语言需求草案这三条 AI 高频入口，让已有 `matchReason` 升级为机器可读 evidence 数组。
- 前端在自然语言需求草案详情中展示证据来源、置信度和文档引用；字段库继续保留原有命中原因，同时类型层暴露 evidence。
- 同步 README、TODO 和 OpenSpec 主规格，明确第一版边界。

## Capabilities

### New Capabilities
- `explain-trace`: 为 AI 输出提供轻量、可读、可测试的引用证据结构。

### Modified Capabilities
- `field-recommendation` / `requirement-draft` 的响应新增可选 evidence 数组，不改变既有字段语义。

## Impact

- 已有基础：已有标准快照、字段推荐原因、SQL 检查记录、fixedSql、AI 回放、字段质量评分和执行证据包待办。
- 缺口：推荐和需求草案已经有原因文本，但缺少统一结构，无法稳定追踪“这个建议为什么出现”。
- 落地产物：后端新增契约模型和首批接入；前端展示证据来源；测试锁定关键响应字段；文档记录第一版范围。
- 验收标准：字段推荐、字段检索和需求草案中的关键字段都有可读 evidence；缺失候选能说明来源和置信度；契约有测试防漂移。
- 边界：不引入完整分布式 tracing 平台，不记录业务数据行，不把 evidence 作为强审批依据。

## Verification Evidence

- `openspec validate add-explain-trace --strict` 通过。
- `mvn "-Dtest=FieldServiceImplTest,RequirementDraftServiceImplTest" test` 通过，40 tests, 0 failures。
- `mvn test` 通过，362 tests, 0 failures。
- `pnpm test` 通过，97 tests, 0 failures。
- `pnpm build` 通过；仅保留既有 Rolldown pure annotation 和 chunk size warning。
- 本地结构化评审已检查功能、兼容、安全、性能、测试和文档；未发现未处理 finding。
