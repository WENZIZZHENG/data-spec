## Why

DataSpec 的 P6 待办已经结构化，但从 TODO 条目进入 OpenSpec change 仍依赖人工复制与临场发挥。P6-47 要把单个 TODO 条目转换为可审阅的 OpenSpec 草稿，避免 AI 漏掉“为什么做、已有基础、缺口、验收标准和边界”。

## What Changes

- 新增本地 TODO 到 OpenSpec 交接助手，读取指定 `P6-x` 条目并生成 change 草稿目录。
- 生成 `.openspec.yaml`、`proposal.md`、`design.md`、`specs/<capability>/spec.md` 和 `tasks.md` 初稿。
- 支持 JSON/text 输出，返回 change id、生成文件、保留的 TODO 字段和需要人工确认的问题。
- README 验证/开发说明和 TODO 状态同步记录第一版能力。
- 不自动实现代码、不自动归档、不把模糊待办当成已确认需求。

## Capabilities

### New Capabilities
- `todo-openspec-handoff`: 从结构化 TODO 条目生成 OpenSpec change 草稿的规则、输出契约和人工确认边界。

### Modified Capabilities
- 无。

## Impact

- 新增 `tools/dataspec-todo-openspec-handoff.mjs` 与对应 Node test。
- 更新 README/TODO 和 OpenSpec 规格。
- 不新增后端 API、数据库表或前端页面；第一版只做本地文件生成和结构化提示。

## Verification Evidence

- `openspec validate add-todo-openspec-handoff --strict`：通过，当前 change 有效。
- `node --test tools\dataspec-config.test.mjs tools\dataspec-cli.test.mjs tools\dataspec-mcp.test.mjs tools\dataspec-verify-advisor.test.mjs tools\dataspec-todo-openspec-handoff.test.mjs`：通过，112 tests / 0 failures。
- `node tools\dataspec-verify-advisor.mjs --changed --format json`：通过，推荐 handoff/advisor/OpenSpec/diff 四类验证命令。
- `node tools\dataspec-todo-openspec-handoff.mjs --item P6-48 --change add-test-generated-glossary-review --capability test-generated-glossary --format json` 后执行 `openspec validate add-test-generated-glossary-review --strict`：通过，临时 change 已安全清理。
- `git diff --check`：通过，仅有 Windows 行尾提示，无空白错误。
- 本地结构化代码评审：按功能、路径安全、测试、文档和 OpenSpec 生成物检查；发现并修复 `writeOpenSpecDraft` 导出函数缺少生成文件路径越界保护的问题，新增路径保护和回归测试；未使用子 agent。
