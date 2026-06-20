## 1. 后端 compare 模型与测试

- [x] 1.1 新增 compare 结果模型，包含 summary、tableDiffs、fieldDiffs、changes 和状态枚举。
- [x] 1.2 在 `ReverseImportServiceTest` 或新增测试中覆盖字段名/alias 命中、属性变化、新增字段和缺注释。
- [x] 1.3 在 `DatabaseReverseImportServiceTest` 覆盖直连 metadata compare 复用链路。

## 2. 后端 API 实现

- [x] 2.1 在 `ReverseImportService` 实现基于 `TableDef` 与项目标准字段的 compare 方法。
- [x] 2.2 在 `DatabaseReverseImportService` 新增 `compare(DatabaseConnectionReq)` 并复用现有读取 selected tables 逻辑。
- [x] 2.3 在 `ReverseImportController` 新增 `POST /api/reverse-import/database/compare`。

## 3. 前端差异视图

- [x] 3.1 更新前端 API/types，新增 database compare 调用和必要类型。
- [x] 3.2 在 `ReverseImport.vue` 增加“生成差异”入口、状态筛选、摘要指标和按表分组差异表。
- [x] 3.3 保持现有 preview/import 流程可用，不让 compare 自动写入字段库。

## 4. 文档、待办与验证

- [x] 4.1 更新 README 反向导入说明，补数据库二次比对能力。
- [x] 4.2 更新 `TODO.md`，将 P5-3 标记为已完成第一版，并推进下一步顺序。
- [x] 4.3 运行后端测试、前端测试/build、OpenSpec validate 和 diff 检查。
- [x] 4.4 进行直接代码评审（不使用子 agent），修复发现的问题后提交本地 commit。
