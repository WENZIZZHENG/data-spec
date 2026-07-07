## 1. OpenSpec Artifacts

- [x] 1.1 完成 proposal、design 和 spec delta，明确本次只增强 `nextActions[]` 的 issue code 摘要。
- [x] 1.2 运行 `openspec validate show-status-check-next-action-code-counts --strict`。

## 2. Test-First Coverage

- [x] 2.1 在 `tools/dataspec-status-check.test.mjs` 增加红灯用例，覆盖 warning-only next action 的 `count` 和 `severity`。
- [x] 2.2 覆盖 error next action 的 `count` 和 `severity`。
- [x] 2.3 确认既有前两条 `nextActions[]`、`summary.issueCodes[]`、`issues[]` 和 `checks[]` 语义不变。

## 3. Implementation

- [x] 3.1 让 `buildNextActions()` 复用 issue code 聚合结果生成 `code(count=N,severity=level)` 摘要。
- [x] 3.2 保持 `status`、退出码、`summary`、`checks[]`、`issues[]` 和前两条行动建议语义不变。
- [x] 3.3 更新 `docs/ai-contracts.md`，记录 status-check next action 的兼容增强。

## 4. Verification And Review

- [x] 4.1 运行 status-check 目标单测和真实仓库 status-check。
- [x] 4.2 运行 OpenSpec strict、tools 相关测试、diff check 和敏感字段扫描。
- [x] 4.3 启动独立只读子 agent 评审并关闭，处理 findings。
- [x] 4.4 记录 Verification Evidence，满足 commit 前门禁后创建本地 commit。

## Verification Evidence

- 2026-07-07：`openspec validate show-status-check-next-action-code-counts --strict` 通过。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs` 红灯符合预期，3 个新增断言因 next action 仍为纯 code 列表失败。
- 2026-07-07：实现后 `node --test tools/dataspec-status-check.test.mjs` 通过，8 pass、0 fail。
- 2026-07-07：收口复跑 `openspec validate show-status-check-next-action-code-counts --strict` 通过。
- 2026-07-07：`node --test tools/*.test.mjs` 通过，254 tests、252 pass、2 skip、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`；第三条 `nextActions[]` 为 `当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=24,severity=warning)`。
- 2026-07-07：`git diff --check` 退出码 0；PowerShell 输出仅包含 Git 的 CRLF 工作区提示。
- 2026-07-07：新增 diff 常见敏感字段扫描无命中。
- 2026-07-07：独立只读子 agent `019f3aa0-4697-7e62-ad08-4d83a765617b`（Herschel）完成评审并已关闭；结论 Ready，无 Critical。Important 提醒 OpenSpec change 目录为 untracked，提交前已纳入精确 stage；Minor 建议混合 severity 直接断言，因当前可观察入口不会自然生成同 code 同时 warning/error，且本次主要风险已由 warning/error 分场景覆盖，暂不扩大测试接口。
