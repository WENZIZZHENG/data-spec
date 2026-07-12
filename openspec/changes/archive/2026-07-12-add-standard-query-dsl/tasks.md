## 1. OpenSpec 与准备

- [x] 1.1 读取 proposal/design/spec delta、P6-167 候选详情和现有字段搜索、AI Context、CLI/MCP 代码入口。
- [x] 1.2 运行 `npx.cmd openspec validate add-standard-query-dsl --strict`，确认 artifacts 可实施。

## 2. 后端 DSL 模型与解析

- [x] 2.1 先补失败测试，覆盖有效 FIELD DSL、legacy 参数映射、unsupported filter strict/non-strict、limit/filter bounds、secret redaction。
- [x] 2.2 新增带 Javadoc/Schema 字段说明的 Standard Query request/result/filter/summary/error 模型。
- [x] 2.3 实现 project-scoped `StandardQueryService` 和 allowlist parser，第一版支持 FIELD target、AND 组合、text、category/tag/status/sensitive/sourceBatchId/stableRef/canonicalRef/hasExample/updatedSince/limit/explain。
- [x] 2.4 新增只读 Standard Query API，并复用 `FieldService.search` 输出字段命中、stableRef/canonicalRef 和 query summary。

## 3. 字段搜索与 AI Context 接入

- [x] 3.1 先补字段搜索回归测试，证明现有 GET `/api/fields/search` 参数行为不变且 additive 输出 DSL explanation。
- [x] 3.2 将 legacy 字段搜索参数转换为 DSL plan，保留现有 response shape 并新增 querySummary/appliedFilters/ignoredFilters/resultCount/nextQueryHints。
- [x] 3.3 先补 AI Context 测试，覆盖 field catalog/package 使用 DSL 裁剪、manifest 记录红acted query summary、legacy scope 兼容。
- [x] 3.4 将 AI Context scope options additive 接入 Standard Query DSL，不改变旧 scope/query/status/limit 入口。

## 4. Schema Registry、CLI、MCP 与 fixtures

- [x] 4.1 扩展 Schema Registry，登记 Standard Query DSL request/result/filter/summary/error schema，并补 schema description 测试。
- [x] 4.2 先补 CLI/MCP 失败测试和 fixture drift 测试，覆盖 DSL JSON/file/stdin、search-fields DSL 参数、read-only safety 和 secret redaction。
- [x] 4.3 实现 CLI Standard Query DSL 查询入口或 `search-fields --dsl`，保持稳定 JSON 和 DataSpecError 诊断。
- [x] 4.4 实现 MCP DSL 查询 tool 或 `search_fields` additive DSL 参数，补 input schema description、structuredContent 和安全元数据。
- [x] 4.5 更新 `cli-mcp-contract-fixtures` 与统一 tools 验证入口，确保 DSL 协议漂移可回归。

## 5. 前端最小闭环

- [x] 5.1 为 Standard Query DSL 增加 API wrapper、TypeScript 公共类型注释和独立单测。
- [x] 5.2 在字段库或 AI Context 页面保持现有控件不变，至少展示/传递 DSL 查询摘要或为后续 UI 暴露类型化入口；避免新增复杂 JSON 编辑器。
- [x] 5.3 更新前端 smoke，确保关键路由、字段搜索和 AI Context scope wiring 不漂移。

## 6. 验证、评审与收口

- [x] 6.1 运行受影响后端测试、`pnpm test`/`pnpm build`、tools tests、OpenSpec strict/all、状态检查、`git diff --check` 和 secrets scan。
- [x] 6.2 启动独立子 agent 做 API/CLI/MCP/AI 契约、安全、逻辑和测试评审，修复或记录全部 findings，并关闭 agent。
- [x] 6.3 更新 TODO/候选池/完成归档和本节 Verification Evidence，记录关键命令、结果、评审证据、未覆盖风险和 active change 状态。
- [x] 6.4 按 Git 门禁精确 stage 本次变更并创建本地 commit，不自动 push、不自动 archive OpenSpec。

## Verification Evidence

- 后端目标回归：`mvn "-Dtest=StandardQueryServiceImplTest,StandardQueryControllerTest,AiContextExportServiceTest,FieldServiceImplTest,SchemaRegistryServiceImplTest,PerformanceBaselineTest" test`，107 tests，0 failures，0 errors。
- 后端全量：`mvn test`，607 tests，0 failures，0 errors。保留既有本地 Maven repository `javax.annotation-api` / `jvnet-parent` POM warning，不影响测试结果。
- 前端：`pnpm test`，180 pass；`pnpm build` 通过，保留既有 Rolldown pure annotation、chunk size 和 plugin timing warning。
- CLI/MCP 目标回归：`node --test tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs`，217 total，215 pass，2 skipped（symlink EPERM）。
- tools 全量：`node --test tools/*.test.mjs`，397 total，395 pass，2 skipped（symlink EPERM）。
- OpenSpec：`npx.cmd openspec validate add-standard-query-dsl --strict` valid；`npx.cmd openspec validate --all` 127 passed。
- 状态检查：`node tools/dataspec-status-check.mjs --format json` 为 warn，仅 3 个 `OPENSPEC_ACTIVE_CHANGE_PRESENT` warning：`add-ai-context-safety-controls`、`add-stable-standard-refs-and-ai-output-checks`、`add-standard-query-dsl`，符合 active change 暂保留约定。
- 通用检查：`git diff --check` 通过，仅 LF/CRLF warning；`git diff -U0 | rg -n "password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn"` 命中均为脱敏说明、测试假值、字段名或 token 变量名，未见真实凭据。
- 独立评审：原 agent `019f4ec7-88fc-7232-b365-90c9a03adf3c`（Helmholtz）初评发现 3 个 P2：AI Context 未复用 `StandardQueryService`、stableRef/canonicalRef 未校验 projectId、DSL validation schema 未接入稳定错误响应；均已修复并补测试。复评指出 `R.error` 未携带 `STANDARD_QUERY_DSL_INVALID`，已补 `StandardQueryController` error detail 和 CLI/MCP validation diagnostic 回归测试。最终复评 agent `019f4f0a-6f20-7433-917e-a1d7eada074e`（Curie）发现 1 个 P1 和 1 个 P2：`canonicalRef` 过滤未按 replacement canonical 语义执行、DSL `limit` 契约与字段搜索实际上限不一致；已补失败测试并修复为 canonicalRef 匹配字段实际 canonical 引用、DSL limit 统一为 1..50。修复后目标回归：`mvn "-Dtest=StandardQueryServiceImplTest,StandardQueryControllerTest,FieldServiceImplTest,SchemaRegistryServiceImplTest" test` 74 pass；`node --test tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs` 217 total，215 pass / 2 skipped；`pnpm test -- standardQuerySummary.test.ts` 180 pass；`npx.cmd openspec validate add-standard-query-dsl --strict` valid。Curie 复评结论 Approved，无阻塞 findings，agent 已关闭。
- 剩余风险：v1 仅执行 `FIELD` target；非 FIELD target 在 non-strict 模式进入 `ignoredFilters`，strict 模式返回 `STANDARD_QUERY_DSL_INVALID`。OpenSpec change 完成后仍按约定保留 active，不自动 archive。
