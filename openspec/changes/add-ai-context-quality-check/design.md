## Context

`context-budget plan` 已经能在导出前判断 token 预算和 artifact 取舍，但它依赖后端 planner，且不会读取已经导出的 AI Context。实际使用中，AI Agent 常常拿到的是 `.dataspec/context/` 目录、zip 包或一次预算 plan JSON；这些输入需要一个本地、确定性、可重复的质量检查，帮助判断上下文是否足够继续执行任务。

本次变更属于 CLI/AI 可观察协议新增，按 SDD standard 处理。第一版保持 CLI-only，不新增后端 API、不写缓存、不改变数据库或项目状态。

## Goals / Non-Goals

**Goals:**

- 提供 `context-quality check` 本地只读命令。
- 支持读取 AI Context 目录、AI Context zip 或 `context-budget plan` JSON。
- 输出稳定 JSON，包含评分、等级、token/资源分布、缺失关键资源、截断资源、分类覆盖、任务适配提示和下一步动作。
- 保持确定性评分，不依赖外部 LLM、外部网络或运行中的 DataSpec 后端。
- 把命令加入 CLI/MCP contract fixture 和工具测试。

**Non-Goals:**

- 不保证 AI 任务结果一定正确，评分只代表上下文静态可用性。
- 不自动重新导出 Context、不写 `.dataspec/context/`、不修改业务仓库文件。
- 不新增前端展示或后端评分 API；这些留给后续待办。

## Decisions

1. 使用 CLI-only 第一版。
   - 选择原因：P6-186 明确允许第一版先做 CLI-only；本地检查能最快闭环 AI Agent 的实际使用场景。
   - 备选方案：新增后端 API 统一评分。暂不采用，因为会扩大 API 契约和服务依赖，且已导出 zip/目录的离线场景仍需要本地能力。

2. 支持三类输入：`--context-dir`、`--context-zip`、`--budget-plan`。
   - 选择原因：覆盖“已缓存目录”“下载 zip”“导出前预算计划”三种常见入口。
   - 备选方案：只支持目录。暂不采用，因为 zip 和 budget plan 是现有工作流的一等产物。

3. 使用启发式确定性评分。
   - 目录/zip 输入按文件名和内容摘要识别 manifest、字段目录、规则、schema/contract、README/prompt、业务术语、样例/evidence 等资源类别。
   - budget plan 输入复用 `selectedArtifacts`、`droppedArtifacts`、`estimation`、`qualityRisk` 和推荐动作推导可用性。
   - 评分输出同时提供数值 `contextQualityScore` 和等级 `LOW|MEDIUM|HIGH`，避免 AI 只看到不可解释的分数。

4. 输出安全边界保持只读和脱敏。
   - 命令不打印完整文件内容、不输出 raw token、password、Authorization、完整 JDBC URL、DSN 或连接串。
   - 文件路径作为上下文资源标识输出；错误信息走现有 CLI redaction。

## Risks / Trade-offs

- [Risk] 静态评分可能低估或高估某些任务的真实上下文需求。→ Mitigation：输出 `taskFitHints` 和 `nextContextActions` 解释原因，并在文档中声明不替代真实任务验证。
- [Risk] AI Context zip/目录文件命名未来变化导致分类不准。→ Mitigation：分类使用多关键词启发式，并保留 `unclassified` 覆盖项，不让未知文件导致命令失败。
- [Risk] 读取大 zip 或目录影响 CLI 响应。→ Mitigation：第一版只读取文件名、大小和有限文本片段；zip 使用现有安全解析基础，不解压到磁盘。
- [Risk] 新增 CLI 协议后 fixture 与实现漂移。→ Mitigation：补 contract fixture 和 checker 测试，纳入 tools 测试入口。

## Migration Plan

- 新增命令对现有 CLI 完全兼容；没有旧命令需要迁移。
- 回滚时删除 `context-quality check` 分支、测试、fixture 和 OpenSpec delta，不影响现有 `context-budget plan`、`export-context` 或后端 API。

## Open Questions

- 无需用户额外决策。后续如果要把评分展示到前端或做后端 API，再单独进入新的 OpenSpec change。
