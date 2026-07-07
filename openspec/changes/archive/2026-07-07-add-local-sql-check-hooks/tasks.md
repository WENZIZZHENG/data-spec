## 1. OpenSpec 与范围确认

- [x] 1.1 校验 proposal、design 和 spec delta 与 P6-91 范围一致：只做本地 hook/IDE 模板和 CLI 输出，不改后端 API、不自动 archive。
- [x] 1.2 运行 `openspec validate add-local-sql-check-hooks --strict`，修复 artifacts 格式或契约问题。

## 2. CLI 本地 hook 安装

- [x] 2.1 先补失败测试：`install-hook` 写入 DataSpec 管理 pre-commit hook、可选 VS Code task/problem matcher、拒绝覆盖非托管 hook、非 git 仓库诊断和生成文件不含敏感值。
- [x] 2.2 实现 `install-hook` 命令、help 文本、稳定 JSON/text 输出、托管 marker、hook chmod 和安全诊断。
- [x] 2.3 补充 `lint-changed --format text` 失败测试，覆盖 issue 行格式、无 SQL 变更诊断和退出码。
- [x] 2.4 实现 `lint-changed --format text`，确保 file/line/column/severity/rule/message/suggestion 可被 IDE Problem Matcher 解析，并复用既有 JSON 语义。

## 3. 契约 fixture 与文档

- [x] 3.1 更新 CLI/MCP contract fixture 和 fixture 校验测试，覆盖 `install-hook` 输出 shape、安全 metadata、unsafe 示例拒绝和 recommended next actions。
- [x] 3.2 更新 README，记录 `install-hook`、VS Code task/problem matcher、`lint-changed --format text`、安全边界和不替代 CI 的说明。
- [x] 3.3 更新 TODO，将 P6-91 标记为已完成第一版并记录已完成能力、限制和验收证据。

## 4. 验证、评审与提交

- [x] 4.1 运行相关验证：定点 CLI 测试、CLI/MCP fixture 测试、`node --test tools/*.test.mjs`、OpenSpec strict/all、`git diff --check`。
- [x] 4.2 在本文件追加 `Verification Evidence`，记录命令结果、敏感词扫描、子 agent 信息和未覆盖风险。
- [x] 4.3 启动独立子 agent 做强制只读代码评审，处理或记录 findings，并关闭子 agent。
- [x] 4.4 按项目 Git 规则核对 staged diff、敏感词扫描并创建本地 commit；不主动 push，不自动 archive OpenSpec。

## Verification Evidence

- 时间：2026-07-07。
- RED 验证：
  - `node --test --test-name-pattern "install-hook|lint-changed text" tools/dataspec-cli.test.mjs`：初始失败，`install-hook` 未识别、`lint-changed --format text` 被拒绝。
  - `node --test --test-name-pattern "install-hook|bundled CLI/MCP contract fixtures" tools/dataspec-cli-mcp-contract-check.test.mjs`：初始失败，缺少 `install-hook` fixture。
  - 评审修复阶段新增红测：VS Code task 未内联 matcher、linked worktree hook path 返回 2、`.git` 目录链接未被拒绝。
- CLI / tools 验证：
  - `node --test --test-name-pattern "install-hook|lint-changed text" tools/dataspec-cli.test.mjs`：8 tests，7 passed / 1 skipped；跳过项为当前 Windows 权限无法创建文件级 symlink，`.git` 目录链接防护测试已通过。
  - `node --test --test-name-pattern "install-hook|bundled CLI/MCP contract fixtures" tools/dataspec-cli-mcp-contract-check.test.mjs`：2 tests passed。
  - `node --test tools/*.test.mjs`：232 tests，230 passed / 2 skipped，0 failed；跳过项均为当前平台 symlink 权限限制。
- OpenSpec / 通用检查：
  - `openspec validate add-local-sql-check-hooks --strict`：Change valid。
  - `openspec validate --all`：112 passed，0 failed。
  - `git diff --check`：exit 0；仅 Windows CRLF 工作区提示。
  - `node tools/dataspec-status-check.mjs --format json`：status=warn，0 errors；仅提示 active changes 存在，本次按约定保留 active 不 archive。
