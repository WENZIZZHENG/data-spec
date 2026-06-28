## 1. OpenSpec 准备

- [x] 1.1 运行 `openspec validate add-todo-openspec-handoff --strict`，确认 proposal/design/spec/tasks 格式有效。
- [x] 1.2 梳理 TODO 条目结构、现有 OpenSpec change 格式和 Node 工具测试组织。

## 2. 测试先行

- [x] 2.1 新增 TODO 解析单测，覆盖指定 P6 条目字段提取和缺失条目错误。
- [x] 2.2 新增草稿生成单测，覆盖 `.openspec.yaml`、proposal/design/spec/tasks 文件内容保留 TODO 边界与验收。
- [x] 2.3 新增 dry-run/JSON/text 输出单测，覆盖 openQuestions、nextActions 和不写文件。

## 3. 实现与文档

- [x] 3.1 新增 `tools/dataspec-todo-openspec-handoff.mjs`，支持 `--item`、`--todo`、`--output-dir`、`--change`、`--capability`、`--dry-run`、`--force`、`--format text|json`。
- [x] 3.2 实现 TODO 字段解析、change/capability 命名、草稿文件生成和覆盖保护。
- [x] 3.3 更新 README 验证/开发说明和 TODO P6-47 状态。

## 4. 验证与收口

- [x] 4.1 运行 `openspec validate add-todo-openspec-handoff --strict`。
- [x] 4.2 运行 Node 单测、验证建议和 `git diff --check`。
- [x] 4.3 执行本地结构化代码评审并修复 findings，不使用子 agent。
- [ ] 4.4 完成提交并归档 OpenSpec change。
