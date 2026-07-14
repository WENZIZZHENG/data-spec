## Why

DataSpec 已分别拥有 SQL lint issue、AI output post-check issue、GitHub inline/fallback 评论和 Evidence Package，但缺少同一份可验证 Finding 语义。AI 或 CI 目前无法稳定关联问题、位置、证据、评论交付和豁免状态，也无法判断外部 AI finding 是否经过确定性证据校验。

## What Changes

- 新增 additive、versioned、secret-safe 的共享 Review Finding 契约，统一 code、severity、subject、location、trigger、expected、observed、evidenceRefs、confidence、suggestedFix、autoFixSafe 和 waiver。
- SQL lint 结果和 AI output post-check 结果新增 `findings[]`；保留既有 `issues[]` 及其字段语义，旧客户端可忽略新增字段。
- AI output post-check 接受可选结构化 findings；高置信或 ERROR finding 必须携带可由当前项目 evidence resolver 验证的引用，空 findings 是合法成功结果。
- Evidence Package 新增经 post-check 验证的 findings 摘要；SQL check 来源自动携带可解析的 SQL check evidence ref，不存 raw AI output、GitHub token 或业务数据行。
- `review-pr --format json` 增加稳定交付 envelope，输出 PR head SHA、汇总/inline 评论 URL、去重 findings、SQL check record IDs、post-check 状态和 Evidence Package 导出入口，同时保留现有 summary、inline、files 和退出码。
- CLI/MCP post-check 输入、fixtures、OpenAPI、README 和 AI 契约文档同步共享 finding 字段及安全边界。

## Capabilities

### New Capabilities

- `review-finding-contract`: 定义跨 SQL lint、AI post-check、PR review 和 Evidence Package 的共享 Finding/Evidence 结构、验证和兼容语义。

### Modified Capabilities

- `ai-output-postcheck`: 接受并验证外部结构化 findings，输出统一 findings，且允许无高置信问题时返回空数组。
- `ai-evidence-package`: 携带已验证 findings 和 canonical evidence refs，拒绝把未 post-check 的外部 finding 包装成可信证据。
- `github-inline-review`: 输出 commit SHA、评论 URL、Finding 去重结果、SQL check IDs、post-check 状态和 evidence package 入口。
- `sql-inline-review-location`: 将 SQL issue 的文件/range 和 suppression 语义映射到共享 Finding location/waiver。
- `cli-mcp-contract-fixtures`: 覆盖共享 finding、CLI structured findings 文件输入、MCP findings 参数和 review delivery envelope。

## Impact

- 后端公共契约：新增 finding model；additive 扩展 `LintResult`、`AiOutputPostCheckRequest/Result`、`AiEvidencePackageReq/Package` 和 OpenAPI schema。
- 后端服务：SQL lint 生成 canonical SQL check evidence ref；post-check 和 Evidence Package 使用现有 project-scoped `EvidenceClaimResolver` 验证 findings。
- CLI/MCP：扩展 `ai-output check`、`check_ai_output`、`review-pr` JSON/Markdown 输出和 contract fixtures；GitHub 远程写入仍保持现有 token、权限、marker 去重和失败诊断边界。
- 测试与文档：后端单测、CLI/MCP fixtures、GitHub API fixture、Evidence Package fixture、OpenAPI drift、README 和 AI 契约文档。
- 兼容性：无字段删除、重命名或数据库迁移；既有 issue、summary、inline、files、评论 marker 和退出码保持不变。

## Verification Evidence

- 日期：2026-07-14。
- 后端：`mvn test`，758 tests，0 failures，0 errors，`BUILD SUCCESS`；评审整改后的 Finding/OpenAPI/post-check/evidence 定向测试 32 tests 通过。
- 前端：Node 22 Docker 容器内 `pnpm test`，192 tests passed；`pnpm build` 通过；OpenAPI drift 检查确认 `src/api/schema.ts` 最新。
- tools：`node --test tools/*.test.mjs`，458 tests，456 passed，0 failed，2 个 Windows symlink 权限场景按测试设计跳过；`review-pr` 定向 10 tests 通过，覆盖多页 files 和每次评论写入前的 head 复查。
- 契约：`node tools/dataspec-cli-mcp-contract-check.mjs --format json` 通过；`openspec validate unify-review-findings-and-evidence --strict` 通过；`openspec validate --all` 138 passed、0 failed。
- 状态与 diff：`node tools/dataspec-status-check.mjs --format json` 无 error，仅保留 active change 未归档预期 warning；`git diff --check` 无空白错误，仅有 Windows line-ending 提示。
- secrets：扫描 changed diff 与新增目录中的 `password/passwd/token/secret/authorization/api_key/apikey/jdbc:/dsn`，命中均为脱敏实现、测试假值或安全边界文档，未发现真实凭据。
- 评审：首轮 agent `019f5ddf-801b-73a1-97c2-dd8e36573796` 已完成并关闭，7 项 finding 均已整改；第二轮 agent `019f5e13-ce91-7312-a80a-4c0ccf09687c` 已完成并关闭，2 项 Important 已通过 PR files 分页、逐次远端写入前 head 复查和回归测试修复，1 项 Minor 已通过 Javadoc、OpenAPI、README、AI 契约与 OpenSpec 明确 receipt 的进程内重放边界。
- 已知非阻断警告：本机 Maven 缓存中的 `jvnet-parent-3.pom` 存在既有解析警告，但 Surefire 仍完成全部测试并返回成功；Vite 构建保留既有 Rolldown pure annotation 与大 chunk 警告。
- 未覆盖风险：未对真实 GitHub PR 并发写入做线上集成测试；head 在最后一次 GET 校验与紧随其后的评论写请求之间仍存在 GitHub API 无条件写入造成的极短不可消除窗口，多副本服务也不会共享进程内 receipt。
- 归档：主规格已在归档前逐份确认包含全部 6 份 delta；change 使用 `--skip-specs` 避免重复同步，并归档到 `openspec/changes/archive/2026-07-14-unify-review-findings-and-evidence`。归档后 OpenSpec、状态、diff 与 staged 门禁结果见本次本地 commit。
