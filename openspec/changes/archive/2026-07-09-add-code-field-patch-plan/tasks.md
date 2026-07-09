## 1. OpenSpec 与测试先行

- [x] 1.1 确认 `proposal.md`、`design.md` 和三份 delta spec 覆盖 P6-179 的 CLI、本地工具 API、契约 fixture、安全边界和非目标。
- [x] 1.2 运行 `openspec validate add-code-field-patch-plan --strict`，确认 artifacts 可校验。
- [x] 1.3 先为 `code-patch plan` JSON、Markdown、参数失败、默认路径缺失、敏感信息脱敏和 fixture 覆盖编写失败测试。
- [x] 1.4 运行定点测试并确认新增测试因命令或模块缺失而红灯。

## 2. Patch Plan 核心实现

- [x] 2.1 新增 `tools/dataspec-code-patch-plan.mjs`，导出 `buildCodeFieldPatchPlan()`、Markdown formatter 和必要的公共字段说明注释。
- [x] 2.2 复用 `buildCodeFieldReferenceIndex()`，把字段重命名、类型变化和枚举变化转换为 `candidateEdits[]`、`manualSteps[]`、`verificationCommands[]`、`rollbackHint`、`safety`、`diagnostics[]` 和 `nextActions[]`。
- [x] 2.3 为 rename 候选生成只读 `dryRunDiff`，对类型和枚举变化输出人工确认步骤，不写业务文件、不提供 apply 入口。
- [x] 2.4 确保 snippets、diff、diagnostics、Markdown 和命令模板脱敏，不输出 token、password、Authorization、API key、完整 JDBC URL、DSN 或连接串。

## 3. CLI 与契约 fixture

- [x] 3.1 在 `tools/dataspec-cli.mjs` 接入 `code-patch plan`，支持 `--field`、`--to-field`、`--from-type`、`--to-type`、`--enum-change`、`--alias`、`--path`、`--format json|markdown`。
- [x] 3.2 复用 `.dataspec/config.json` `defaultPaths` 和显式 `--path` 逻辑；无扫描路径时返回 `DATASPEC_DEFAULT_PATHS_MISSING`，不扫描全仓库。
- [x] 3.3 更新 `tools/fixtures/cli-mcp-contracts.json` 和 `tools/dataspec-cli-mcp-contract-check.mjs`，新增 `code-patch-plan` 必需入口与安全元数据校验。
- [x] 3.4 更新 CLI help 文案，说明 `code-patch plan` 只输出 dry-run 计划，不修改业务仓库。

## 4. 验证、评审与归档

- [x] 4.1 运行定点测试：`node --test --test-name-pattern "code-patch plan|contract fixture" tools/dataspec-cli.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs`。
- [x] 4.2 运行 tools 验证：`node --test tools/*.test.mjs` 和 `node tools/dataspec-cli-mcp-contract-check.mjs --format json`。
- [x] 4.3 运行 OpenSpec 验证：`openspec validate add-code-field-patch-plan --strict`。
- [x] 4.4 按安全规则运行 `git diff --check`，并在提交前执行 staged diff 和敏感词扫描。
- [x] 4.5 启动独立代码评审子 agent，记录 agent id、用途、结论和关闭状态；修复 Critical/Important findings 或说明技术理由。
- [x] 4.6 在本文件补充 `Verification Evidence`，记录关键命令、结果、评审证据和未覆盖风险。
- [x] 4.7 归档 `add-code-field-patch-plan`，运行 `openspec validate --all`，并更新 `TODO.md` 中 P6-179 状态与验证证据。
- [x] 4.8 满足门禁后按项目 Git 规则创建本地 commit，不主动 push。

## Verification Evidence

- OpenSpec artifacts：`openspec validate add-code-field-patch-plan --strict`，结果 `Change 'add-code-field-patch-plan' is valid`。
- TDD 红灯：`node --test --test-name-pattern "code-patch plan" tools/dataspec-cli.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs`，新增 5 个测试先因 `code-patch` 命令和 `code-patch-plan` fixture 缺失失败。
- TDD 评审修复红灯：`node --test --test-name-pattern "code-patch plan" tools/dataspec-cli.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs`，新增 JSON 深度脱敏、alias 重跑命令和 fixture shape 断言后结果 3 pass、3 fail，失败点与评审 findings 一致。
- 定点验证：`node --test --test-name-pattern "code-patch plan|contract fixture" tools/dataspec-cli.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs`，结果 6 pass、0 fail。
- Tools 验证：`node --test tools/*.test.mjs`，结果 370 tests，368 pass，2 skipped，0 fail；skip 均为当前平台 symlink 权限限制。
- 契约验证：`node tools/dataspec-cli-mcp-contract-check.mjs --format json`，结果 `ok=true`、`diagnostics=0`、`cliCommands=21`。
- Diff 检查：`git diff --check`，结果通过，仅提示若干工作区文件未来会 LF/CRLF 转换。
- 独立评审：只读评审子 agent `019f45e5-5ef6-7452-9d86-d538117d6574`，用途为 P6-179 CLI/工具/API 契约、安全边界和 OpenSpec 一致性评审；结论为 0 Critical、2 Important、1 Minor，Important/Minor 均已修复并用定点测试和 tools 验证覆盖；agent 已关闭，关闭前状态 completed。
- 归档验证：`openspec archive add-code-field-patch-plan --yes`，同步 `cli-mcp-contract-fixtures`、创建 `code-field-patch-plan`、更新 `dataspec-cli` 主规格，并归档到 `openspec/changes/archive/2026-07-09-add-code-field-patch-plan/`；`openspec validate --all` 结果 121 passed、0 failed。
- 文档同步：`TODO.md` 已把 P6-179 更新为已完成第一版并记录验证证据；`README.md` 已补充 `code-patch plan` CLI 能力和只读脱敏边界。
- 状态检查：`node tools/dataspec-status-check.mjs --format json` 结果 `status=pass`、0 issues；归档后 `openspec list --json` 结果 `changes=[]`。
- 提交前检查：`git diff --check` 结果通过，仅提示若干工作区文件未来会 LF/CRLF 转换；commit 前执行 staged diff、staged stat 和敏感词扫描。
- 未覆盖风险：第一版不实现服务端 HTTP API、不自动应用补丁、不保证识别动态 SQL 或运行时拼接字段；这些边界已在 proposal/design/spec 中记录。
