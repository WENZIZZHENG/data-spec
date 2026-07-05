## 1. OpenSpec Contracts

- [x] 1.1 完成 proposal、design 和 spec delta，并通过 `openspec validate add-sql-rule-debugger --strict`。
- [x] 1.2 确认 debug API / CLI / 前端类型字段均有语义说明，不改变既有 `/api/lint` 响应契约。

## 2. Backend Debug API

- [x] 2.1 新增 SQL 规则调试 DTO 和 `/api/lint/debug` controller 入口。
- [x] 2.2 在 `SqlLintService` 中复用现有规则配置、source range、fix policy 和 suppression 逻辑生成规则 trace，且 debug 请求不保存 SQL 检查记录。
- [x] 2.3 覆盖启用命中、启用未命中、项目禁用、修复策略和豁免状态的后端测试。

## 3. CLI And AI Catalog

- [x] 3.1 新增 CLI `lint-debug` 命令，支持文件 / stdin、project/profile/task/fix policy 参数和稳定 JSON 输出。
- [x] 3.2 补充 CLI 契约测试，覆盖成功、stdin 和请求失败退出码。
- [x] 3.3 扩展 AI capability catalog，暴露 `sql-rule-debugger` 只读能力、API/CLI surface 和 next actions。

## 4. Frontend Debug Panel

- [x] 4.1 更新前端 lint API wrapper、类型和 schema，让 debug 结果字段可被页面和测试引用。
- [x] 4.2 在 SQL 校验页新增规则调试面板，展示规则列表、详情、参数快照、match trace、source range、fixStrategy 和 suppressionStatus。
- [x] 4.3 补充前端 smoke 测试或等价验证，覆盖调试按钮、加载失败和调试结果展示。

## 5. Validation, Review, Commit

- [x] 5.1 运行受影响模块验证：后端测试、CLI tools 测试、前端测试 / build、OpenSpec strict 和 `git diff --check`。
- [x] 5.2 启动独立子 agent 评审 API / CLI / AI 外部协议变更，修复或记录 findings。
- [x] 5.3 记录 Verification Evidence，完成 staged diff / secrets 检查并创建符合 Conventional Commits 的本地 commit。

## Verification Evidence

- 2026-07-05 `mvn test`（`dataspec-server`）：459 tests，0 failures，BUILD SUCCESS。Maven 输出保留既有本地依赖 POM warning、JDK agent warning 和性能基线 warning，无测试失败。
- 2026-07-05 `node --test tools/*.test.mjs`：164 tests，0 failures。
- 2026-07-05 `pnpm test; pnpm build`（`dataspec-web`）：136 tests，0 failures；`vue-tsc --noEmit && vite build` 成功。build 输出保留既有 `@vueuse/core` Rolldown `INVALID_ANNOTATION` warning 和 chunk size warning。
- 2026-07-05 `openspec validate add-sql-rule-debugger --strict`：valid。
- 2026-07-05 `git diff --check`：exit 0，仅输出 Git 行尾转换 warning。
- 强制独立评审：子 agent `019f313e-8104-7722-8500-8d3d906a0ca5`（用途：API / CLI / AI 外部协议只读代码评审）已完成并关闭。评审发现已处理：统一 `/api/lint` 与 `/api/lint/debug` 规则配置执行语义、修正文档字段清单、复用 `SensitiveDataSanitizer` 脱敏参数快照、外移前端 debug 错误面板、补齐 `sql-rule-debug-result` Schema Registry contract。
- Commit 前检查：`git status --short` 仅包含本 change 相关 staged 文件；`git diff --cached --check` exit 0；`git diff --cached --stat` / `git diff --cached --name-only` 已核对 30 个 staged 文件；`git diff --cached -U0 | rg -n -i "password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn"` 命中项均为文档中的脱敏规则说明、CLI 参数名或单元测试假值 / 断言，无真实凭据。
