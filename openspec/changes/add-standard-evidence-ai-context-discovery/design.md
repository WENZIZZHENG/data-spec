## Context

AI Context zip 和离线 `.dataspec/context/` 缓存都会包含 `.dataspec/capabilities.json`，该文件由 `AiContextExportService` 调用 `AiCapabilityCatalogService` 生成。`standard-evidence` 已进入在线 catalog，因此离线包当前应自然包含它；本次变更的重点是把这个行为写入规格和测试，避免未来导出逻辑或能力裁剪时回退。

## Goals / Non-Goals

**Goals:**

- 让 `ai-context-package` 规格明确 `.dataspec/capabilities.json` 包含 `standard-evidence`。
- 用现有 AI Context 导出测试验证离线 capabilities 文件中的 `standard-evidence` 是 read-only、API-only，并指向 `GET /api/standard-evidence`。
- 更新 README，让离线 agent 知道 capabilities 文件可发现 standard evidence。

**Non-Goals:**

- 不新增 AI Context zip 文件。
- 不改 `AiContextExportService` 的生产导出逻辑。
- 不新增 CLI/MCP standard evidence 入口。

## Decisions

1. **复用现有 catalog 导出，不新增专用生成逻辑。**
   - 原因：`.dataspec/capabilities.json` 的权威来源已经是 `AiCapabilityCatalogService`，重复拼装会制造漂移。

2. **测试读取 zip 内真实 `.dataspec/capabilities.json`。**
   - 原因：相比只测 service，zip 级断言能覆盖 AI 离线包真实消费路径。

3. **只锁定 API-only 边界。**
   - 原因：`standard-evidence` 第一版没有 CLI/MCP surface，离线 agent 只能通过 capabilities 文件发现 API。

## Risks / Trade-offs

- **[Risk] 测试依赖 catalog 内置能力列表** → Mitigation：这是本次要保护的契约；如果 catalog 变更，应同步更新 AI Context 规格。
- **[Risk] README 误导 AI 以为离线包包含证据结果** → Mitigation：文档说明 capabilities 只用于发现，实际证据需要调用 API。
