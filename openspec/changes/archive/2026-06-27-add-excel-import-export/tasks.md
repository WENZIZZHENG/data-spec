## 1. OpenSpec

- [x] 1.1 新增 P2-5 proposal/design/tasks/spec delta。
- [x] 1.2 运行 `openspec validate add-excel-import-export --strict`。

## 2. 后端

- [x] 2.1 增加 Excel 导入导出服务测试，先覆盖模板/导出、预览、确认导入。
- [x] 2.2 引入 Apache POI `.xlsx` 读写依赖。
- [x] 2.3 增加 Excel 预览/导入结果模型。
- [x] 2.4 扩展 `ImportExportService` 支持模板、导出、预览和确认导入。
- [x] 2.5 扩展 `ImportExportController` 暴露 Excel 接口。

## 3. 前端

- [x] 3.1 增加导入导出 API 封装和类型。
- [x] 3.2 将 `ImportExport.vue` 从占位页升级为可用页面。
- [x] 3.3 页面支持模板下载、项目 Excel 导出、上传预览、确认导入和错误展示。

## 4. 验证与提交

- [x] 4.1 运行 `mvn test`。
- [x] 4.2 运行 `pnpm build`。
- [x] 4.3 运行 OpenSpec validate 和 diff 空白检查。
- [x] 4.4 直接代码评审，不使用子 agent。
- [x] 4.5 更新 TODO.md P2-5 状态并创建本地 commit。
