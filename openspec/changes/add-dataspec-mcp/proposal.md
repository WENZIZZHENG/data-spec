## Why

DataSpec 已有 AI Context 导出包和 CLI，但 AI client 的原生集成入口更适合用 MCP。第一版 MCP Server 需要先把“读标准”和“校验 SQL”两条核心链路打通，让 Codex/Cursor/Claude Code 等工具可以直接把字段目录和 lint 结果放进工作上下文。

## What Changes

- 新增 HTTP-backed MCP stdio adapter，默认连接 `http://localhost:8090`，通过 `--server` 覆盖。
- 启动时通过 `--project <id>` 指定默认项目；工具调用可显式覆盖 `projectId`。
- 暴露只读 resources：字段目录、数据库规则、规则 YAML。
- 暴露 prompts：按 DataSpec 创建表、评审 SQL、把业务需求转字段设计。
- 暴露 tools：`lint_sql` 和 `get_field_catalog`。
- 新增 Node MCP 单元测试和 README 使用说明。
- 本阶段不实现鉴权、多项目会话状态、远程托管、`suggest_fields`、`generate_table_ddl` 或离线规则引擎。

## Capabilities

### New Capabilities

- `dataspec-mcp`: 定义 DataSpec 第一版 MCP resources、prompts、tools、错误输出和启动参数。

### Modified Capabilities

- 无。

## Impact

- 新增 `tools/dataspec-mcp.mjs` 和对应 Node test。
- README 增加 MCP 用法与验证命令。
- 依赖：不新增 npm 包，使用 Node 18+ 内置 `fetch`、`readline` 和 `node:test`。
