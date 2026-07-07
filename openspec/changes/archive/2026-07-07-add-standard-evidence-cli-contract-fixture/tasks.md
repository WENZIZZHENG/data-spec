## 1. OpenSpec 与测试准备

- [x] 1.1 校验 proposal、design 和 spec delta 一致，确认只修改 `dataspec-cli` 相关契约。
- [x] 1.2 先补 CLI capability fixture 失败测试，覆盖 `standard-evidence` 的 list/show、READ_ONLY、API-only、无 CLI/MCP。

## 2. Fixture 与测试实现

- [x] 2.1 更新 `tools/dataspec-cli.test.mjs` 的 `capabilityCatalogFixture()`，加入 `standard-evidence` 且设置正确 surfaces。
- [x] 2.2 更新 `tools/fixtures/cli-mcp-contracts.json` 的 capability list/show 示例，展示 `standard-evidence`。
- [x] 2.3 自查未新增真实 CLI command、MCP resource 或 MCP tool。

## 3. 验证与收口

- [x] 3.1 运行 CLI capability/contract fixture 定点测试。
- [x] 3.2 运行 `openspec validate add-standard-evidence-cli-contract-fixture --strict`。
- [x] 3.3 运行 `git diff --check` 和敏感词扫描。
- [x] 3.4 启动独立子 agent 只读评审 CLI 外部协议、fixture、安全边界和测试；关闭子 agent 并处理 findings。
- [x] 3.5 追加 `Verification Evidence` 并创建本地 commit。

## Verification Evidence

- 2026-07-07：先运行 `node --test tools/dataspec-cli.test.mjs --test-name-pattern "capability"`，红灯 2 项，原因是本地 capability fixture 尚未包含 `standard-evidence`；补齐 fixture 与断言后重跑通过，结果为 139 pass、0 fail、2 skipped（Windows symlink 权限相关跳过）。
- 2026-07-07：`node --test tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，结果为 21 pass、0 fail。
- 2026-07-07：`openspec validate add-standard-evidence-cli-contract-fixture --strict` 通过。
- 2026-07-07：`git diff --check` 通过；工作区新增行敏感词扫描无真实凭据匹配。commit 前 `git diff --cached --unified=0 | rg -n -i "^\\+.*(password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn)"` 命中 2 处，均为 OpenSpec 安全边界描述和扫描命令本身，人工确认不是 raw secret、Authorization header、JDBC URL 或 DSN。
- 2026-07-07：独立只读评审子 agent `019f3a18-a355-7883-a22a-bc30fa01bad3`（Godel）用于评审 CLI 外部协议、fixture、安全边界和测试，结论为通过，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。采纳其可选增强建议，在 `capability show standard-evidence` 测试中补充 `category`、`requiredInputs` 和 `outputContracts` 断言。
- 2026-07-07：本地 commit 由本次验证收口后的 Git 提交动作完成，最终 commit hash 以仓库 Git history 为准。

## Archive Verification Evidence

- 2026-07-07：将 `dataspec-cli` delta 同步到 `openspec/specs/dataspec-cli/spec.md`，并将 change 移动到 `openspec/changes/archive/2026-07-07-add-standard-evidence-cli-contract-fixture/`。
- 2026-07-07：`openspec validate --all` 通过，119 passed、0 failed。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，44 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，active change warning 从 19 降至 13；第三条 next action 为 `当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=13,severity=warning)`。
- 2026-07-07：`git diff --check` 退出码 0，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：独立只读复评子 agent `019f3ab2-2e82-7ae0-82e5-291a3a3b6dd7`（Pasteur）复评 staged archive diff，结论 Ready，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。
