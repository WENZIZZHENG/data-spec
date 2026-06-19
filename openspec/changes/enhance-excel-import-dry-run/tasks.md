## 1. 后端 dry-run 明细

- [x] 1.1 新增 Excel 预览项和 diff 模型，挂到 `ExcelImportPreview.items`。
- [x] 1.2 扩展 `ImportExportService` 预览逻辑，为字段、代码集、枚举值生成 create/update/conflict 明细。
- [x] 1.3 扩展后端单测覆盖更新 diff、新增提交值、重复/未知引用冲突明细。

## 2. 前端预览展示

- [x] 2.1 更新前端 Excel 导入类型，增加预览项和 diff 类型。
- [x] 2.2 改造 `ImportExport.vue`，展示 dry-run 明细表、状态标签、原因和字段 diff。

## 3. 文档与验证

- [x] 3.1 更新 README/TODO 中 Excel dry-run 状态。
- [x] 3.2 运行后端测试、前端测试/构建、OpenSpec validate 和 diff 检查。
- [x] 3.3 进行直接代码评审（不使用子 agent），修复发现的问题后提交本地 commit。
