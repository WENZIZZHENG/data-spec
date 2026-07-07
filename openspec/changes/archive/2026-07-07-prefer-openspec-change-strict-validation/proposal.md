## Why

`dataspec-verify-advisor` 目前对任意 `openspec/` 路径都推荐 `openspec validate --all`。当本次只改一个 active change 时，项目 SDD 更常用、更快的验证入口是 `openspec validate <change-id> --strict`，AI agent 不应每次都被引导到全量校验。

## What Changes

- 当输入路径只涉及单个 active OpenSpec change 时，`openspec-validate` 建议命令改为 `openspec validate <change-id> --strict`。
- 当输入涉及多个 active change、主规格 `openspec/specs/`、归档目录或其他 OpenSpec 路径时，继续推荐 `openspec validate --all`。
- 保持命令 id、category、summary、nextActions 和 diff-check 推荐语义不变。
- 增加 Node 单测覆盖单 change strict、多 change/all fallback 和 CLI `--changed` 输出。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `ai-contract-fixtures`: 验证建议工具对 OpenSpec 路径给出更贴近变更范围的验证命令，帮助 AI agent 稳定选择最小有效验证。

## Impact

- 影响 `tools/dataspec-verify-advisor.mjs` 和 `tools/dataspec-verify-advisor.test.mjs`。
- 不新增命令、不访问网络、不执行 OpenSpec validate，仅改变本地建议输出。
- 这是 AI 可读 CLI 输出语义的兼容优化，按 SDD standard 记录并在 commit 前执行独立只读评审。
