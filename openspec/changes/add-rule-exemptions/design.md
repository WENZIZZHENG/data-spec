## Context

当前规则配置只有启停和参数，一旦历史表不符合规则，用户只能关闭整条规则或忍受重复误报。`LintIssue` 已包含 `ruleCode`、`tableName`、`columnName` 和定位信息，足以支持第一版按规则、表、字段做确定性豁免。

## Goals / Non-Goals

**Goals:**
- 豁免必须属于项目，必须有 `ruleCode` 和 `reason`。
- 至少提供表名或字段名范围，避免无范围全局静默。
- 命中豁免的 issue 保留在结果中并标记 `suppressed=true`，但 active 统计不再计入 ERROR/WARNING/SUGGESTION。
- 前端可创建、禁用和查看例外。
- AI Context 导出包含例外说明。

**Non-Goals:**
- 不做审批流、组织级审计或复杂权限。
- 不支持任意 SQL 正则范围。
- 不自动创建豁免，不自动从 lint 结果一键提交豁免。
- 不改变现有规则配置语义。

## Data Model

`ds_rule_exemption`:
- `id`
- `project_id`
- `rule_code`
- `table_name`
- `column_name`
- `reason`
- `enabled`
- `expires_at`
- `created_at`
- `updated_at`
- `is_deleted`

## Matching Rules

- `projectId` 必须一致。
- `enabled=true` 且未过期。
- `ruleCode` 必须等于 issue.ruleCode。
- `tableName` 为空表示不限表；否则必须等于 issue.tableName。
- `columnName` 为空表示不限字段；否则必须等于 issue.columnName。
- 创建时至少要求 tableName 或 columnName 非空，避免全局静默。

## Decisions

- **保留 suppressed issue。** AI 和用户仍能看到历史例外，但不会让 CI 因已知问题失败。
- **统计 active issues。** `LintResult.errorCount/warningCount/suggestionCount` 只统计未被 suppress 的 issue；新增 `suppressedCount` 方便展示。
- **先做禁用，不做硬删除恢复。** 删除可沿用软删；禁用保留历史原因。

## Risks / Trade-offs

- **[Risk] 例外过多会削弱规则约束。** → 创建要求原因和范围，AI 导出提醒例外不是新标准。
- **[Risk] 过期时间对时区敏感。** → 后端使用 `LocalDateTime` 与现有实体风格一致，第一版按服务端时间判断。
- **[Risk] 旧前端类型不含 suppressed。** → 手写类型补充，兼容旧字段。

## Migration Plan

新增表不影响现有数据。回滚时删除 Controller/service/repository、lint 接入和前端页面；已创建的表可保留无害。
