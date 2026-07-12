## Context

DataSpec 已经有字段 ID、字段名、别名、生命周期状态、replacementFieldId、标准快照、字段引用索引、Schema Registry、AI Context、AI 证据包、CLI/MCP 契约和 AI 任务记录。缺口不是“完全没有 ID”，而是这些 ID 没有成为 AI/CLI/MCP 可依赖的稳定引用契约，也没有统一的解析结果来说明旧字段名、别名、废弃字段或错误引用应该如何处理。

P6-165 与 P6-166 应合并实施：先有稳定引用和解析语义，后置校验才能判断 AI 输出中的字段、枚举、规则和快照引用是否存在、是否过期、是否需要替代字段。该变更影响 API、CLI、MCP、AI Context 和证据包，属于 SDD standard 偏 full；提交前必须经过独立子 agent 评审。

## Goals / Non-Goals

**Goals:**

- 为字段、枚举、规则和标准快照输出稳定引用摘要，第一版优先字段和枚举。
- 提供项目内引用解析能力，把 stableRef、当前名、别名、历史名和自然文本候选解析为结构化结果。
- 提供 AI 输出后置校验能力，对 SQL/DDL/Markdown/JSON 中可确定的标准引用输出 PASS/WARN/FAIL、问题、替代建议和 evidence links。
- 将 stable refs 和 post-check 摘要 additive 接入 AI Context、Schema Registry、Evidence、CLI/MCP fixtures。
- 保持无数据库迁移的第一版实现，必要的历史别名从现有字段名、aliases、change log、replacementFieldId 和标准快照摘要派生。

**Non-Goals:**

- 不自动改写业务仓库、历史 SQL 检查记录或 AI 输出正文。
- 不判断所有自然语言事实，只校验 DataSpec 能确定的字段、枚举、规则、快照和证据引用。
- 不新增组织级全局 ID 服务，不保证跨项目合并后的 ID 永不变化；跨项目复用由 standard reuse pack 后续增强。
- 不引入外部 LLM、向量数据库或在线安全服务。

## Decisions

1. **stableRef 先采用 deterministic URI-like 字符串，不新增表列。**
   - 选择：字段使用 `field:<projectId>:<fieldId>`，枚举使用 `enum:<projectId>:<codeSetId>`，规则使用 `rule:<projectId>:<ruleCode>`，快照使用 `snapshot:<projectId>:<snapshotId|specVersion>`；同时输出 `canonicalRef`。
   - 原因：现有 projectId/objectId 已具备项目内稳定性；additive 输出不需要迁移，也不会破坏旧 API。
   - 替代方案：新增 UUID/ULID 列。暂不采用，因为会触发迁移、导入兼容和 UI 填充问题，超出第一版必要范围。

2. **aliasHistory 第一版为派生摘要，而不是完整历史事实表。**
   - 选择：从当前 `name`、`displayName`、`aliases`、`replacementFieldId`、字段变更日志可见摘要和 snapshot 字段名中派生 `aliasHistory` / `deprecatedRefs`。
   - 原因：足以解决 AI 读取 Context、证据包和后置校验时的引用漂移；不强行补历史数据。
   - 替代方案：新增 alias history 表并回填。暂不采用，后续如果业务仓库迁移交付包需要严格审计再开独立变更。

3. **引用解析 service 是后置校验的底座。**
   - 选择：新增 `StandardReferenceResolutionService`，输入 projectId、refType、refs/text，输出 `ReferenceResolutionResult`，包含 `inputRef`、`refType`、`resolutionStatus`、`stableRef`、`canonicalRef`、`objectId`、`currentName`、`matchedAlias`、`lifecycleStatus`、`replacementRef`、`confidence`、`evidenceLinks` 和 `warnings`。
   - 原因：API、CLI/MCP、AI Context 和 post-check 共用同一语义，避免各处重复判断“字段是否存在”。
   - 替代方案：只在 post-check 内部解析。放弃，因为 P6-165 需要独立可用的稳定引用解析能力。

4. **AI 输出后置校验只做确定性检查。**
   - 选择：SQL/DDL 优先复用现有 SQL parser/lint 的 identifier 提取能力；Markdown/JSON 用 bounded pattern 提取 `field:<...>`、反引号字段、枚举/规则/快照引用和 DataSpec evidence refs，再调用 resolver。
   - 原因：可测试、可解释、无外部依赖；误报可以用 severity 和 confidence 表达。
   - 替代方案：调用 LLM 判断输出是否引用正确。放弃，因为不可重复，也会扩大安全边界。

5. **结果契约按 evidence-first 设计。**
   - 选择：post-check 输出 `kind`、`schemaVersion`、`status`、`summary`、`issues[]`、`resolvedRefs[]`、`suggestedFixes[]`、`evidenceLinks[]`、`nextActions[]` 和 `safeToUse`；证据包仅存摘要和 issue refs，不保存 raw secret。
   - 原因：AI agent 需要可机读门禁决定继续、追问、修复或停止。
   - 替代方案：只返回布尔 pass/fail。放弃，因为用户需要知道替代字段、旧快照和证据缺口。

## Risks / Trade-offs

- **[Risk] 派生 stableRef 与未来跨项目复用冲突。** → Mitigation：第一版明确 project-scoped；standard reuse pack 可输出 source stableRef 和 local stableRef 的映射。
- **[Risk] 文本提取误报自然语言词。** → Mitigation：按来源分类 confidence，SQL/DDL identifier 高置信，自然语言反引号/显式 ref 中置信，自由文本低置信并给 WARN。
- **[Risk] 旧字段名历史不足。** → Mitigation：aliasHistory 标记来源和置信度；无法确认时返回 `UNKNOWN` 或 `AMBIGUOUS`，不伪装成确定命中。
- **[Risk] 新 CLI/MCP 契约漂移。** → Mitigation：同步更新 `cli-mcp-contract-fixtures`，增加 fixture check。
- **[Risk] 输出文本含 secret。** → Mitigation：所有 excerpt、diagnostic、raw AI output 摘要和 issue snippet 复用 `SensitiveDataSanitizer`；测试覆盖 token/password/JDBC/DSN。

## Migration Plan

- 第一版不做数据库迁移；stableRef 由现有字段、枚举、规则和快照记录派生。
- API/CLI/MCP 输出仅 additive 增加字段和新命令；旧客户端可忽略。
- 回滚时移除新增 API/CLI/MCP 入口和 additive 字段即可；不会改变持久化数据。
- 实现完成后保留 active change，不自动 archive；若用户要求 archive，再同步主规格。

## Open Questions

- 是否在第一版前端新增独立页面，还是先在 AI 回放/AI Context 导出页展示 post-check 摘要？默认选择后者，保持最小闭环。
- 历史字段名优先从 change log 还是 snapshot payload 派生？实现时以更容易测试且不新增迁移的来源为准。
