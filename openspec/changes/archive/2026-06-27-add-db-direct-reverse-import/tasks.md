## 1. OpenSpec

- [x] 1.1 新增 P4-11 proposal/design/tasks/spec delta。
- [x] 1.2 运行 `openspec validate add-db-direct-reverse-import --strict`。

## 2. 后端

- [x] 2.1 新增数据库直连 metadata 服务测试，覆盖 PostgreSQL/MySQL 请求参数、预览映射和确认导入去重。
- [x] 2.2 新增数据库直连请求/响应模型。
- [x] 2.3 实现连接测试、表列表、metadata 预览和确认导入。
- [x] 2.4 扩展 `ReverseImportController` API。

## 3. 前端

- [x] 3.1 新增 reverseImport API wrapper 和类型。
- [x] 3.2 改造 `ReverseImport.vue`，增加 SQL DDL/数据库直连 tabs。
- [x] 3.3 增加连接测试、表选择、预览、确认导入和结果反馈。

## 4. 验证与提交

- [x] 4.1 更新 TODO.md P4-11 状态。
- [x] 4.2 运行后端相关测试与 `mvn test`。
- [x] 4.3 运行前端 `pnpm build`。
- [x] 4.4 运行 OpenSpec validate 和 diff 空白检查。
- [x] 4.5 直接代码评审，不使用子 agent。
- [x] 4.6 创建本地 commit。
