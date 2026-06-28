## Why

数据库直连反向导入、二次比对和覆盖率报告已经是核心入口，但连接测试目前只回答“能不能连上”。个人/小团队常会直接使用高权限账号，DataSpec 需要在不阻塞使用的前提下提示只读账号、权限范围和敏感信息边界，帮助用户和 AI 更安全地调用直连能力。

## What Changes

- 增强现有数据库连接测试结果，返回数据库类型、当前用户、只读/写权限推断、可访问 schema/table 统计、危险权限提示和推荐 SQL/动作。
- 前端反向导入、覆盖率报告等直连入口展示连接安全诊断，明确“适合反向导入/比对”或“建议切换只读账号”。
- 所有诊断输出继续脱敏，不返回 password、完整 JDBC URL、token 或源数据库数据行。
- 新增后端和前端测试，锁定只读账号、高权限账号、连接失败和输出脱敏边界。

## Capabilities

### New Capabilities

- `db-readonly-security-diagnostics`: 覆盖数据库直连测试的只读安全诊断、权限风险提示、推荐最小权限 SQL 和前端展示。

### Modified Capabilities

- 无。第一版以新增诊断字段的方式增强现有数据库连接测试，不移除或重命名已有字段。

## Impact

- 后端：增强 reverse import / coverage 共用的数据库连接测试模型和 service，增加只读安全诊断 DTO、JDBC metadata 查询和方言化权限推断。
- 前端：更新反向导入与覆盖率报告页面，展示连接安全等级、当前用户、可访问范围和推荐动作。
- OpenAPI/类型：新增连接安全诊断相关 schema 字段，前端 API wrapper 与类型同步。
- 测试：补后端 service 单测和前端 smoke/utility 测试；验证命令继续接入 `mvn test`、`pnpm test`、`pnpm build` 和 OpenSpec validate。
