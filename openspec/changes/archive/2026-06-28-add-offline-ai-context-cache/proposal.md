## Why

AI agent 在业务仓库中工作时，不一定随时能连上 DataSpec 服务。现有 AI Context zip 更像一次性下载物，缺少业务仓库内稳定缓存、离线读取提示和缓存新鲜度诊断，导致服务不可用时 AI 很难判断能否继续按最近标准工作。

## What Changes

- 为 CLI `export-context` 增加 `--cache` 模式，把 AI Context zip 解包并写入业务仓库 `.dataspec/context/`。
- 在缓存目录写入机器可读 metadata，记录项目、服务端、导出时间、标准版本/hash/source、scope/query/status/limit 和内容摘要。
- 扩展 `doctor`，检查 `.dataspec/context/` 是否存在、是否过期、是否能与远端当前 AI Context metadata 对比。
- 保持离线缓存只读：服务不可用时只能给 AI/用户读取最近上下文和 stale 提示，不允许写入 DataSpec 服务端状态。
- 不缓存 token、数据库密码、完整 JDBC URL 或业务数据行。

## Capabilities

### New Capabilities

- `offline-ai-context-cache`: 定义业务仓库 `.dataspec/context/` 离线缓存布局、metadata、新鲜度状态和只读边界。

### Modified Capabilities

- `dataspec-cli`: `export-context` 增加 `--cache` 模式，下载 AI Context 后写入业务仓库缓存目录。
- `dataspec-doctor`: `doctor` 输出上下文缓存状态、过期提示和远端快照差异诊断。

## Impact

- CLI：扩展 `tools/dataspec-cli.mjs` 的 `export-context` 和 `doctor`；新增 zip 解包、缓存 metadata、stale 计算和安全输出。
- 配置：复用 `.dataspec/config.json` 的 rootDir/projectId/server/defaultPaths，不改变现有配置格式。
- 测试：新增 CLI 单测覆盖缓存写入、metadata 脱敏、离线 doctor、远端差异和无缓存场景。
- 文档：更新 README/TODO，说明 `.dataspec/context/` 用法、离线边界、验证命令和不缓存敏感信息。

## Verification Evidence

- `node --test tools/dataspec-cli.test.mjs tools/dataspec-config.test.mjs`：53 tests, 0 failures。
- `npx.cmd openspec validate add-offline-ai-context-cache`：Change valid。
- `git diff --check`：exit 0，仅 CRLF 工作区换行提示。
- 本地结构化代码评审：修复危险 zip 后置路径导致旧缓存被清理的问题；修复远端 manifest 对比传入原始 entries 导致 `remote-different` 漏报的问题。