- 敏感词扫描：
  - `git diff --name-only | Select-String 'password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn'`：0 个文件名命中。
  - `git diff -- . | Select-String 'password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn'`：19 个文本命中，人工复核均为 README/TODO 安全说明、CLI 参数名、脱敏函数名、测试断言或 `tokenBudget` 既有字段名；未发现真实凭据、完整 JDBC URL、DSN、Authorization header 或 API key。
- 独立代码评审：
  - 子 agent：`019f391a-6251-7650-9c8a-118987ff1f16`，用途：P6-91 首轮只读代码评审；关闭结果：已调用 `close_agent`，previous_status 为 completed。
  - 首轮 findings：P1 VS Code task 未内联 matcher；P1 托管写入未防 symlink；P2 linked worktree hook 路径错误；P3 proposal 与实现对 VS Code 输出格式不一致。
  - 处理结果：已把 matcher 内联到 `tasks.json`，保留独立 matcher 示例；新增 symlink/越界 root 检查；hook 路径改用 `git rev-parse --git-path hooks/pre-commit`；proposal 改为 `lint-changed --format text`；补对应测试。
  - 子 agent：`019f391f-a8dc-7a81-8849-ada507bb81e4`，用途：P6-91 修复后只读复评；关闭结果：已调用 `close_agent`，previous_status 为 completed。
  - 复评 finding：P1 `allowedRootDir` 本身未检查 symlink，可能让 `.git` 目录链接写到仓库外。
  - 处理结果：`validateManagedArtifactTarget` 先检查 allowed root 的 `lstat().isSymbolicLink()`；新增 `.git` 目录链接测试，确认不写外部 hooks。
- 结构化自审：
  - 需求覆盖：`install-hook`、可选 VS Code task/problem matcher、`lint-changed --format text`、fixture、README/TODO 和 OpenSpec 均已同步。
  - 安全边界：不覆盖非 DataSpec marker 管理文件；拒绝目标、父目录和允许根目录 symlink；linked worktree hook 路径通过 git 解析；生成文件不写凭据。
  - 测试覆盖：安装成功、非托管 hook 拒绝、非 git 仓库、文件级 symlink 平台用例、`.git` 目录链接、linked worktree、text issue line、text no-SQL、fixture required command 均覆盖。
  - 无关改动：未修改后端/前端，未归档 OpenSpec，未 push。
- 未覆盖风险：
  - 写入前校验与实际写入之间仍存在本地 TOCTOU 窗口；当前按个人/小团队本地工具定位接受，后续高安全场景可再引入更原子化文件创建策略。
  - VS Code 第一版只生成 task/problem matcher 示例，不实现保存即自动运行的插件或编辑器扩展。
  - 当前平台无法创建文件级 symlink，因此文件级 symlink 测试在本机跳过；实现路径仍对目标文件、父目录和 root symlink 均做拒绝。

## Archive Verification Evidence

- 2026-07-07：执行 `openspec archive add-local-sql-check-hooks --yes`，同步 `cli-mcp-contract-fixtures`、`dataspec-cli`、`local-sql-check-hooks` 主规格，并归档到 `openspec/changes/archive/2026-07-07-add-local-sql-check-hooks/`。
- 2026-07-07：补齐新建主规格 `openspec/specs/local-sql-check-hooks/spec.md` 的 Purpose，确认默认占位文本扫描无命中。
- 2026-07-07：`openspec validate --all` 通过，118 passed、0 failed。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，44 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，active change warning 从 8 降至 2；第三条 next action 为 `当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=2,severity=warning)`。
- 2026-07-07：`git diff --check` 退出码 0，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：独立只读复评子 agent `019f3ac9-9655-7961-9767-c75277266b0a`（Noether）复评 staged archive diff，结论 Ready，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。
