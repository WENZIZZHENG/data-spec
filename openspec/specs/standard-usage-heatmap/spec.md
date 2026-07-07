# standard-usage-heatmap Specification

## Purpose
定义 DataSpec 如何生成标准使用热区报告，用安全摘要展示字段近期使用、质量风险和清理优先级，辅助用户优先治理高影响字段。
## Requirements
### Requirement: 标准使用热区报告
系统 SHALL 为指定项目生成只读标准使用热区报告，按字段聚合近期 SQL 检查命中、近期 AI 作业命中、来源类型、字段质量和字段冲突，并输出 `usageScore`、`cleanupPriority` 与 `suggestedNextAction`。

#### Scenario: 聚合高使用低质量字段
- **WHEN** 字段在近期 SQL 检查或 AI 作业中被命中，并且字段质量评分低或存在冲突
- **THEN** 系统返回较高的 `usageScore` 与较高的 `cleanupPriority`，并在 `suggestedNextAction` 中提示优先修复而不是直接删除

#### Scenario: 聚合低使用低证据字段
- **WHEN** 字段近期没有 SQL 或 AI 命中，来源类型为空或字段状态为废弃/停用
- **THEN** 系统返回较低的 `usageScore`、较高的清理优先级，并建议确认是否归档、补充来源或迁移到替代字段

### Requirement: 只读与安全摘要
系统 SHALL 通过 `GET /api/standard-usage/heatmap?projectId=<id>` 暴露标准使用热区报告，并且响应不得包含 SQL 原文、AI 原始输入输出、raw issue JSON、raw source metadata、JDBC URL、DSN、token、password 或 Authorization。

#### Scenario: 查询热区 API
- **WHEN** 调用方请求 `GET /api/standard-usage/heatmap?projectId=<id>`
- **THEN** 系统返回 `summary` 与字段级 `items`，并沿用项目访问边界校验

#### Scenario: 不暴露原始记录
- **WHEN** 近期 SQL 检查或 AI 作业中存在敏感文本
- **THEN** 系统只返回命中计数、来源类型、最近命中时间和建议动作，不返回原始 SQL 或 AI payload
