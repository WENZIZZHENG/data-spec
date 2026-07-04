## 1. 规格与测试

- [x] 1.1 校验 OpenSpec proposal/design/spec/tasks 与 P6-56 范围一致。
- [x] 1.2 新增/更新后端测试：保留字/危险词、大小写碰撞、ambiguous alias、AI Context 命名风险导出。
- [x] 1.3 新增/更新前端测试：字段冲突页支持新增类型标签和摘要入口。

## 2. 后端实现

- [x] 2.1 扩展 `FieldConflictType`，新增 `RESERVED_WORD`、`DANGEROUS_SQL_NAME`、`CASE_COLLISION`、`AMBIGUOUS_ALIAS`。
- [x] 2.2 在 `FieldConflictServiceImpl` 中内置 PostgreSQL/MySQL/通用 SQL 高频保留字和危险词检测，输出 dialect evidence 与替代命名建议。
- [x] 2.3 检测 field name/alias 的大小写碰撞和 alias/canonical 歧义，保持报告只读。
- [x] 2.4 扩展 AI Context `DATABASE_RULES.md`，导出命名风险摘要。

## 3. 前端实现

- [x] 3.1 更新 OpenAPI schema 与前端类型。
- [x] 3.2 扩展 `fieldConflictDisplay` 和 `FieldConflicts.vue`，展示新增冲突类型、风险证据和建议动作。

## 4. 文档、验证与收口

- [x] 4.1 更新 README/TODO，记录 P6-56 第一版能力和边界。
- [x] 4.2 运行 `openspec validate add-field-naming-risk-detection --strict`。
- [x] 4.3 运行后端相关测试、`mvn test`、前端 `pnpm test`、`pnpm build`，后端可用时运行 `pnpm gen:api`/`pnpm check:api`。
- [x] 4.4 使用独立代码评审 agent 审查本次变更，修复 findings 后复跑必要验证。
- [x] 4.5 归档 OpenSpec change 并提交。

## Verification Evidence

- `openspec validate add-field-naming-risk-detection --strict`：通过。
- `mvn "-Dtest=FieldConflictServiceImplTest,AiContextExportServiceTest,PerformanceBaselineTest" test`：25 tests, 0 failures, 0 errors。
- `pnpm gen:api`：从 `http://localhost:8090/api-docs` 重新生成 `dataspec-web/src/api/schema.ts`。
- `pnpm check:api`：`OpenAPI schema.ts 已是最新`。
- 独立代码评审 agent `Laplace`：发现 1 个 P1，`DATABASE_RULES.md` 命名风险摘要绕过 AI Context scope 裁剪；已改为基于 scoped fields 检测，并补 scoped package 回归测试。
- `mvn test`：381 tests, 0 failures, 0 errors。
- `pnpm test`：100 tests, 0 failures。
- `pnpm build`：通过，保留既有 Rolldown pure annotation 和 chunk size warning。
- `openspec validate --all`：92 items passed, 0 failed。
- `git diff --check`：通过，仅保留 Windows LF/CRLF 提示。
