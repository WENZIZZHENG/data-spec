## 1. OpenSpec Artifacts

- [x] 1.1 完成 proposal、design 和 spec delta，明确 OpenSpec 验证建议的动态收窄规则。
- [x] 1.2 运行 `openspec validate prefer-openspec-change-strict-validation --strict`。

## 2. Test-First Coverage

- [x] 2.1 在 `tools/dataspec-verify-advisor.test.mjs` 增加红灯用例，覆盖单 active change 推荐 `openspec validate <change-id> --strict`。
- [x] 2.2 覆盖多个 active change、主规格和 archive 路径回退 `openspec validate --all`。
- [x] 2.3 覆盖 CLI `--changed --format json` 输出使用动态 OpenSpec command。

## 3. Implementation

- [x] 3.1 在 `buildValidationAdvice()` 中基于 normalized paths 调整 `openspec-validate` command/reason。
- [x] 3.2 保持 `openspec-validate` id、category、cwd、summary、nextActions 和 diff-check 推荐语义不变。
- [x] 3.3 检查是否需要同步 README 或 AI 契约文档。

## 4. Verification And Review

- [x] 4.1 运行 verify-advisor 目标单测和 CLI changed/json 抽样。
- [x] 4.2 运行 OpenSpec strict、tools 相关测试、diff check 和敏感词扫描。
- [x] 4.3 启动独立只读子 agent 评审并关闭，处理 findings。
- [x] 4.4 记录 Verification Evidence，满足 commit 前门禁后创建本地 commit。

## Verification Evidence

- 2026-07-07：`openspec validate prefer-openspec-change-strict-validation --strict` 通过。
- 2026-07-07：`node --test tools/dataspec-verify-advisor.test.mjs` 红灯符合预期，单 active change 和 CLI changed/json 用例仍返回 `openspec validate --all`。
- 2026-07-07：实现后 `node --test tools/dataspec-verify-advisor.test.mjs` 通过，13 pass、0 fail。
- 2026-07-07：`node tools/dataspec-verify-advisor.mjs --path openspec/changes/prefer-openspec-change-strict-validation/tasks.md --format json` 推荐 `openspec validate prefer-openspec-change-strict-validation --strict`。
- 2026-07-07：`node tools/dataspec-verify-advisor.mjs --path openspec/changes/add-a/tasks.md --path openspec/changes/add-b/tasks.md --format json` 推荐 `openspec validate --all`。
- 2026-07-07：`openspec validate prefer-openspec-change-strict-validation --strict` 再次通过。
- 2026-07-07：`node --test tools/*.test.mjs` 通过，250 pass、2 skip（Windows symlink 权限）、0 fail。
- 2026-07-07：`git diff --check` 通过，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：敏感词扫描新增 diff 行无命中。
- 2026-07-07：独立只读评审子 agent `019f3a80-9898-74b0-a8ec-44386f69c0f5`（Jason）发现 1 个 Important：change id 来自路径并拼入建议命令，需 allowlist 防止 shell 控制符注入；已调用 `close_agent` 关闭。
- 2026-07-07：补充 unsafe change id 回退 `openspec validate --all` 和 active change 目录路径识别测试；`node --test tools/dataspec-verify-advisor.test.mjs` 通过，15 pass、0 fail。
- 2026-07-07：修复后 `openspec validate prefer-openspec-change-strict-validation --strict` 再次通过。
- 2026-07-07：修复后 `node --test tools/*.test.mjs` 通过，252 pass、2 skip（Windows symlink 权限）、0 fail。
- 2026-07-07：修复后 `git diff --check` 通过，仅输出 Windows LF/CRLF 提示；敏感词扫描新增 diff 行无命中。
- 2026-07-07：复评子 agent `019f3a85-2d9e-72b2-896b-4c36f0cf6fe0`（Einstein）确认上一个 Important 已修复，无 Critical / Important / Minor，结论 Ready to commit；已调用 `close_agent` 关闭。
- 2026-07-07：commit 前 `git status --short`、`git diff --cached --check`、`git diff --cached --stat`、`git diff --cached --name-only`、staged diff 浏览和 staged 敏感词扫描均完成；无空白错误或敏感词命中。
- 2026-07-07：归档时已同步 `openspec/specs/ai-contract-fixtures/spec.md`，并移动到 `openspec/changes/archive/2026-07-07-prefer-openspec-change-strict-validation/`。
- 2026-07-07：归档后 `openspec validate --all` 通过，125 passed、0 failed。
- 2026-07-07：归档后 `node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs` 通过，23 pass、0 fail。
- 2026-07-07：归档后 `node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，active change warning 从 23 降为 19；第三条 `nextActions[]` 为 `当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=19,severity=warning)`。
- 2026-07-07：归档后 `git diff --check` 退出码 0；PowerShell 输出仅包含 Git 的 CRLF 工作区提示。
- 2026-07-07：独立只读子 agent `019f3aaa-2d98-7b01-8042-2e5e432ffeb5`（Pauli）完成 4 个归档 diff 复评并已关闭；结论 Ready，无 Critical/Important/Minor。
