## Context

P6-53 的目标是把已有“单次报告”收束成可持续改进入口。当前字段质量、覆盖率、AI 反馈和候选 Inbox 都是即时报告；用户和 AI 缺少一个可回看的项目健康时间线，也缺少按优先级维护标准的计划文本。

## Goals / Non-Goals

**Goals:**
- 新增 `ds_standard_health_snapshot`，保存项目级健康快照和 AI 可读 payload。
- 新增后端 API：创建快照、查询趋势、查询最新快照和复制用改进计划。
- 新增前端“标准健康”视图：显示当前快照、本周/月变化、趋势表、Top actions 和复制计划按钮。
- 复用字段质量评分、AI 反馈、候选 Inbox、规则例外和 SQL 检查记录；覆盖率指标由创建快照请求传入最近一次覆盖率摘要。
- 用户能看到本周/本月标准质量和覆盖率变化；AI 可读取 Top actions 并按优先级补字段、修别名或调整规则；趋势数据不包含业务数据行。

**Non-Goals:**
- 不做组织 KPI，不接外部 BI，不采集用户行为监控。
- 不自动调度周期任务；第一版由用户或 AI 手动创建快照。
- 不保存数据库连接信息、SQL 原文或业务数据行；只保存统计值、字段/规则名称和建议。

## Decisions

1. **手动快照优先，不做定时采集。**
   - 原因：个人/小团队第一版更需要“需要时保存一张状态”，不是组织级监控。

2. **覆盖率由调用方传入。**
   - 原因：覆盖率报告依赖 SQL、数据库 metadata 或 dump 输入，服务端没有一个天然的项目全量 schema 来源；第一版不假装有全量业务库监控。

3. **Top actions 以稳定 JSON + Markdown 双输出。**
   - JSON 用于前端和 AI，Markdown 用于复制到 Codex/Cursor/Claude Code。

4. **快照 payload 只保存元数据和统计。**
   - 允许字段名、规则编码、候选数量、覆盖率数值；禁止 SQL 原文、数据库连接、token、密码和业务数据行。

## Risks / Trade-offs

- [Risk] 没有传入覆盖率摘要时趋势中 coverageRate 为空。→ Mitigation：API/前端显示 `coverageStatus=not_collected`，Top action 提醒先运行覆盖率报告。
- [Risk] 保存过多细节可能泄漏业务内容。→ Mitigation：仅保存字段名/规则 code/统计和建议，不保存 SQL/连接/数据行。
- [Risk] 快照趋势被误当 KPI。→ Mitigation：文档和页面文案定位为个人维护计划，不做排行、评分排名或组织看板。

## Open Questions

- 无。
