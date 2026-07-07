# AI 输出契约

本文件记录 DataSpec 第一版 AI 可消费稳定字段。它服务于 CLI、MCP、MCP agent guidance pack、CLI/MCP contract fixtures、AI Context、AI capability catalog、版本兼容握手、AI task profiles、SQL lint、AI evidence package、字段推荐、字段检索、标准字段合并和 DDL 预览的自动化使用场景。

## 兼容策略

- 兼容：新增可选字段、新增说明文本、增加不影响原字段语义的附加 metadata。
- 需要同步更新契约测试：删除字段、字段改名、稳定枚举改名、字段类型变化、同名字段语义变化。
- 不作为稳定契约：时间戳具体值、Markdown 文案全文、数组中与业务数据相关的完整数量、内部 DTO 私有实现细节。

## Schema Registry

Schema Registry 是 DataSpec 对 AI 可消费输出结构的只读索引，不是权限、审批、发布流程或写入安全策略。AI、CLI、MCP 和离线 Context 包可以先读取它，再决定哪些字段名和版本可依赖。

稳定入口：

- API `GET /api/contracts`: 返回 registry catalog。
- API `GET /api/contracts/{contractId}`: 返回单个契约详情。
- CLI `contract list|show|check`: 读取 registry 并做轻量 invariants 检查。
- MCP `schema-registry` resource: URI 形如 `dataspec://project/{projectId}/schema-registry`。
- AI Context `.dataspec/schema-registry.json`: 离线包内的 registry catalog。

Registry catalog 稳定字段：

- `kind`: 固定为 `dataspec-schema-registry`。
- `schemaVersion`: registry 文档结构版本，当前为整数。
- `registryVersion`: 当前内置 registry 版本。
- `compatibilityPolicy`: 兼容策略，稳定包含 `level`、`compatibleSince`、`additiveFieldPolicy`、`breakingChangePolicy`、`deprecationPolicy`、`compatibilityWindow`。
- `contracts[]`: contract summary 列表。
- `requiredContractIds[]`: 当前 DataSpec 认为 AI 高频入口必须存在的 contract id。
- `nextActions[]`: AI 读取 registry 后可执行的下一步建议。

Contract summary 稳定字段：

- `contractId`
- `displayName`
- `description`
- `schemaVersion`
- `jsonSchemaRef`
- `stableFields[]`
- `deprecatedFields[]`
- `compatibility`
- `docsRef`

Contract detail 在 summary 基础上额外稳定提供 `jsonSchema` 和 `examples[]`。第一版核心 contract id 至少包含 `field`、`standard-field-merge`、`enum-dict`、`rule-config`、`template`、`standard-snapshot`、`lint-result`、`ai-evidence-package`、`ai-context-manifest`、`ai-context-field-catalog` 和 `ai-task-profile`。

`contract check` 的成功只说明 registry 结构对 AI 可用；它不会验证当前项目业务标准是否完整，也不会授权任何写入动作。

## AI Capability Catalog

Capability catalog 是 DataSpec 面向 AI agent 的只读能力目录，不是任务编排、鉴权、审批或 dry-run 机制。AI 可以先读取它，再决定使用 doctor、Context、lint、字段检索、DDL、反向导入或证据包等具体入口。

稳定入口：

- API `GET /api/capabilities`: 返回能力清单，可带 `projectId` 获取项目级诊断。
- API `GET /api/capabilities/{id}`: 返回单个能力条目。
- API `GET /api/capabilities/version`: 返回只读版本兼容握手。
- CLI `capability list|show|check`: 读取 catalog 并做轻量 invariants 检查。
- CLI `compat check`: 读取版本兼容握手并输出本地 CLI 版本、服务端版本和兼容状态。
- MCP `capability-catalog` resource: URI 形如 `dataspec://project/{projectId}/capability-catalog`，也支持全局只读 `dataspec://capability-catalog`。
- MCP `version-compatibility` resource: URI 为 `dataspec://version-compatibility`，返回同一份兼容握手。
- AI Context `.dataspec/capabilities.json`: 离线包内的 capability catalog。

Catalog 稳定字段：

