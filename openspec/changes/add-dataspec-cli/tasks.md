## 1. OpenSpec 与测试

- [x] 1.1 验证 `add-dataspec-cli` OpenSpec artifacts 通过 `openspec validate`。
- [x] 1.2 新增 CLI 单元测试，并先观察缺失脚本导致的失败。

## 2. CLI 实现

- [x] 2.1 新增 `tools/dataspec-cli.mjs`，实现参数解析、server 配置和帮助输出。
- [x] 2.2 实现 `lint <path|-> --project <id> --format json`，输出 JSON 并按 `errorCount` 设置退出码。
- [x] 2.3 实现 `export-context --project <id> --output <zip>`，下载并写入 zip 文件。

## 3. 文档与验证

- [x] 3.1 更新 README 和 TODO 中的 CLI 状态/用法。
- [x] 3.2 运行 Node CLI 单元测试。
- [x] 3.3 运行后端测试和前端构建，确认本轮未破坏已有链路。
- [x] 3.4 运行 OpenSpec validate 和 diff 空白检查。
- [x] 3.5 进行直接代码评审，检查参数、退出码、错误输出和文件写入安全性。
- [x] 3.6 通过验证后提交本功能改动。
