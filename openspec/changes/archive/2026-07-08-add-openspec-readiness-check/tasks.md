## 1. OpenSpec artifacts

- [x] 1.1 确认 change id `add-openspec-readiness-check` 与 capability `openspec-readiness-check`。
- [x] 1.2 将 TODO 草稿改成可实施的 proposal/design/spec/tasks，明确只读边界、输出字段和验收方式。
- [x] 1.3 运行 `openspec validate add-openspec-readiness-check --strict`，确认 artifacts 有效。

## 2. 测试先行

- [x] 2.1 新增 `tools/dataspec-openspec-readiness.test.mjs`，先覆盖 ready change JSON 输出字段、分数等级、affectedSpecs 和 validationPlan。
- [x] 2.2 新增 incomplete change 失败测试，覆盖缺验收/边界/验证命令时的 `missingFacts`、降分和 `humanQuestions`。
- [x] 2.3 新增参数和安全边界测试，覆盖缺 change、unsafe id、archive change 和低分仍返回 0。

## 3. 实现

- [x] 3.1 新增 `tools/dataspec-openspec-readiness.mjs`，实现参数解析、只读 artifact 扫描、评分、JSON/text 输出和安全诊断。
- [x] 3.2 复用项目现有 tools 风格，公共导出函数补充 TSDoc，便于测试直接调用。
- [x] 3.3 更新 `README.md` 开发验证说明和 `TODO.md` P6-177 状态，记录第一版能力、边界和验证证据。

## 4. 验证与评审

- [x] 4.1 运行 `node --test tools/dataspec-openspec-readiness.test.mjs`。
- [x] 4.2 运行 `openspec validate add-openspec-readiness-check --strict`、`node tools/dataspec-openspec-readiness.mjs --change add-openspec-readiness-check --format json`、`node tools/dataspec-status-check.mjs --format json` 和 `git diff --check`。
- [x] 4.3 因新增 AI/CLI 可读本地工具，启动独立子 agent 只读评审并关闭 agent；修复 findings 或记录技术理由。
- [x] 4.4 在本文件记录 `Verification Evidence`，包含测试、OpenSpec、状态检查、评审和剩余风险。

## Verification Evidence

- TDD 红灯：`node --test tools/dataspec-openspec-readiness.test.mjs` 先因 `ERR_MODULE_NOT_FOUND` 失败，确认测试覆盖新工具缺失。
- 评审回归红灯：补充空 spec delta、裸 `TODO`、validationPlan 脱敏、空 `Impact`、archive 精确匹配 5 个用例后，`node --test tools/dataspec-openspec-readiness.test.mjs` 出现 5 个预期失败，确认覆盖独立评审 findings。
- 独立子 agent 评审：`019f3d97-f09d-7872-808e-fa3d0d11ffa8`（Euler the 2nd）只读评审本 change，发现 4 个 Important 与 1 个 Minor；已修复有效 spec delta 判定、裸 TODO/TBD 占位识别、validationPlan 脱敏、空 Impact 判定和 archive 精确匹配，并已关闭该 agent。
- `node --test tools/dataspec-openspec-readiness.test.mjs`：10 pass。
- `node --test tools/dataspec-verify-advisor.test.mjs`：30 pass。
- `node --test tools/*.test.mjs`：358 pass，2 skipped（当前平台无法创建部分 symlink）。
- `openspec validate add-openspec-readiness-check --strict`：valid。
- `node tools/dataspec-openspec-readiness.mjs --change add-openspec-readiness-check --format json`：`readinessScore=100`，`readinessLevel=READY`，`missingFacts=[]`。
- `node tools/dataspec-status-check.mjs --format json`：status warn；唯一 warning 为本 change 仍处于 active，归档后需复跑确认清空。
- `git diff --check`：通过，仅 Git LF/CRLF 提示。
- 归档后 `node --test tools/*.test.mjs`：358 pass，2 skipped（当前平台无法创建部分 symlink）。
- 归档后 `openspec validate --all`：120 passed，0 failed，包含主规格 `openspec-readiness-check`。
- 归档后 `openspec list --json`：`changes=[]`，确认无 active change。
- 归档后 `node tools/dataspec-status-check.mjs --format json`：status pass，0 issues。
- 归档后 `git diff --check`：通过，仅 Git LF/CRLF 提示。
- 剩余风险：readiness 仍是静态启发式检查，只辅助开工前自检；不执行验证命令，也不替代人工判断或项目评审门禁。
