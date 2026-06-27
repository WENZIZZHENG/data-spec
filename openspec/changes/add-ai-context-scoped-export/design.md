## Context

现有 AI Context 以项目为单位输出完整 `field-catalog.json`、`rules.yaml`、`DATABASE_RULES.md` 和 zip 包。字段目录已经包含字段名、显示名、别名、分类、状态、敏感标记、代码集和示例值；这些字段足以支持第一版确定性裁剪。MCP 目前只能读取完整 resource，CLI 也只能下载完整 zip。

## Goals / Non-Goals

**Goals:**
- 默认行为保持完整导出，不破坏现有 API、CLI、MCP 和前端。
- 支持 `scope=field|domain|tag|table|changed|all` 的稳定参数；第一版 `domain` 复用字段 `category`，`tag` 复用别名/显示名/备注等文本匹配，`changed` 降级为带提示的确定性文本裁剪。
- 输出 `contextScope` 元数据，说明裁剪条件、命中原因、字段数量和提示。
- MCP 提供检索式工具，让 AI agent 可按当前需求读取较小字段目录。
- zip 包内 README 说明完整包与按需包的使用时机。

**Non-Goals:**
- 不引入向量数据库、全文检索服务或外部 LLM。
- 不新增数据库表，不记录每次导出历史。
- 不改变完整 AI Context 的文件路径和核心字段结构。
- 不实现复杂 SQL 文件语义解析；表名/任务描述只用于确定性文本匹配。

## Decisions

- **裁剪参数使用 query string。** 现有预览和下载都是 GET，扩展可选 `scope/query/status/limit` 参数即可保持兼容。
- **字段过滤集中在 service 层。** Controller、CLI、MCP 和前端只传参数，过滤规则由后端统一保证。
- **`contextScope` 为附加字段。** 现有 JSON Schema 需要同步允许该字段；`projectId/fields/enums` 仍保持原合同。
- **命中原因按字段输出。** 每个字段可带 `matchReasons`，供 AI agent 判断为什么该字段进入上下文。
- **`changed` 第一版不依赖快照 diff。** 若未提供 query，则返回 0 个字段并给出提示；若提供 query，则按文本裁剪并标注 `changed` 目前为任务相关裁剪。

## Risks / Trade-offs

- **[Risk] tag 目前没有独立字段。** → 第一版把 `tag` 映射到字段名、显示名、别名、分类、备注和示例值文本命中，并在元数据中说明。
- **[Risk] limit 过小可能漏字段。** → 输出 `warnings` 和 `totalMatchedFieldCount`，让 AI 知道上下文被截断。
- **[Risk] 按表名裁剪没有表字段关系表。** → 第一版按 query 中的表名/业务词与字段文本匹配；后续可接反向导入源或模板字段引用。

## Migration Plan

新增参数全部可选，旧调用无需迁移。回滚时删除新增模型、过滤逻辑、CLI/MCP/前端参数即可，完整导出仍可沿用原方法。
