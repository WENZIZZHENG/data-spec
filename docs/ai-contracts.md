# AI 输出契约

本文件记录 DataSpec 第一版 AI 可消费稳定字段。它服务于 CLI、MCP、AI Context、AI task profiles、SQL lint、字段推荐、字段检索和 DDL 预览的自动化使用场景。

## 兼容策略

- 兼容：新增可选字段、新增说明文本、增加不影响原字段语义的附加 metadata。
- 需要同步更新契约测试：删除字段、字段改名、稳定枚举改名、字段类型变化、同名字段语义变化。
- 不作为稳定契约：时间戳具体值、Markdown 文案全文、数组中与业务数据相关的完整数量、内部 DTO 私有实现细节。

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

- `.dataspec/manifest.json`: `kind`、`schemaVersion`、`projectId`、`standard.specVersion`、`standard.specHash`、`generatedAt`、`files[]`、`contextScope.profileId`、`contextScope.taskType`、`commands.lint`、`commands.exportContext`、`commands.workflowList`。
- `.dataspec/field-catalog.json`: `projectId`、`standard.specVersion`、`standard.specHash`、`contextScope`、`fields[]`、`enums[]`。
- `fields[]`: `name`、`dataType`、`nullable`、`sensitive`、`status`、`comment`、`displayName`、`category`、`tags`、`codeSetId`、`example`、`aliases[]`、`matchReasons[]`。
- `.dataspec/rules.yaml`: `standard`、`naming`、`rules`、`rule_exemptions`。
- `.dataspec/workflows.md`: `create-table`、`review-pr-sql`、`reverse-import-standards`、`export-min-context` 四个 recipe id。

## SQL Lint

稳定字段：

- `LintResult`: `tables`、`issues`、`errorCount`、`warningCount`、`suggestionCount`、`suppressedCount`、`fixedSql`、`fixedSqlDiff`、`fixPolicy`、`fixDryRun`、`fixChanges`、`fixExplanations`、`fixSummary`、`fixNextActions`。
- `LintRequest`: `sql`、`projectId`、`profileId`、`taskType`、`fixPolicy`。
- `FixPolicy`: `mode`、`maxRiskLevel`、`enabledRuleCodes`、`disabledRuleCodes`、`includeExplanations`。
- `FixChange`: `status`、`reasonCode`、`ruleCode`、`ruleName`、`riskLevel`、`changeType`、`tableName`、`columnName`、`before`、`after`、`explain`、`confidence`、`sourceStart`、`sourceEnd`。
- `FixPlanSummary`: `availableCount`、`appliedCount`、`plannedCount`、`skippedCount`。
- `LintIssue`: `severity`、`ruleCode`、`ruleName`、`message`、`tableName`、`columnName`、`suggestion`、`replacement`、`before`、`after`、`confidence`、`fixRiskLevel`、`fixChangeType`、`fixStatus`、`fixExplain`、`fixReasonCode`、`line`、`column`、`lineEnd`、`columnEnd`、`sourceStart`、`sourceEnd`、`locationKind`、`suppressed`、`suppressionId`、`suppressionReason`。
- 稳定枚举：`severity` 使用 `ERROR`、`WARNING`、`SUGGESTION`；`locationKind` 至少包含 `table`、`column`、`comment_column`；`FixPolicy.mode` 使用 `GENERATE`、`DRY_RUN`、`DISABLED`；`fixRiskLevel` 使用 `LOW`、`MEDIUM`、`HIGH`；`fixStatus` 使用 `APPLIED`、`PLANNED`、`SKIPPED`。

`fixedSql` 仍只是候选输出，不代表已经写回业务仓库；`fixPolicy.mode=DRY_RUN` 时 AI 必须把结果视为预览，并结合 `fixedSqlDiff`、`fixChanges` 和 `dialectDiagnostics` 人工确认后再继续。`fixPolicy.includeExplanations=false` 时 `fixExplanations` 可为空，AI 仍应以 `fixChanges.status/reasonCode` 判断跳过项。请求同时包含 `profileId/taskType` 和显式 `fixPolicy` 时，显式 `fixPolicy` 是有效策略来源。

## 字段推荐

稳定字段：

- `recommendedName`
- `score`
- `matchReason`
- `existing`
- `field`

`field` 命中已有标准字段时应保留标准字段基础元数据，例如 `name`、`displayName`、`dataType`、`aliases`、`category`、`sensitive`、`status`、`codeSetId` 和 `exampleValue`。

## 字段检索

稳定字段：

- `FieldSearchResult`: `projectId`、`query`、`summary`、`items[]`、`nextActions[]`。
- `FieldSearchSummary`: `totalCandidates`、`matchedCount`、`returnedCount`、`truncated`、`appliedFilters`、`hints[]`。
- `FieldSearchItem`: `field`、`score`、`matchReasons[]`、`recommendedUse`、`nextActions[]`。

字段检索是只读能力。AI 可依赖 `matchReasons[]` 判断命中来源，依赖 `nextActions[]` 决定收窄检索、采用标准字段或进入候选补全流程；不得把 `score` 当作跨版本绝对分值，只能用于同一次结果内排序参考。

## DDL 预览

稳定字段：

- `ddl`
- `lintResult`
- `standardSnapshot`

`lintResult` 遵循 SQL Lint 契约，`standardSnapshot` 至少保留 `specVersion` 与 `specHash`，用于 AI 交付说明和回放。

## CLI 与 MCP

稳定字段：

- CLI `lint`、`lint-files`、`suggest-field`、`search-fields`、`generate-ddl` 透传后端稳定字段。
- CLI `profile list/show` 输出 AI Task Profiles 稳定字段；未知 profile 或 taskType 返回参数错误退出码。
- CLI `workflow list/show` 输出 `kind`、`schemaVersion`、`recipes[]`、`recipe`；recipe 保留 `id`、`title`、`goal`、`requiredInputs`、`prechecks`、`steps`、`expectedArtifacts`、`failureHandling`、`nextActions`。
- MCP resources/tools 返回 `structuredContent` 和 `content[].text`；`content[].text` 应保持可解析 JSON 或明确文本 fallback。
- MCP `ai-task-profiles` resource 输出 `AiTaskProfileCatalog` 兼容结构。
- MCP `workflow-recipes` resource 输出 `kind`、`schemaVersion`、`projectId`、`recipes[]`。
- MCP `search_fields` tool 返回字段检索稳定字段，`content[].text` 保持可解析 JSON。
