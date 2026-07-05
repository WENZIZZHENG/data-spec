## 1. 契约与测试先行

- [x] 1.1 补后端 capability catalog 和写保护失败路径测试，先覆盖 `safety` 字段、缺少幂等 key 诊断和脱敏边界。
- [x] 1.2 补 CLI 契约测试，覆盖 `capability check/show` 的 safety 校验与展示、后端 safety error 透传和 stderr 脱敏。
- [x] 1.3 补 MCP 契约测试，覆盖 `tools/list` safety metadata、capability resource 透传和 JSON-RPC safety error。
- [x] 1.4 补前端最小展示/helper 测试，覆盖高风险 dry-run 摘要字段和 nextActions 展示。

## 2. 后端实现

- [x] 2.1 新增 AI 写入安全 metadata 公共模型，补 Javadoc / schema description，说明字段语义、边界和脱敏要求。
- [x] 2.2 扩展 `AiCapabilityEntry` 和 `AiCapabilityCatalogServiceImpl`，为所有内置 capability 输出兼容新增的 `safety` 对象，并保留旧字段。
- [x] 2.3 扩展幂等写保护或相关执行入口，使声明为必须幂等 key 的高风险写入缺 key 时返回结构化、可恢复、已脱敏的 safety diagnostic。
- [x] 2.4 自查后端公共表面注释、错误处理、权限边界、脱敏和无关改动。

## 3. CLI 与 MCP 实现

- [x] 3.1 更新 CLI capability 检查与文本展示逻辑，校验 safety 必填字段并保留兼容 JSON 输出。
- [x] 3.2 更新 CLI 错误透传和脱敏，确保 safety diagnostic 可被 AI 解析且不输出 secret。
- [x] 3.3 更新 MCP `tools/list`、prompts 或本地 tool metadata，暴露 safety 信息或 safety reference。
- [x] 3.4 更新 MCP 参数校验和错误透传，确保缺少安全参数时返回 `error.data.dataspecError`。

## 4. 前端与文档

- [x] 4.1 新增或更新前端 safety/dry-run 展示 helper，并在高风险批量写入确认入口展示 dry-run 摘要和 nextActions。
- [x] 4.2 更新 README / AI 契约文档中直接受影响的 CLI/MCP/API 安全边界说明。
- [x] 4.3 更新 TODO.md，将 P6-69 标记为已完成并记录落地范围。
- [x] 4.4 同步 OpenSpec artifacts 与实际实现，避免 proposal/design/spec/tasks 偏离代码。

## 5. 验证、评审与提交

- [x] 5.1 运行 OpenSpec strict 校验：`openspec validate add-ai-write-safety-protocol --strict`。
- [x] 5.2 运行受影响验证：后端 `mvn test`、CLI/MCP `node --test tools/*.test.mjs`、前端 `pnpm test` / `pnpm build`。
- [x] 5.3 运行通用检查：`git diff --check`，暂存后运行 `git diff --cached --check`、`git diff --cached --stat` 和敏感字段扫描。
- [x] 5.4 启动独立子 agent 做强制代码评审，记录 agent id、用途、findings 处理结果并关闭子 agent。
- [x] 5.5 回填 Verification Evidence，列出关键命令、结果、评审证据和未覆盖风险。
- [x] 5.6 满足门禁后按最新 `AGENTS.md` 创建本地 commit，commit message 使用轻量 Conventional Commits，正文说明影响范围、兼容策略和关键验证证据。

## Verification Evidence