- `kind`: 固定为 `dataspec-ai-capability-catalog`。
- `schemaVersion`: catalog 文档结构版本，当前为整数。
- `catalogVersion`: 当前内置 catalog 版本。
- `generatedAt`
- `projectId`
- `capabilities[]`
- `requiredCapabilityIds[]`
- `recommendedFirstActions[]`
- `diagnostics[]`

Capability entry 稳定字段：

- `id`
- `category`
- `title`
- `summary`
- `status`
- `stability`
- `requiresProject`
- `writeRisk`
- `requiredInputs[]`
- `optionalInputs[]`
- `outputContracts[]`
- `apiEndpoints[]`
- `cliCommands[]`
- `mcpResources[]`
- `mcpTools[]`
- `frontendRoutes[]`
- `contractIds[]`
- `workflowIds[]`
- `profileIds[]`
- `examples[]`
- `preflightChecks[]`
- `nextActions[]`
- `safety`
- `docsRef`

`safety` 是 AI 写入安全 metadata，稳定字段包括 `readOnly`、`writesProject`、`requiresDryRun`、`supportsUndo`、`requiresIdempotencyKey`、`sensitiveInputs[]` 和 `nextActions[]`。`writeRisk` 继续作为旧客户端可读的摘要风险字段保留；新客户端应优先读取 `safety` 判断是否需要 dry-run、幂等 key、证据导出或人工确认。

`capability check` 的成功只说明 catalog 结构、核心能力条目和必需 `safety` 字段存在；它不会执行这些能力，也不会验证当前项目标准质量。

## 版本兼容握手

版本兼容握手是只读 preflight 契约，用于 AI、CLI 和 MCP 判断当前客户端是否能安全消费服务端能力。它不读取业务项目数据、不连接源数据库、不写入 DataSpec 状态，也不得返回 token、password、Authorization、JDBC URL、DSN 或 source row。

稳定入口：

- API `GET /api/capabilities/version?client=cli&clientVersion=0.1.0`
- CLI `compat check --format json`
- CLI `doctor --format json` 的 `compatibility` check
- MCP resource `dataspec://version-compatibility`

稳定字段：

- `kind`: 固定为 `dataspec-version-compatibility`。
- `schemaVersion`: 握手响应结构版本。
- `serverVersion`: 当前服务端版本。
- `apiSchemaHash`: 当前公开 API/AI capability 契约摘要 hash。
- `minCliVersion`: 服务端推荐的最小 CLI 版本。
- `supportedCapabilities[]`: 稳定包含 `id`、`status` 和 `minClientVersion`。
- `deprecatedFields[]`: 稳定包含 `contractId`、`field`、`deprecatedSince`、`replacement`、`removeAfter` 和 `note`。
- `compatibility`: 稳定包含 `status`、`clientVersion`、`compatible`、`reasons[]` 和 `nextActions[]`；`status` 稳定值为 `COMPATIBLE`、`INCOMPATIBLE` 或 `UNKNOWN`。
- `upgradeHints[]`
- `generatedAt`

CLI `compat check` 兼容时退出码为 `0`，服务端报告 `compatibility.compatible=false` 时退出码为 `1`，服务不可达或响应错误时退出码为 `2` 并输出脱敏 JSON 诊断。MCP resource 失败时通过 JSON-RPC `error.data.dataspecError` 返回 `VERSION_COMPATIBILITY_UNAVAILABLE`。

### field

标准字段契约，覆盖字段库和 AI Context 中的字段基础元数据，例如 `name`、`dataType`、`nullable`、`comment`、`status`、`replacementFieldId`、`replacementReason`、`aliases[]` 和 `matchReasons[]`。`status` 稳定值至少包含 `draft`、`enabled`、`deprecated` 和 `disabled`。

### standard-field-merge

标准字段合并契约，覆盖正式字段 merge preview 和 apply result。preview 稳定字段包括 `kind`、`schemaVersion`、`projectId`、`recommendedTargetFieldId`、`target`、`source`、`targetAfter`、`sourceAfter`、`changes[]`、`risks[]`、`impactItems[]`、`rollbackHints[]` 和 `nextActions[]`；result 稳定字段额外包括 `applied` 和 `preview`。`risks[].blocking=true` 时 AI 和前端不得调用 apply；`rollbackHints[].targetPath` 只给出回退定位方式，仍需用户选择具体变更日志。

