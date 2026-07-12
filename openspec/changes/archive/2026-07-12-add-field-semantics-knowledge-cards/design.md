## Context

DataSpec 已有字段基础模型、字段格式约束、字段生命周期、usage contract、业务术语表、标准使用示例、表结构标准、稳定引用、标准查询 DSL、标准问答、AI Context、CLI/MCP 和 Schema Registry。当前缺口不是再新增一份孤立说明文档，而是把字段之间的语义关系、枚举值生命周期、命名翻译偏好和指标口径沉淀成稳定、可导出、可被 AI 引用的结构化契约。

本 change 合并 P6-77、P6-107、P6-161、P6-175 和 P6-184，按 SDD full 执行。原因是第一版需要 additive database migration、OpenAPI schema 刷新、CLI/MCP/AI Context 外部协议扩展、Schema Registry 契约登记和多端测试。所有新增文本字段都必须 secret-safe，不保存业务数据行、连接凭据、raw SQL secret、JDBC URL、DSN、Authorization 或 token。

## Goals / Non-Goals

**Goals:**

- 为标准字段提供结构化语义规则：derivedFrom、unitConversion、aggregationRule、timeGranularity、sourceOfTruth、recommendedUse 和 antiPatterns。
- 为代码集枚举值提供生命周期：status、aliases、replacementValue、validFrom、validTo、sourceEvidence 和 mappingHints。
- 提供 FieldKnowledgeCard 只读聚合结果，让 AI 在单字段上下文中读取用途、禁用场景、格式、示例、枚举、语义、指标和命名证据。
- 提供轻量 MetricDefinitionMapping，把业务指标口径映射到标准字段、过滤规则、聚合方式、时间粒度和示例 SQL。
- 将语义规则、枚举生命周期、知识卡、指标口径和命名翻译导出到 AI Context、Schema Registry、数据字典、字段搜索/推荐和 CLI/MCP 只读入口。

**Non-Goals:**

- 不执行真实数据计算、单位换算或指标结果校验。
- 不连接业务库统计枚举分布，不采集真实业务数据行。
- 不自动替换生产 SQL，不默认写业务仓库。
- 不接入外部翻译、语义检索、BI、指标平台或数据血缘平台。
- 不要求所有历史字段一次性补齐长知识卡；第一版优先支持人工维护和自动聚合摘要。

## Decisions

1. **字段语义使用独立表，命名翻译使用字段扩展列。**
   - 方案：新增 `ds_field_semantic_rule` 保存字段间关系和口径规则；在 `ds_field` 增加少量命名翻译列，如 `localized_names_json`、`preferred_english_name`、`forbidden_translations_json`、`translation_aliases_json`、`translation_notes`。
   - 理由：语义规则是一对多关系，放独立表便于按 fieldId 查询、校验源字段归属和后续扩展；命名翻译是字段自身描述，放字段扩展列能被现有字段 list/detail/search/backup/snapshot 快速带出。
   - 备选：把全部语义和翻译塞入 `ds_field` JSON。放弃原因是派生规则、sourceOfTruth 和 antiPatterns 会让字段表过宽，难以做归属校验和定向导出。

2. **枚举生命周期在 `ds_enum_value` 上 additive 扩展，不新增完整枚举版本系统。**
   - 方案：为枚举值增加 `status`、`aliases_json`、`replacement_value`、`valid_from`、`valid_to`、`source_evidence`、`mapping_hints`、`ai_usage_notes`。
   - 理由：P6-107 第一版关注 AI 读懂允许值、废弃值和替代值；完整版本系统会扩大快照、diff、审批和迁移成本。
   - 备选：新增 enum version / enum value history 表。放弃原因是当前个人/小团队闭环更需要可见约束，而不是企业级变更治理。

3. **知识卡采用只读聚合服务，不把长文卡片作为主存储。**
   - 方案：新增 FieldKnowledgeCard service/API，按字段聚合 Field、format、usage contract、semantic rules、enum values、usage examples、glossary/naming、metric mappings、provenance/conflict/quality evidence，并支持 bounded list/detail 导出。
   - 理由：知识卡本质是 AI 消费视图；将它作为派生视图能减少重复存储和过期风险。
   - 备选：新增 `ds_field_knowledge_card` 表手工维护。放弃原因是会产生双源，需要额外同步机制；第一版只保留可选 notes 字段来源，不存完整卡片正文。

