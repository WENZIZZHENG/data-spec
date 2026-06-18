## 背景

P3-3 需要让 DataSpec 在 GitHub Pull Request 中输出可读的 SQL Review 评论。P3-2 已有 `lint-files` 批量校验和 GitHub Actions 示例，但 CI 只失败不够友好，开发者和 AI agent 还需要看到文件级问题、规则编码和建议替换。

## 变更

- 新增 CLI 命令 `review-pr <path...>`，在批量 lint 后创建或更新 PR 评论。
- 评论内容包含汇总指标、文件概览和问题明细。
- 使用固定 marker 查找并更新旧评论，避免每次 CI 运行都刷屏。
- README 和 GitHub Actions 示例补充 `GITHUB_TOKEN` 权限与调用方式。

## 非目标

- 不实现行号级 inline review comment；当前 lint issue 暂无稳定 source span。
- 不支持 GitLab/Gitea。
- 不做审批流或数据库发布流程。
