## 1. OpenSpec

- [x] 1.1 新增 P3-2 proposal/design/tasks/spec delta。
- [x] 1.2 运行 `openspec validate add-ci-lint-files --strict`。

## 2. 实现

- [x] 2.1 为 `lint-files` 新增 CLI 单元测试。
- [x] 2.2 实现批量 SQL 文件/目录扫描。
- [x] 2.3 聚合每个文件的 lint 结果并保留稳定退出码。
- [x] 2.4 补充 README 和 GitHub Actions 示例。

## 3. 验证与提交

- [x] 3.1 更新 TODO.md P3-2 状态。
- [x] 3.2 运行 CLI 单元测试。
- [x] 3.3 运行 OpenSpec validate 和 diff 空白检查。
- [x] 3.4 直接代码评审，不使用子 agent。
- [x] 3.5 创建本地 commit。
