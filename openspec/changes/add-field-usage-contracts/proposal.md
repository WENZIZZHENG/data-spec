## Why

字段“是什么”和“什么时候该用”是两类不同信息。当前字段标准已有生命周期、敏感标记、格式约束、使用示例和标准问答，但 AI 生成 SQL/DDL 时仍只能从注释、标签和示例推断统计口径、默认过滤、聚合方式、join 条件和禁用场景，容易把展示字段、内部状态、废弃字段或候选字段误用于写入和统计。

P6-187 需要补齐字段级 usage contract，让 DataSpec 能明确表达推荐使用场景、禁用场景、聚合/过滤/join 提示、替代字段建议和常见误用，并把这些信息带入字段详情、检索、AI Context、DDL/Prompt 相关上下文和标准问答。

## What Changes

- 新增字段使用契约能力，支持字段级 `preferredUseCases`、`avoidWhen`、`joinHints`、`defaultFilters`、`aggregationHints`、`replacementGuidance` 和 `misuseExamples`。
- 扩展标准字段持久化模型和 API 请求/响应，使使用契约可在字段库创建、编辑、查询和导出中读写。
- 字段检索、标准问答和字段推荐在命中字段时返回使用契约摘要，低置信或禁用场景下提示人工确认或转候选。
- AI Context 的 `field-catalog.json`、schema、`DATABASE_RULES.md` 和相关 prompt guidance 输出使用契约，帮助 AI 在生成 SQL/DDL 前读取使用边界。
- DDL/Prompt 相关流程把字段使用契约作为只读上下文，不自动改写用户 SQL，不替代字段使用示例库。
- 不做完整指标平台、审批流、血缘平台或所有字段强制补齐；第一版优先兼容空契约和高风险字段的轻量文本/列表说明。

## Capabilities

### New Capabilities
- `field-usage-contracts`: 定义字段级使用契约的存储语义、用户可见编辑、AI 可读输出和低置信处理边界。

### Modified Capabilities
- `field-model`: 标准字段元数据新增兼容性 usage contract 字段，并保持默认空契约兼容。
- `field-standard-search`: 字段检索结果新增使用契约摘要和基于禁用场景的推荐动作。
- `ai-context-package`: AI Context 字段目录、schema、数据库规则和 guidance 输出字段使用契约。
- `ddl-generator-tool`: DDL/Prompt 生成上下文读取字段使用契约，提醒 AI 避免误用展示、内部、废弃或统计专用字段。

## Impact

- 后端：Flyway 迁移、Field entity/request/response、FieldService、字段检索、标准问答/推荐相关服务、AI Context export、DDL/Prompt 上下文。
- 前端：字段库表单与详情、标准问答展示、可能的字段检索摘要展示。
- 测试：后端字段 CRUD/搜索/AI Context/DDL prompt 测试，前端字段表单和标准问答源码级测试，OpenSpec strict 验证。
- 兼容性：新增字段为可选、默认空；旧项目迁移后现有字段行为不变；现有 API 调用方不需要立即传入 usage contract。
- 安全：使用契约是标准元数据，不应包含密码、token、完整 JDBC URL、DSN、Authorization 或业务数据行；保存和导出路径需要沿用现有脱敏/拒绝策略。
