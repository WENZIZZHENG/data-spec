## Why

DataSpec 已有 CLI/MCP 单点能力，但 AI agent 在真实仓库里仍需要临时拼接“自检、取标准、lint、推荐字段、生成/修复、交付结果”的步骤。P6-11 需要把这些高频路径沉淀为稳定 workflow recipes，让 AI 可以先读取任务说明，再按机器可读契约执行。

## What Changes

- 新增 AI workflow recipes 文档，覆盖建表前取标准、PR SQL review、数据库反向导入补标准、最小上下文导出等常见任务。
- 新增 CLI workflow 入口或等价命令封装，输出机器可读 JSON，包含步骤、命令、前置条件、失败恢复和下一步建议。
- 新增 MCP resource/prompt 暴露 recipes，让 MCP client 可以读取同一份工作流说明。
- AI Context 包和 `.dataspec/README.md` 附带 workflow 片段，方便业务仓库中的 coding agent 直接引用。
- 第一版不调用外部 LLM，不自动修改业务仓库，不替代现有原子命令。

## Capabilities

### New Capabilities

- `ai-workflow-recipes`: 面向 AI/CLI/MCP 的任务化工作流模板、机器可读输出和失败恢复建议。

### Modified Capabilities

无。

## Impact

- CLI：新增 workflow/recipes 入口，复用现有 `doctor`、`lint-files`、`review-pr`、`export-context`、`suggest-field` 和 `generate-ddl` 命令能力。
- MCP：新增 workflow recipes resource 或 prompt，保持 HTTP-backed stdio adapter 架构。
- 后端：AI Context zip 增加 workflow 文档文件，不新增数据库表。
- 文档/测试：README、TODO、CLI/MCP 单测和 AI Context 导出测试需要更新。
