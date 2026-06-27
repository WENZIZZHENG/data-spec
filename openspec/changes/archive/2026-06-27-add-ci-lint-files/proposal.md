## 背景

P3-2 需要让 DataSpec 能在 CI 中批量校验业务仓库里的 SQL 文件。现有 CLI 已支持单个 `lint <path|->`，但缺少递归扫描目录、聚合结果和 GitHub Actions 使用示例。

## 变更

- 新增 CLI 命令 `lint-files <path...> --project <id> --format json`。
- 支持传入一个或多个 SQL 文件/目录，递归收集 `.sql` 文件并逐个调用 `/api/lint`。
- 输出适合 CI/AI 解析的 JSON 聚合结果，并在任一文件存在 ERROR 时返回退出码 `1`。
- 补充 GitHub Actions 示例，说明如何在业务仓库中启动 DataSpec 服务并运行批量 lint。

## 非目标

- 不发布 GitHub Marketplace Action。
- 不实现 PR 评论或 reviewdog 集成；PR 评论式反馈留 P3-3。
- 不连接数据库或执行 SQL，只做静态 lint。
