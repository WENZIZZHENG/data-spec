## Why

个人版第一阶段不直接调用外部 LLM，但需要把 DataSpec 的字段目录、命名规则和 lint 结果整理成可复制的 prompt。这样用户可以在 Codex/ChatGPT 中直接进行建表或 SQL 修正，同时保持字段标准一致。

## What Changes

- 新增 `generateCreateTablePrompt(projectId, businessDescription)`，输出建表 prompt。
- 新增 `generateFixSqlPrompt(projectId, sql)`，先执行 lint，再输出 SQL 修正 prompt。
- 新增后端 API：`/api/ai-context/prompts/create-table`、`/api/ai-context/prompts/fix-sql`。
- 更新 README/TODO 和 OpenAPI TypeScript schema。
- 本阶段不接外部 LLM API，不做前端 AI 助手页重做。

## Capabilities

### New Capabilities

- `ai-prompt-generator`: 生成可复制给 AI 编程工具的建表/修正提示词。

### Modified Capabilities

- `ai-context-package`: 复用字段目录和规则导出作为 prompt 内容来源。

## Impact

- 后端 `aicontext` service/controller 增加 prompt 方法。
- 新增服务与控制器测试。
- 前端 schema 产物同步新增接口。
