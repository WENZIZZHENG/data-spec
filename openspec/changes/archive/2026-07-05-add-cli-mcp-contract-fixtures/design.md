## Context

CLI/MCP 是 DataSpec 面向 AI 的稳定操作面。现有 Node tests 已覆盖大量命令行为，但缺少一份可读的契约 fixture 来集中说明“有哪些入口、如何调用、输出长什么样、失败时怎样处理、哪些输入敏感”。这会让 AI prompt、README、AGENTS 片段和业务仓库脚本很难在工具演进时快速发现漂移。

本次变更处于 SDD standard：新增的是 CLI/MCP/AI 外部协议验收面，但第一版不改变现有后端 API、CLI 命令或 MCP tool 的真实业务行为。

## Goals / Non-Goals

**Goals:**

- 提供一份机器可读的 CLI/MCP contract fixture，覆盖核心 commands/tools/resources/prompts 的稳定名称、输入边界、输出 shape、成功示例、失败示例、安全 metadata 和推荐下一步。
- 提供本地验收命令，能够在无后端服务、无真实 token、无数据库连接的情况下校验 fixture 结构和 MCP 描述一致性。
- 将验收命令接入 `node --test tools/*.test.mjs` 或等价 Node 验证入口。
- 保持示例脱敏，确保 fixture 不包含 raw token、password、Authorization、JDBC URL、DSN 或连接串。

**Non-Goals:**

- 不实现完整 MCP 兼容性测试平台。
- 不一次性覆盖所有历史 CLI 命令；第一版优先覆盖 AI 高频入口。
- 不调用外部 LLM、不访问远端服务、不连接源数据库。
- 不改变现有 CLI/MCP 命令的参数、退出码或输出语义。

## Decisions

1. Fixture 使用单个 JSON 文件而不是散落 Markdown。
   - 选择：新增 `tools/fixtures/cli-mcp-contracts.json`，顶层包含 `kind`、`schemaVersion`、`cliCommands[]`、`mcpTools[]`、`mcpResources[]`、`mcpPrompts[]`。
   - 理由：JSON 可被 AI、Node test 和未来 agent 启动包直接读取；比 Markdown 更适合做结构校验。
   - 替代方案：把示例继续写在 README。缺点是不可稳定校验，AI 也难以抽取 input/output 结构。

2. 验收命令使用独立 Node 脚本而不是扩展主 CLI。
   - 选择：新增 `tools/dataspec-cli-mcp-contract-check.mjs`，默认读取 fixture 并调用 `createMcpHandler(...).tools/list|resources/list|prompts/list` 做本地一致性检查。
   - 理由：这是开发门禁工具，不是用户日常命令；放进主 CLI 会扩大外部命令面和帮助文本维护成本。
   - 替代方案：新增 `dataspec contract-fixtures check` 主 CLI 子命令。后续若需要给业务仓库直接调用，可再升级。

3. 第一版校验“关键 shape 与安全边界”，不做全量快照比对。
   - 选择：校验必需条目存在、fixture 字段类型正确、示例脱敏、MCP tool `inputSchema` 和 `safety` 与 fixture 对齐、resources/prompts 名称可枚举。
   - 理由：AI 工具契约允许 additive 字段；全量快照容易导致无意义失败，降低开发速度。

## Risks / Trade-offs

- [Risk] Fixture 覆盖过宽会拖慢交付。
  - Mitigation: 第一版只覆盖 TODO 指定的高频入口，并允许后续逐步追加。
- [Risk] Fixture 与真实 CLI 行为仍可能局部漂移。
  - Mitigation: 验收脚本覆盖 MCP descriptors；CLI 命令第一版用 fixture 结构和现有 CLI 单测兜底，后续可逐步增加 sample runner。
- [Risk] 示例误放真实凭据。
  - Mitigation: 验收脚本递归扫描 fixture 示例和错误示例，命中常见 secret 形态时失败。
- [Risk] 新脚本变成另一个没人运行的工具。
  - Mitigation: 新增 Node test 调用脚本核心校验，并更新验证建议 / README 入口。

## Migration Plan

- 新增 fixture 与校验脚本，不迁移历史数据。
- 保持所有现有 CLI/MCP 命令可继续使用。
- 如后续发现 fixture 不完整，可 additive 增加条目或字段；删除/重命名稳定字段必须同步 OpenSpec、fixture 和测试。

## Open Questions

- 无需额外确认。第一版按 TODO 中的高频入口和现有 MCP/CLI 能力落地，后续再扩大覆盖。