### explain-trace

轻量 AI 输出引用证据契约，覆盖 `sourceType`、`sourceId`、`snapshotVersion`、`matchReason`、`confidence`、`ruleCode` 和 `docsRef`。第一版用于字段推荐、字段检索和自然语言需求草案，不是分布式 tracing、审批依据或写入授权。

### enum-dict

枚举字典契约，覆盖代码集、枚举值、展示标签和排序信息。

### rule-config

规则配置契约，覆盖 SQL lint 规则 code、名称、严重级别、启用状态和参数 JSON。

### template

表模板契约，覆盖 DDL 生成所需的模板元数据和模板字段。

### standard-snapshot

标准快照契约，覆盖 `specVersion`、`specHash`、来源和是否已版本化。

### lint-result

SQL 校验结果契约，覆盖问题列表、统计、fixedSql、diff、修复计划和方言诊断。

### sql-rule-debug-result

SQL 规则调试结果契约，覆盖 lint 结果、规则启用状态、匹配 trace、source range、修复策略、豁免状态和调试说明。该契约用于解释规则为什么命中、未命中或未执行，不代表写入 SQL 检查记录。

### ai-evidence-package

AI 执行证据包契约，覆盖 source、标准快照、输入摘要、输出摘要、验证摘要、产物、下一步动作和推荐命令。它是只读交付结构，不是审计、审批、权限或防篡改机制。

### ai-context-manifest

AI Context manifest 契约，覆盖离线包入口、标准版本、文件清单、命令和 registry 摘要。

### ai-context-field-catalog

AI Context 字段目录契约，覆盖字段、枚举、上下文裁剪条件和标准版本。

### ai-task-profile

AI 任务模式契约，覆盖 profileId、taskType、上下文范围、规则集、fixedSql 策略、输出格式和推荐命令。

## AI Task Profiles

稳定字段：

- `AiTaskProfileCatalog`: `projectId`、`defaultProfileId`、`selectedProfileId`、`profiles[]`、`diagnostics[]`、`supportedTaskTypes[]`。
- `AiTaskProfile`: `profileId`、`taskType`、`displayName`、`description`、`contextScope`、`ruleset`、`fixedSqlPolicy`、`outputFormat`、`maxContextFields`、`recommendedCommands[]`、`nextActions[]`、`defaultProfile`。
- `AiTaskContextScope`: `scope`、`query`、`status`、`limit`。
- `AiTaskRuleset`: `strictness`、`requiredRuleCodes[]`、`optionalRuleCodes[]`。
- `AiTaskOutputFormat`: `format`、`schemaRef`、`includeEvidence`、`includeNextActions`。
- `AiProfileDiagnostic`: `code`、`status`、`message`、`nextAction`。
- `AiTaskProfileDetail`: `projectId`、`requestedProfile`、`profile`、`diagnostics[]`、`supportedProfileIds[]`、`supportedTaskTypes[]`。

稳定内置 profile id 至少包含 `create-table`、`sql-fix`、`reverse-import`、`pr-review`、`minimal-context`；稳定 taskType 至少包含 `CREATE_TABLE`、`SQL_FIX`、`REVERSE_IMPORT`、`PR_REVIEW`、`MINIMAL_CONTEXT`。新增 profile 或 taskType 是兼容变更，删除或改名需要同步契约测试。

Profile 是任务默认建议，不是权限或 provider 配置。AI 可以用它决定默认 context scope、fixedSql policy、输出格式和推荐命令；调用方显式传入 `fixPolicy`、`scope/query/status/limit` 或工具参数时，显式参数优先。

## AI Context

稳定字段：

