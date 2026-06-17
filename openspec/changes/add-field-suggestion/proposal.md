## Why

AI 在写 SQL 前最需要的是“这个业务含义应使用哪个标准字段”。只靠 lint 会让 AI 先犯错再修，字段推荐可以让 API、CLI、MCP 在建表前就查询标准字段库，优先复用已有字段。

## What Changes

- 新增字段推荐 service，基于字段名、显示名、注释、别名、分类和标签做确定性匹配。
- 新增后端 API：`GET /api/fields/suggest?projectId=&query=&limit=`。
- 新增 CLI 命令：`suggest-field <query> --project <id> --format json`。
- 新增 MCP tool：`suggest_fields`。
- 更新 OpenAPI TS schema、前端 API wrapper、README 和 TODO。

## Capabilities

### New Capabilities

- `field-suggestion`: 根据业务描述推荐标准字段候选。

### Modified Capabilities

- `dataspec-cli`: 提供字段推荐命令。
- `dataspec-mcp`: 暴露字段推荐 tool。

## Impact

- 后端 `field` service/controller 增加只读推荐能力。
- CLI/MCP 通过同一 HTTP API 复用后端推荐结果。
- 不新增数据库结构，不接外部 LLM，不重做前端 AI 助手页。
