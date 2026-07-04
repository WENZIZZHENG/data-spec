## Why

字段名、类型和单个自由文本示例不足以让 AI 稳定生成正确 SQL、DDL 或校验规则。金额单位、手机号/邮箱格式、时间戳时区、JSON 结构、状态码取值等约束需要进入标准字段的结构化契约。

## What Changes

- 在 `ds_field` 上新增轻量字段格式约束扩展，覆盖 `formatType`、`formatPattern`、`formatUnit`、`formatPrecision`、`formatTimezone`、`formatNullPolicy`、`validExamplesJson`、`invalidExamplesJson` 和 `formatNotes`。
- 字段创建、编辑、回退、变更预览和 OpenAPI 类型保留这些格式约束。
- AI Context 的 `DATABASE_RULES.md` 和 `field-catalog.json` 导出字段格式说明，field catalog schema 同步新增 `format` 对象。
- 字段质量评分对金额、手机号、邮箱、时间/日期、JSON、状态/枚举等格式敏感字段提示缺少格式样例。
- 前端字段库提供格式约束编辑与列表摘要，便于个人/小团队给 AI 补齐字段值形态。

## Capabilities

### New Capabilities

- `field-format-constraints`: 字段值格式与校验样例库。

### Modified Capabilities

- `ai-context-package`: 字段目录和数据库规则导出格式约束。
- `field-quality-scoring`: 质量评分提示关键字段缺少格式样例。
- `ddl-generator-tool`: AI 生成 DDL 的上下文可读取单位和格式约束。

## Impact

- 后端：新增 Flyway 迁移、扩展 `Field`、`FieldReq`、字段服务、变更预览、AI Context 和质量评分。
- 前端：更新 OpenAPI schema/type，扩展字段库表单和展示。
- 测试：补 AI Context 导出、字段质量评分、字段 API/变更预览和前端源码冒烟测试。
- 文档：更新 README/TODO，记录第一版能力和边界。

## SDD Level

`full`。本次包含数据库迁移、数据模型、公共 API、AI Context 契约和用户可见前端入口，必须先明确规格、设计、任务和验证策略。
