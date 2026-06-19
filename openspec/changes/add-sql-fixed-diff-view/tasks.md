## 1. OpenSpec

- [x] 1.1 新增 P4-2 proposal/design/tasks/spec delta。
- [x] 1.2 运行 `openspec validate add-sql-fixed-diff-view --strict`。

## 2. 后端

- [x] 2.1 新增 `SqlDiffGenerator` 行级 unified diff 测试。
- [x] 2.2 实现 `SqlDiffGenerator`。
- [x] 2.3 `LintResult` 增加 `fixedSqlDiff` 字段。
- [x] 2.4 `SqlLintService` 在生成 `fixedSql` 后同步生成 `fixedSqlDiff`。
- [x] 2.5 增加 service 测试，确认 lint 响应包含 diff。

## 3. 前端

- [x] 3.1 更新 `schema.ts` 类型契约。
- [x] 3.2 `SqlLint.vue` 展示当前 lint 结果 diff 视图。
- [x] 3.3 检查记录详情展示 diff 视图。

## 4. 文档与验证

- [x] 4.1 更新 TODO.md P4-2 状态。
- [x] 4.2 运行后端测试与 `mvn test`。
- [x] 4.3 运行前端 `pnpm build`。
- [x] 4.4 运行 OpenSpec validate 和 diff 空白检查。
- [x] 4.5 直接代码评审，不使用子 agent。
- [x] 4.6 创建本地 commit。
