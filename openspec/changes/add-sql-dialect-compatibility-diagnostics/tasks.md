## 1. 后端诊断模型与矩阵

- [ ] 1.1 新增方言诊断模型、能力枚举和支持级别，统一输出 `dialect`、`capability`、`level`、`code`、`message`、`nextAction`。
- [ ] 1.2 新增 `SqlDialectCompatibilityService`，支持 SQL 文本方言识别、PostgreSQL/MySQL 静态能力矩阵和数据库直连诊断。
- [ ] 1.3 为方言识别、能力矩阵和 unsupported/unknown 场景补单元测试。

## 2. 后端链路接入

- [ ] 2.1 扩展 `LintResult` 与 `SqlLintService`，让 SQL lint/fixedSql 返回 dialectDiagnostics 并落入检查记录 JSON。
- [ ] 2.2 扩展 `DdlGenerateResult` 与 `DdlGeneratorService`，声明生成 SQL 的 PostgreSQL 目标方言和兼容边界。
- [ ] 2.3 扩展 `ReverseImportPreview`、`ReverseImportServiceImpl` 和 `DatabaseReverseImportServiceImpl`，让 SQL 文本和数据库直连预览返回 dialectDiagnostics。
- [ ] 2.4 补充 PostgreSQL/MySQL fixture 或现有单测断言，覆盖 COMMENT、自增、索引/table option、schema/catalog、quoted identifier 和 fixedSql 风险提示。

## 3. 前端与 CLI 展示

- [ ] 3.1 更新前端 OpenAPI 类型产物和类型导出，包含 `DialectDiagnostic`。
- [ ] 3.2 在 SQL 校验、DDL 生成和反向导入页面展示方言摘要、warning/partial/unsupported 诊断与 nextAction。
- [ ] 3.3 更新 CLI lint 文本输出摘要，JSON 输出保留完整 dialectDiagnostics，并补 Node 测试。

## 4. 文档、待办与验证

- [ ] 4.1 更新 README 方言能力矩阵、已验证能力和已知边界；TODO 将 P6-22 标记为已完成第一版并更新下一步顺序。
- [ ] 4.2 运行 `mvn test`、`pnpm test`、`pnpm build`、CLI/MCP Node 测试、`npx.cmd openspec validate add-sql-dialect-compatibility-diagnostics` 和 `git diff --check`。
- [ ] 4.3 按代码评审清单做直接评审，不使用子 agent；修复发现的问题或记录暂不处理理由。
- [ ] 4.4 通过验证后创建本地 commit。
