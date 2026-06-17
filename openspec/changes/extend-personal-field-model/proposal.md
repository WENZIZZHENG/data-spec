## Why

DataSpec 要优先服务 AI，字段目录不能只有字段名和类型。个人版字段标准需要表达“同义叫法”“是否敏感”“是否仍推荐使用”“关联代码集/枚举”“示例值”等语义，否则 AI 在生成 SQL 或做字段推荐时很难复用标准字段。

## What Changes

- 扩展 `ds_field` 字段模型：`aliases`、`category`、`codeSetId`、`sensitive`、`status`、`exampleValue`。
- 新增 Flyway 迁移，保持老库可升级。
- 扩展字段 CRUD 请求/响应实体，服务层补默认值。
- 扩展 AI `field-catalog.json` 和 schema，导出别名数组、敏感标记、状态、代码集和示例值。
- 更新 TODO/README 和 OpenAPI TypeScript 生成产物。
- 本阶段不新建别名表，不做字段库前端页面重做，不引入草稿/审核/发布流程。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `field-model`: 标准字段模型增加 AI 可读的个人字段元数据。
- `ai-context-package`: 字段目录导出包含新的字段元数据。

## Impact

- 数据库迁移：新增 `V3__extend_field_metadata.sql`。
- 后端模块：`field`、`aicontext`、`importexport` 通过 `Field` 实体自动携带新增字段。
- 前端契约：更新 `dataspec-web/src/api/schema.ts`。
