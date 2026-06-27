## Why

CLI/MCP 已经能服务 AI agent，但每次都要显式传 `--project` 和 `--server`。业务仓库一旦放入 `.dataspec/config.json`，DataSpec 应该能像本地工具一样自动读取默认项目和服务地址，降低日常 lint、导出和 MCP 启动成本。

## What Changes

- 新增 `.dataspec/config.json` 读取能力，支持 `projectId`、`server`、`defaultPaths`。
- CLI 命令在缺少 `--project`、`--server` 或 `lint-files` 路径时，从配置文件补默认值。
- MCP Server 启动参数在缺少 `--project`、`--server` 时，从配置文件补默认值。
- 显式命令行参数继续优先于配置文件，避免破坏现有脚本。
- README 增加本地配置示例和简化命令说明。

## Capabilities

### New Capabilities

- `dataspec-local-config`: DataSpec CLI/MCP 在业务仓库内读取 `.dataspec/config.json` 作为默认项目配置。

### Modified Capabilities

无。

## Impact

- 主要影响 `tools/dataspec-cli.mjs`、`tools/dataspec-mcp.mjs` 与对应 Node 测试。
- 更新 README 和 TODO 中的 P4-3 状态。
- 不修改后端 API、数据库模型或前端页面。
- 不引入 npm 发布安装器；继续使用仓库内 Node 脚本入口。
