## 1. OpenSpec

- [x] 1.1 创建 P6-35 proposal、design、spec 和 tasks。
- [x] 1.2 通过 OpenSpec change 校验。

## 2. 后端修复策略模型

- [x] 2.1 新增 `FixPolicy`、`FixRiskLevel`、`FixChange`、`FixedSqlPlan` 等 lint 模型，覆盖默认策略、dry-run、disabled 和风险比较。
- [x] 2.2 扩展 `LintIssue` 与 `LintResult`，输出 issue 级 fixer 风险/状态/解释和 result 级 fix plan/summary/nextActions。
- [x] 2.3 扩展 `/api/lint` request，接收可选 `fixPolicy` 并保持旧请求兼容。

## 3. 后端生成器与记录链路

- [x] 3.1 重构 `FixedSqlGenerator` 返回固定 SQL计划，支持规则 allow/deny、风险上限、dry-run 和 disabled 策略。
- [x] 3.2 在 `SqlLintService` 中接入 plan，生成 fixedSql/fixedSqlDiff/dialectDiagnostics 并把策略状态写入检查记录 issues JSON 和 AI 回放输出。
- [x] 3.3 补后端单测，覆盖默认兼容、safe-only、disabled、dry-run、suppressed 跳过和 unsafe rebuild 解释。

## 4. 前端与契约

- [x] 4.1 重新生成或手动更新 OpenAPI TS schema/types，保证新增 `FixPolicy`、`FixChange`、`FixedSqlPlan` 字段可用。
- [x] 4.2 更新 SQL 校验页，增加策略控件、dry-run 标识、fix summary、变更列表和记录详情 issue 级风险展示。
- [x] 4.3 更新前端源码级 smoke/类型检查，覆盖策略控件、变更列表和风险文案。

## 5. 文档与收尾

- [x] 5.1 更新 README、TODO 和 AI 输出契约文档，说明 fixedSql 策略、dry-run 和人工确认边界。
- [x] 5.2 执行后端、前端、OpenSpec 和 diff 验证；记录 Docker/服务类不可用限制（如有）。
- [x] 5.3 完成结构化代码评审并修复 findings。
- [x] 5.4 创建本地 commit。
- [x] 5.5 归档 OpenSpec change 并再次验证。

## Verification Evidence

- `mvn "-Dtest=FixedSqlGeneratorTest,SqlLintServiceTest" test`：23 tests, 0 failures。
- `mvn test`：289 tests, 0 failures。
- `pnpm test`：73 tests, 0 failures。
- `node --test tools/dataspec-config.test.mjs tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs tools/prompt-template-eval.test.mjs tools/dataspec-local-smoke.test.mjs`：79 tests, 0 failures。
- `pnpm build`：`vue-tsc --noEmit && vite build` 通过；保留现有 Rolldown pure annotation 与 chunk size warning。
- `npx.cmd openspec validate add-fixed-sql-policy-dry-run`：通过。
- `git diff --check`：通过；仅有 CRLF/LF 工作区换行提示。
- 结构化代码评审：已检查功能覆盖、兼容性、记录链路、前端状态和契约文档；修复了 `includeExplanations=false` 时文档未说明 `fixExplanations` 可为空的发现。
