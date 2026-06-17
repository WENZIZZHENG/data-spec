## Why

DataSpec 已有后端 API 和 AI Context 包，但 AI agent / CI 在真实工作流中更适合调用命令行。第一版 CLI 需要先提供稳定的 lint 与 context 导出入口，让业务项目能在脚本、CI 或 coding agent 中使用 DataSpec。

## What Changes

- 新增 HTTP-backed CLI wrapper，默认连接 `http://localhost:8090`，支持通过 `--server` 覆盖。
- 新增 `lint <path|-> --project <id> --format json`，从文件或 stdin 读取 SQL，调用 `/api/lint`，输出 JSON，存在 `ERROR` 时返回非 0。
- 新增 `export-context --project <id> --output <zip>`，调用 `/api/ai-context/package/download` 保存 AI Context zip 包。
- 新增 Node CLI 单元测试和 README 使用说明。
- 本阶段不实现 `generate-ddl`、`suggest-field`、包管理器发布或独立离线规则引擎。

## Capabilities

### New Capabilities

- `dataspec-cli`: 定义 DataSpec 第一版命令行入口的行为、输出和退出码。

### Modified Capabilities

- 无。

## Impact

- 新增 `tools/dataspec-cli.mjs` 和对应 Node test。
- README 增加 CLI 用法与验证命令。
- 依赖：不新增 npm 包，使用 Node 18+ 内置 `fetch`、`fs`、`node:test`。
