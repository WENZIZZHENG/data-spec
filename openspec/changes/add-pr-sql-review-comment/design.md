## 设计

### CLI 命令

```bash
node tools/dataspec-cli.mjs review-pr <path...> --project <id> --repo <owner/name> --pr <number> --token <token> [--server <url>]
```

`review-pr` 复用 `lint-files` 的文件扫描和 DataSpec `/api/lint` 调用逻辑，生成 Markdown 评论后调用 GitHub REST API。

### 评论更新策略

评论体以固定标记开头：

```markdown
<!-- dataspec-sql-review -->
```

CLI 先读取 PR issue comments，若找到包含该 marker 的评论，则 PATCH 更新；否则 POST 新评论。这样同一个 PR 多次运行只保留一个 DataSpec Review 评论。

### 权限边界

GitHub Actions 中建议授予：

- `contents: read`
- `pull-requests: write`
- `issues: write`

其中写权限只用于创建或更新 PR 评论；DataSpec CLI 不读取仓库 secret 以外的权限信息，不执行 SQL，不连接数据库。

### 退出码

评论发布或更新完成后再返回退出码：

- `0`：所有 SQL 文件无 ERROR。
- `1`：至少一个 SQL 文件存在 ERROR。
- `2`：参数、DataSpec 请求或 GitHub API 请求失败。
