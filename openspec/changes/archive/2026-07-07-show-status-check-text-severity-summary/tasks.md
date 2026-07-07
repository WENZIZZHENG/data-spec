## 1. OpenSpec Artifacts

- [x] 1.1 完成 proposal、design 和 spec delta，明确本次只增强 status-check text 输出。
- [x] 1.2 运行 `openspec validate show-status-check-text-severity-summary --strict`。

## 2. Test-First Coverage

- [x] 2.1 在 `tools/dataspec-status-check.test.mjs` 增加红灯用例，覆盖 text 输出包含“检查项”段。
- [x] 2.2 覆盖 text 输出中的 check 行包含 `status`、`issues`、`errors` 和 `warnings`。
- [x] 2.3 确认 JSON 输出相关断言保持不变。

## 3. Implementation

- [x] 3.1 调整 `formatStatusReportText()`，基于 `report.checks[]` 输出 check 级摘要。
- [x] 3.2 保持 JSON 输出、退出码、issue code、问题明细和 nextActions 语义不变。
- [x] 3.3 检查是否需要同步 README 或 AI 契约文档。

## 4. Verification And Review

- [x] 4.1 运行 status-check 目标单测和真实仓库 text/json status-check。
- [x] 4.2 运行 OpenSpec strict、tools 相关测试、diff check 和敏感词扫描。
- [x] 4.3 启动独立只读子 agent 评审并关闭，处理 findings。
- [x] 4.4 记录 Verification Evidence，满足 commit 前门禁后创建本地 commit。

## Verification Evidence

- 2026-07-07：`openspec validate show-status-check-text-severity-summary --strict` 通过。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs` 红灯符合预期，text 输出缺少“检查项”摘要导致 2 个新增断言失败；同轮 JSON 相关断言保持覆盖。
- 2026-07-07：实现后 `node --test tools/dataspec-status-check.test.mjs` 通过，8 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format text` 输出“检查项”段，每个 check 行包含 status、issues、errors 和 warnings。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回既有 `dataspec.status-check` JSON shape，状态为 `warn`、0 errors。
- 2026-07-07：`openspec validate show-status-check-text-severity-summary --strict` 再次通过。
- 2026-07-07：`node --test tools/*.test.mjs` 通过，248 pass、2 skip（Windows symlink 权限）、0 fail。
- 2026-07-07：`git diff --check` 通过，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：敏感词扫描新增 diff 行无命中。
- 2026-07-07：独立只读评审子 agent `019f3a77-daa3-70b1-be37-a93388e59350`（Hume）结论为 Ready to commit，无 Critical / Important / Minor；已调用 `close_agent` 关闭。
- 2026-07-07：采纳评审低风险建议，补充“检查项”段位于“问题明细”前的断言；`node --test tools/dataspec-status-check.test.mjs` 通过，8 pass、0 fail。
- 2026-07-07：评审后 `openspec validate show-status-check-text-severity-summary --strict` 再次通过。
- 2026-07-07：评审后 `node --test tools/*.test.mjs` 再次通过，248 pass、2 skip（Windows symlink 权限）、0 fail。
- 2026-07-07：评审后 `git diff --check` 通过，仅输出 Windows LF/CRLF 提示；敏感词扫描新增 diff 行无命中。
- 2026-07-07：归档时已同步 `openspec/specs/ai-contract-fixtures/spec.md`，并移动到 `openspec/changes/archive/2026-07-07-show-status-check-text-severity-summary/`。
- 2026-07-07：归档后 `openspec validate --all` 通过，125 passed、0 failed。
- 2026-07-07：归档后 `node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs` 通过，23 pass、0 fail。
- 2026-07-07：归档后 `node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，active change warning 从 23 降为 19；第三条 `nextActions[]` 为 `当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=19,severity=warning)`。
- 2026-07-07：归档后 `git diff --check` 退出码 0；PowerShell 输出仅包含 Git 的 CRLF 工作区提示。
- 2026-07-07：独立只读子 agent `019f3aaa-2d98-7b01-8042-2e5e432ffeb5`（Pauli）完成 4 个归档 diff 复评并已关闭；结论 Ready，无 Critical/Important/Minor。
