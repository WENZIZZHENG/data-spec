## Context

DataSpec 已经有后端单测、CLI/MCP 单测、OpenAPI schema 漂移检查和 AI Context JSON Schema，但这些验证分散在各模块。AI agent 真正消费的是一组跨入口输出：`.dataspec/field-catalog.json`、`.dataspec/rules.yaml`、`LintIssue`/`fixedSql`、字段推荐、DDL 预览、CLI JSON 和 MCP resources/tools。当前如果这些输出的关键字段改名、枚举变化或结构缺失，可能只在人工使用时才暴露。

P6-12 需要建立第一版 AI contract fixtures：少量高价值、可读、可维护的 golden 样例，接入现有验证入口。目标是防止破坏性漂移，而不是把所有 DTO 字段永久冻结。

## Goals / Non-Goals

**Goals:**

- 建立 AI 可消费输出的 golden fixture 基线。
- 覆盖后端 AI Context、lint/fixedSql、字段推荐、DDL 预览，以及 CLI/MCP JSON 输出。
- 明确稳定字段和兼容新增策略，让 AI agent 和脚本知道哪些字段可依赖。
- 所有测试接入现有 `mvn test` 与 `node --test`。

**Non-Goals:**

- 不冻结全部内部 DTO 或数据库实体字段。
- 不阻止向后兼容新增字段。
- 不引入外部契约服务、snapshot testing 依赖或大型 golden 更新工具。
- 不重新设计 OpenAPI 生成或前端类型系统。

## Decisions

1. **使用结构化断言 + 小型 fixture，而不是整包全文快照。**
   - 原因：AI Context zip、README 和 Markdown 会随文案迭代变化，全文快照维护成本高。
   - 替代方案：保存完整 JSON/Markdown golden 文件。该方案漂移可见性强，但容易把文案变更误判为契约破坏。

2. **后端契约优先锁核心字段路径。**
   - 覆盖 `field-catalog.json` 的 `kind/schemaVersion/projectId/standard/contextScope/fields[]`，`rules.yaml` 的命名规则关键段，`LintIssue` 的 severity/ruleCode/location/suggestion/fixedSql，字段推荐的 recommendedName/score/matchReason/existing，DDL 预览的 ddl/lintResult。
   - 原因：这些是 AI agent 最常读取的稳定字段。

3. **CLI/MCP 契约继续放在 Node test 中。**
   - CLI/MCP 已有可注入 fetch 的测试模式，适合锁 JSON 输出和 resource/tool 结构。
   - 不新增运行时依赖，保持 `node --test tools/...` 作为统一入口。

4. **兼容策略写入文档。**
   - 稳定字段改名、删除或语义变化视为破坏性变更；新增字段默认兼容；枚举新增需要测试和文档说明。
   - 原因：AI agent 需要可读契约说明，而不仅是测试失败。

## Risks / Trade-offs

- [Risk] Golden 断言过宽导致仍漏掉漂移 → 重点锁字段路径、类型和稳定枚举，不只检查字符串包含。
- [Risk] Golden 断言过窄导致维护困难 → 不锁全文文案、时间戳和排序无关内容。
- [Risk] 后端与 CLI/MCP fixture 重复 → 第一版接受少量重复，后续 P6-43 可把能力清单和契约说明统一导出。

## Migration Plan

无需数据库迁移。新增测试与 fixture 后，任何破坏 AI 契约的改动会在本地验证阶段暴露；若需要调整契约，应同时更新 fixture、README 兼容说明和相关 OpenSpec 记录。

## Open Questions

- 后续是否需要提供 `dataspec contract update` 工具来辅助人工审查 golden 变化，留给后续质量工具专项处理。
