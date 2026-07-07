## ADDED Requirements

### Requirement: 查询字段跨来源证据视图
系统 SHALL 提供只读 API `GET /api/standard-evidence?projectId=<id>&subjectType=FIELD&subjectId=<fieldId>`，按项目和字段标识返回该标准字段的跨来源证据视图。第一版仅支持 `subjectType=FIELD`，且响应不得改变任何字段、候选、来源、SQL 检查、AI 作业或变更日志数据。

#### Scenario: 成功查询字段证据
- **WHEN** 调用方提供存在的 `projectId`、`subjectType=FIELD` 和归属该项目的 `subjectId`
- **THEN** 系统返回字段摘要、证据摘要、证据列表、AI 可复制摘要和覆盖说明
- **AND** 响应至少能表达字段来源、来源可信度、使用热区、候选决策、变更日志、SQL 检查命中和 AI 作业使用中的已保存安全证据

#### Scenario: 不支持的对象类型
- **WHEN** 调用方传入 `subjectType` 不是 `FIELD`
- **THEN** 系统拒绝请求并返回稳定业务错误，而不是返回空证据或误用其他对象语义

#### Scenario: 字段不存在或不属于项目
- **WHEN** `subjectId` 对应字段不存在，或字段不属于传入 `projectId`
- **THEN** 系统拒绝请求并返回稳定业务错误，而不是泄漏其他项目字段信息

### Requirement: 证据视图只返回安全摘要
系统 SHALL 只返回 DataSpec 已保存记录中的安全摘要字段，并 SHALL NOT 在响应或 AI 摘要中包含 SQL 原文、AI raw payload、候选 raw evidence、raw source metadata、业务数据行、JDBC URL、DSN、token、password 或 Authorization。

#### Scenario: 证据来源包含敏感片段
- **WHEN** 已保存来源、候选、SQL 检查或 AI 作业记录中存在 `password`、`token`、`Authorization`、`jdbc:` 或 `dsn` 等敏感片段
- **THEN** 证据视图响应和 `aiEvidenceSummary` 均不包含这些 raw 片段
- **AND** 系统仅返回来源类型、状态、计数、时间、字段名和脱敏引用等安全摘要

#### Scenario: SQL 和 AI 作业只表达命中摘要
- **WHEN** SQL 检查记录或 AI 作业摘要引用目标字段
- **THEN** 系统可以返回命中次数、最近引用时间、作业类型或检查状态
- **AND** 系统不得返回 SQL 文本、AI 输入 payload、AI 输出 payload 或原始 issue JSON

### Requirement: AI 可复制证据摘要
系统 SHALL 基于结构化证据列表生成 `aiEvidenceSummary`，供用户复制给 AI 判断标准字段的可信度、使用情况和复核重点。该摘要 SHALL 仅复述响应中已经提供的安全事实，不引入不可追溯的新判断。

#### Scenario: 生成 AI 摘要
- **WHEN** 字段存在至少一条安全证据
- **THEN** `aiEvidenceSummary` 包含字段名、置信度等级或复核提示、主要来源类别、近期使用摘要和候选/变更决策摘要
- **AND** 摘要可以在不访问原始数据库连接或业务数据行的情况下被 AI 消费

#### Scenario: 证据不足
- **WHEN** 字段缺少来源、使用或决策证据
- **THEN** `aiEvidenceSummary` 明确提示证据不足或需要人工复核
- **AND** 响应的覆盖说明列出未覆盖的证据类别
