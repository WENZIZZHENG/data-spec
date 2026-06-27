## 1. 配置模型与测试

- [x] 1.1 新增共享配置工具测试，覆盖向上查找 `.dataspec/config.json`、非法 JSON、字段规范化和缺省值。
- [x] 1.2 新增 `tools/dataspec-config.mjs`，实现配置查找、读取、解析和 `defaultPaths` 规范化。

## 2. CLI/MCP 接入

- [x] 2.1 更新 CLI 测试，覆盖 `lint` 从配置读取 `projectId/server`、显式参数覆盖配置、`lint-files` 使用 `defaultPaths`。
- [x] 2.2 更新 MCP 测试，覆盖 `parseServerArgs` 从配置读取默认项目和服务地址，并保持显式参数优先。
- [x] 2.3 将配置 loader 接入 `tools/dataspec-cli.mjs` 与 `tools/dataspec-mcp.mjs`。

## 3. 文档与验证

- [x] 3.1 更新 README 的 CLI/MCP 使用说明，补 `.dataspec/config.json` 示例和简化命令。
- [x] 3.2 更新 TODO.md 中 P4-3 状态。
- [x] 3.3 运行 Node 测试、OpenSpec validate、必要的 diff 检查。
- [x] 3.4 进行直接代码评审（不使用子 agent），修复发现的问题后提交本地 commit。
