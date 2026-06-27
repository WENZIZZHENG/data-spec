## Context

`review-pr` 当前会批量 lint SQL 文件，并创建或更新一个带 `<!-- dataspec-sql-review -->` marker 的 PR 汇总评论。P5-5 已让 `LintIssue` 带有文件内 `line/column/lineEnd/columnEnd/locationKind`，但 GitHub inline comment 需要 PR diff 中的 `commit_id`、`path`、`line` 或 `position`，且只能评论在 diff 可见行上。

第一版目标是把能稳定映射的问题发到具体行，不能映射的问题继续留在汇总评论，不牺牲现有 review-pr 的可用性。

## Goals / Non-Goals

**Goals:**

- 从 GitHub 获取 PR head commit 和 diff/patch 信息。
- 将 lint issue 的文件路径和起始行映射到 PR diff 新增行。
- 对可映射 issue 发布 inline review comment。
- 对不可映射 issue 保持汇总评论 fallback，并在摘要中说明原因。
- 避免重复运行刷出重复 inline comment。

**Non-Goals:**

- 不覆盖所有历史 SQL 问题，只评论本次 diff 可见行。
- 不做 GitHub Checks API 或 Review API 的复杂批量 pending review。
- 不要求修复跨文件、重命名文件或二进制 patch 的所有边界。
- 不改变现有后端 lint API。

## Decisions

1. **使用 GitHub REST issue comments + pull request review comments。**
   - 汇总评论继续使用 issues comments marker。
   - inline 使用 `/repos/{owner}/{repo}/pulls/{pull_number}/comments` 创建 review comment。
   - 替代方案：使用 Review API 批量提交。该方案更接近代码审查模型，但需要 pending review 生命周期和更多错误处理，第一版过重。

2. **以 diff 新行行号作为第一版映射核心。**
   - 解析 PR files API 返回的 `patch`，建立 `path + newLine -> diff position/line` 映射。
   - 只对 `LintIssue.line` 落在新增/修改行上的问题发 inline。
   - 替代方案：用本地 git diff 映射。CLI 运行环境不一定有完整远端 refs，GitHub API 更稳定。

3. **用隐藏 marker 去重。**
   - inline comment body 带 `<!-- dataspec-inline-review:<path>:<line>:<ruleCode> -->`。
   - 运行前读取已有 review comments，命中 marker 时跳过或更新。
   - 第一版可先跳过重复评论，保留汇总评论更新作为最新状态。

4. **保留现有退出码语义。**
   - ERROR 仍返回 1；参数、网络或 GitHub 权限错误返回 2。
   - inline 发布失败时，若汇总评论可发布，应在汇总中说明 inline 失败；认证/权限失败仍整体失败。

## Risks / Trade-offs

- [Risk] GitHub patch 对大文件可能 truncated → 将无法映射的问题放入 fallback，并输出 reason。
- [Risk] 重复评论判断不完美 → 使用稳定 marker，优先跳过重复，后续再做更新/resolve。
- [Risk] API rate limit 或权限不足 → 给出权限、repo/pr/token 诊断，不吞掉失败。
- [Risk] Windows 路径分隔符与 GitHub path 不一致 → 统一转为 `/` 后匹配。

## Migration Plan

无需数据库迁移。`review-pr` 原有用法保持可用；新增 inline 行为默认启用，无法映射时仍有汇总评论。若需要回滚，可保留汇总评论逻辑并禁用 inline 分支。

## Open Questions

- 后续是否增加 `--no-inline` 或 `--inline-mode off|auto|required`，留给真实 CI 使用反馈后决定。
