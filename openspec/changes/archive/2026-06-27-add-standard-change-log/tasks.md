## 1. OpenSpec

- [x] 1.1 新增 P2-6 proposal/design/tasks/spec delta。
- [x] 1.2 运行 `openspec validate add-standard-change-log --strict`。

## 2. 后端

- [x] 2.1 增加 Flyway V5 迁移，创建 `ds_standard_change_log`。
- [x] 2.2 增加变更日志实体、Mapper、Repository、Service 和查询 Controller。
- [x] 2.3 在字段服务 create/update/delete 自动记录日志。
- [x] 2.4 在代码集与代码值服务 create/update/delete 自动记录日志。
- [x] 2.5 在规则配置服务 create/update/delete/toggle 自动记录日志。
- [x] 2.6 增加单元测试覆盖字段更新 before/after、代码集创建和规则 toggle。

## 3. 文档与验证

- [x] 3.1 更新 TODO.md P2-6 状态。
- [x] 3.2 运行 `mvn test`。
- [x] 3.3 运行 OpenSpec validate 和 diff 空白检查。
- [x] 3.4 直接代码评审，不使用子 agent。
- [x] 3.5 创建本地 commit。
