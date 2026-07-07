## Why

AI Context zip 是业务仓库离线使用 DataSpec 的主要入口。`standard-evidence` 已进入在线 capability catalog，但离线 `.dataspec/capabilities.json` 还缺少明确回归保障，未来改动可能让离线 agent 看不到单字段跨来源证据视图。

## What Changes

- 在 `ai-context-package` 规格中明确：导出的 `.dataspec/capabilities.json` 应包含 `standard-evidence` 能力。
- 补充 AI Context 导出测试，确认 capabilities 文件包含 `standard-evidence`，且该能力只声明 `GET /api/standard-evidence`，不声明 CLI/MCP。
- 更新 AI Context README 说明，让离线 agent 知道可通过 capabilities 文件发现 standard evidence。
- 不修改 AI Context zip 结构、不新增生产导出逻辑、不修改 `GET /api/standard-evidence` 响应结构。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `ai-context-package`: 离线能力清单需要包含 `standard-evidence`，用于 AI 离线发现跨来源标准证据视图。

## Impact

- 后端测试：更新 `AiContextExportServiceTest`，锁定 `.dataspec/capabilities.json` 的 standard evidence 发现能力。
- 文档：更新 README 的 AI Context 导出包说明。
- 安全：只读取能力清单，不执行证据聚合，不返回 raw SQL、AI payload、候选 raw evidence、raw source metadata、token、password、Authorization、JDBC URL、DSN 或业务数据行。
- 验证：运行 AI Context/capability 定点测试、OpenSpec strict、`git diff --check`、敏感词扫描和独立子 agent 评审。
