## Why

DataSpec 已经能维护字段标准并基于表模板生成 DDL，但 AI 建表时仍缺少“业务对象之间如何关联、表结构应有哪些主键/唯一键/索引/外键/审计/软删除约束”的稳定上下文。P6-76 与 P6-106 应合并交付一个轻量闭环，让用户和 AI 在生成订单、用户、支付等常见表时不仅列名正确，也能得到可解释、可 lint、可导出的表级结构建议。

## What Changes

- 新增业务对象与表结构标准能力，支持按项目维护业务对象、目标表模式、必选/可选标准字段、关系、外键提示、审计字段和常见反模式。
- 扩展表模板或等价表级标准模型，表达 `primaryKey`、`uniqueKeys`、`indexes`、`foreignKeys`、`checkHints`、`auditPolicy`、`softDeletePolicy`、`dialectNotes` 和 `aiUsageNotes`。
- DDL preview 在使用表模板时消费表级约束和关系提示，输出主键、唯一键、索引、外键建议、lint 摘要和可解释 evidence。
- AI Context 可按业务对象或表模板导出结构标准，让 AI 能在离线包中读取对象关系、字段依赖和表级策略。
- Schema Registry 登记新增的业务对象、表结构标准、表关系和 DDL 约束摘要契约，供 CLI、MCP 和 AI 客户端校验稳定字段。
- CLI/MCP 增加只读入口或扩展既有 DDL/Context 输出，使 AI 可以获取表结构标准，不需要调用写入 API。
- 前端补一个最小可用维护入口：业务对象/表模板标准列表、详情、字段选择、DDL preview/lint 摘要和简易关系图。
- 不引入 breaking change；现有字段标准、DDL preview、AI Context、CLI/MCP 输出保持兼容，新增字段均为 additive。

## Capabilities

### New Capabilities

- `business-object-table-standards`: 维护并消费项目级业务对象、表模板依赖、对象关系和表级约束/索引/外键/审计/软删除标准。

### Modified Capabilities

- `ddl-generator-tool`: DDL preview 应消费表级结构标准并返回约束、索引、关系和 lint evidence 摘要。
- `ai-context-package`: AI Context 应导出业务对象、表模板、表级结构标准和关系提示，并支持按业务对象或表模板裁剪。
- `standard-schema-registry`: Schema Registry 应登记业务对象、表结构标准、关系和 DDL 约束摘要契约。
- `dataspec-cli`: CLI 应提供只读方式获取表结构标准或在 DDL/Context 相关命令中保留新增结构标准字段。
- `dataspec-mcp`: MCP 应暴露只读表结构标准资源或工具，并在建表提示中引导 AI 读取该上下文。

## Impact

- 后端：新增或扩展业务对象/表模板标准 API、service、repository、DTO/schema，扩展 DDL preview、AI Context 和 Schema Registry 组装逻辑。
- 前端：新增或扩展表模板/业务对象标准维护页面、API wrapper、类型和关系图/DDL preview 展示。
- CLI/MCP/tools：新增只读命令/资源/fixture，或扩展既有 DDL/Context 契约测试以覆盖新增字段。
- OpenSpec/文档：新增能力规格并更新受影响能力 delta；任务完成前需要记录新鲜 Verification Evidence。
- 安全边界：第一版不执行数据库迁移，不自动改写业务库，不读取业务数据行，不保存或展示数据库凭据，不强制所有历史表补齐表级标准。
