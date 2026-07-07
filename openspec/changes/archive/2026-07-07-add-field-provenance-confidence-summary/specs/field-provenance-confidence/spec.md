## ADDED Requirements

### Requirement: 字段来源可信度摘要
系统 SHALL 为指定项目生成只读字段来源可信度摘要，聚合标准字段、字段来源、标准候选决策和字段质量评分，并为每个字段输出 AI 可消费的 `aiConfidence`、`confidenceLevel`、`recommendedUse`、`warnings`、`sourceRefs` 和证据计数。

#### Scenario: 聚合可信字段
- **WHEN** 项目字段存在有效字段状态、来源记录、已采纳或已合并候选，以及良好的字段质量评分
- **THEN** 系统返回该字段的来源引用、证据数量、质量评分、候选证据和 `VERIFIED` 或 `REVIEW` 级别的置信度摘要

#### Scenario: 标记低证据字段
- **WHEN** 项目字段缺少来源证据、存在未决候选、字段状态为草稿或废弃，或字段质量评分较低
- **THEN** 系统返回 `LOW` 或 `UNKNOWN` 级别，并在 `warnings` 与 `recommendedUse` 中说明 AI 使用前需要人工复核的原因

### Requirement: 只读与安全输出
系统 SHALL 通过只读 API 暴露字段来源可信度摘要，不写入字段、候选、来源批次或质量评分数据，并且不得返回 raw `metadataJson`、raw `evidenceJson`、JDBC URL、DSN、token、password 或 Authorization。

#### Scenario: 查询项目摘要
- **WHEN** 调用方请求 `GET /api/fields/provenance-confidence?projectId=<id>`
- **THEN** 系统返回该项目的字段来源可信度摘要，并沿用项目访问边界校验

#### Scenario: 脱敏来源引用
- **WHEN** 候选来源引用或来源摘要包含可复制凭据、连接串或 Authorization 信息
- **THEN** 系统在响应中返回脱敏后的来源引用，不暴露原始敏感值
