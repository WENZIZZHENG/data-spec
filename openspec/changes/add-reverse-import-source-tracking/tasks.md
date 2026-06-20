## 1. 数据模型与迁移

- [x] 1.1 新增 Flyway 迁移，创建 `ds_reverse_import_batch` 与 `ds_field_source` 表、索引和注释。
- [x] 1.2 新增批次和字段来源 entity/mapper/service 模型，保持普通字段无来源记录也兼容。
- [x] 1.3 补充来源模型单元测试或 service 测试，覆盖新增来源、查询字段来源和无来源字段。

## 2. 导入写入链路

- [x] 2.1 扩展数据库直连确认导入请求，携带 databaseType/databaseName/schemaName/tableNames 等非敏感来源上下文。
- [x] 2.2 在反向导入确认导入时，同事务写入批次和新字段来源记录；跳过字段不记录为本批次导入。
- [x] 2.3 确认不会持久化数据库密码、完整连接 URL 或源库写操作。
- [x] 2.4 补充 `ReverseImportServiceTest`/`DatabaseReverseImportServiceTest` 覆盖批次统计、字段来源和全跳过场景。

## 3. 来源查询与前端展示

- [x] 3.1 新增字段来源查询 API，按 fieldId 返回批次摘要和字段级来源明细。
- [x] 3.2 更新前端 API/types，在字段库或字段详情展示来源摘要和无来源状态。
- [x] 3.3 反向导入确认导入请求传递来源上下文，导入结果保留现有新增/跳过反馈。

## 4. 文档、待办与验证

- [x] 4.1 更新 README 反向导入说明，补来源与批次追踪能力和安全边界。
- [x] 4.2 更新 `TODO.md`，将 P5-4 标记为已完成第一版，并推进下一步顺序。
- [x] 4.3 运行后端测试、前端测试/build、OpenSpec validate 和 diff 检查。
- [x] 4.4 进行直接代码评审（不使用子 agent），修复发现的问题后提交本地 commit。