- `.dataspec/manifest.json`: `kind`、`schemaVersion`、`projectId`、`standard.specVersion`、`standard.specHash`、`generatedAt`、`files[]`、`contextScope.profileId`、`contextScope.taskType`、`contracts.schemaVersion`、`contracts.registryVersion`、`contracts.file`、`contracts.contractIds[]`、`commands.lint`、`commands.exportContext`、`commands.workflowList`、`commands.contractList`、`commands.capabilityList`。
- `.dataspec/schema-registry.json`: Schema Registry catalog，稳定字段遵循上方 Schema Registry 契约。
- `.dataspec/capabilities.json`: AI Capability Catalog，稳定字段遵循上方 Capability Catalog 契约。
- `.dataspec/field-catalog.json`: `projectId`、`standard.specVersion`、`standard.specHash`、`contextScope`、`fields[]`、`enums[]`。
- `fields[]`: `name`、`dataType`、`nullable`、`sensitive`、`status`、`replacementFieldId`、`replacementReason`、`comment`、`displayName`、`category`、`tags`、`codeSetId`、`example`、`aliases[]`、`matchReasons[]`。
- `.dataspec/rules.yaml`: `standard`、`naming`、`rules`、`rule_exemptions`。
- `.dataspec/workflows.md`: `create-table`、`review-pr-sql`、`reverse-import-standards`、`export-min-context`、`standard-evidence-review` 五个 recipe id。

## SQL Lint

稳定字段：

- API `POST /api/lint`: 返回 `LintResult` 并保存 SQL 检查记录。
- API `POST /api/lint/debug`: 返回 `SqlLintDebugResult`，只读解释规则执行过程，不保存 SQL 检查记录、不生成 AI replay、不写回业务仓库。
- `LintResult`: `tables`、`issues`、`errorCount`、`warningCount`、`suggestionCount`、`suppressedCount`、`fixedSql`、`fixedSqlDiff`、`fixPolicy`、`fixDryRun`、`fixChanges`、`fixExplanations`、`fixSummary`、`fixNextActions`。
- `LintRequest`: `sql`、`projectId`、`profileId`、`taskType`、`fixPolicy`。
- `FixPolicy`: `mode`、`maxRiskLevel`、`enabledRuleCodes`、`disabledRuleCodes`、`includeExplanations`。
- `FixChange`: `status`、`reasonCode`、`ruleCode`、`ruleName`、`riskLevel`、`changeType`、`tableName`、`columnName`、`before`、`after`、`explain`、`confidence`、`sourceStart`、`sourceEnd`。
- `FixPlanSummary`: `availableCount`、`appliedCount`、`plannedCount`、`skippedCount`。
- `LintIssue`: `severity`、`ruleCode`、`ruleName`、`message`、`tableName`、`columnName`、`suggestion`、`replacement`、`before`、`after`、`confidence`、`fixRiskLevel`、`fixChangeType`、`fixStatus`、`fixExplain`、`fixReasonCode`、`line`、`column`、`lineEnd`、`columnEnd`、`sourceStart`、`sourceEnd`、`locationKind`、`suppressed`、`suppressionId`、`suppressionReason`。
- `SqlLintDebugResult`: `debugVersion`、`lintResult`、`rules[]`、`debugNotes[]`。
- `SqlRuleDebugTrace`: `ruleCode`、`ruleName`、`enabled`、`severity`、`paramsSnapshot`、`matchTrace[]`、`sourceRange`、`fixStrategy`、`suppressionStatus`、`debugNotes[]`。
- `SqlRuleMatchTrace`: `status`、`message`、`severity`、`issueMessage`、`tableName`、`columnName`、`sourceRange`、`fixStatus`、`fixReasonCode`、`suppressionId`。
- `SqlRuleSourceRange`: `line`、`column`、`lineEnd`、`columnEnd`、`sourceStart`、`sourceEnd`、`locationKind`、`tableName`、`columnName`。
- `SqlRuleFixStrategy`: `fixPolicy`、`fixDryRun`、`fixSummary`、`changes[]`、`nextActions[]`。
- `SqlRuleSuppressionStatus`: `activeIssueCount`、`suppressedIssueCount`、`suppressionIds[]`、`suppressionReasons[]`、`summary`。
- 稳定枚举：`severity` 使用 `ERROR`、`WARNING`、`SUGGESTION`；`locationKind` 至少包含 `table`、`column`、`comment_column`；`FixPolicy.mode` 使用 `GENERATE`、`DRY_RUN`、`DISABLED`；`fixRiskLevel` 使用 `LOW`、`MEDIUM`、`HIGH`；`fixStatus` 使用 `APPLIED`、`PLANNED`、`SKIPPED`；`SqlRuleMatchStatus` 使用 `MATCHED`、`NO_MATCH`、`DISABLED`、`UNPARSED`、`ERROR`。

