## 1. OpenSpec 与测试准备

- [x] 1.1 校验 proposal、design 和 spec delta 一致，确认只修改 `ai-capability-catalog`。
- [x] 1.2 先写 capability catalog 失败测试，覆盖 `standard-evidence` 出现在列表、单项查询、version supported capabilities 和 safety metadata 中。
- [x] 1.3 运行定点测试并确认因 catalog entry 缺失而失败。

## 2. 实现与文档

- [x] 2.1 更新 `AiCapabilityCatalogServiceImpl`，新增 `standard-evidence` entry。
- [x] 2.2 更新 README 的 AI 能力清单和自动化说明，加入跨来源标准证据视图。
- [x] 2.3 确认能力条目只列 API surface，不声明不存在的 CLI/MCP 命令。

## 3. 验证与收口

- [x] 3.1 运行 capability catalog 定点测试并修复失败。
- [x] 3.2 运行 `openspec validate add-standard-evidence-capability-catalog-entry --strict`。
- [x] 3.3 运行 `git diff --check` 和敏感词扫描。
- [x] 3.4 启动独立子 agent 只读评审 AI 契约、安全边界、测试和 README；关闭子 agent 并处理 findings。
- [x] 3.5 追加 `Verification Evidence` 并创建本地 commit。

## Verification Evidence

- 红灯测试：`mvn "-Dtest=AiCapabilityCatalogServiceImplTest" test` 曾因 `standard-evidence` catalog entry 缺失失败，覆盖 list、single capability、version supported capabilities 和 safety metadata。
- 后端定点测试：在 `dataspec-server` 运行 `mvn "-Dtest=AiCapabilityCatalogServiceImplTest,AiCapabilityCatalogControllerTest" test`，19 tests，0 failures，`BUILD SUCCESS`。本机 Maven 仓库仍打印既有 `jvnet-parent-3.pom` parse warning，不影响本次测试结果。
- OpenSpec：`openspec validate add-standard-evidence-capability-catalog-entry --strict` 通过。
- 通用检查：`git diff --check` 通过。
- 敏感词扫描：`git diff --unified=0 -- README.md dataspec-server/src/main/java/com/dataspec/capability/service/impl/AiCapabilityCatalogServiceImpl.java dataspec-server/src/test/java/com/dataspec/capability/AiCapabilityCatalogServiceImplTest.java openspec/changes/add-standard-evidence-capability-catalog-entry | rg -n -i "^\\+.*(password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn)"` 仅命中 README 中“不返回 token/password/Authorization/JDBC URL/DSN”的安全边界说明，未发现真实凭据或可复制连接串。
- 独立评审：子 agent `019f39f4-16c8-7721-99e0-26cd65bacd76` 只读评审，已关闭。评审未发现 Critical；Important findings 已处理：OpenSpec API surface 与 example request 分离、tasks 补充验证证据；Minor finding 已处理：测试断言收紧为 exact required inputs、API endpoints、output contracts、CLI/MCP 空列表和 example request。

## Archive Verification Evidence

- 2026-07-07：执行 `openspec archive add-standard-evidence-capability-catalog-entry --yes`，将 `ai-capability-catalog` delta 同步到 `openspec/specs/ai-capability-catalog/spec.md`，并归档到 `openspec/changes/archive/2026-07-07-add-standard-evidence-capability-catalog-entry/`。
- 2026-07-07：`openspec validate --all` 通过，118 passed、0 failed。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，44 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，active change warning 从 13 降至 8；第三条 next action 为 `当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=8,severity=warning)`。
- 2026-07-07：`git diff --check` 退出码 0，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：独立只读复评子 agent `019f3abb-7619-78c0-98de-9672aa19b115`（Planck）复评 staged archive diff，结论 Ready，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。
- 2026-07-07：补齐新增主规格 Purpose 后，独立只读复评子 agent `019f3ac0-d02f-75a1-801f-b97679d4f29c`（Helmholtz）复评 staged diff，结论 Ready，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。
