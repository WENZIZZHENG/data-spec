## Context

DataSpec 已能导出 `DATABASE_RULES.md`、`field-catalog.json`、`rules.yaml` 和 MCP/CLI 入口。Prompt 生成器应复用这些内容，不再维护一套平行规则。

## Decisions

- Prompt 输出纯文本，不调用外部模型。
- 建表 prompt 包含业务描述、字段目录、结构化规则和输出要求。
- 修正 prompt 先调用 `SqlLintService.lint(sql, projectId)`，把问题列表嵌入 prompt。
- API 使用 POST JSON body，便于后续前端表单、CLI、MCP 复用。

## Risks

- Prompt 文本可能较长；个人版优先可读性，后续再做摘要或字段筛选。
- 修正 prompt 依赖当前 lint 覆盖范围，未覆盖规则仍需要 AI 根据 rules.yaml 自检。
