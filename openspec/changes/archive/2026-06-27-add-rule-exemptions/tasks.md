## 1. OpenSpec 与测试

- [x] 1.1 新增 `add-rule-exemptions` OpenSpec artifacts 并通过校验。
- [x] 1.2 新增后端 rule exemption service/controller/lint 抑制测试。
- [x] 1.3 新增前端展示工具函数测试。

## 2. 后端实现

- [x] 2.1 新增 Flyway migration `ds_rule_exemption`。
- [x] 2.2 新增 entity/repository/service/controller。
- [x] 2.3 `SqlLintService` 应用项目例外，suppressed issue 不计入 active 统计。
- [x] 2.4 `AiContextExportService` 导出例外说明。

## 3. 前端实现

- [x] 3.1 新增 API wrapper 和类型定义。
- [x] 3.2 新增规则例外管理页，支持列表、新建、禁用/删除。
- [x] 3.3 新增路由和侧边栏入口。

## 4. 文档、验证与收尾

- [x] 4.1 更新 README。
- [x] 4.2 更新 TODO，将 P6-9 标记为已完成并指向 P6-10。
- [x] 4.3 运行后端、前端与 OpenSpec 验证。
- [x] 4.4 进行直接代码评审并修复发现问题。
- [x] 4.5 创建本地 commit 后继续下一个待办。
