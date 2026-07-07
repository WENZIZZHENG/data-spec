## 1. OpenSpec 与范围确认

- [x] 1.1 校验 proposal、design 和 spec delta 与 P6-92 范围一致：只读合成样例包、后端 API、CLI/fixture、测试与文档；不写数据库、不调用外部 LLM、不自动 archive。
- [x] 1.2 运行 `openspec validate add-synthetic-standard-examples --strict`，修复 artifacts 格式或契约问题。

## 2. 后端合成样例生成器

- [x] 2.1 先补失败测试：四类场景生成稳定包、`specHash` 确定性、标准字段/模板摘要变化会改变 hash、fallback diagnostics、安全 metadata 和敏感值脱敏。
- [x] 2.2 实现 `syntheticexample` model/service/controller，新增 `GET /api/synthetic-examples/generate`，读取标准字段和模板摘要并生成 good/bad SQL、DDL preview 输入、字段推荐问题、标准 Q&A 与预期诊断。
- [x] 2.3 补充 controller 测试，覆盖成功响应、非法 scenario 参数、默认参数、响应字段说明和只读安全边界。
- [x] 2.4 补充或更新后端 fixture/golden 断言，确保 generated bad SQL 的 expectedDiagnostics 与 case id 对齐。

## 3. CLI 与契约 fixture

- [x] 3.1 先补失败测试：`synthetic-examples generate --format json` 调用后端并保留稳定字段，`--format text` 输出摘要，非法参数/服务错误返回 exit code 2 且脱敏。
- [x] 3.2 实现 CLI `synthetic-examples generate`、help 文本、参数校验、JSON/text 输出和请求错误诊断。
- [x] 3.3 更新 CLI/MCP contract fixture 和 fixture 校验测试，覆盖 `synthetic-examples generate` 输出 shape、安全 metadata、unsafe 示例拒绝和 recommended next actions。

## 4. 文档与 TODO

- [x] 4.1 更新 README，记录合成样例生成 API/CLI、场景、输出包字段、安全边界和不替代人工样例维护的说明。
- [x] 4.2 更新 TODO，将 P6-92 标记为已完成第一版，并记录已完成能力、限制和验收证据。

## 5. 验证、评审与提交

- [x] 5.1 运行相关验证：后端定点 Maven 测试、CLI 定点测试、CLI/MCP fixture 测试、`node --test tools/*.test.mjs`、OpenSpec strict/all、`git diff --check`。
- [x] 5.2 在本文件追加 `Verification Evidence`，记录命令结果、敏感词扫描、子 agent 信息、结构化自审和未覆盖风险。
- [x] 5.3 启动独立子 agent 做强制只读代码评审，处理或记录 findings，并关闭子 agent。
- [x] 5.4 按项目 Git 规则核对 staged diff、敏感词扫描并创建本地 commit；不主动 push，不自动 archive OpenSpec。

## Verification Evidence

### 2026-07-07 P6-92 实现验证

- TDD RED：在 `dataspec-server` 运行 `mvn "-Dtest=SyntheticStandardExampleServiceImplTest,SyntheticStandardExampleControllerTest" test`，新增 `generate_changesSpecHashWhenCodeSetReferenceChanges` 先失败，失败原因为 `codeSetReferenceCount` 从 0 变 1 但 `specHash` 未变化。
- TDD GREEN：修复后在 `dataspec-server` 运行 `mvn "-Dtest=SyntheticStandardExampleServiceImplTest,SyntheticStandardExampleControllerTest" test`，8 tests passed。
- 后端定点验证：在 `dataspec-server` 运行 `mvn "-Dtest=SyntheticStandardExampleServiceImplTest,SyntheticStandardExampleControllerTest,SyntheticStandardExampleContractFixtureTest" test`，9 tests passed；本地 Maven 仓库仍有既有 `javax.annotation-api` / `jvnet-parent` POM warning，但 build success。
- CLI 定点验证：`node --test --test-name-pattern "synthetic-examples generate" tools/dataspec-cli.test.mjs`，3 passed。
- CLI/MCP fixture 验证：`node --test --test-name-pattern "synthetic examples readonly|bundled CLI/MCP contract fixtures" tools/dataspec-cli-mcp-contract-check.test.mjs`，2 passed。
- tools 全量验证：`node --test tools/*.test.mjs`，236 tests，234 passed / 2 skipped / 0 failed；2 个 skipped 为 Windows symlink 权限相关既有跳过。
- OpenSpec 验证：`openspec validate add-synthetic-standard-examples --strict`，valid；`openspec validate --all`，113 passed / 0 failed。
- 通用空白检查：`git diff --check`，exit 0，仅报告 Windows 工作副本 LF 将被 CRLF 替换的提示。

