## 1. OpenSpec 与测试基线

- [x] 1.1 新增 `add-ai-workflow-recipes` OpenSpec proposal/design/spec/tasks 并通过校验。
- [x] 1.2 新增 CLI workflow catalog/list/show 测试。
- [x] 1.3 新增 MCP workflow resource 和 AI Context workflows 文件导出测试。

## 2. Workflow Catalog 与 CLI

- [x] 2.1 新增共享 workflow recipe catalog，覆盖 create-table、review-pr-sql、reverse-import-standards、export-min-context。
- [x] 2.2 在 `dataspec-cli.mjs` 新增 `workflow list` 和 `workflow show <id>`，支持 text/json 输出和未知 id 诊断。
- [x] 2.3 CLI 输出包含输入、前置检查、步骤命令、产物、失败恢复和下一步建议。

## 3. MCP 与 AI Context

- [x] 3.1 在 `dataspec-mcp.mjs` 暴露 workflow recipes resource。
- [x] 3.2 AI Context zip 新增 `.dataspec/workflows.md`，manifest 和 `.dataspec/README.md` 引用该文件。
- [x] 3.3 保持现有原子 CLI/MCP/API 行为不变，不自动执行 workflow 步骤。

## 4. 文档、验证与收尾

- [x] 4.1 更新 README 和 TODO，将 P6-11 标记为已完成并指向 P6-12。
- [x] 4.2 运行 CLI/MCP、后端与 OpenSpec 验证。
- [x] 4.3 进行直接代码评审并修复发现问题。
- [x] 4.4 创建本地 commit 后继续下一个待办。
