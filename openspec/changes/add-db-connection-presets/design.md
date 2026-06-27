## Context

反向导入页已经支持数据库直连测试、加载表、metadata 预览、二次比对和确认导入；P5-8 也通过浏览器 localStorage 记住了非敏感的最近使用状态。P6-10 需要把“可复用连接预设”提升为项目级能力，让前端、后端和后续 AI/CLI 都能读取同一组非敏感连接配置。

现有安全边界仍然有效：DataSpec 不保存数据库密码、API token、完整 JDBC URL，也不创建连接池或后台同步任务。

## Goals / Non-Goals

**Goals:**
- 按项目保存多个数据库连接预设。
- 仅保存非敏感字段：`name`、`databaseType`、`host`、`port`、`databaseName`、`schemaName`、`tableNames`。
- 前端反向导入页可以加载预设、保存当前非敏感连接信息为预设，并继续要求用户当次输入密码。
- 使用预设后不改变现有测试连接、加载表、预览、compare 和确认导入 API 的语义。
- 为敏感字段排除增加测试，防止后续误把 password/token/url 写入模型或前端提交。

**Non-Goals:**
- 不保存数据库密码、token、完整 JDBC URL 或连接串。
- 不做连接池、后台定时同步、健康监控或自动重连。
- 不把预设扩展成团队权限、审批或组织级连接管理。
- 不改造现有反向导入服务的 metadata 读取逻辑。

## Decisions

- **服务端保存项目级预设。** 相比只用 localStorage，服务端预设能跨浏览器、跨 AI agent 复用，并沿用项目访问控制；localStorage 仍可保留临时表单记忆。
- **表选择使用 JSON 文本存储。** 第一版只需要保存 `tableNames` 列表，使用 `table_names_json` 避免新增子表和排序复杂度；后续若需要表级策略再拆分。
- **保存接口只接受白名单字段。** Controller request 不包含 `password`、`token`、`jdbcUrl` 等字段；service 再做二次校验，避免前端或 AI 误传敏感字段。
- **预设不直接发起连接。** 加载预设只回填非敏感字段，密码仍由用户当次输入；现有连接测试和加载表接口继续接收完整临时请求体。
- **不自动覆盖同名预设。** 第一版提供创建、更新、删除；前端“保存为预设”默认创建，编辑/覆盖可在预设列表中完成，避免误覆盖历史连接。

## Risks / Trade-offs

- **[Risk] 用户误以为预设会保存密码。** → 前端文案和字段设计明确不保存密码，接口模型也不包含密码。
- **[Risk] tableNames JSON 损坏导致页面异常。** → service 提供解析兜底，前端展示空数组而不是中断。
- **[Risk] 预设过多影响选择体验。** → 列表按更新时间倒序，第一版不做复杂分组。
- **[Risk] 服务端预设与浏览器本地记忆冲突。** → 加载预设时显式覆盖当前非敏感表单字段；密码字段保持当前值或清空，不从预设恢复。

## Migration Plan

新增 `ds_database_connection_preset` 表，不影响现有反向导入数据。回滚时删除新增 controller/service/repository/frontend 页面入口；表可保留无害，也可手动删除。

## Open Questions

- 预设是否需要在后续暴露给 CLI/MCP 使用，留到 P6-11 工作流模板或后续 AI profile 中决定。