`fixedSql` 仍只是候选输出，不代表已经写回业务仓库；`fixPolicy.mode=DRY_RUN` 时 AI 必须把结果视为预览，并结合 `fixedSqlDiff`、`fixChanges` 和 `dialectDiagnostics` 人工确认后再继续。`fixPolicy.includeExplanations=false` 时 `fixExplanations` 可为空，AI 仍应以 `fixChanges.status/reasonCode` 判断跳过项。请求同时包含 `profileId/taskType` 和显式 `fixPolicy` 时，显式 `fixPolicy` 是有效策略来源。

`/api/lint/debug` 与 CLI `lint-debug` 使用同一份 `LintRequest`。调试结果中的 `paramsSnapshot` 仅用于说明规则运行参数，服务端会对 password、token、secret、Authorization、JDBC URL、DSN 等敏感值返回 `[REDACTED]`；调用方不得把该字段当作可还原配置源。

## AI Evidence Package

稳定入口：

- API `POST /api/evidence-packages`: 返回 JSON evidence package。
- API `POST /api/evidence-packages/download`: 返回 zip，固定包含 `evidence.json`、`summary.md` 和 `README.md`。
- CLI `evidence export`: 支持 `--format json|zip`；zip 必须显式 `--output`。
- MCP `export_evidence_package` tool: 返回 `structuredContent` 和可解析 JSON text。

稳定字段：

- `AiEvidencePackage`: `kind`、`schemaVersion`、`packageId`、`projectId`、`generatedAt`、`source`、`standardSnapshot`、`inputsSummary`、`outputsSummary`、`validationSummary`、`artifacts[]`、`nextActions[]`、`suggestedCommands[]`、`diagnostics[]`。
- `AiEvidenceSource`: `sourceType`、`sourceId`、`sourceTitle`、`status`、`persisted`。
- `AiEvidenceStandardSnapshot`: `snapshotId`、`specVersion`、`specHash`、`versioned`。
- `AiEvidenceArtifact`: `artifactType`、`title`、`format`、`summary`。
- `AiEvidenceDiagnostic`: `level`、`code`、`message`。
- `AiEvidencePackageReq`: `projectId`、`sourceType`、`sourceId`、`sourceTitle`、`coverageReport`、`standardSnapshot`、`payloadSummary`。

稳定来源类型：`AI_JOB`、`SQL_CHECK`、`COVERAGE_REPORT`、`AI_BATCH_RUN`。`COVERAGE_REPORT` 可以是 payload source，`persisted=false` 表示当前报告不是服务端长期记录。

证据包不得暴露 token、password、Authorization header、完整 JDBC URL 或业务数据行。AI 可以使用 evidence package 继续修复、复盘或生成交付说明，但不得把 evidence package 视为企业审计记录、审批结果或写入授权。

## 字段推荐

稳定字段：

- `recommendedName`
- `score`
- `matchReason`
- `existing`
- `field`
- `evidence[]`

`field` 命中已有标准字段时应保留标准字段基础元数据，例如 `name`、`displayName`、`dataType`、`aliases`、`category`、`sensitive`、`status`、`replacementFieldId`、`replacementReason`、`codeSetId` 和 `exampleValue`。默认推荐只返回 `enabled` 字段；`draft`、`deprecated` 和 `disabled` 字段应通过显式字段检索查看。`evidence[]` 使用 `ExplainTrace`，已有字段命中时 `sourceType=FIELD`，fallback 建议可使用无 `sourceId` 的建议来源。

## 字段检索

稳定字段：

