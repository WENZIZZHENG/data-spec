## Why

Excel 导入预览目前只有新增/更新/冲突统计和错误列表，用户无法看到每一行将如何处理、哪些字段会改变，以及冲突原因。批量维护字段标准时，这会增加误导入和反复试错成本。

## What Changes

- 在 Excel 导入预览中新增行级明细 `items[]`，包含 sheet、行号、业务键、动作、状态、原因和字段 diff。
- 后端预览阶段为字段、代码集、枚举值生成 create/update/conflict 明细。
- 字段更新时输出 before/after 差异；新增时输出待写入值。
- 前端导入预览页展示明细表、状态标签、原因和字段 diff。
- 保持确认导入逻辑和 Excel 模板格式不变。

## Capabilities

### New Capabilities

- `excel-import-dry-run-details`: Excel 导入 dry-run 明细、字段 diff 和前端可读预览。

### Modified Capabilities

无。

## Impact

- 后端影响 `ExcelImportPreview` 模型、导入导出 service 和对应测试。
- 前端影响导入导出页面、手写类型和可能的 API schema。
- 不新增数据库表，不改变实际导入写入流程。
