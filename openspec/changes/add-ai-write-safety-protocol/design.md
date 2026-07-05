## Context

DataSpec 当前已有只读 AI capability catalog、CLI/MCP capability 读取、`writeRisk`、`preflightChecks`、`nextActions`、API Token、敏感信息脱敏和单机幂等写保护。缺口在于：这些信息分散在不同字段和文档里，AI 无法用同一协议判断写入风险、dry-run 要求、幂等 key 要求、可撤销证据和敏感输入边界。

本变更触及 AI 外部协议、CLI/MCP 输出契约和写入安全边界，按 SDD full 执行。实现必须保持向后兼容：新增字段是兼容新增，既有 `writeRisk`、`preflightChecks`、`nextActions`、HTTP 状态语义和 CLI/MCP 成功输出形状不得被删除或改名。

## Goals / Non-Goals

**Goals:**

- 为 AI/CLI/MCP/API/前端共享一个稳定 `safety` metadata 对象。
- 让 AI 可以先读取 capability catalog 或 MCP tools/list，判断 `readOnly`、`writesProject`、`requiresDryRun`、`supportsUndo`、`requiresIdempotencyKey`、`sensitiveInputs` 和 `nextActions`。
- 对声明为需要幂等 key 或 dry-run 的高风险写入，返回结构化、可恢复、已脱敏的错误诊断。
- 让 CLI `capability check` 和 MCP tool metadata 能检查协议存在性，避免 catalog 漂移。
- 让前端批量写入确认前复用同一字段展示 dry-run 摘要和风险提示。

**Non-Goals:**

- 不引入组织审批、多人审核、复杂 RBAC、外部队列或分布式锁。
- 不阻断单条低风险个人 CRUD，也不把所有记录型写入强制升级为高风险。
- 不改变现有 API Token 鉴权模型，不暴露或持久化 raw secret。
- 不移除旧 AI 契约字段；旧客户端无需迁移即可继续读取。

## Decisions

### Decision 1: 兼容新增 `safety` 对象，不替换旧字段

`AiCapabilityEntry` 新增 `safety` 字段，旧的 `writeRisk`、`preflightChecks`、`nextActions` 保留。`safety` 是 AI 优先读取的新协议对象，包含：

- `readOnly`: 能力是否不写 DataSpec 状态、不写外部系统。
- `writesProject`: 是否会写入当前 DataSpec project 范围的记录或标准资产。
- `requiresDryRun`: apply/confirm 前是否必须先产生 dry-run/preview/plan 摘要。
- `supportsUndo`: 是否存在明确回滚、跳过、恢复或 evidence 证据路径。
- `requiresIdempotencyKey`: 写入调用是否必须带 `Idempotency-Key` 或等价参数。
- `sensitiveInputs`: 可能包含敏感内容的输入类别或参数名，只列类别，不返回 raw value。
- `nextActions`: 面向 AI 的安全下一步，复用或收敛已有 nextActions。

替代方案是只扩展 `writeRisk` 枚举或把约束塞进 `preflightChecks` 文案。该方案解析成本高，AI 不容易稳定判断布尔条件，因此不采用。

### Decision 2: 风险策略由 capability catalog 定义，执行层做必要硬校验

能力清单提供统一声明，执行层只对声明为强约束的操作硬校验：缺少 `Idempotency-Key`、缺少 dry-run/apply 证据或传入敏感明文时返回结构化 DataSpecError。这样能先覆盖高风险批量写入和已有幂等保护链路，同时避免把低风险或只读命令误阻断。

替代方案是在 CLI/MCP 本地维护一份独立风险表。该方案容易与后端 catalog 漂移，因此 CLI/MCP 只校验 catalog 返回的协议形状，调用执行仍以服务端安全校验为准。

### Decision 3: dry-run 是协议证据，不是新审批流

对已有 preview/dry-run 能力，例如标准复用包 apply preview、项目恢复 dry-run、反向导入 preview/compare、标准合并 preview，前端和 AI 协议读取同一摘要字段并在 confirm/apply 前展示或校验。第一版不新增审批状态表，只要求 apply 前可展示摘要、风险和下一步。

替代方案是新增通用 `dry_run_session` 持久表。当前项目优先个人/小团队快速闭环，成本较高且会扩大迁移范围，因此不采用。

### Decision 4: 错误诊断必须可机器恢复且默认脱敏

缺少安全参数时返回稳定诊断，例如 `AI_WRITE_SAFETY_REQUIRED`、`IDEMPOTENCY_KEY_REQUIRED`、`DRY_RUN_REQUIRED`，字段包括 `category=SAFETY`、`retryable`、`missing`、`capabilityId`、`operation`、`safety`、`nextActions` 和 `docsRef`。诊断不得包含 token/password/Authorization/JDBC URL/DSN/raw payload。

替代方案是继续只返回中文错误消息。该方案无法让 AI 自动恢复或选择下一步，因此不采用。

## Risks / Trade-offs

- [Risk] 新增 `safety` 字段可能与旧 `writeRisk` 文案表达不一致 → 通过后端单测、CLI `capability check`、MCP resource/tool 测试和 AI contract fixture 校验一致性。
- [Risk] 过度要求幂等 key 会破坏现有个人快捷写入 → 第一版只对 catalog 标记为 `requiresIdempotencyKey=true` 的高风险批量/apply 类写入硬校验；单条低风险 CRUD 不阻断。
- [Risk] dry-run 证据在不同业务模块中的字段名不同 → 前端和 AI 协议只要求摘要语义，不强制统一业务 DTO；必要时使用轻量 display helper 做展示适配。
- [Risk] MCP tools/list 的自定义 metadata 可能被部分客户端忽略 → 同时在 capability catalog resource 和工具调用错误中返回 `safety`，客户端可从任一入口读取。
- [Risk] 敏感输入类别不等于完整 PII 检测 → 第一版只覆盖技术 secret 和已知敏感参数名，不承诺识别所有自然语言隐私片段；后续由个人安全红线策略继续收紧。

## Migration Plan

- 新增后端模型和兼容字段，无数据库迁移。
- 后端 capability catalog 增量输出 `safety`；旧字段继续输出。
- CLI/MCP 更新检查和展示逻辑；旧命令参数继续可用。
- 高风险写入缺少安全参数时返回结构化错误；已有带 key 或 preview/apply 正常路径不变。
- 回滚时删除新增 `safety` 字段和校验逻辑即可；旧字段仍保留，不需要数据迁移。

## Verification Strategy

- 后端：补 `AiCapabilityCatalogServiceImplTest`、`WriteGuardServiceTest` 或相关 controller/service 测试，覆盖 safety 字段、缺少 idempotency key 的结构化诊断和脱敏。
- CLI/MCP：补 `tools/dataspec-cli.test.mjs`、`tools/dataspec-mcp.test.mjs`，覆盖 `capability check` 校验 `safety`、文本输出展示、MCP tools/list metadata 和安全错误透传。
- 前端：补最小 display/helper 或页面源码级测试，覆盖 dry-run 摘要展示字段。
- OpenSpec：运行 `openspec validate add-ai-write-safety-protocol --strict` 和 `openspec validate --all`。
- 收口：运行受影响模块测试、`git diff --check`、强制独立子 agent 评审，并在 `tasks.md` 记录 Verification Evidence。
