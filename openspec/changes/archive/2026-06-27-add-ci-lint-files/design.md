## 设计

### CLI 命令

新增 `lint-files` 作为现有 `tools/dataspec-cli.mjs` 的扩展命令，继续复用 HTTP-backed 模式：

```bash
node tools/dataspec-cli.mjs lint-files <path...> --project <id> --format json [--server <url>]
```

### 文件扫描

- 位置参数可同时包含文件和目录。
- 文件路径只接收 `.sql`。
- 目录会递归扫描 `.sql` 文件。
- 扫描时跳过 `.git`、`node_modules`、`dist`、`build`、`target` 等常见缓存/产物目录，避免 CI 在仓库根目录运行时误扫依赖或构建输出。

### 输出与退出码

输出 JSON 包含：

- `summary.totalFiles`
- `summary.failedFiles`
- `summary.errorCount`
- `summary.warningCount`
- `summary.suggestionCount`
- `files[].path`
- `files[].result`

退出码沿用现有 CLI 约定：

- `0`：所有 SQL 文件无 ERROR。
- `1`：至少一个 SQL 文件存在 ERROR。
- `2`：参数错误、网络错误或 DataSpec 服务返回失败。

### GitHub Actions 示例

示例文件使用 `.yml.example` 后缀，避免当前仓库误启用。业务仓库复制后可改名为 `.github/workflows/dataspec-sql-lint.yml` 并按实际项目调整 DataSpec 服务启动方式、`projectId` 和扫描路径。
