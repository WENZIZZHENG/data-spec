# AI 输出契约

本文件记录 DataSpec 第一版 AI 可消费稳定字段。它服务于 CLI、MCP、AI Context、SQL lint、字段推荐和 DDL 预览的自动化使用场景。

## 兼容策略

- 兼容：新增可选字段、新增说明文本、增加不影响原字段语义的附加 metadata。
- 需要同步更新契约测试：删除字段、字段改名、稳定枚举改名、字段类型变化、同名字段语义变化。
- 不作为稳定契约：时间戳具体值、Markdown 文案全文、数组中与业务数据相关的完整数量、内部 DTO 私有实现细节。

## AI Context

稳定字段：

- `.dataspec/manifest.json`: `kind`、`schemaVersion`、`projectId`、`standard.specVersion`、`standard.specHash`、`generatedAt`、`files[]`、`commands.lint`、`commands.exportContext`、`commands.workflowList`。
- `.dataspec/field-catalog.json`: `projectId`、`standard.specVersion`、`standard.specHash`、`contextScope`、`fields[]`、`enums[]`。
- `fields[]`: `name`、`dataType`、`nullable`、`sensitive`、`status`、`comment`、`displayName`、`category`、`tags`、`codeSetId`、`example`、`aliases[]`、`matchReasons[]`。
- `.dataspec/rules.yaml`: `standard`、`naming`、`rules`、`rule_exemptions`。
- `.dataspec/workflows.md`: `create-table`、`review-pr-sql`、`reverse-import-standards`、`export-min-context` 四个 recipe id。

## SQL Lint

稳定字段：

- `LintResult`: `tables`、`issues`、`errorCount`、`warningCount`、`suggestionCount`、`suppressedCount`、`fixedSql`、`fixedSqlDiff`。
- `LintIssue`: `severity`、`ruleCode`、`ruleName`、`message`、`tableName`、`columnName`、`suggestion`、`replacement`、`before`、`after`、`confidence`、`line`、`column`、`lineEnd`、`columnEnd`、`sourceStart`、`sourceEnd`、`locationKind`、`suppressed`、`suppressionId`、`suppressionReason`。
- 稳定枚举：`severity` 使用 `ERROR`、`WARNING`、`SUGGESTION`；`locationKind` 至少包含 `table`、`column`、`comment_column`。

## 字段推荐

稳定字段：

- `recommendedName`
- `score`
- `matchReason`
- `existing`
- `field`

`field` 命中已有标准字段时应保留标准字段基础元数据，例如 `name`、`displayName`、`dataType`、`aliases`、`category`、`sensitive`、`status`、`codeSetId` 和 `exampleValue`。

## DDL 预览

稳定字段：

- `ddl`
- `lintResult`
- `standardSnapshot`

`lintResult` 遵循 SQL Lint 契约，`standardSnapshot` 至少保留 `specVersion` 与 `specHash`，用于 AI 交付说明和回放。

## CLI 与 MCP

稳定字段：

- CLI `lint`、`lint-files`、`suggest-field`、`generate-ddl` 透传后端稳定字段。
- CLI `workflow list/show` 输出 `kind`、`schemaVersion`、`recipes[]`、`recipe`；recipe 保留 `id`、`title`、`goal`、`requiredInputs`、`prechecks`、`steps`、`expectedArtifacts`、`failureHandling`、`nextActions`。
- MCP resources/tools 返回 `structuredContent` 和 `content[].text`；`content[].text` 应保持可解析 JSON 或明确文本 fallback。
- MCP `workflow-recipes` resource 输出 `kind`、`schemaVersion`、`projectId`、`recipes[]`。
