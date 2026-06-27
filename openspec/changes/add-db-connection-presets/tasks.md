## 1. OpenSpec 与测试基线

- [x] 1.1 新增 `add-db-connection-presets` OpenSpec artifacts 并通过校验。
- [x] 1.2 新增后端 preset service/controller/敏感字段排除测试。
- [x] 1.3 新增前端 preset 展示与敏感字段剔除工具测试。

## 2. 后端实现

- [x] 2.1 新增 Flyway migration `ds_database_connection_preset`。
- [x] 2.2 新增 entity/repository/service/controller。
- [x] 2.3 service 只接受并返回非敏感字段，tableNames 以 JSON 持久化并解析兜底。
- [x] 2.4 接入项目访问边界，支持列表、新建、更新、删除。

## 3. 前端实现

- [x] 3.1 新增 API wrapper、类型定义和 display/sanitize 工具。
- [x] 3.2 改造反向导入页直连表单，支持选择预设并加载非敏感字段。
- [x] 3.3 支持将当前直连表单保存为预设，提交前剔除 password/token/JDBC URL。
- [x] 3.4 保持现有测试连接、加载表、preview、compare、confirm import 流程可用。

## 4. 文档、验证与收尾

- [x] 4.1 更新 README。
- [x] 4.2 更新 TODO，将 P6-10 标记为已完成并指向 P6-11。
- [x] 4.3 运行后端、前端与 OpenSpec 验证。
- [x] 4.4 进行直接代码评审并修复发现问题。
- [x] 4.5 创建本地 commit 后继续下一个待办。
