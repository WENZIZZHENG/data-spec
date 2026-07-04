## Context

DataSpec 已有 `dataspec doctor`、AI Capability Catalog、AI Context、MCP resources/tools、标准快照和 API Token。P6-61 缺的是“新会话第一跳”：AI 需要一份机器可读包一次性判断当前项目是否能继续 lint、导出 Context、反向导入或生成 DDL，并知道失败时的 nextActions。

该能力横跨后端 API、CLI、MCP 和 OpenAPI 契约，因此按 A 档流程处理；第一版保持只读，不新增持久化表，也不调用外部 LLM。

## Goals / Non-Goals

**Goals:**
- 输出稳定 JSON，包含 `projectId`、`server`、`authMode`、`specVersion`、`availableCapabilities`、`recommendedCommands`、`knownRisks`、`docsRefs` 和 `nextActions`。
- 复用能力清单与标准快照，避免启动包和 catalog 互相漂移。
- CLI/MCP 都能作为 AI 会话入口；CLI 在服务不可达时仍输出本地 fallback JSON。
- 不泄漏明文 token、Authorization header、数据库密码或完整 JDBC URL。

**Non-Goals:**
- 不自动执行 lint、导出 Context、反向导入、DDL 生成或任何写操作。
- 不新增企业审批、权限流或发布流。
- 不缓存启动包，不新增数据库迁移。
- 不做前端页面；本项优先服务 AI/CLI/MCP。

## Decisions

1. 后端新增 `/api/bootstrap/session`，而不是把字段追加到 doctor。
   - 原因：doctor 是 CLI 本地环境诊断，启动包是服务端项目上下文聚合；分开后 CLI 可以在服务不可达时 fallback。
   - 备选：只扩展 capability catalog。放弃原因是 catalog 描述“能做什么”，但不表达当前 project/token/specVersion 的 readiness。

2. 启动包复用 `AiCapabilityCatalogService` 获取能力入口。
   - 原因：能力 id、CLI/MCP/API surfaces 已在 catalog 中维护，启动包只筛选出 AI 常用入口和风险提示。
   - 取舍：启动包会依赖 catalog 的稳定字段；若 catalog 增加能力，启动包无需改数据库。

3. 标准版本读取 `StandardSnapshotService.getCurrentSnapshot(projectId)`。
   - 原因：当前快照已经表达 `specVersion/specHash/versioned/source`；无快照时返回 `unversioned` 并给出 nextAction。
   - 边界：不在启动包里生成或修复快照。

4. CLI `bootstrap` 默认 JSON，返回码使用 readiness。
   - `READY` 返回 `0`。
   - `DEGRADED` 或 `BLOCKED` 返回 `1`。
   - 参数错误仍返回 `2`。
   - 服务不可达 fallback 也用 JSON 输出，便于 AI 继续读 nextActions。

5. MCP 同时暴露 resource 和 tool。
   - resource：`dataspec://project/<id>/session-bootstrap`，适合会话开始读取。
   - tool：`get_session_bootstrap`，适合需要覆盖 projectId 时调用。

## Risks / Trade-offs

- [Risk] 启动包与 capability catalog 重复能力字段。→ Mitigation：只存能力摘要，来源以 catalog 为准。
- [Risk] 服务不可达时 CLI fallback 信息不完整。→ Mitigation：明确 `status=BLOCKED`，只输出本地 server/project/authMode 和 nextActions，不伪造远端 specVersion。
- [Risk] token 或数据库凭据被带入输出。→ Mitigation：只输出 `authMode` 和脱敏 server，不输出 token 值、Authorization header、密码或 JDBC URL。
- [Risk] 启动包字段未来扩展导致前端类型漂移。→ Mitigation：提交 OpenAPI 生成产物，并用 `pnpm check:api` 校验。

## Migration Plan

- 无数据库迁移。
- 后端 API 为新增只读端点，现有调用方不受影响。
- CLI/MCP 新增入口为 additive change，旧命令与旧 resource/tool 保持兼容。
