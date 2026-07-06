## Context

字段标准的治理问题已经从“有没有标准”进入“哪些标准最该先处理”：有些字段高频出现在 SQL 检查和 AI 作业里，一旦质量低或冲突高，会持续污染 AI 输出；有些字段长期没有命中、缺少来源或已经废弃，则适合进入清理队列。现有能力分散在字段质量、字段冲突、字段来源、SQL 检查记录和 AI 作业记录中，缺少统一的只读热区报告。

本变更是 SDD standard：新增只读 API 可观察契约，但不改变数据库 schema、写入语义、权限边界或前端导航。

## Goals / Non-Goals

**Goals:**

- 生成项目字段级使用热区报告。
- 输出 `usageScore` 表示近期使用热度，输出 `cleanupPriority` 表示治理/清理优先级。
- 聚合 `qualityScore`、`conflictCount`、`sourceKinds`、`lintHits`、`aiJobHits`、`lastReferencedAt` 和 `suggestedNextAction`。
- 默认只在内存中读取近期 SQL 检查文本做字段命中计数，并且 AI 作业只读取摘要列，避免泄露 SQL 原文和 AI payload。

**Non-Goals:**

- 不新增表、索引、缓存、定时任务或迁移。
- 不集成业务代码引用索引、CLI/MCP 或前端页面。
- 不自动清理字段、不自动修改状态、不写入热区结果。
- 不返回 raw SQL、raw issues、raw AI input/output、raw source metadata 或凭据。

## Decisions

1. 新增独立 `standardusageheatmap` 模块。
   - 理由：热区报告横跨多个只读信号，独立 service 能避免污染 `FieldService`、`FieldQualityService` 或 dashboard。
   - 备选：扩展 dashboard。放弃原因是 dashboard 面向首页指标，热区报告有字段级 API 契约和 AI 消费语义。

2. 近期使用命中采用确定性文本匹配。
   - 规则：只在 SQL 检查记录的原始 SQL/修正 SQL/问题 JSON 和 AI 作业的标题/摘要/prompt/status 等摘要字段中匹配字段名；输出只保留计数和最近命中时间。
   - 理由：第一版无需引入语义索引或 embedding，且命中逻辑可解释、可测试。
   - 风险控制：响应不返回被匹配的原文，只返回 `lintHits`、`aiJobHits` 和 `lastReferencedAt`。

3. `usageScore` 与 `cleanupPriority` 分开计算。
   - `usageScore` 偏热度：SQL 命中、AI 命中、来源证据越多分越高。
   - `cleanupPriority` 偏治理：低质量、高冲突、废弃/停用、低使用会提高优先级；高使用低质量会给出“优先修复”而非“删除”。
   - 这样可以避免把高频字段误判为该删除，也避免长期无人使用字段被热度分掩盖。

4. 只读服务直接依赖 Repository 和已有 service。
   - 读取字段用 `FieldService`，以复用项目访问边界。
   - 读取质量/冲突用已有 service，读取来源/SQL/AI 记录用现有 repository 的近期查询。
   - 所有新增查询保持参数化和内部固定 limit，不拼接用户输入。

## Risks / Trade-offs

- 文本命中可能漏掉别名或业务同义词 → 第一版先以字段标准名为稳定锚点，后续可接入别名、语义索引或业务代码引用。
- 近期记录 limit 可能无法覆盖长期使用 → 输出字段说明这是近期热区，不代表全历史统计；后续可加缓存或离线重建。
- 调用字段冲突服务会读取全量字段并聚合冲突 → 当前项目规模可接受；若后续字段量上升，可改为缓存或分页。
- SQL/AI 原文可能包含敏感信息 → 只在内存中做命中计数，响应不返回原文，并用测试覆盖不暴露 payload。

## Migration Plan

- 部署：新增只读 API，无数据库迁移；旧客户端不受影响。
- 回滚：删除新增模块和 OpenSpec change 即可，不影响已有数据。
- 验证：新增 service/controller 单测，运行受影响后端测试、OpenSpec strict、`git diff --check`、敏感词扫描和独立子 agent 评审。