- `openspec validate add-ai-write-safety-protocol --strict`：通过，change `add-ai-write-safety-protocol` valid。
- `openspec validate --all`：通过，101 items passed，0 failed。
- `mvn -q "-Dtest=AiCapabilityCatalogServiceImplTest,WriteGuardServiceTest,AiCapabilityCatalogControllerTest" test`（`dataspec-server`）：通过。
- `mvn -q "-Dtest=ProjectBackupServiceImplTest,ReverseImportServiceTest,DatabaseReverseImportServiceTest,WriteGuardServiceTest" test`（`dataspec-server`）：通过，用于验证强制 `Idempotency-Key` 后旧成功路径已显式传测试 key。
- `mvn -q "-Dtest=ReverseImportServiceTest,DatabaseReverseImportServiceTest,AiCapabilityCatalogServiceImplTest" test`（`dataspec-server`）：通过，用于验证签名 `dryRunToken`、候选 hash 碰撞防护和 capability safety 对齐。
- `mvn -q "-Dtest=ProjectBackupServiceImplTest,GlobalExceptionHandlerTest,AiCapabilityCatalogServiceImplTest,ErrorCatalogTest,SensitiveDataSanitizerTest" test`（`dataspec-server`）：通过，用于验证项目恢复 token 不持久化、重名项目名稳定、全局异常日志脱敏和公共诊断字段说明。
- `mvn test`（`dataspec-server`）：通过，454 tests，0 failures，0 errors；存在既有 SQL parser warning、性能 baseline slow warning 和 ByteBuddy Java agent warning。
- `node --test tools\dataspec-cli.test.mjs tools\dataspec-mcp.test.mjs`：通过，128 tests，0 failed。
- `node --test tools\dataspec-mcp.test.mjs`：通过，36 tests，0 failed，用于验证 MCP 非 Bearer Authorization、DSN、JDBC URL 和 safety diagnostic 脱敏。
- `node --test tools\*.test.mjs`：通过，161 tests，0 failed。
- `node --test tests\standardReusePackDisplay.test.ts`（`dataspec-web`）：通过，4 tests，0 failed。
- `node --test tests\standardReusePackDisplay.test.ts tests\frontendSmoke.test.ts`（`dataspec-web`）：通过，32 tests，0 failed。
- `pnpm test`（`dataspec-web`）：通过，136 tests，0 failed。
- `pnpm build`（`dataspec-web`）：通过；存在既有 Rolldown/Vite `INVALID_ANNOTATION` 和 chunk size warning。
- `git diff --check`：通过，无 whitespace error；仅提示 Windows 下若干文件 `LF will be replaced by CRLF`。
- `git diff --cached --check`：通过，无 whitespace error。
- `git diff --cached --stat`：通过，54 files changed，1985 insertions(+)，80 deletions(-)。
- 敏感字段扫描：`git diff --cached -G"password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn" --name-only` 和 `git diff --cached | Select-String ...` 已执行；命中均为 README/OpenSpec/字段说明、脱敏正则、dummy 测试值或断言，未发现真实 token、密码、JDBC URL、DSN 或 Authorization。
- 强制独立评审：曾尝试启动子 agent `019f30c7-659e-7b80-9858-e5224ef367a0`，用途为 P6-69 SDD full / CLI-MCP-AI 协议与安全边界只读评审；该 agent 因 workspace credits 返回失败，随后 `close_agent` 返回 `not found`，推断失败 agent 已被系统清理。
- 强制独立评审：子 agent `019f30d0-24c9-7783-860a-26f22d842b78` 完成 P6-69 只读评审并已关闭；findings 已处理，补齐项目恢复/反向导入 dry-run 强校验和前端幂等 key 调用。
- 强制独立评审：子 agent `019f30eb-db89-7a80-ae2b-f5e1c7374cec` 完成 P6-69 安全协议复评并已关闭；findings 已处理，改为签名 `dryRunToken`，收窄 `requiresDryRun=true` 语义，修正 `ai-batch-sql-lint` CLI 表述并补 DSN 脱敏。
- 强制独立评审：子 agent `019f30fa-0865-76e0-945e-a33d118a9201` 完成二次复评并已关闭；findings 已处理，反向导入候选证据 hash 改为 JSON array 序列化，标准复用能力不声明后端强 dry-run，CLI/MCP 裸 DSN 脱敏已补测试。
- 强制独立评审：子 agent `019f3107-b5fb-7b91-a385-1ee7d3bc443f` 完成提交前复评并已关闭；findings 已处理，项目恢复新项目重名兜底改为确定性命名，apply 结果/恢复记录不持久化 `dryRunToken`，MCP 非 Bearer Authorization 脱敏、全局异常日志脱敏、标准复用包条件展示 `requiresDryRun` 标签，并补公共契约字段 `@Schema` 说明。
- 强制独立评审：子 agent `019f3119-8929-7532-9178-74cfdf641c65` 完成最终 staged diff 只读复评并已关闭；结论为无阻塞 finding，检查范围覆盖 54 个 staged files、dryRunToken 签名与绑定、项目恢复记录不暴露 token、确定性重名兜底、`requiresDryRun` 对齐、标准复用包 UI、CLI/MCP/API 与日志脱敏、公共字段说明、README/TODO/OpenSpec/tasks 和验证证据。
- 提交准备：本地 commit message 使用 `feat: 增加 AI 写入安全 dry-run 协议`，正文将说明影响范围、兼容策略和关键验证证据；最终 commit hash 由提交后 `git rev-parse --short HEAD` 和最终回复记录。
