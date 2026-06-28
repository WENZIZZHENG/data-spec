## Context

DataSpec 已经具备大量面向 AI 的入口：OpenAPI、Schema Registry、AI Context、workflow recipes、AI task profiles、doctor、CLI、MCP tools/resources 和前端任务入口。当前问题不是能力缺失，而是入口分散：AI agent 初次接入项目时，需要先猜应该读 README、运行 doctor、查 OpenAPI、读 workflow 还是直接调用 MCP tool。

P6-43 第一版目标是提供一个统一、机器可读、只读的 capability catalog。它应该告诉 AI：当前 DataSpec 支持哪些稳定任务能力、在哪些 surface 可调用、需要哪些参数/权限/前置检查、输出契约是什么、失败后下一步怎么恢复。

## Goals / Non-Goals

**Goals:**
- 新增后端只读 `/api/capabilities` catalog，支持可选 `projectId`，输出能力条目、surface 映射、权限/风险、示例、契约和 nextActions。
- CLI/MCP 暴露同一份 catalog，供 agent 启动时先读取能力清单。
- AI Context zip/离线缓存包含 `.dataspec/capabilities.json`，让离线 agent 也能读取最近能力信息。
- 能力条目优先覆盖当前已稳定的高频 AI 任务：doctor、export context、SQL lint/fixedSql、字段检索/推荐、DDL 生成、反向导入、覆盖率、schema registry、evidence package、workflow/profile。
- 保持第一版轻量可测试，不引入数据库表或动态配置 UI。

**Non-Goals:**
- 不自动执行能力，不做任务编排引擎。
- 不替代 OpenAPI、Schema Registry、workflow recipes、AI task profiles 或 doctor；catalog 只是目录和路由。
- 不声明实验性内部接口为稳定能力。
- 不做前端完整能力市场或插件系统。
- 不改变现有权限模型；安全仍由 API Token、ProjectAccessGuard 和具体 API 负责。

## Decisions

1. **代码内置 registry，而不是数据库管理 catalog。**
   - 原因：能力清单应随代码版本发布并被测试锁定，避免个人版引入能力发布/审核/权限 UI。
   - 备选：数据库可编辑 catalog。暂不采用，因为会带来同步、校验和文档漂移问题。

2. **后端统一生成 catalog，CLI/MCP/AI Context 复用后端或共享静态定义。**
   - 原因：后端可以附加项目可访问性诊断，OpenAPI 也能生成 TS 类型。
   - CLI 在线命令读取 `/api/capabilities`；MCP resource 在服务可用时读取后端，失败时给出诊断，不伪造成功。
   - AI Context 导出时由后端写入同一份 JSON，保证离线包与服务端能力一致。

3. **能力条目使用稳定 ID 和多 surface 映射。**
   - 每个 capability 包含 `id/category/title/summary/status/stability`、`requiresProject`、`writeRisk`、`inputs`、`outputs`、`apiEndpoints`、`cliCommands`、`mcpResources`、`mcpTools`、`frontendRoutes`、`contractIds`、`workflowIds`、`profileIds`、`examples`、`preflightChecks`、`nextActions` 和 `docsRef`。
   - 允许新增可选字段，但删除或改名需要更新契约测试。

4. **项目级诊断只做只读提示。**
   - 传入 `projectId` 时 catalog 可返回 `projectAvailable`、`securityMode`、`recommendedFirstActions` 和 warning，例如“先运行 doctor”“当前能力需要项目 ID”“需要只读数据库账号”。
   - 第一版不在 catalog 中查询大量业务统计，避免让能力发现接口变慢。

5. **CLI/MCP 命名保持直观。**
   - CLI 新增 `capability list`、`capability show <id>`、`capability check`。
   - MCP 新增 `capability-catalog` resource，URI 支持全局和项目维度。
   - 命令只读取 catalog，不执行 capability 本身。

## Risks / Trade-offs

- [Risk] 代码内置 catalog 可能和真实 CLI/MCP 命令漂移。→ Mitigation：补 Node smoke/contract 测试扫描关键命令、resource/tool 名称和 catalog 条目。
- [Risk] 条目太多导致 AI 读不完。→ Mitigation：catalog 支持 list/show；每条能力保持摘要式字段，详细契约通过 `contractIds/docsRef` 跳转。
- [Risk] 项目级诊断被误认为权限授权。→ Mitigation：字段命名为 diagnostics/recommendedFirstActions，文档明确不替代鉴权。
- [Risk] 后端服务不可用时 CLI/MCP 读不到在线 catalog。→ Mitigation：doctor 仍是服务诊断入口；AI Context 离线包包含最近导出的 capabilities。

## Migration Plan

- 不新增数据库迁移。
- 部署后新增只读 API、CLI/MCP resource/command 和 AI Context 文件；旧客户端不受影响。
- 回滚代码时已导出的 `.dataspec/capabilities.json` 只是离线说明文件，不影响服务端状态。

## Open Questions

- 第一版前端是否需要展示 catalog 页面：本 change 暂不做，只在 README 和 AI Context 中引导；若后续需要可放到 P6-105 AI 一页式工作台或独立前端任务。
