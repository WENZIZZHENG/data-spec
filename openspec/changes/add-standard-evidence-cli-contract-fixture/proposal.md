## Why

`standard-evidence` 已进入在线 capability catalog、session bootstrap 和 AI Context 离线包，但 CLI 测试里的本地 capability catalog fixture 仍没有该能力。这样会让 CLI capability list/show 的本地回归样例落后于真实服务，AI 或开发者读取本地契约时也更难发现标准证据入口。

## What Changes

- 更新 CLI 测试中的 capability catalog fixture，加入 `standard-evidence`。
- 补充 CLI capability list/show 测试，确认 `standard-evidence` 可通过现有 `capability` 命令展示，并保持 READ_ONLY、API-only、无 CLI/MCP surface。
- 更新 `tools/fixtures/cli-mcp-contracts.json` 的 capability list/show 示例，让本地 AI-readable contract fixture 显示 standard evidence。
- 不新增 `standard-evidence` CLI command，也不新增 MCP resource/tool。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `dataspec-cli`: CLI capability catalog 的本地 fixture 和测试需要覆盖 `standard-evidence` 的只读 API-only 能力。

## Impact

- Tools：更新 `tools/dataspec-cli.test.mjs` 和 `tools/fixtures/cli-mcp-contracts.json`。
- 文档：无需新增用户命令说明，README 已描述通过 `capability show standard-evidence` 查看能力。
- 安全：fixture 只描述 API surface，不执行请求、不包含 raw SQL、AI payload、候选 raw evidence、raw source metadata、token、password、Authorization、JDBC URL、DSN 或业务数据行。
- 验证：运行 Node CLI/fixture 定点测试、OpenSpec strict、`git diff --check`、敏感词扫描和独立子 agent 评审。
