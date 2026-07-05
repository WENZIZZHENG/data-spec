## 1. Shared Task Card Protocol

- [x] 1.1 添加 task card 共享构建与渲染测试，覆盖 create-table/reverse-import/review-pr-sql/export-min-context、缺输入 BLOCKED、secret 脱敏和 Markdown 字段。
- [x] 1.2 实现共享 task card 模块，复用 workflow recipes，输出稳定 JSON/Markdown 和受限状态枚举。

## 2. CLI Task Card Commands

- [x] 2.1 添加 CLI `task-card create/show/update` 失败测试，覆盖 JSON/Markdown 输出、安全写文件、未知 workflow、非法 step/status 和不执行工作流。
- [x] 2.2 实现 CLI `task-card` 命令族、help 文档、输出路径安全检查和本地文件更新。

## 3. MCP Task Card Tools

- [x] 3.1 添加 MCP `create_task_card`、`render_task_card` 失败测试，覆盖 structuredContent、未知 workflow、敏感输入和无副作用。
- [x] 3.2 实现 MCP task card tools，并在 tool schema/help 中说明只生成或渲染任务卡。

## 4. Frontend Task Card Display

- [x] 4.1 添加前端 task card 展示工具或组件测试，覆盖摘要字段、invalid state 和 Markdown copy。
- [x] 4.2 实现轻量 task card 展示能力，并接入现有任务入口或 smoke gate。

## 5. Documentation, Review, And Release

- [x] 5.1 更新 README 与 TODO P6-62 状态，说明任务卡第一版命令、MCP tool 和安全边界。
- [x] 5.2 运行 CLI/MCP、前端、OpenSpec 和 diff 验证；必要时运行后端确认无影响。
- [x] 5.3 使用独立 agent 做代码评审，并修复或记录 findings。
- [x] 5.4 补充 Verification Evidence，归档 OpenSpec，最终验证后创建本地 commit。

## Verification Evidence

- CLI / MCP / 共享协议测试：`node --test tools/dataspec-task-card.test.mjs tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs`，结果 130 pass，0 fail。
- 前端单测：`pnpm test`，结果 107 pass，0 fail。
- 前端构建：`pnpm build`，结果成功；仅保留第三方 `@vueuse/core` pure annotation、chunk size、plugin timing 警告。
- OpenSpec：`openspec validate --all`，结果 96 passed，0 failed。
- Diff 检查：`git diff --check`，结果通过；仅有 Git 提示部分文件提交时会按仓库配置处理 LF / CRLF。
- 后端验证：本次没有修改 Java 后端运行代码、数据库迁移或后端 API，因此未运行 `mvn test`。
- 独立评审 1：agent `019f2d9a-96ad-72d0-9999-df0a9b2eea0f`，用途为首轮代码与规范评审；发现的 secret redaction、BLOCKED 下一步、前端 malformed card、子 agent 关闭表述、非法 step/status 覆盖问题均已修复；已调用关闭工具释放。
- 独立评审 2：agent `019f2da6-93f1-7061-801e-88dadfc935ef`，用途为复审任务卡安全与协议边界；发现的敏感父级 key、非 Bearer Authorization、缺输入 BLOCKED 更新、MCP schema 表述问题均已修复；已调用关闭工具释放。
- 最终复评尝试：agent `019f2daf-2df5-7813-81a5-7a1d7e64da4c` 因工作区额度不足返回错误，关闭时提示 `not found`；已按用户要求继续完成当前任务，并以两轮已完成独立评审加最终结构化自查收口。
- OpenSpec 归档：已移动到 `openspec/changes/archive/2026-07-05-add-ai-task-card-protocol/`，主 specs 已同步。
