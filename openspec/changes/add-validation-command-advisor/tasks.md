## 1. OpenSpec 准备

- [x] 1.1 运行 `openspec validate add-validation-command-advisor --strict`，确认 proposal/design/spec/tasks 格式有效。
- [x] 1.2 梳理 README 验证入口、CLI init AGENTS 片段和现有 Node 测试组织，确认第一版接入点。

## 2. 测试先行

- [x] 2.1 新增验证建议工具单测，先覆盖后端、前端、CLI/MCP、OpenSpec、文档和 Docker/local smoke 路径推荐。
- [x] 2.2 新增 CLI init AGENTS 片段测试，先锁定验证建议入口文案。

## 3. 实现与文档

- [x] 3.1 新增 `tools/dataspec-verify-advisor.mjs`，支持 `--path`、`--changed`、`--format text|json` 和稳定 JSON 输出。
- [x] 3.2 接入声明式路径规则，输出命令、原因、预计耗时、分类和失败后的下一步。
- [x] 3.3 更新 `dataspec init --with-agents` 片段，引用验证建议入口。
- [x] 3.4 更新 README 验证小节和 TODO P6-46 状态。

## 4. 验证与收口

- [x] 4.1 运行 `openspec validate add-validation-command-advisor --strict`。
- [x] 4.2 运行 Node 单测、后端/前端必要验证和 `git diff --check`。
- [x] 4.3 执行本地结构化代码评审并修复 findings，不使用子 agent。
- [ ] 4.4 完成提交并归档 OpenSpec change。
