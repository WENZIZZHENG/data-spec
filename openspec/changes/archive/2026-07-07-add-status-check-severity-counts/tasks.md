## 1. OpenSpec Artifacts

- [x] 1.1 完成 proposal、design 和 spec delta，明确本次只增加兼容 severity count 字段。
- [x] 1.2 运行 `openspec validate add-status-check-severity-counts --strict`。

## 2. Test-First Coverage

- [x] 2.1 在 `tools/dataspec-status-check.test.mjs` 增加红灯用例，覆盖 warning-only check 的 `warningCount`。
- [x] 2.2 覆盖 error check 的 `errorCount`。

## 3. Implementation

- [x] 3.1 扩展 `buildChecks()`，为每个 check 输出 `errorCount` 和 `warningCount`。
- [x] 3.2 保持既有 `status`、`issueCount`、退出码和顶层 summary 语义不变。
- [x] 3.3 更新 `docs/ai-contracts.md`，记录 status-check 兼容新增字段。

## 4. Verification And Review

- [x] 4.1 运行 status-check 目标单测和真实仓库 status-check。
- [x] 4.2 运行 OpenSpec strict、diff check 和敏感词扫描。
- [x] 4.3 启动独立只读子 agent 评审并关闭，处理 findings。
- [x] 4.4 满足 commit 前门禁，随后创建本地 commit。

## Verification Evidence

- 2026-07-07：`openspec validate add-status-check-severity-counts --strict` 通过。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs` 红灯符合预期，新增断言因 `checks[].warningCount/errorCount` 缺失而失败。
- 2026-07-07：实现后 `node --test tools/dataspec-status-check.test.mjs` 通过，8 pass、0 fail。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，40 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`、0 errors；`openspec-state` check 保持 `status=pass`，并输出 `errorCount=0`、`warningCount=20`。
- 2026-07-07：`openspec validate add-status-check-severity-counts --strict` 再次通过。
- 2026-07-07：`git diff --check` 通过，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：敏感词扫描新增 diff 行无命中。
- 2026-07-07：`node --test tools/*.test.mjs` 通过，248 pass、2 skip（Windows symlink 权限）、0 fail。
- 2026-07-07：独立只读评审子 agent `019f3a60-7a45-7603-915f-7ef2029d1a10`（Ramanujan）结论为 Ready to merge，无 Critical / Important；已调用 `close_agent` 关闭。Minor 建议收紧测试可读性，已处理。
- 2026-07-07：处理评审 Minor 后，`node --test tools/dataspec-status-check.test.mjs` 通过，8 pass、0 fail。
- 2026-07-07：归档时已同步 `openspec/specs/ai-contract-fixtures/spec.md`，并移动到 `openspec/changes/archive/2026-07-07-add-status-check-severity-counts/`。
- 2026-07-07：归档后 `openspec validate --all` 通过，125 passed、0 failed。
- 2026-07-07：归档后 `node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs` 通过，23 pass、0 fail。
- 2026-07-07：归档后 `node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，active change warning 从 23 降为 19；第三条 `nextActions[]` 为 `当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=19,severity=warning)`。
- 2026-07-07：归档后 `git diff --check` 退出码 0；PowerShell 输出仅包含 Git 的 CRLF 工作区提示。
- 2026-07-07：独立只读子 agent `019f3aaa-2d98-7b01-8042-2e5e432ffeb5`（Pauli）完成 4 个归档 diff 复评并已关闭；结论 Ready，无 Critical/Important/Minor。
