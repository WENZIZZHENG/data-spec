## 1. OpenSpec

- [x] 1.1 新增 P4-1 proposal/design/tasks/spec delta。
- [x] 1.2 运行 `openspec validate add-sql-issue-source-span --strict`。

## 2. 后端

- [x] 2.1 为 `LintIssue` 增加 `line/column/sourceStart/sourceEnd` 字段。
- [x] 2.2 新增 source span resolver，按表名/字段名回填定位。
- [x] 2.3 在 `SqlLintService` 主链路接入定位回填，并保证检查记录保存新字段。
- [x] 2.4 增加后端测试，覆盖字段级、表级和不可定位问题。

## 3. 前端

- [x] 3.1 更新前端类型契约，补齐 `LintIssue` 定位字段。
- [x] 3.2 `SqlLint.vue` 展示问题位置并支持点击跳转 Monaco。
- [x] 3.3 检查记录详情展示位置字段。

## 4. 文档与验证

- [x] 4.1 更新 TODO.md P4-1 状态。
- [x] 4.2 运行后端测试与 `mvn test`。
- [x] 4.3 运行前端 `pnpm build`。
- [x] 4.4 运行 OpenSpec validate 和 diff 空白检查。
- [x] 4.5 直接代码评审，不使用子 agent。
- [x] 4.6 创建本地 commit。
