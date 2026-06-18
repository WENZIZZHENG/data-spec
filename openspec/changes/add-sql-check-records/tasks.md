## 1. OpenSpec

- [x] 1.1 新增 P1-8 OpenSpec proposal/design/tasks/spec delta。
- [x] 1.2 运行 `openspec validate add-sql-check-records --strict`。

## 2. 后端契约确认

- [x] 2.1 确认 `LintResult.fixedSql`、`ds_sql_check_record`、记录 service/repository/controller 已可用。
- [x] 2.2 运行后端测试，确保半成品后端通过验证。

## 3. 前端实现

- [x] 3.1 更新 `schema.ts`、`types/index.ts` 和 `api/lint.ts`。
- [x] 3.2 `SqlLint.vue` 展示修正 SQL，支持复制。
- [x] 3.3 `SqlLint.vue` 展示最近检查记录、分页和详情 dialog。

## 4. 文档与验证

- [x] 4.1 更新 TODO.md P1-8 状态。
- [x] 4.2 运行 `pnpm build`、后端测试、OpenSpec validate 和 diff 空白检查。
- [x] 4.3 直接代码评审，不使用子 agent。
- [x] 4.4 修复评审发现后创建本地 commit。
