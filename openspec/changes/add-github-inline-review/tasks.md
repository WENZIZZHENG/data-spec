## 1. OpenSpec 与测试基线

- [x] 1.1 新增 `add-github-inline-review` OpenSpec proposal/design/spec/tasks 并通过校验。
- [x] 1.2 新增 diff hunk 映射单测，覆盖新增行、未映射行、Windows 路径归一化和 truncated patch。
- [x] 1.3 新增 `review-pr` inline 单测，覆盖创建 inline、跳过重复、fallback 汇总和权限错误。

## 2. Diff 映射与 GitHub API

- [x] 2.1 新增 PR files patch 解析函数，生成 `path + newLine -> position/line` 映射。
- [x] 2.2 扩展 GitHub API wrapper，读取 PR 详情、PR files、已有 review comments，并创建 inline review comment。
- [x] 2.3 为 inline comment body 增加稳定 DataSpec marker，包含 path、line、ruleCode。

## 3. review-pr 行为集成

- [x] 3.1 在 `review-pr` 批量 lint 后匹配可 inline 的 issue，并发布 inline comment。
- [x] 3.2 无法映射的问题保留在汇总评论，输出 fallback reason。
- [x] 3.3 保持现有汇总评论 marker、更新逻辑和 ERROR 退出码语义兼容。
- [x] 3.4 输出 inline/fallback/skipped 统计，便于 CI 或 AI agent 读取。

## 4. 文档、验证与收尾

- [x] 4.1 更新 README、TODO 和 GitHub Actions 示例说明 inline 行为。
- [x] 4.2 运行 Node CLI 测试、OpenSpec validate 和 `git diff --check`；必要时运行后端回归确认无影响。
- [x] 4.3 进行直接代码评审并修复发现问题。
- [x] 4.4 创建本地 commit 后继续下一个待办。
