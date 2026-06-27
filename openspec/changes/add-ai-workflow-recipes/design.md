## Context

DataSpec 目前已有原子能力：`doctor`、`lint`、`lint-files`、`review-pr`、`export-context`、字段推荐、DDL 生成、MCP resources/prompts/tools 和 AI Context zip。AI agent 在业务仓库执行任务时，仍需要人工或 prompt 现场组合步骤，容易漏掉自检、项目配置、最小上下文和失败恢复。

P6-11 的目标是把这些能力组织成可读、可复制、可机器解析的 workflow recipes。第一版以“任务说明 + 命令计划 + 失败建议”为主，不做自动执行器，避免引入写业务仓库、外部 LLM 或复杂调度风险。

## Goals / Non-Goals

**Goals:**
- 定义一组稳定 workflow recipes，至少覆盖建表前取标准、PR SQL review、数据库反向导入补标准、最小上下文导出。
- CLI 能列出和查看 recipes，并输出 JSON 结构。
- MCP 能暴露同一类 recipes resource，便于 MCP client 先读取工作流。
- AI Context zip 附带 `.dataspec/workflows.md`，`.dataspec/README.md` 和 manifest 能指向该文件。
- recipes 明确输入、前置检查、步骤、产物、失败恢复和下一步建议。

**Non-Goals:**
- 不自动执行多步工作流，不自动修改业务仓库。
- 不内置外部 LLM 调用，不保存 LLM provider 配置。
- 不做异步任务调度、断点续跑、幂等任务锁。
- 不替代现有原子 CLI/MCP/API，只在其上提供任务化说明。

## Decisions

- **使用静态 recipe catalog。** 第一版 workflow 是稳定文档和命令计划，放在 CLI/MCP 可读取的共享 JS catalog 中；AI Context 由后端导出等价 Markdown。相比引入数据库模型或后端 API，静态 catalog 更轻，适合个人/小团队本地工具。
- **CLI 只做 list/show。** 命令形态为 `dataspec workflow list` 和 `dataspec workflow show <id>`，支持 `--format text|json`。它返回步骤和命令，不直接运行步骤，避免隐藏副作用。
- **MCP 暴露 resource 而不是 tool。** Recipes 本质是任务说明和计划，resource 更适合被 agent 读取；实际执行仍用现有 `lint_sql`、`get_field_catalog`、`suggest_fields`、`generate_table_ddl` 等 tools。
- **AI Context 包含 Markdown。** `.dataspec/workflows.md` 给 coding agent 离线读取；manifest 文件清单和 `.dataspec/README.md` 说明先读 workflows，再按任务读取字段目录和规则。

## Risks / Trade-offs

- **[Risk] CLI/MCP catalog 与后端 AI Context Markdown 漂移。** → 用单测锁定核心 recipe id，并在 README/测试中同时覆盖 CLI/MCP/Context 入口；后续可抽到共享生成源。
- **[Risk] 用户期待 workflow 自动执行。** → 命令名和输出明确为 recipe/plan，不宣称自动执行；需要自动批处理时留给 P6-26/P6-58。
- **[Risk] recipes 过长占用 AI Context。** → 第一版只导出 4 个高频短 recipe，每个包含必要命令和失败恢复，不写教程式长文。

## Migration Plan

无需数据库迁移。新增 CLI/MCP/AI Context 文件后保持原命令兼容；若需要回滚，删除 workflow catalog、CLI/MCP 入口和 AI Context workflows 文件即可。

## Open Questions

- 后续是否需要把 workflow recipes 暴露成后端 `/api/capabilities` 的一部分，留给 P6-43 AI 能力清单处理。
