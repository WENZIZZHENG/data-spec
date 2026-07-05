## Why

DataSpec 的 CLI 与 MCP 已经是 AI 使用项目的主要入口，但当前契约验证主要散落在单测断言和 README 描述里。工具名称、入参、输出 shape、示例和安全 metadata 一旦漂移，AI 往往要到真实调用时才发现不兼容。

P6-75 需要把高频 CLI/MCP 入口沉淀为可机器读取、可回归验证的 fixture，并提供本地验收命令，让后续开发可以更快判断一次工具变更是否破坏 AI 使用面。

## What Changes

- 新增 CLI/MCP contract fixtures，覆盖核心 CLI commands、MCP tools、MCP resources 和 MCP prompts 的名称、说明、输入边界、输出 shape、示例、错误示例、安全 metadata 与推荐下一步。
- 新增本地验收命令，用于校验 fixture 结构、必需入口、示例脱敏和与 MCP `tools/list` / `resources/list` / `prompts/list` 的关键契约一致性。
- 将验收命令接入现有 Node test / 验证入口，保证修改工具参数、输出字段或安全 metadata 时能产生可读失败。
- 同步 README、AI 契约文档、TODO 和 OpenSpec 规格，说明 fixture 是契约验收辅助，不执行工具、不访问真实服务、不保存凭据。
- 不做 breaking change；不改变现有 CLI/MCP 命令的真实业务行为。

## Capabilities

### New Capabilities
- `cli-mcp-contract-fixtures`: 描述 CLI/MCP 工具契约 fixture、验收命令、覆盖范围、安全脱敏和本地验证入口。

### Modified Capabilities
- `dataspec-cli`: 增加 CLI contract fixture 验收命令及其稳定输出语义。
- `dataspec-mcp`: 增加 MCP tools/resources/prompts 与 contract fixtures 的一致性要求。

## Impact

- 影响 `tools/` 下的 CLI/MCP 辅助脚本、测试和验证建议。
- 新增或更新 fixture 文件，优先覆盖 `doctor`、`compat check`、`capability`、`contract`、`lint`、`export-context`、字段检索、DDL、MCP `lint_sql`、`search_fields`、`generate_table_ddl`、`get_session_bootstrap`、`export_evidence_package` 等高频入口。
- 影响文档：`README.md`、`docs/ai-contracts.md`、`TODO.md`。
- SDD 等级：SDD standard，因为本次新增 CLI/MCP/AI 外部协议验收面；commit 前需要独立子 agent 评审。
