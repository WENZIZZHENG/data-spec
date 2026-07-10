## 1. OpenSpec 与实施准备

- [x] 1.1 创建 proposal/design/spec delta，覆盖 P6-165/P6-166 的稳定引用、后置校验、API/CLI/MCP/AI Context/Evidence 契约。
- [x] 1.2 运行 `openspec validate add-stable-standard-refs-and-ai-output-checks --strict`，读取 apply instructions 和所有 context files。

## 2. 标准对象稳定引用后端

- [x] 2.1 先补失败测试，覆盖字段 stableRef/canonicalRef、别名解析、废弃替代、未知/歧义/cross-project 和 secret-safe 结果。
- [x] 2.2 新增带 Javadoc/Schema 字段说明的引用请求、结果和枚举模型，以及 deterministic stableRef formatter。
- [x] 2.3 实现 project-scoped `StandardReferenceResolutionService`，优先覆盖字段、枚举、规则和快照；复用现有生命周期、aliases、replacement 和 snapshot 数据。
- [x] 2.4 新增只读引用解析 API，并把 stableRef/canonicalRef additive 接入字段搜索结果。

## 3. AI 输出后置校验后端

- [x] 3.1 先补失败测试，覆盖 SQL/DDL/Markdown/JSON/plain text 的字段、枚举、规则、快照和 evidence ref 校验，以及 PASS/WARN/FAIL 语义。
- [x] 3.2 新增带 Javadoc/Schema 字段说明的 post-check 请求、结果、issue、summary 和 content-type/status 枚举。
- [x] 3.3 实现确定性 `AiOutputPostCheckService`：复用 resolver、SQL parser/lint 和 bounded ref extraction，输出 replacement/evidence/nextActions，不修改项目或业务文件。
- [x] 3.4 新增只读 post-check API，限制输入大小并对 content、excerpt、diagnostic、fix 和 evidence label 统一脱敏。

## 4. AI Context、Schema Registry 与 Evidence

- [x] 4.1 为 `AiContextExportServiceTest` 增加失败用例，再在 field-catalog/manifest/guidance 中 additive 输出 stable refs、alias history、replacement ref 和 post-check command。
- [x] 4.2 扩展 Schema Registry，登记 reference resolution 与 post-check result contract，并补 schema/description/fixture 测试。
- [x] 4.3 扩展 AI evidence package，使 AI job/task/payload evidence 可携带 redacted post-check summary、blocking refs、replacement refs 和建议命令。

## 5. CLI、MCP 与契约 fixtures

- [x] 5.1 先补 CLI/MCP 失败测试和 fixture drift 测试，覆盖 `ref resolve`、`ai-output check`、exit codes、read-only safety 和 secret redaction。
- [x] 5.2 实现 CLI `ref resolve` 与 `ai-output check`，支持文件/stdin、稳定 JSON 和 DataSpecError 诊断。
- [x] 5.3 实现 MCP `resolve_standard_refs` 与 `check_ai_output` tools，补完整 input schema description、structuredContent 和安全元数据。
- [x] 5.4 更新 `cli-mcp-contract-fixtures` 与统一 tools 验证入口，确保后续协议漂移可回归。

## 6. 前端最小闭环

- [x] 6.1 为现有 AI 输出/回放界面补 API wrapper、TypeScript 公共类型注释和独立单测，支持触发 post-check。
- [x] 6.2 在复制或下载 AI 产物前展示 PASS/WARN/FAIL、blocking refs、replacement refs 和 nextActions；保持现有页面布局与错误恢复模式。

## 7. 验证、评审与收口

- [x] 7.1 运行受影响后端测试、`pnpm test`/`pnpm build`、tools tests、OpenSpec strict/all、状态检查、`git diff --check` 和 secrets scan。
- [x] 7.2 更新 TODO/候选池/完成归档和本节 Verification Evidence，记录关键命令、结果、未覆盖风险和 active change 状态。
- [x] 7.3 启动独立子 agent 做 API/CLI/MCP/AI 契约、安全、逻辑和测试评审，修复或记录全部 findings，并关闭 agent。
- [x] 7.4 按 Git 门禁精确 stage 本次变更并创建本地 commit，不自动 push、不自动 archive OpenSpec。

