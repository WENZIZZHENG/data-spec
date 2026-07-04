## Why

数据库直连反向导入、二次比对和覆盖率报告已经成为核心入口；AI 在调用前需要知道连接是否可用、失败是否可恢复、当前方言支持哪些 metadata 能力，而不是只拿到泛化的“连接失败”。

## What Changes

- 扩展现有数据库连接测试响应，在保留 `security` 诊断的同时增加结构化 `health` 画像。
- `health` 输出 connectionStatus、latencyMs、databaseProduct、version、schema/comment/index capability、readonlyCheck、requiredPrivileges、warnings 和 nextActions。
- 失败连接也返回脱敏的结构化诊断，区分认证失败、网络不可达、schema 不存在、权限不足和不支持的方言等高频场景。
- 反向导入页和覆盖率报告页复用现有诊断卡片展示 health/capability 摘要，不保存密码、token 或完整 JDBC URL。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `db-readonly-security-diagnostics`: 在现有只读安全诊断上增加连接健康与方言能力画像。

## Impact

- 后端：扩展 `DatabaseConnectionResult`、新增连接健康模型，复用 `DatabaseReverseImportServiceImpl.testConnection`。
- 前端：扩展 `DatabaseConnectionSecurityDiagnostic`/连接结果类型，更新反向导入和覆盖率页诊断展示。
- OpenAPI：重新生成 `dataspec-web/src/api/schema.ts`。
- 测试：补后端连接失败分类/能力画像测试，补前端 smoke/展示测试。
