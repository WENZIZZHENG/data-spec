## 1. OpenSpec

- [x] 1.1 创建 P6-36 proposal、design、delta specs 和 tasks。
- [x] 1.2 通过 OpenSpec change 校验。

## 2. 后端 AI Profile 契约

- [x] 2.1 新增 AI task profile DTO/registry，定义内置 profile、contextScope、ruleset、fixedSqlPolicy、outputFormat、recommendedCommands 和 diagnostics。
- [x] 2.2 新增 `/api/ai-profiles` API，支持按项目列出 profiles、按 taskType/profileId 查询详情，并返回未知 profile 诊断。
- [x] 2.3 扩展 `/api/lint` request，支持 profile/profileId/taskType 作为 fixedSqlPolicy 默认来源，同时保持显式 `fixPolicy` 优先。
- [x] 2.4 补后端单测，覆盖 profile 列表、未知 profile、lint profile 默认策略和显式策略优先。

## 3. CLI、MCP 与本地配置

- [x] 3.1 扩展 `.dataspec/config.json` 解析，支持 `aiProfile` 和 `taskType`，并校验类型。
- [x] 3.2 新增 CLI profile list/show 能力，并让 doctor 输出 `ai-profile` check。
- [x] 3.3 扩展 CLI lint/export-context，将配置或参数中的 profile/taskType 透传为默认参数。
- [x] 3.4 扩展 MCP resources/prompts/tools，暴露 profile resource 并在 lint/context 工具中支持 profile hint。
- [x] 3.5 补 Node CLI/MCP/config/doctor 测试，覆盖 profile JSON、诊断和参数优先级。

## 4. 前端与契约

- [x] 4.1 手工更新或重新生成 OpenAPI TS schema/types，导出 AI profile 相关类型。
- [x] 4.2 新增前端 AI Profile API wrapper 和页面/工作台入口，展示 profiles、diagnostics、推荐命令和当前选择。
- [x] 4.3 让 SQL 校验页读取当前前端 profile 默认值，并仍允许页面控件显式覆盖 fixedSqlPolicy。
- [x] 4.4 补前端 smoke/utility 测试，覆盖路由、页面文案、profile API 和 SQL lint profile 透传。

## 5. 文档、验证与收尾

- [x] 5.1 更新 README、TODO 和 AI contract 文档，说明 AI profile 的使用边界和非权限属性。
- [x] 5.2 执行后端、前端、CLI/MCP、OpenSpec 和 diff 验证。
- [x] 5.3 完成结构化代码评审并修复 findings。
- [x] 5.4 创建本地 commit。
- [x] 5.5 归档 OpenSpec change 并再次验证。

## Verification Evidence

- `mvn test`（dataspec-server）：299 tests, 0 failures, 0 errors, BUILD SUCCESS。
- `pnpm test`（dataspec-web）：77 tests, 0 fail。
- `pnpm build`（dataspec-web）：`vue-tsc --noEmit && vite build` 通过；保留既有 `@vueuse/core` pure annotation 与 chunk size 警告。
- `node --test tools\\dataspec-config.test.mjs tools\\dataspec-cli.test.mjs tools\\dataspec-mcp.test.mjs tools\\dataspec-local-smoke.test.mjs tools\\prompt-template-eval.test.mjs`：90 tests, 0 fail。
- `npx.cmd openspec validate add-ai-task-profiles`：valid。
- `npx.cmd openspec validate --all`：73 passed, 0 failed。
- `git diff --check`：通过；仅提示工作区换行将按 Git 配置转换。

## Review Evidence

- 按用户要求未使用子 agent，已用结构化自审覆盖功能正确性、契约兼容、显式参数优先级、安全边界、前端状态、测试覆盖和文档一致性。
- 已修复 findings：README 中 `profile list/show` 退出码说明过宽；SQL 校验页清空 profile 后 `profile 策略`开关状态可能误导。
