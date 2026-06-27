## Why

反向导入、Excel 导入和字段质量检查会一次性暴露大量待维护字段；逐条编辑会拖慢个人/小团队日常维护，误操作后也缺少快速恢复入口。

## What Changes

- 扩展字段后端能力：新增项目内字段批量维护接口，支持显式 patch 更新 `status`、`category`、`tags`、`sensitive`、`codeSetId` 等常用属性。
- 增加轻量影响预览：提交批量维护前返回选中字段数量、将更新的字段、关键属性变化和风险提示。
- 基于现有 `ds_standard_change_log` 增加单条字段变更回退能力：从最近字段变更日志恢复上一版关键属性，并写入新的回退日志。
- 强化前端“标准字段库”：在现有多选基础上新增批量维护对话框、预览确认和最近变更回退入口。
- 保持现有字段 CRUD、批量归组、变更日志记录和项目边界校验兼容。

## Capabilities

### New Capabilities
- `field-bulk-maintenance`: 字段库批量维护常用属性、提交前预览，以及基于字段变更日志的单条可控回退。

### Modified Capabilities

无。

## Impact

- 后端：`FieldService`/`FieldController` 增加批量维护预览、批量维护提交和字段变更回退接口；`StandardChangeLogService` 增加按目标查询最近日志或按日志回退所需读取能力。
- 前端：`FieldLibrary.vue` 增加批量维护入口、预览确认和回退操作；`src/api/field.ts`、类型导出与 OpenAPI schema 同步。
- 测试：新增字段服务/控制器测试，覆盖显式 patch、跨项目拒绝、预览、变更日志写入和回退；前端至少通过类型构建。
- 文档：README 与 TODO 更新 P6-15 状态、边界和验证方式。