## Verification Evidence

- 后端复评目标测试：在 `dataspec-server` 运行 `mvn "-Dtest=AiOutputPostCheckServiceImplTest,StandardReferenceResolutionServiceImplTest,FieldServiceImplTest" test`，63 tests / 0 failures / 0 errors，BUILD SUCCESS；覆盖 matchedAlias 脱敏、SQL identifier/alias 提取、JSON enum claim 和稳定引用解析。
- 后端全量测试：在 `dataspec-server` 运行 `mvn test`，593 tests / 0 failures / 0 errors，BUILD SUCCESS；保留本地 Maven `javax.annotation-api` POM warning、JDK dynamic agent warning和既有性能基线日志。
- 前端目标测试：`node --test dataspec-web/tests/aiOutputPostCheckDisplay.test.ts`，4 pass。
- 前端统一入口：在 `dataspec-web` 运行 `pnpm test`，176 pass；`pnpm build` 通过，保留既有 `@vueuse/core` pure annotation 和 chunk size warning。
- OpenAPI 漂移检查：临时以当前进程环境变量 `SPRING_FLYWAY_ENABLED=false` 启动 8090 服务后运行 `pnpm check:api`，确认 `src/api/schema.ts` 已是最新；未运行 Flyway repair、未修改数据库，检查后已停止服务并确认 8090 释放。
- Tools 目标测试：`node --test tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs`，238 pass、2 个 symlink skip。
- Tools 全入口：`node --test tools/*.test.mjs`，387 pass、2 个 symlink skip（当前平台无法创建 symlink）。
- OpenSpec：`npx.cmd openspec validate add-stable-standard-refs-and-ai-output-checks --strict` valid；`npx.cmd openspec validate --all` 126 passed。
- 状态检查：`node tools/dataspec-status-check.mjs --format json` 返回 warn，仅 `OPENSPEC_ACTIVE_CHANGE_PRESENT` 两项，分别为 `add-ai-context-safety-controls` 和本 change；符合项目“完成后暂保留 active，不自动 archive”的约定。
- 空白检查：`git diff --check` 通过，仅 LF/CRLF warning。
- Secrets scan：`git diff -U0 -- . | rg -n "(?i)(password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn)"` 命中均为 schema/文档安全说明、CLI 参数名、脱敏测试假值或 redaction 断言；未发现真实 token、password、Authorization、JDBC URL、DSN 或连接串。commit 前会对 staged diff 再执行同类扫描。
- Git 门禁：commit 前按显式 pathspec stage 本 change 相关文件，并运行 `git diff --cached --check`、`git diff --cached --stat`、staged diff 抽查和 staged secrets scan；不 push、不 archive OpenSpec。
- 子 agent：前端/文档 worker agent `019f4c6c-3267-7a93-8e93-dbad55c5c3c3` 修复 schema/types/smoke/docs 并通过 smoke，已关闭；前端/tools 协议 worker agent `019f4c8c-da45-7de3-8a08-2ea964784d10` 修复 `TEXT`/`PLAIN_TEXT` 兼容、前端 string[] 类型和 CLI/MCP fixture 回归，已关闭。
- 独立评审：评审 agent `019f4ca0-6769-72c3-bed6-077c7cdb9f88`（Locke）发现 4 个 Important 和 2 个 Minor，涉及 OpenAPI 路由/模型、matchedAlias 脱敏、SQL 隐式 alias、JSON enum 字段顺序、MCP sensitive input 和前端 replacement tag；全部修复后已关闭。复评 agent `019f4cc7-e682-7f50-9624-77f780f99da2`（Kepler）确认原 6 项均关闭，无 Critical/Important，`Ready to commit: Yes`；其 Minor 指出前端 summary fallback 应使用契约字段 `totalRefCount`，已修复并通过前端目标 4 pass、复评组合 83 pass，agent 已关闭。
- 剩余风险：Git stage/commit 门禁尚未完成；本 change 暂不 archive，待用户明确要求 archive 时再同步主规格。
