## Why

字段标准变多后，单一字段表格搜索已经不够。个人/小团队需要按数据域、分类和标签快速组织字段，并定位未分组字段；AI 也需要知道当前导出的字段上下文属于哪个业务范围。

## What Changes

- 扩展字段库后端能力：提供项目内字段分组摘要，并支持批量更新字段的 `domainId`、`category`、`tags` 等轻量分组属性。
- 强化前端“标准字段库”：新增分组视图、未分组字段提示、按分组筛选和批量归组入口。
- 增强 AI Context 字段目录：在按需裁剪时输出分组摘要，让 AI 明确当前字段包覆盖哪些数据域、分类和标签。
- 保持现有字段 CRUD、数据域 CRUD、分页查询和 AI Context 旧字段兼容。

## Capabilities

### New Capabilities

- `field-grouping-experience`: 项目内字段按数据域、分类、标签和未分组状态浏览、批量归组，以及 AI Context 分组摘要。

### Modified Capabilities

无。

## Impact

- 后端：`/api/fields` 增加分组摘要与批量元数据更新接口；字段服务补充批量归组和统计逻辑。
- 前端：`FieldLibrary.vue` 增加分组视图、筛选、选择和批量归组对话框；`src/api/field.ts` 与类型导出同步。
- AI Context：`field-catalog.json`、`manifest.json` 或 `contextScope` 增加可选分组摘要字段，保持向后兼容。
- 测试：新增字段服务/控制器测试、AI Context 分组摘要测试；前端至少通过类型构建。
- 文档：README 与 TODO 更新 P6-14 状态和使用说明。
