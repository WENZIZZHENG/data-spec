## 1. OpenSpec Artifacts

- [x] 1.1 完成 proposal、design 和 spec delta，明确本次只修正 status-check 的 severity-aware nextActions 文案。
- [x] 1.2 运行 `openspec validate make-status-check-next-actions-severity-aware --strict`。

## 2. Test-First Coverage

- [x] 2.1 在 `tools/dataspec-status-check.test.mjs` 增加红灯用例，覆盖 warning-only report 不输出 error-first 首条建议。
- [x] 2.2 覆盖 error report 仍保留 severity=error 优先建议。
- [x] 2.3 覆盖 clean report 仍输出无需处理建议。

## 3. Implementation

- [x] 3.1 调整 `buildNextActions()`，基于 `issues[]` 判断当前最高 severity。
- [x] 3.2 保持 `status`、退出码、`summary`、`checks[]`、`issues[]` 和 issue code 不变。
- [x] 3.3 检查是否需要同步 `docs/ai-contracts.md` 的 status-check 行动建议说明。

## 4. Verification And Review

- [x] 4.1 运行 status-check 目标单测和真实仓库 status-check。
- [x] 4.2 运行 OpenSpec strict、tools 相关测试、diff check 和敏感词扫描。
- [x] 4.3 启动独立只读子 agent 评审并关闭，处理 findings。
- [x] 4.4 记录 Verification Evidence，满足 commit 前门禁后创建本地 commit。

## Verification Evidence

- 2026-07-07：`openspec validate make-status-check-next-actions-severity-aware --strict` 通过。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs` 红灯符合预期，warning-only 用例因首条 `nextActions[]` 仍包含 `severity=error` 失败；同轮 error 和 clean 行动建议断言已覆盖。
- 2026-07-07：实现后 `node --test tools/dataspec-status-check.test.mjs` 通过，8 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`、0 errors；首条 `nextActions[]` 为处理或确认 `severity=warning` 的状态漂移。
- 2026-07-07：`openspec validate make-status-check-next-actions-severity-aware --strict` 再次通过。
- 2026-07-07：`node --test tools/*.test.mjs` 通过，248 pass、2 skip（Windows symlink 权限）、0 fail。
- 2026-07-07：`git diff --check` 通过，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：敏感词扫描新增 diff 行无命中。
- 2026-07-07：独立只读评审子 agent `019f3a6d-80fa-7e21-8b36-e54e9aa6262c`（Wegener）结论为 Ready to commit，无 Critical / Important / Minor；已调用 `close_agent` 关闭。
- 2026-07-07：commit 前 `git status --short`、`git diff --cached --check`、`git diff --cached --stat`、`git diff --cached --name-only`、staged diff 浏览和 staged 敏感词扫描均完成；无空白错误或敏感词命中。
- 2026-07-07：归档时已同步 `openspec/specs/ai-contract-fixtures/spec.md`，并移动到 `openspec/changes/archive/2026-07-07-make-status-check-next-actions-severity-aware/`。
- 2026-07-07：归档后 `openspec validate --all` 通过，125 passed、0 failed。
- 2026-07-07：归档后 `node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs` 通过，23 pass、0 fail。
- 2026-07-07：归档后 `node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，active change warning 从 23 降为 19；第三条 `nextActions[]` 为 `当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=19,severity=warning)`。
- 2026-07-07：归档后 `git diff --check` 退出码 0；PowerShell 输出仅包含 Git 的 CRLF 工作区提示。
- 2026-07-07：独立只读子 agent `019f3aaa-2d98-7b01-8042-2e5e432ffeb5`（Pauli）完成 4 个归档 diff 复评并已关闭；结论 Ready，无 Critical/Important/Minor。