- `FieldSearchResult`: `projectId`、`query`、`summary`、`items[]`、`nextActions[]`。
- `FieldSearchSummary`: `totalCandidates`、`matchedCount`、`returnedCount`、`truncated`、`appliedFilters`、`hints[]`。
- `FieldSearchItem`: `field`、`score`、`matchReasons[]`、`recommendedUse`、`nextActions[]`、`evidence[]`。

字段检索是只读能力。AI 可依赖 `matchReasons[]` 和 `evidence[]` 判断命中来源，依赖 `recommendedUse` 和 `nextActions[]` 决定收窄检索、采用标准字段或进入候选补全流程。默认检索只返回 `enabled` 字段；显式传入非 enabled `status` 时，返回项必须说明状态、替代字段或替代原因。不得把 `score` 当作跨版本绝对分值，只能用于同一次结果内排序参考。Explain Trace 不包含业务数据行、token、password 或完整 JDBC URL。

## 标准字段合并

稳定字段：

- `StandardFieldMergePreview`: `kind`、`schemaVersion`、`projectId`、`recommendedTargetFieldId`、`target`、`source`、`targetAfter`、`sourceAfter`、`changes[]`、`risks[]`、`impactItems[]`、`rollbackHints[]`、`nextActions[]`。
- `StandardFieldMergeResult`: `kind`、`schemaVersion`、`projectId`、`applied`、`preview`、`rollbackHints[]`、`nextActions[]`。
- `StandardFieldMergeRisk`: `severity`、`code`、`message`、`blocking`、`manualAction`。

字段合并是写入型标准维护能力。AI 必须先调用 preview 并展示 `changes[]`、`risks[]`、`impactItems[]` 和 `rollbackHints[]`，只有 `risks[].blocking` 全为 false 且用户提供明确 `reason` 后才能调用 apply。apply 只自动合并 aliases/tags，把来源字段标记为 `deprecated` 并写入 replacement 关系；dataType、nullable、codeSetId、sensitive 和格式约束冲突只作为风险或人工审阅项，不会静默覆盖目标字段。preview/result 不得包含 password、token、Authorization、完整 JDBC URL、DSN 或源库业务行值。

## 数据库 Metadata Browser

稳定字段：

- `DatabaseMetadataBrowser`: `kind`、`schemaVersion`、`projectId`、`databaseType`、`databaseName`、`schemaName`、`selectedTableNames[]`、`summary`、`tables[]`、`aiReadableSummary`、`nextActions[]`、`preview`、`compare`、`coverage`。
- `DatabaseMetadataBrowserSummary`: `tableCount`、`columnCount`、`indexCount`、`candidateCount`、`missingCommentCount`、`changedCount`、`unmanagedCount`。
- `DatabaseMetadataBrowserTable`: `schemaName`、`tableName`、`tableType`、`comment`、`columnCount`、`indexCount`、`candidateCount`、`missingCommentCount`、`changedCount`、`unmanagedCount`、`indexes[]`、`columns[]`、`warnings[]`。
- `DatabaseMetadataBrowserColumn`: `schemaName`、`tableName`、`columnName`、`dataType`、`nullable`、`defaultValue`、`comment`、`standardFieldName`、`standardDisplayName`、`matchStatus`、`matchReason`、`candidateKey`、`importCandidate`、`selectedByDefault`、`missingComment`、`typeChanged`、`unmanaged`、`indexNames[]`、`changes[]`。

该能力只读取 schema metadata，不执行任意 SQL、不采样源库业务数据行、不保存数据库密码。`aiReadableSummary` 用于 AI 继续生成候选导入计划或覆盖率说明；其中不得包含 password、token、Authorization、完整 JDBC URL 或业务数据行。候选写入仍必须走既有显式确认导入接口。

## 数据库 Metadata Scan Plan

稳定字段：

