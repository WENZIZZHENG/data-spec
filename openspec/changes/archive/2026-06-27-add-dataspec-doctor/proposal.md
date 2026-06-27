## Why

DataSpec 已经提供 CLI、MCP、仓库级 `.dataspec/config.json` 和 OpenAPI 契约检查，但 AI agent 或 CI 调用失败时，目前需要人工分别排查服务、项目、token、默认路径和契约漂移。新增 `dataspec doctor` 可以把这些前置条件聚合为一个可读、可机器解析的自检入口，让 AI 在真正 lint 或生成前先判断环境是否可用。

## What Changes

- 新增 CLI 命令 `doctor`，支持默认文本输出和 `--format json`。
- `doctor` 读取现有 CLI 参数和 `.dataspec/config.json`，检查 server、projectId、API token、defaultPaths 和 OpenAPI 契约状态。
- 输出每个检查项的 `status`、`message` 和可选 `details`，整体成功时退出码为 `0`，存在失败项时退出码为 `1`，参数/运行时异常时退出码为 `2`。
- README 增加 `doctor` 用法，TODO 中 P5-2 标记为已完成第一版。

## Capabilities

### New Capabilities

- `dataspec-doctor`: DataSpec CLI 提供环境自检命令，供 AI agent、CI 和个人本地使用。

### Modified Capabilities

无。

## Impact

- 主要影响 `tools/dataspec-cli.mjs` 和 `tools/dataspec-cli.test.mjs`。
- 复用 `tools/dataspec-config.mjs` 的配置查找与参数优先级逻辑。
- 可能读取 `dataspec-web/src/api/schema.ts` 并拉取 `/api-docs` 用于契约漂移检查；不会修改后端、数据库或前端页面。
- 更新 README、TODO 和 OpenSpec artifacts。
