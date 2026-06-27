## Why

DataSpec 已能在业务仓库中读取 `.dataspec/config.json`，也有 `doctor` 自检，但第一次接入仍要人工拼配置、README 和 agent 指令片段。P6-4 需要提供一个最小可重复的 `dataspec init`，让个人/小团队把 DataSpec 接入真实业务仓库时少走样。

## What Changes

- 新增 CLI 命令 `init`，支持参数化生成或更新业务仓库中的 `.dataspec/config.json`、`.dataspec/README.md` 和可选 `AGENTS.md` 片段。
- `init` 支持 `--project`、`--server`、`--default-path`、`--with-agents`、`--force`、`--format text|json` 等参数，并复用现有配置优先级和路径解析约定。
- 初始化完成后自动执行一次轻量 `doctor`，输出配置路径、写入文件、跳过文件和自检结果。
- 重复执行默认不覆盖用户已有文件；只有 `--force` 才允许覆盖 DataSpec 管理的初始化文件。
- README/TODO 更新 P6-4 状态、命令示例和边界。

## Capabilities

### New Capabilities

- `dataspec-init`: DataSpec CLI 提供业务仓库初始化向导，生成 `.dataspec` 配置与 AI agent 使用说明，并运行环境自检。

### Modified Capabilities

无。

## Impact

- CLI：修改 `tools/dataspec-cli.mjs`，新增 `init` 分支、参数解析、文件写入、JSON/text 输出和 help 文案。
- 配置：复用 `tools/dataspec-config.mjs` 读取 `.dataspec/config.json` 的格式，不改变已有配置字段语义。
- 测试：新增/扩展 `tools/dataspec-cli.test.mjs` 和必要的配置工具测试，覆盖首次初始化、重复执行不覆盖、`--force` 覆盖、JSON 输出和 doctor 调用。
- 文档：更新 `README.md` CLI 使用说明和 `TODO.md` P6-4 状态。
- 边界：不修改业务代码，不自动提交业务仓库，不写入明文 API token 到可提交文件。
