## Why

DataSpec 已经部分支持 PostgreSQL/MySQL 的 SQL 解析、lint、DDL 生成、fixedSql 和数据库反向导入，但当前结果没有说明“这是按哪个方言判断的、哪些能力已验证、哪些地方是降级或不支持”。AI、CLI 和前端用户在真实业务仓库中很难判断诊断可靠边界，容易把未验证语法当成稳定支持。

## What Changes

- 新增 PostgreSQL/MySQL 方言能力矩阵，覆盖 `COMMENT`、自增、索引/约束、类型映射、schema/catalog、quoted identifier 和 fixedSql 风险等高频能力。
- 在 SQL lint/fixedSql、DDL 生成和反向导入结果中返回统一的 `dialectDiagnostics`，包含 `dialect`、`capability`、`level`、`code`、`message`、`nextAction` 与 AI 可解析上下文。
- 支持 SQL 文本入口的轻量方言识别；数据库直连入口使用用户选择的 `databaseType` 生成诊断。
- 前端 SQL 校验、DDL 生成和反向导入页面展示当前方言及降级原因；CLI JSON/文本输出保留机器可读诊断。
- README 标明第一版已验证方言能力和边界，TODO 将 P6-22 标记为已完成第一版。

## Capabilities

### New Capabilities

- `sql-dialect-compatibility-diagnostics`: 覆盖方言能力矩阵、可机器读取诊断、前端/CLI 展示和文档化边界。

### Modified Capabilities

- `sql-parser`: 解析结果需要暴露方言识别和不支持/降级语法诊断。
- `sql-lint-rules`: lint 结果需要包含当前方言和规则适用性的诊断信息。
- `structured-lint-fixes`: fixedSql 需要声明方言相关风险或降级原因。
- `ddl-generator-tool`: DDL 生成结果需要说明生成 SQL 的目标方言和兼容边界。
- `reverse-import`: SQL/数据库反向导入预览需要返回所用方言和 metadata 能力诊断。

## Impact

- 后端：新增方言诊断模型/服务；扩展 `LintResult`、`DdlGenerateResult`、`ReverseImportPreview` 和数据库反向导入流程；补充 PostgreSQL/MySQL fixture 和单测。
- 前端：SQL 校验、DDL 生成、反向导入页面展示诊断摘要；更新 OpenAPI 类型产物。
- CLI/MCP：CLI lint 输出展示方言诊断，JSON 输出保留新增字段；MCP 可通过现有 API 透传。
- 文档：README 增加方言能力矩阵与已知边界说明，TODO 更新 P6-22 状态。
- 边界：不新增数据库写入行为，不引入完整 parser 重写，不宣称 Oracle/SQL Server 全量支持。

## Verification Evidence

- `mvn test`：229 tests，0 failures，0 errors。
- `pnpm test`：55 tests，0 failures。
- `pnpm build`：通过，保留第三方 `@vueuse/core` pure annotation 与 chunk size warning。
- `node --test tools\dataspec-cli.test.mjs tools\dataspec-config.test.mjs tools\dataspec-mcp.test.mjs`：62 tests，0 failures。
- `npx.cmd openspec validate add-sql-dialect-compatibility-diagnostics`：valid。
- `git diff --check`：通过，仅输出 Windows LF/CRLF 提示。
- 代码评审：按 `code-review-checklist` 做直接评审，未使用子 agent；已修复 MySQL `UNSIGNED/KEY` 弱特征识别不足和前端方言摘要标签未按最严重诊断染色的问题。
