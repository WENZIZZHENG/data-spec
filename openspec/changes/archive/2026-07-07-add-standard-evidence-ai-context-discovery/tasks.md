## 1. OpenSpec 与测试准备

- [x] 1.1 校验 proposal、design 和 spec delta 一致，确认只修改 `ai-context-package`。
- [x] 1.2 定位 AI Context zip 中 `.dataspec/capabilities.json` 的现有导出路径，确认复用 `AiCapabilityCatalogService`。

## 2. 测试与文档

- [x] 2.1 更新 `AiContextExportServiceTest`，断言 `.dataspec/capabilities.json` 包含 `standard-evidence`。
- [x] 2.2 在测试中锁定 `standard-evidence` 为 READ_ONLY、API-only、无 CLI/MCP。
- [x] 2.3 更新 README 的 AI Context 导出包说明，提示离线 agent 可通过 capabilities 文件发现 standard evidence。

## 3. 验证与收口

- [x] 3.1 运行 AI Context/capability 定点测试。
- [x] 3.2 运行 `openspec validate add-standard-evidence-ai-context-discovery --strict`。
- [x] 3.3 运行 `git diff --check` 和敏感词扫描。
- [x] 3.4 启动独立子 agent 只读评审 AI Context 外部协议、安全边界、测试和 README；关闭子 agent 并处理 findings。
- [x] 3.5 追加 `Verification Evidence` 并创建本地 commit。

## Verification Evidence

- 后端定点测试：在 `dataspec-server` 运行 `mvn "-Dtest=AiContextExportServiceTest,AiCapabilityCatalogServiceImplTest" test`，39 tests，0 failures，`BUILD SUCCESS`。本机 Maven 仓库仍打印既有 `jvnet-parent-3.pom` parse warning，不影响本次测试结果。
- OpenSpec：`openspec validate add-standard-evidence-ai-context-discovery --strict` 通过。
- 通用检查：`git diff --check` 通过。
- 敏感词扫描：`git diff --unified=0 -- README.md dataspec-server/src/test/java/com/dataspec/aicontext/AiContextExportServiceTest.java openspec/changes/add-standard-evidence-ai-context-discovery | rg -n -i "^\\+.*(password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn)"` 无匹配，未发现真实凭据或可复制连接串。
- 独立评审：子 agent `019f3a0c-ff42-7840-8d6c-40703fa86cc3` 只读评审，已关闭。评审未发现 Critical、Important 或 Minor；可选建议已处理：测试显式断言 `cliCommands`、`mcpResources`、`mcpTools` 是数组且为空。

## Archive Verification Evidence

- 2026-07-07：将 `ai-context-package` delta 同步到 `openspec/specs/ai-context-package/spec.md`，并将 change 移动到 `openspec/changes/archive/2026-07-07-add-standard-evidence-ai-context-discovery/`。
- 2026-07-07：`openspec validate --all` 通过，119 passed、0 failed。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，44 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，active change warning 从 19 降至 13；第三条 next action 为 `当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=13,severity=warning)`。
- 2026-07-07：`git diff --check` 退出码 0，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：独立只读复评子 agent `019f3ab2-2e82-7ae0-82e5-291a3a3b6dd7`（Pasteur）复评 staged archive diff，结论 Ready，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。
