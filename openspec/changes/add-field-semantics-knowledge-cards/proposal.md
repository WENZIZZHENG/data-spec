## Why

AI 生成表结构、SQL、测试和标准问答时，当前字段标准仍偏“字段清单”形态，缺少字段之间的派生口径、枚举生命周期、命名翻译偏好、指标口径和可直接引用的知识卡。P6-77、P6-107、P6-161、P6-175 和 P6-184 应合并成一条字段语义闭环，避免继续分散扩展字段、枚举、AI Context 和前端入口。

## What Changes

- 新增字段语义规则能力：记录源字段、派生关系、单位换算、时间粒度、聚合口径、source of truth、推荐场景和反例说明。
- 增强代码集 / 枚举值生命周期：支持枚举值状态、别名、替代值、有效期、来源证据和 AI 可读映射提示。
- 新增字段知识卡：聚合字段元数据、格式约束、usage contract、示例/反例、语义规则、枚举提示、命名翻译、指标引用、风险说明和最近验证时间，面向 API/前端/CLI/MCP/AI Context 消费。
- 新增轻量指标口径映射：用指标定义把标准字段、过滤条件、聚合规则、时间粒度和示例 SQL 连接起来，帮助 AI 区分字段标准与业务指标口径。
- 扩展字段命名翻译辅助：支持推荐英文名、中文名、翻译别名、禁用翻译和命名理由，供搜索、推荐、数据字典和 AI Context 使用。
- 第一版不执行真实数据计算，不连接业务库统计枚举分布，不自动改生产 SQL，不接入外部翻译或 BI 平台。

## Capabilities

### New Capabilities

- `field-semantics-knowledge-cards`: 字段语义规则、字段知识卡和命名翻译辅助的核心契约。
- `enum-value-lifecycle`: 代码集 / 枚举值生命周期、别名、替代值和映射提示契约。
- `metric-definition-mapping`: 轻量指标口径定义与标准字段映射契约。

### Modified Capabilities

- `field-model`: 字段 API 需要返回新增语义、命名翻译和知识卡摘要相关字段，保持 additive 兼容。
- `field-standard-search`: 字段搜索需要使用命名翻译、语义规则和知识卡摘要解释匹配理由与风险。
- `field-suggestion`: 字段推荐需要使用推荐英文名、禁用翻译和语义规则改进 fallback 与 match reason。
- `ai-context-package`: AI Context 需要导出字段语义规则、字段知识卡、枚举生命周期和指标口径摘要。
- `data-dictionary`: Markdown 数据字典需要展示字段知识卡、枚举生命周期和指标口径摘要。
- `standard-schema-registry`: Schema Registry 需要登记新增语义、枚举生命周期、指标口径和 AI Context contract。

## Impact

- 后端：字段、枚举字典、AI Context、Schema Registry、搜索/推荐、数据字典和标准问答相关 service/controller/model/repository；需要 additive migration。
- 前端：字段库、枚举字典、AI Context、标准问答或数据字典相关 API wrapper、类型和页面展示；OpenAPI schema 需要重新生成。
- CLI/MCP/tools：新增或扩展只读知识卡 / 语义导出入口，并更新 contract fixture 与测试。
- 文档与 OpenSpec：README、TODO、AI contract 文档和本 change 的 Verification Evidence 需要同步。
- 风险：涉及数据库 migration、OpenAPI、CLI/MCP/AI 外部协议和跨模块 AI Context，按 SDD full 执行，commit 前必须独立子 agent 评审。
