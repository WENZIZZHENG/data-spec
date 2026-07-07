## 1. OpenSpec 与测试准备

- [x] 1.1 校验 proposal、design 和 spec delta 一致，确认只修改 `ai-session-bootstrap`。
- [x] 1.2 先写 bootstrap service 失败测试，覆盖 `standard-evidence` 出现在 `availableCapabilities`，并保持 READ_ONLY、API-only、无 CLI/MCP。
- [x] 1.3 运行定点测试并确认因 bootstrap 白名单缺失而失败。

## 2. 实现与文档

- [x] 2.1 更新 `AiSessionBootstrapServiceImpl` 的 bootstrap capability 白名单，加入 `standard-evidence`。
- [x] 2.2 更新 README 的 AI 会话启动包说明，加入 standard evidence 发现入口。
- [x] 2.3 自查未改变 bootstrap JSON shape、证据 API 响应结构或 CLI/MCP surfaces。

## 3. 验证与收口

- [x] 3.1 运行 bootstrap/capability 定点测试并修复失败。
- [x] 3.2 运行 `openspec validate add-standard-evidence-session-bootstrap-discovery --strict`。
- [x] 3.3 运行 `git diff --check` 和敏感词扫描。
- [x] 3.4 启动独立子 agent 只读评审 AI 外部协议、安全边界、测试和 README；关闭子 agent 并处理 findings。
- [x] 3.5 追加 `Verification Evidence` 并创建本地 commit。

## Verification Evidence

- 红灯测试：在 `dataspec-server` 运行 `mvn "-Dtest=AiSessionBootstrapServiceImplTest" test` 曾失败，`AiSessionBootstrapServiceImplTest.buildReadyBootstrapWithProjectCapabilitiesAndSnapshot` 在查找 `standard-evidence` 时抛出 `NoSuchElementException`，证明 bootstrap 白名单缺少该能力。
- 后端定点测试：在 `dataspec-server` 运行 `mvn "-Dtest=AiSessionBootstrapServiceImplTest,AiSessionBootstrapControllerTest,AiCapabilityCatalogServiceImplTest" test`，20 tests，0 failures，`BUILD SUCCESS`。本机 Maven 仓库仍打印既有 `jvnet-parent-3.pom` parse warning，不影响本次测试结果。
- OpenSpec：`openspec validate add-standard-evidence-session-bootstrap-discovery --strict` 通过。
- 通用检查：`git diff --check` 通过。
- 敏感词扫描：`git diff --unified=0 -- README.md dataspec-server/src/main/java/com/dataspec/bootstrap/service/impl/AiSessionBootstrapServiceImpl.java dataspec-server/src/test/java/com/dataspec/bootstrap/AiSessionBootstrapServiceImplTest.java openspec/changes/add-standard-evidence-session-bootstrap-discovery | rg -n -i "^\\+.*(password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn)"` 仅命中 README 中“不返回 token/password/Authorization/JDBC URL/DSN”的安全边界说明，未发现真实凭据或可复制连接串。
- 独立评审：子 agent `019f3a02-dc7f-7863-9a2c-02394412708b` 只读评审，已关闭。评审未发现 Critical；Important finding 已处理：README 已明确 session bootstrap 只暴露发现摘要，不执行证据聚合，也不返回 raw/sensitive 内容。

## Archive Verification Evidence

- 2026-07-07：将 `ai-session-bootstrap` delta 同步到 `openspec/specs/ai-session-bootstrap/spec.md`，并将 change 移动到 `openspec/changes/archive/2026-07-07-add-standard-evidence-session-bootstrap-discovery/`。
- 2026-07-07：`openspec validate --all` 通过，119 passed、0 failed。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，44 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，active change warning 从 19 降至 13；第三条 next action 为 `当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=13,severity=warning)`。
- 2026-07-07：`git diff --check` 退出码 0，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：独立只读复评子 agent `019f3ab2-2e82-7ae0-82e5-291a3a3b6dd7`（Pasteur）复评 staged archive diff，结论 Ready，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。
