## Why

DataSpec 当前在字段搜索和推荐中只做基础 snake/camel 拆分，业务术语表又维护另一套 substring 匹配；连续 acronym、数字、单位、中文多词和缩写歧义因此无法稳定解析或解释。`P6-190` 已补齐历史别名与可信 evidence，现在需要统一命名理解，避免后续推荐回归和候选管道继续依赖分散特例。

## What Changes

- 新增 project-scoped 确定性命名解析能力，统一处理分隔符、camelCase、连续 acronym、数字边界和单位分类，并输出有界、脱敏的 token evidence。
- 复用现有业务术语表做中文/多词最长匹配、term/synonym/root/abbreviation 展开、disabled term 标记和缩写歧义判断；不新增第二套词典，不自动写术语表。
- 让字段检索、字段推荐和 Standard Query 共享同一解析结果；标准问答消费字段搜索返回的 token evidence，不再自行决定 glossary canonical 命中。
- 对歧义缩写、禁用词和未解析 token 保持可解释的保守行为，不把它们自动升级为 canonical 字段，也不改变既有必填字段或错误码。
- 增加 `HTTPStatus2Code`、`会员手机号`、`ord_amt` 及边界样例的 deterministic golden tests；不调用外部 LLM、Python/jieba 运行时或向量数据库。

## Capabilities

### New Capabilities

- `deterministic-name-tokenization`: 定义命名词法拆分、项目词典最长匹配、缩写/禁用/歧义状态、token evidence 和安全边界。

### Modified Capabilities

- `business-glossary-synonym-roots`: 术语表成为确定性命名解析的唯一项目词典来源，并提供最长匹配与歧义语义。
- `field-suggestion`: 字段推荐共享命名解析结果，并为 canonical、歧义、禁用和 fallback 结果返回 token evidence。
- `field-standard-search`: 字段检索按统一 token/canonical 结果评分，在摘要和条目证据中解释命中来源。
- `standard-query-dsl`: Standard Query FIELD text 复用字段搜索的命名解析，并在 normalized 结果中暴露 additive token evidence。
- `explain-trace`: Explain Trace 增加稳定的 query-token/glossary 证据来源与规则代码语义，不改变现有字段结构。

## Impact

- 后端：新增 `querynormalization` 内部 service/model，收敛 `FieldServiceImpl` 与 `BusinessGlossaryServiceImpl` 的重复 token/compact 逻辑；Standard Query 继续通过 FieldService 执行。
- 前端：标准问答和字段搜索展示消费后端 token evidence；不新增页面或第二套问答 API。
- 契约：Field Search summary、Field Suggestion 和 Standard Query normalized 仅增加可选 token evidence；同步 Schema Registry、OpenAPI 生成类型和 CLI/MCP 契约 fixture。
- 数据与安全：不改数据库 schema、不迁移历史数据；每次解析只读取当前项目启用 glossary，输出经过长度限制与敏感文本脱敏。
