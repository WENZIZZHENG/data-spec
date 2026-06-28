## Why

DataSpec 的验证入口已经比较完整，但 AI 或开发者在只改 README、OpenSpec、前端、后端或 CLI 时，经常不知道最小应该跑哪几条命令。P6-46 要把“按变更范围选验证命令”变成稳定的本地建议入口，减少过度验证和漏验证。

## What Changes

- 新增本地验证建议工具，根据变更路径输出推荐命令、原因、预计耗时和失败后的下一步。
- 支持 text 与 JSON 输出，JSON 面向 AI agent、CLI/证据包后续引用。
- README 验证小节和 `dataspec init --with-agents` 生成的 AGENTS 片段引用该工具。
- 增加 Node 单测覆盖后端、前端、CLI/MCP、OpenSpec、文档和 Docker/local smoke 等典型路径。

## Capabilities

### New Capabilities
- `validation-command-advisor`: 根据文件变更范围推荐 DataSpec 本地验证命令的规则、输出契约和文档入口。

### Modified Capabilities
- `dataspec-cli`: `init --with-agents` 生成片段补充验证建议入口，不改变既有命令协议。

## Impact

- 新增 `tools/dataspec-verify-advisor.mjs` 与对应 Node test。
- 更新 README 验证说明和 CLI init 生成的 AGENTS 片段。
- 不新增后端 API、数据库表或前端页面；第一版只做本地建议，不自动执行验证命令。

## Verification Evidence

- `node tools\dataspec-verify-advisor.mjs --changed --format json`：通过，确认当前 tracked 与 untracked 变更均能进入建议输入。
- `node --test tools\dataspec-config.test.mjs tools\dataspec-cli.test.mjs tools\dataspec-mcp.test.mjs tools\dataspec-verify-advisor.test.mjs`：通过，105 tests / 0 failures。
- `openspec validate add-validation-command-advisor --strict`：通过。
- `git diff --check`：通过，仅提示 Windows 工作区行尾转换。
- 本地结构化代码评审：已执行，不使用子 agent；发现并修复 `--changed` 漏收未跟踪新文件的问题。