### 2026-07-07 强制独立评审

- 子 agent `019f393d-c163-7301-8587-e82e6816c557`，用途：P6-92 commit 前只读代码评审；已调用 `close_agent` 关闭。
- 第一轮评审结论：Ready to commit? With fixes。Critical 为本文件尚未补 Verification Evidence；Important 为 `specHash` 未纳入 `sourceSummary/codeSetId` 摘要、Controller 测试未验证真实路由；Minor 为 CLI/MCP fixture 的 `goodSql/badSql` successExample 为空。
- 处理结果：补充 `codeSetId` hash 回归测试并将 `sourceSummary`、脱敏 `standardFieldSummary` 纳入 hash payload；补充 MockMvc 路由、默认参数和错误脱敏测试；将 CLI/MCP fixture successExample 改为最小非空 good/bad SQL 示例。
- 子 agent `019f3948-2b80-7251-8e0c-613328579aab`，用途：P6-92 修复后最终只读复评；已调用 `close_agent` 关闭。
- 第二轮复评结论：代码层面无 Critical / Important / Minor 阻断项；Ready to commit? With fixes，仅剩本文件证据记录、staged diff、敏感词扫描和本地 commit 门禁。

### 2026-07-07 结构化自审

- 需求覆盖：已覆盖只读 API、CLI JSON/text 输出、四类场景、fallback diagnostics、good/bad SQL、DDL preview 输入、字段推荐问题、标准 Q&A、expected diagnostics、safety 和 nextActions。
- 边界与安全：生成流程只读取标准字段和模板摘要，不写库、不导入、不调用外部 LLM、不生成真实业务行；敏感值通过 `SensitiveDataSanitizer` 脱敏，并由服务端和 CLI 测试覆盖。
- 契约一致性：OpenSpec、README、TODO、后端 fixture、CLI/MCP fixture 和 CLI help 均记录 `synthetic-examples generate` 只读边界与稳定输出字段。
- 未覆盖风险：未新增前端页面；后端 contract fixture 未额外把 `diagnostics` 加入可选复评建议中的 requiredTopLevelFields，但 service、OpenSpec、README 和 CLI/MCP outputShape 已覆盖该字段，第一版不作为阻断。

### 2026-07-07 Git 门禁

- stage 范围：使用显式 pathspec stage README/TODO、后端 `syntheticexample` 源码与测试、CLI/tools fixture、OpenSpec change artifacts；未 stage 无关文件。
- `git status --short`：仅本次 P6-92 相关文件处于 staged 状态。
- `git diff --cached --check`：exit 0。
- `git diff --cached --stat`：32 files changed，1716 insertions(+)，10 deletions(-)。
- staged 文件名敏感词扫描：`password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn` 无命中。
- staged diff 敏感词扫描：命中均为 README/CLI 参数名、OpenSpec 安全边界说明、脱敏函数名、测试假值 `raw-secret`、`Authorization` 脱敏用例或 `dataspec-token` 字段说明；未发现真实 password、token、Authorization、API key、JDBC URL 或 DSN。
- 本地 commit：门禁通过后创建；不 push，不自动 archive OpenSpec。

## Archive Verification Evidence

- 2026-07-07：执行 `openspec archive add-synthetic-standard-examples --yes`，同步 `ai-contract-fixtures`、`cli-mcp-contract-fixtures`、`core-golden-fixtures`、`dataspec-cli`、`standard-usage-examples`、`synthetic-standard-examples` 主规格，并归档到 `openspec/changes/archive/2026-07-07-add-synthetic-standard-examples/`。
- 2026-07-07：补齐新建主规格 `openspec/specs/synthetic-standard-examples/spec.md` 的 Purpose，确认默认占位文本扫描无命中。
- 2026-07-07：`openspec validate --all` 通过，118 passed、0 failed。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，44 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，active change warning 从 8 降至 2；第三条 next action 为 `当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=2,severity=warning)`。
- 2026-07-07：`git diff --check` 退出码 0，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：独立只读复评子 agent `019f3ac9-9655-7961-9767-c75277266b0a`（Noether）复评 staged archive diff，结论 Ready，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。
