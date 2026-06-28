## Context

DataSpec 已经有多个能反映 AI 使用质量的来源：`ds_ai_job_record` 记录 AI 生成/修复回放，`ds_sql_check_record` 保存 SQL lint 问题和 fixedSql，`ds_rule_exemption` 表示误报或项目例外，`ds_field_source` 表示反向导入后转正的字段来源，字段库本身有别名、注释、状态和质量评分能力。

P6-27 的第一版目标是把这些已有记录聚合成项目级“标准改进信号”。它不是用户行为分析，也不新增自动标准变更；报告只帮助用户或 AI 决定下一步去哪里修标准。

## Goals / Non-Goals

**Goals:**

- 提供项目级只读反馈报告 API，聚合 AI job、SQL 检查、规则例外、反向导入来源和字段库基础信息。
- 输出机器可读 summary、fieldSignals、ruleSignals、fixedSqlSignals、unmanagedSignals 和 nextActions。
- 前端提供“AI 反馈”页面，支持查看高频字段/规则/fixedSql 信号，并跳转到字段库、字段质量、规则配置、规则例外或 SQL 校验记录。
- 不读取业务数据行，不保存 token/password/完整连接串，不调用外部分析服务。

**Non-Goals:**

- 不做精确用户行为埋点，不记录点击、停留时长或个人监控指标。
- 不新增自动采纳标准、自动创建规则例外或自动改字段别名的写入能力。
- 不保证所有推荐命中/未命中都有历史统计；没有持久化来源的指标只作为可解释的缺口信号。
- 不引入搜索引擎、向量库或外部 BI 依赖。

## Decisions

### 1. 第一版使用只读聚合 service，不新增反馈写入表

从已有 repository 拉取当前项目最近记录，在 service 内做轻量聚合。这样避免先设计一套反馈事件模型，也不会引入行为监控味道。后续如果确实需要精确推荐日志，再作为独立待办新增 `field_suggestion_event`。

备选方案是给字段推荐、DDL、fixedSql、AI 回放都加事件写入。它更完整，但会扩大数据模型和隐私边界，不适合个人/小团队第一版。

### 2. 输出“信号 + 下一步动作”，不输出自动修复

报告项包含 `signalType`、`title`、`count`、`severity`、`evidence`、`targetRoute` 和 `suggestedAction`。前端只负责展示和跳转；是否补别名、改注释、调规则或加例外仍由用户在原页面完成。

### 3. 统计口径以可解释证据为准

- 规则信号来自 SQL 检查记录的 `issuesJson` 和规则例外列表。
- fixedSql 信号来自 SQL 检查记录的 `fixedSql` 是否存在，以及 issue 的 `ruleCode/replacement/before/after`。
- 字段引用信号来自 AI job payload、SQL issue 的 `columnName/replacement` 和字段标准名的文本命中。
- 未纳管/转正信号来自 `ds_field_source` 和 SQL issue 中疑似推荐/命名类问题。

无法可靠证明的“推荐未命中率”不伪造百分比，只在 summary 中暴露 `insufficientSuggestionHistory=true` 和建议后续补推荐事件记录。

## Risks / Trade-offs

- [Risk] 纯文本解析 AI job payload 可能误判字段引用。→ 只把它作为“引用信号”，并保留 evidence 说明来源，不作为自动修改依据。
- [Risk] 最近记录很多时聚合变慢。→ 第一版限制最近记录数量，默认读取最近 100 条 AI job/SQL check，报告中返回 `sampleSize`。
- [Risk] 只读聚合无法得出精确转化率。→ 用“转正数量/来源批次/最近导入字段”表达已有事实，不生成虚假 KPI。
- [Risk] 页面跳转参数与目标页能力不完全匹配。→ 第一版优先使用现有 query 参数或保守跳转到目标页，不改目标页写入逻辑。

## Migration Plan

- 无数据库迁移。
- 新 API 和前端页面均为 additive；旧 AI 回放、SQL 检查记录、字段库和规则例外行为不变。
- 回滚时删除新增 report API、前端页面和类型即可，不影响已有记录。

## Open Questions

- 后续是否为字段推荐新增精确事件日志，需要等第一版报告使用后再决定。
