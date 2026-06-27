## Why

P5-5 已经给 SQL issue 增加文件内行列范围，但 CLI `review-pr` 仍只发一条汇总评论。P6-13 需要把可映射到 PR diff 的 SQL 问题落到具体代码行，减少开发者和 AI 在评论与文件之间来回定位。

## What Changes

- 扩展 `review-pr` CLI：读取 GitHub PR diff，尝试把 `LintIssue` 的文件路径与行号映射到 diff hunk position。
- 对可映射的问题发布 inline review comment；不可映射的问题继续进入汇总评论 fallback。
- 增加 inline 去重或幂等策略，避免重复运行刷屏。
- 输出 JSON/文本摘要，说明 inline、fallback、skipped 数量和失败原因。
- 保持现有汇总评论能力和退出码语义兼容。

## Capabilities

### New Capabilities

- `github-inline-review`: GitHub Pull Request SQL lint inline comment、diff hunk 映射、去重和 fallback 汇总评论。

### Modified Capabilities

无。

## Impact

- CLI：`tools/dataspec-cli.mjs` 的 `review-pr` 增加 diff 拉取、inline comment 发布和摘要输出。
- 测试：扩展 `tools/dataspec-cli.test.mjs`，覆盖 inline 成功、fallback、重复运行和 GitHub 权限错误诊断。
- 文档：README、TODO 和 GitHub Actions 示例说明 inline 行为与 fallback 边界。
- 不新增后端 API、数据库表或前端页面。
