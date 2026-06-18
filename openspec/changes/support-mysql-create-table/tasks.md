## 1. OpenSpec

- [x] 1.1 新增 P3-1a proposal/design/tasks/spec delta。
- [x] 1.2 运行 `openspec validate support-mysql-create-table --strict`。

## 2. 实现

- [x] 2.1 增加 MySQL `CREATE TABLE` 解析回归测试。
- [x] 2.2 支持列级 `COMMENT '...'`。
- [x] 2.3 支持表级 `COMMENT='...'`。
- [x] 2.4 保持 PostgreSQL `COMMENT ON` 测试通过。

## 3. 验证与提交

- [x] 3.1 更新 TODO.md P3-1a 状态。
- [x] 3.2 运行 `mvn test`。
- [x] 3.3 运行 OpenSpec validate 和 diff 空白检查。
- [x] 3.4 直接代码评审，不使用子 agent。
- [x] 3.5 创建本地 commit。
