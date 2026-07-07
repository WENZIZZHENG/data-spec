## 1. OpenSpec Artifacts

- [x] 1.1 完成 proposal、design 和 spec delta，明确本次只增加兼容 issue code 摘要字段。
- [x] 1.2 运行 `openspec validate add-status-check-issue-code-summary --strict`。

## 2. Test-First Coverage

- [x] 2.1 在 `tools/dataspec-status-check.test.mjs` 增加红灯用例，覆盖重复 warning code 的 `summary.issueCodes[]`。
- [x] 2.2 覆盖 error code 的 `summary.issueCodes[]` 和最高 severity。
- [x] 2.3 确认既有 `issues[]`、`checks[]` 和状态语义不变。

## 3. Implementation

- [x] 3.1 增加 issue code 聚合函数，基于现有 `issues[]` 生成 `summary.issueCodes[]`。
- [x] 3.2 保持 `status`、退出码、`summary.errors/warnings`、`checks[]`、`issues[]` 和 `nextActions[]` 语义不变。
- [x] 3.3 更新 `docs/ai-contracts.md`，记录 status-check 兼容新增字段。

## 4. Verification And Review

- [x] 4.1 运行 status-check 目标单测和真实仓库 status-check。
- [x] 4.2 运行 OpenSpec strict、tools 相关测试、diff check 和敏感词扫描。
- [x] 4.3 启动独立只读子 agent 评审并关闭，处理 findings。
- [x] 4.4 记录 Verification Evidence，满足 commit 前门禁后创建本地 commit。

## Verification Evidence

- 2026-07-07：`openspec validate add-status-check-issue-code-summary --strict` 通过。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs` 红灯符合预期，4 个新增断言因 `summary.issueCodes` 缺失失败。
- 2026-07-07：实现后 `node --test tools/dataspec-status-check.test.mjs` 通过，8 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回既有 `dataspec.status-check` JSON shape，新增 `summary.issueCodes[]` 汇总 `OPENSPEC_ACTIVE_CHANGE_PRESENT` count=24、severity=warning。
- 2026-07-07：收口复跑 `openspec validate add-status-check-issue-code-summary --strict` 通过。
- 2026-07-07：收口复跑 `node --test tools/dataspec-status-check.test.mjs` 通过，8 pass、0 fail。
- 2026-07-07：`node --test tools/*.test.mjs` 通过，254 tests、252 pass、2 skip、0 fail。
- 2026-07-07：收口复跑 `node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，仅保留既有 active change warning；`summary.issueCodes[]` 汇总 `OPENSPEC_ACTIVE_CHANGE_PRESENT` count=24、severity=warning。
- 2026-07-07：`git diff --check` 退出码 0；PowerShell 输出仅包含 Git 的 CRLF 工作区提示。
- 2026-07-07：新增 diff 常见敏感字段扫描无命中。
- 2026-07-07：独立只读子 agent `019f3a91-7b19-7683-ac23-91838b2d2628`（Feynman）完成评审并已关闭；结论 Ready，无 Critical/Important。唯一 Minor 为可选补充同 code 混合 severity 直接测试；因现有可观察入口不会自然生成同 code 同时 warning/error，若为该用例导出内部聚合函数会扩大公共代码表面，暂不处理。
- 2026-07-07：commit 前复跑 `openspec validate add-status-check-issue-code-summary --strict` 通过。
- 2026-07-07：commit 前复跑 `node --test tools/dataspec-status-check.test.mjs` 通过，8 pass、0 fail。
- 2026-07-07：commit 前复跑 `git diff --check` 退出码 0；PowerShell 输出仅包含 Git 的 CRLF 工作区提示。
- 2026-07-07：归档时已同步 `openspec/specs/ai-contract-fixtures/spec.md`，并移动到 `openspec/changes/archive/2026-07-07-add-status-check-issue-code-summary/`。
- 2026-07-07：归档后 `openspec validate --all` 通过，129 passed、0 failed。
- 2026-07-07：归档后 `node --test tools/dataspec-status-check.test.mjs` 通过，8 pass、0 fail。
- 2026-07-07：归档后 `node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，active change warning 从 24 降为 23；`summary.issueCodes[]` 汇总 `OPENSPEC_ACTIVE_CHANGE_PRESENT` count=23、severity=warning。
- 2026-07-07：归档后 `git diff --check` 退出码 0；PowerShell 输出仅包含 Git 的 CRLF 工作区提示。
- 2026-07-07：独立只读子 agent `019f3a98-182b-7c52-aaff-69fcd1852ff6`（Singer）完成归档 diff 复评并已关闭；结论 Ready，无 Critical/Important。