- `DatabaseMetadataScanReq`: 继承数据库连接请求，新增 `scanId`、`cursor`、`pageSize`、`cancel`。
- `DatabaseMetadataScanResult`: `kind`、`schemaVersion`、`projectId`、`databaseType`、`databaseName`、`schemaName`、`scanId`、`estimatedTableCount`、`cursor`、`tables[]`、`progress`、`partialSummary`、`resumeCommand`、`cancelled`、`nextActions[]`。
- `DatabaseMetadataScanProgress`: `processedTableCount`、`remainingTableEstimate`、`pageSize`、`hasMore`。
- `DatabaseMetadataScanSummary`: `pageTableCount`、`selectedTableCount`、`estimatedTableCount`。

该能力用于大库分批浏览，只返回表级分页计划和当前页表 metadata。`cursor` 第一版是短期偏移量，适合同一连接上下文内继续下一页，不是长期任务游标；`resumeCommand` 必须脱敏，不得包含 password、token、Authorization、完整 JDBC URL、DSN 或源库业务数据行。`cancelled=true` 只表示当前响应停止继续扫描，不会写入标准库、不修改源数据库、不保存连接凭据。当前页需要字段级详情时，应继续调用既有 metadata browser 或 preview，并仅传入当前批次选中的 `tableNames`。

## 自然语言需求草案

稳定字段：

- `RequirementMatchedField`: `field`、`score`、`matchReasons[]`、`recommended`、`evidence[]`。
- `RequirementMissingCandidate`: `candidateName`、`displayName`、`dataType`、`comment`、`evidence`、`confidence`、`inboxPayload`、`evidenceTrace[]`。
- `RequirementAmbiguousCandidate`: `field`、`score`、`matchReasons[]`、`evidence[]`。
- `RequirementRecommendedTemplate`: `id`、`name`、`description`、`tablePrefix`、`score`、`matchReasons[]`、`evidence[]`。

缺失候选保留原有字符串 `evidence` 兼容前端复制 payload，同时使用 `evidenceTrace[]` 暴露结构化来源。第一版只做确定性检索和模板化草案，不调用外部 LLM、不自动写入字段库或候选 Inbox。

## DDL 预览

稳定字段：

- `ddl`
- `lintResult`
- `standardSnapshot`

`lintResult` 遵循 SQL Lint 契约，`standardSnapshot` 至少保留 `specVersion` 与 `specHash`，用于 AI 交付说明和回放。

## CLI 与 MCP

稳定字段：

