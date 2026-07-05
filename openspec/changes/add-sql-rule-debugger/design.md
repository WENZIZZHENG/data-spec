## Context

SQL lint 现在已经具备规则配置、source range、fixed SQL、suppression 和 AI 可读错误码基础，但现有 `/api/lint` 返回的是最终问题列表，缺少规则级解释。P6-70 需要让用户和 AI 看见每条规则的启用状态、参数快照、命中理由、未命中说明、修复策略与豁免状态，同时保持原有 lint 结果兼容。

本次变更跨后端 API、CLI、前端和 AI 能力清单，但不涉及数据库迁移、凭据、权限或写入语义。调试入口必须是只读能力，不保存 SQL 检查记录，不改变现有 `/api/lint`、CLI `lint` 和 MCP `lint_sql` 的响应。

## Goals / Non-Goals

**Goals:**
- 新增 `/api/lint/debug`，返回规则级 debug traces，字段有清晰语义说明，便于人和 AI 排障。
- 复用现有 lint 引擎的 SQL 解析、规则配置、severity override、source range、fixed SQL policy 和 suppression 判断，避免调试结果与真实 lint 结果分叉。
- 新增 CLI `lint-debug`，以稳定 JSON 输出支持本地 AI agent 和 CI 排障。
- 在 SQL 校验页新增调试面板，用户能从当前 SQL 查看规则命中/未命中原因、参数快照、source range、修复策略和豁免建议。
- 扩展 AI 能力清单，让 AI 能发现该只读调试入口和适用场景。

**Non-Goals:**
- 不提供完整 AST 编辑器或 AST 树浏览器。
- 不要求第一版所有规则都有深度内部 trace；未提供深度 trace 的规则可以返回基于 issue、配置和执行状态生成的通用 trace。
- 不改变 `/api/lint` 默认响应、SQL 检查记录持久化格式、MCP `lint_sql` 契约或已有 CLI `lint` 退出码。
- 不新增写入、批量 apply、规则参数在线编辑或豁免创建接口。

## Decisions

### 1. 使用独立只读 `/api/lint/debug`，不扩展 `/api/lint`

选择独立 endpoint，避免现有 `/api/lint` 响应膨胀，也避免 CLI/MCP/前端已有调用方误依赖调试字段。`/api/lint/debug` 复用现有 request 字段（`sql`、`projectId`、`profileId`、`taskType`、`fixPolicy`），返回专门的 `SqlLintDebugResult`。

备选方案是在 `/api/lint` 增加 `debug=true`，但这会让主 lint controller 承担两类响应形态，且更容易破坏历史兼容。独立 endpoint 的发现成本通过 CLI、前端按钮和 AI capability catalog 补足。

### 2. 由 lint service 统一编排规则 trace

调试结果第一版由 `SqlLintService` 在规则执行循环中统一收集：规则是否启用、配置参数快照、severity 来源、执行异常、命中 issue、source range、fix plan 和 suppression 状态。这样能保证 trace 与实际 lint 结果来自同一次规则执行。

备选方案是让每条 `LintRule` 实现独立 debug SPI。该方案可提供更深解释，但会要求批量改所有规则，第一版落地成本高且容易引入不一致。当前先提供通用 trace，未来可在不破坏响应字段的前提下为重点规则补充更细 `debugNotes`。

### 3. 调试字段稳定、机器可读、可为空但不省略核心语义

每条规则 trace 至少包含 `ruleCode`、`ruleName`、`enabled`、`severity`、`paramsSnapshot`、`matchTrace`、`sourceRange`、`fixStrategy`、`suppressionStatus` 和 `debugNotes`。字段级语义通过 DTO 注释、前端类型和 OpenAPI/schema 类型说明保持一致。

`matchTrace` 使用小对象列表，而不是拼接字符串；`debugNotes` 承载人类可读补充。这样 AI 可以按结构化字段修规则、补标准或建议豁免，同时用户仍能快速阅读。

### 4. CLI 优先提供 JSON 调试输出

`lint-debug` 支持文件或 stdin 输入，调用 `/api/lint/debug`，默认或 `--format json` 输出完整 JSON。可保留简短 text 输出用于人工查看，但稳定契约是 JSON，退出码沿用只读诊断命令：请求成功为 0，参数或请求失败为 2，不因 lint error issue 返回 1。

常规 `lint` 保持原有“发现 error issue 则 exit 1”的 CI 语义；debug 命令用于解释而非质量门禁。

### 5. 前端采用可扫描面板，不做重型编辑器

SQL 校验页新增调试按钮和面板，面板以规则列表 + 当前规则详情呈现，展示启用状态、命中数量、参数快照、source range 和建议动作。交互使用语义化 button，加载/错误状态靠近按钮和面板展示，移动端保持纵向堆叠。

该设计符合当前页面工具属性：信息密度高、可扫描、低装饰。避免新增大型可视化或嵌套卡片。

## Risks / Trade-offs

- 调试接口会再次执行 lint，长 SQL 可能有额外开销 → 仅在用户或 CLI 显式请求 debug 时运行，不绑定普通 lint 提交。
- 通用 trace 对复杂规则的解释深度有限 → 通过 `debugNotes` 标明“基于最终 issue 生成”，后续可为重点规则逐步补深度 trace。
- 参数快照可能包含规则配置中的敏感字符串 → 输出前复用现有脱敏 helper 或仅返回规则参数对象中的非凭据配置，避免 password/token/secret/Authorization/JDBC URL 原样暴露。
- debug 输出字段较多，前端可能显得拥挤 → 默认展示摘要列表，详情按选中规则展开；JSON 参数和 notes 用可滚动代码块展示。

## Migration Plan

- 新增 endpoint、DTO、CLI 命令和前端面板后，不迁移历史数据。
- 若发布后需要回滚，只需隐藏前端入口并移除 CLI 调用；既有 `/api/lint` 和历史记录不受影响。
- OpenSpec change 完成后保留为 open change，除非用户后续要求 archive。
