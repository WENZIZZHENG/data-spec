## Context

DataSpec CLI 的 `capability list/show/check` 会直接读取服务端 capability catalog，但本地单测使用 `capabilityCatalogFixture()` 作为稳定样例。`standard-evidence` 已成为服务端能力，如果本地 fixture 继续缺失，CLI 的回归样例无法覆盖 AI 选择该能力时最容易出错的 API-only/无 CLI/MCP 边界。

## Goals / Non-Goals

**Goals:**

- 在 CLI 测试 fixture 中加入 `standard-evidence`。
- 增加 `capability show standard-evidence` 的 JSON 断言，锁定 READ_ONLY、`GET /api/standard-evidence`、无 CLI/MCP。
- 更新 AI-readable contract fixture 的 capability list/show 示例，让它提示 `standard-evidence` 是可发现能力。

**Non-Goals:**

- 不新增 `dataspec standard-evidence` 或其他 CLI 命令。
- 不新增 MCP resource/tool。
- 不改变 CLI 请求路径或错误处理。

## Decisions

1. **只更新测试 fixture 和 contract fixture 示例。**
   - 原因：CLI 运行时已经读取服务端 `/api/capabilities`，不需要硬编码新能力。

2. **在 fixture helper 中为 `standard-evidence` 单独设置 surfaces。**
   - 原因：默认 fixture 会给非特殊能力填 `GET /api/capabilities` 和 `dataspec capability list`，这会错误描述 `standard-evidence`。

3. **不把 `standard-evidence` 作为独立 CLI command fixture。**
   - 原因：该能力第一版只有 API surface，CLI 只通过通用 `capability show` 发现它。

## Risks / Trade-offs

- **[Risk] 本地 fixture 继续和服务端 catalog 漂移** → Mitigation：新增 CLI 测试直接断言 `standard-evidence` 的 surfaces。
- **[Risk] AI 误以为有 CLI/MCP 命令** → Mitigation：fixture 和测试都显式使用空 CLI/MCP 数组。