4. **指标口径使用轻量 project-scoped 表，不接管指标计算。**
   - 方案：新增 `ds_metric_definition`，保存 metricKey、displayName、definition、measureFields、dimensionFields、filterRule、aggregationRule、timeGrain、ownerNotes、exampleSql 和 status。
   - 理由：P6-175 需要让 AI 明确“指标口径是什么”和“依赖哪些标准字段”，不需要运行查询或校验结果。
   - 备选：复用业务术语表或字段 semantic rule。放弃原因是指标具有独立 key、过滤、聚合、时间粒度和示例 SQL，和普通术语/字段关系不同。

5. **外部协议全部 additive，优先只读入口。**
   - 方案：新增或扩展 API/CLI/MCP 只读查询：field knowledge cards、field semantic rules、metric mappings 和 enum lifecycle；现有字段/枚举写 API 做 additive 字段扩展；AI Context 新增 `.dataspec/field-knowledge-cards.json`、`.dataspec/field-semantics.json` 和 `.dataspec/metrics.json` 或等价 manifest artifact。
   - 理由：避免破坏已有前端、CLI、MCP、OpenAPI 客户端；让 AI 先能稳定读取，再决定是否生成后续写入任务。
   - 备选：直接改变现有 field-catalog JSON 的核心结构。放弃原因是会增加兼容风险；第一版只在 field entries 上加 optional summary，并把较重内容放独立 artifact。

6. **安全边界复用现有 SensitiveDataSanitizer 并在写入层拒绝明显 secret。**
   - 方案：所有语义规则、枚举证据、指标说明、示例 SQL、命名 notes 写入前扫描 secret-like 内容；AI Context 和 CLI/MCP 输出再次 sanitize。
   - 理由：这些文本很容易被用户或 AI 粘贴 SQL、连接串、token；仅输出脱敏不够，持久化前也要拒绝明显风险。
   - 备选：只在导出时脱敏。放弃原因是敏感内容一旦入库，会污染快照、备份、数据字典和后续导出。

## Risks / Trade-offs

- [Risk] 能力包过大，知识卡把 AI Context 撑爆。→ Mitigation：默认只导出高价值摘要，支持 scope/query/limit，manifest 记录 truncation。
- [Risk] 派生字段规则被误解为可执行计算。→ Mitigation：字段命名和文档统一使用 guidance/notes/preview，不提供执行接口，不生成生产 SQL 修改。
- [Risk] 枚举 lifecycle 与现有 codeSetId 关系不清。→ Mitigation：枚举值仍归属 `ds_enum_dict`；字段只通过 `codeSetId` 关联，知识卡在读取时聚合。
- [Risk] 指标口径与字段 usage contract 重复。→ Mitigation：usage contract 描述字段用法，metric mapping 描述业务指标定义；知识卡只给引用摘要。
- [Risk] 迁移引入大量可空列和新表。→ Mitigation：全部 additive、nullable/default-safe；旧数据和旧客户端无需立即补齐。
- [Risk] OpenAPI/CLI/MCP contract 漂移。→ Mitigation：刷新 OpenAPI schema，更新 CLI/MCP fixture、Schema Registry 和相关 typed wrapper 测试。

## Migration Plan

1. 新增 additive Flyway migration：字段翻译扩展列、枚举值 lifecycle 列、字段语义规则表、指标口径表和必要索引。
2. 后端先实现 model/entity/repository/service/controller 的只读/维护最小闭环，保持旧字段与旧枚举 API 兼容。
3. 扩展 AI Context、Schema Registry、data dictionary、field search/suggestion、CLI/MCP 和前端类型。
4. 补后端、tools、前端和 OpenSpec 验证；刷新 OpenAPI schema。
5. commit 前执行独立子 agent 评审；本 change 完成后默认保持 open，不自动 archive。

Rollback：由于 migration 为 additive，应用层回滚可先下线新 API/入口；数据库列/表保留为空不影响旧功能。若必须清理，另开显式迁移变更处理。

## Open Questions

- 第一版是否提供枚举 literal 的 SQL lint 建议：默认做最小检测，只覆盖字段明确绑定 codeSetId 且 literal 明显不在允许值中的场景；复杂 SQL 解析增强留给后续。
- 知识卡是否允许手工编辑长文案：默认不做完整手工卡片编辑，只允许从字段、usage examples、semantic rules、metric mappings 等结构化来源聚合。
