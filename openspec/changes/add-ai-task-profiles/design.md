## Context

当前 DataSpec 已有 `.dataspec/config.json`、AI Context scoped export、workflow recipes、fixedSql policy、CLI/MCP 和 `dataspec doctor`。这些能力分散可用，但 AI agent 在执行“建表”“修 SQL”“反向导入”“PR review”时仍需要靠 prompt 临时组合参数，容易读过多上下文、拿错 fixedSql 策略或输出不稳定格式。

P6-36 第一版以个人/小团队使用为目标：提供一组内置、可机器读取的 AI task profiles，并允许本地配置或请求参数选择 profile。为了避免把它做成企业治理配置，本轮不新增数据库迁移，不做权限模型，也不持久保存外部 LLM provider。

## Goals / Non-Goals

**Goals:**

- 提供稳定 AI profile 数据结构，覆盖 taskType、contextScope、ruleset、fixedSqlPolicy、outputFormat、maxContextFields 和推荐命令。
- 后端 API 能返回默认 profile 列表、单个 profile 和诊断结果。
- CLI/MCP 能读取 profile，并让 AI 在调用 lint/export-context 前拿到默认参数。
- `dataspec doctor` 能诊断 `.dataspec/config.json` 中 profile/taskType 是否可用。
- 前端能查看和切换当前任务模式，至少支持复制推荐命令或跳转相关页面。

**Non-Goals:**

- 不新增 `ds_ai_profile` 表，不做项目级复杂持久配置；后续如需要可由标准契约版本或设置页面承接。
- 不保存外部模型、provider、API key 或 prompt secret。
- 不把 profile 作为后端强制授权边界；真实写入安全仍由现有 API、token、dry-run 和 fixedSql policy 负责。
- 不要求所有 CLI/MCP 工具一次性完全自动套用 profile；第一版优先暴露、诊断和高频参数默认值。

## Decisions

1. **内置 profile registry，按项目上下文补诊断。**

   新增后端 `AiTaskProfileRegistry`，维护 `create-table`、`sql-fix`、`reverse-import`、`pr-review`、`minimal-context` 等内置 profile。API 根据 `projectId` 返回 profile 列表和诊断，诊断只读取现有字段/规则/分组等安全元数据，不持久写入。

2. **profile 是建议契约，不是强制执行策略。**

   profile 中的 `contextScope`、`fixedSqlPolicy`、`outputFormat`、`recommendedCommands` 供 AI/CLI/前端默认采用；调用方仍可显式覆盖。这样保留个人快速使用的弹性，也避免误以为配置能替代人工确认。

3. **`.dataspec/config.json` 只保存选择，不复制完整 profile。**

   本地配置新增 `aiProfile` 和 `taskType` 字段，表示默认选择。完整 profile 由 DataSpec 服务端或 CLI 内置 fallback 提供，避免业务仓库里的配置随服务端演进漂移。

4. **CLI/MCP 通过同一 profile schema 输出。**

   CLI 新增 `profile list/show` 或等价命令，`doctor` 读取本地配置并校验 profile 是否存在；MCP 新增 profile resource/prompt，让 agent 在执行任务前先读取当前模式。Node 测试负责锁定 JSON 字段。

5. **前端先做“任务模式查看/切换”，不做复杂编辑器。**

   前端使用 Element Plus 表格/详情展示 profile 和诊断，当前选择可存在页面状态或 localStorage；后续如需要自定义 profile，再进入单独待办。

## Risks / Trade-offs

- [Risk] 内置 profile 不够贴合每个项目。→ Mitigation：允许 CLI/API 显式覆盖参数，第一版只提供默认建议和诊断。
- [Risk] 不持久化项目 profile 会让前端切换只在本地生效。→ Mitigation：`.dataspec/config.json` 可固定业务仓库默认选择，前端本地状态服务个人使用。
- [Risk] profile 字段和 AI Context/fixedSql 策略漂移。→ Mitigation：新增 OpenAPI/TS schema、CLI/MCP contract test 和前端 smoke。
- [Risk] AI 误把 profile 当写入授权。→ Mitigation：文档和 nextActions 明确 profile 只是建议契约，高风险写入仍需 dry-run/人工确认。