- `tools/fixtures/cli-mcp-contracts.json` 是 CLI/MCP contract fixture 的稳定入口，顶层字段包含 `kind`、`schemaVersion`、`description`、`compatibilityPolicy`、`cliCommands[]`、`mcpTools[]`、`mcpResources[]`、`mcpResourceTemplates[]` 和 `mcpPrompts[]`。
- Contract fixture entry 稳定描述命令或工具的 `id/name`、`description`、输入边界、`outputShape[]`、成功示例、失败示例、`safety` metadata 和 `recommendedNextActions[]`；示例必须使用占位符或脱敏 marker，不得包含 raw token、password、Authorization、完整 JDBC URL、DSN 或连接串。
- MCP agent guidance pack resource `dataspec://project/{projectId}/agent-guidance-pack` 输出 `dataspec-mcp-agent-guidance-pack`，稳定包含 `schemaVersion`、`projectId`、`compatibilityPolicy`、`templates[]` 和 `nextActions[]`；每个 template 稳定包含 `id`、`title`、`description`、`requiredInputs[]`、`safeDefaults`、`resourceSequence[]`、`resourceUris[]`、`toolSequence[]`、`stopConditions[]`、`evidenceRequirements[]` 和 `nextActions[]`。
- MCP `resources/templates/list` 输出 `resourceTemplates[]`，每项稳定包含 `uriTemplate`、`name`、`description` 和 `mimeType`；第一版覆盖 session bootstrap、capability catalog、schema registry、field catalog、workflow recipes、AI task profiles 和 agent guidance pack。
- MCP first-class agent prompts 稳定包含 `create_table_with_dataspec`、`review_sql_with_dataspec`、`reverse_import_standards` 和 `answer_field_standard_question`；`prompts/list` 会在这些 prompt descriptor 上提供 `safety` 和 `dataspecGuidance`，其中 `dataspecGuidance` 包含 required inputs、safe defaults、resource sequence、tool sequence、stop conditions、evidence requirements 和 next actions；旧 prompt 名称继续兼容保留。
- `node tools/dataspec-cli-mcp-contract-check.mjs --format json` 输出 `dataspec.cli-mcp-contract-fixtures.check`，稳定包含 `ok`、`fixtureKind`、`summary`、`diagnostics[]` 和 `nextActions[]`；该命令只读取本地 fixture 和本地 MCP descriptors，不调用后端、不连接数据库、不执行 MCP tool。
- `node tools/dataspec-status-check.mjs --format json` 输出 `dataspec.status-check`，稳定包含 `status`、`summary`、`checks[]`、`issues[]` 和 `nextActions[]`；每个 check 保留 `id`、`name`、`status`、`issueCount`，并提供兼容新增的 `errorCount` / `warningCount` 方便 AI 区分 warning-only 检查。
- Contract fixture 的兼容策略是 additive-friendly：新增可选 entry 字段或说明文本默认兼容；删除或重命名稳定 command/tool/resource/prompt、输入字段、输出 shape、安全 metadata、退出码或错误语义时，必须同步 fixture、OpenSpec 和测试。
- CLI `lint`、`lint-files`、`suggest-field`、`search-fields`、`generate-ddl` 透传后端稳定字段。
- CLI `profile list/show` 输出 AI Task Profiles 稳定字段；未知 profile 或 taskType 返回参数错误退出码。
- CLI `contract list/show/check` 输出 Schema Registry 稳定字段；`check` 失败时退出码为 `2`，并返回 `diagnostics[]`。
- CLI `capability list/show/check` 输出 AI Capability Catalog 稳定字段和 `safety` metadata；`show --format text` 展示安全摘要，`check` 失败时退出码为 `2`，并返回 `diagnostics[]`。
- CLI `compat check` 输出版本兼容握手稳定字段，并额外包含 `localCliVersion` 和 `server`。
- CLI `doctor --format json` 包含 `compatibility` check，稳定 details 包括 `localCliVersion`、`serverVersion`、`apiSchemaHash`、`minCliVersion`、`status`、`compatible` 和 `nextActions[]`。
- CLI `workflow list/show` 输出 `kind`、`schemaVersion`、`recipes[]`、`recipe`；recipe 保留 `id`、`title`、`goal`、`requiredInputs`、`prechecks`、`steps`、`expectedArtifacts`、`failureHandling`、`nextActions`。
- CLI `evidence export` 输出 `AiEvidencePackage` JSON 或 zip；失败时返回参数错误退出码并输出脱敏错误。
- MCP resources/tools 返回 `structuredContent` 和 `content[].text`；`content[].text` 应保持可解析 JSON 或明确文本 fallback。
- MCP `ai-task-profiles` resource 输出 `AiTaskProfileCatalog` 兼容结构。
- MCP `capability-catalog` resource 输出 AI Capability Catalog，并为该 resource 返回 `structuredContent` 和可解析 JSON text；MCP `tools/list` 的本地工具描述同步包含 `safety` metadata 或等价安全引用。
- MCP `version-compatibility` resource 输出版本兼容握手，并返回 `structuredContent` 和可解析 JSON text。
- MCP `schema-registry` resource 输出 Schema Registry catalog。
- MCP `workflow-recipes` resource 输出 `kind`、`schemaVersion`、`projectId`、`recipes[]`。
- MCP `search_fields` tool 返回字段检索稳定字段，`content[].text` 保持可解析 JSON。
- MCP `export_evidence_package` tool 返回 `AiEvidencePackage`，并保持后端脱敏结果。
- 安全诊断在 API `error`、CLI `DataSpecError` 和 MCP `error.data.dataspecError` 中保留 `code`、`category`、`missing`、`operation`、`safety` 和 `nextActions`；`IDEMPOTENCY_KEY_REQUIRED` 表示高风险写入缺少必需的 `Idempotency-Key`，`DRY_RUN_REQUIRED` 表示确认写入缺少预览返回的 `dryRunToken`。CLI/MCP 透传这些诊断时会递归脱敏 token、password、Authorization、JDBC URL、DSN 和 connection string。
