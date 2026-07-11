## 1. OpenSpec 与实现边界

- [x] 1.1 校验 proposal、design 和 spec delta 与 P6-76/P6-106 范围一致：第一版只做业务对象、表模板结构标准、DDL preview、AI Context、Schema Registry、CLI/MCP 和前端最小闭环；不做完整 ER 平台、不执行数据库迁移、不自动改写业务库、不读取业务数据行。
- [x] 1.2 确认本 change 按 SDD full 执行，commit 前需要独立子 agent 评审，并在 `Verification Evidence` 记录 agent id、用途、关闭状态和 findings 处理结论。

## 2. 后端数据模型与 API

- [x] 2.1 新增 Flyway migration 和 `schema.sql` 同步：创建业务对象标准表，为 `ds_template` 增加表结构标准 JSON/文本列和必要索引，保持 additive 兼容。
- [x] 2.2 新增业务对象标准 entity/model/DTO/repository/service/controller，覆盖 list/detail/create/update/delete、项目归属校验、唯一性校验和字段级 Javadoc/schema 说明。
- [x] 2.3 扩展模板 entity、request/response、service 和 repository，使模板详情可读写 `primaryKey`、`uniqueKeys`、`indexes`、`foreignKeys`、`checkHints`、`auditPolicy`、`softDeletePolicy`、`dialectNotes` 和 `aiUsageNotes`。
- [x] 2.4 新增只读关系摘要服务/API，返回业务对象、模板、字段和关系 edge，并在无数据时返回空有效结构。
- [x] 2.5 补后端单测或 controller 测试，覆盖项目隔离、结构 JSON 边界、非法 identifier、跨项目 templateId、空关系摘要和正常 CRUD。

## 3. DDL Preview 与 AI Context

- [x] 3.1 扩展 DDL 生成模型和 service：消费表结构标准，安全生成主键、唯一键、索引、外键 preview，并返回 `structureSummary`、skipped hints、policy notes 和 evidence。
- [x] 3.2 确保 `checkHints`、审计/软删除策略、方言说明和 AI 使用说明默认只作为只读 guidance，不拼接 raw SQL，不连接或写入数据库。
- [x] 3.3 扩展 AI replay 记录，使 DDL preview 输出包含脱敏结构摘要，且不包含 token/password/Authorization/JDBC URL/DSN/业务数据行。
- [x] 3.4 扩展 AI Context 导出：新增 `.dataspec/table-standards.json`、manifest artifact、`DATABASE_RULES.md` 摘要和 `scope=business-object|table-template` 裁剪。
- [x] 3.5 补 DDL 与 AI Context 测试，覆盖约束生成、非法约束跳过、空项目兼容、scoped export 和 zip 包新增文件。

## 4. Schema Registry、CLI 与 MCP

- [x] 4.1 扩展 Schema Registry，登记 BusinessObjectStandard、TableStructureStandard、TableRelationHint、TableIndexStandard、TableForeignKeyStandard、TablePolicyStandard 和 AI Context table standards contract。
- [x] 4.2 扩展 CLI：新增只读 `table-standards list/show` 或等价命令，并确保 `generate-ddl --format json` 保留 `structureSummary`。
- [x] 4.3 扩展 MCP：新增 table standards resource/tool，更新 create-table prompt guidance，并确保 `generate_table_ddl` 保留结构摘要。
- [x] 4.4 更新 `tools/fixtures/cli-mcp-contracts.json` 和 contract check 测试，覆盖新增 CLI/MCP 契约、安全 metadata、输出 shape 和推荐下一步。

## 5. 前端最小闭环

- [x] 5.1 扩展前端类型和 API wrapper，避免手改 OpenAPI 生成文件；必要时使用后端生成流程刷新类型。
- [x] 5.2 扩展模板管理页，支持查看/编辑业务对象关联和表结构标准，保留现有模板字段维护能力。
- [x] 5.3 扩展 DDL 生成页，展示结构标准摘要、关系 edge、lint evidence、跳过原因和“未应用到数据库”的安全提示。
- [x] 5.4 补前端单测/smoke，覆盖 API wrapper、模板页关键文案、DDL 页结构摘要和 scoped AI Context 参数。

## 6. 文档、验证与提交

- [x] 6.1 更新直接受影响的 README/TODO/AI contract 文档，记录 P6-76/P6-106 第一版能力、命令、边界和不自动 archive 约定。
- [x] 6.2 运行 OpenSpec strict、后端受影响测试、tools 测试、前端测试/build、`git diff --check` 和必要 secrets 扫描。
- [x] 6.3 启动独立子 agent 做只读代码评审，修复或明确记录 findings，关闭 agent 并记录生命周期。
- [x] 6.4 在本文件追加 `Verification Evidence`，列明关键验证命令、结果、评审证据、未覆盖风险和 commit 前状态。
- [x] 6.5 按项目 Git 规则精确 stage 本次变更，检查 staged diff、敏感项和 `git diff --cached --check` 后创建本地 commit；不主动 push，不自动 archive OpenSpec。

## Verification Evidence

- OpenSpec strict：`npx.cmd openspec validate add-business-object-table-standards --strict` 通过，change valid。
- 后端目标测试：`mvn -q "-Dtest=BusinessObjectStandardServiceImplTest,TableStandardsControllerTest,SchemaRegistryServiceImplTest,SchemaRegistryControllerTest,DdlGeneratorServiceTest,GeneratorControllerTest,AiContextExportServiceTest" test` 通过；仅保留 ByteBuddy/JVM 动态 agent warning。
- 评审修复后后端回归：`mvn -q "-Dtest=BusinessObjectStandardServiceImplTest,TableStandardsControllerTest" test` 通过。
- Tools 目标测试：`node --test tools/dataspec-mcp.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 87 pass；`node --test tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 253 pass / 2 skipped，skipped 为 Windows symlink 权限。
- 前端 targeted：`node --test tests/aiContextScope.test.ts tests/frontendSmoke.test.ts tests/typedApiClient.test.ts` 41 pass；评审修复后 `node --test tests/frontendSmoke.test.ts tests/typedApiClient.test.ts` 36 pass。
- 前端完整测试：`pnpm test` 183 pass。
- 前端构建：`pnpm build` 通过；保留既有 Rolldown pure annotation、chunk size 和 plugin timings warning。
- OpenAPI：临时后端连接用户授权的一次性 PostgreSQL `localhost:5432/ai_test`，显式关闭 Flyway，仅用于读取 `/api-docs`；`pnpm exec openapi-typescript http://localhost:18092/api-docs -o src/api/schema.ts` 完成，`pnpm check:api -- --source http://localhost:18092/api-docs` 通过并提示既有 Node `DEP0190` warning；临时后端已停止。
- OpenSpec all：`npx.cmd openspec validate --all` 128 passed。
- 通用检查：`git diff --check` 通过，仅 LF/CRLF 工作区提示。
- 状态检查：`node tools/dataspec-status-check.mjs --format json` 返回 warn，仅 `OPENSPEC_ACTIVE_CHANGE_PRESENT` x4；本 change 和其他已完成 active changes 按项目约定暂保留 open，不自动 archive。
- 敏感项扫描：`rg -n "password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn" ...` 命中 README/docs/代码中的安全说明、字段名、脱敏测试样例和安全边界注释，未发现新增真实凭据；commit 前还会对 staged diff 再做窄扫描。
- 独立评审 agent `019f4f80-d26b-7d63-aacd-f17dca3764d1`（Aristotle）：只读评审完整 diff，已完成并关闭；发现 5 个 important/minor：table standards endpoint 精确过滤、OpenAPI schema 刷新、Schema Registry 契约对齐、MCP 参数互斥、AiExport scope guard 和 evidence 记录。已分别修复并补测试/文档。
- OpenAPI 调查 agent `019f4f8e-921d-7a53-a091-fe560483d646`（Carver）：只读调查 OpenAPI schema 生成路径，返回 findings 后系统已回收，关闭工具返回 `not found`；结论用于安全生成 `schema.ts`。
- 复评 agent `019f5053-ea1d-7901-bd3b-a9bf9a7fe929`（Euler）：极小范围只读复评。首轮发现 2 个 P2：普通 AI Context 模糊 scope 缺回归测试、前端数组 JSON 输入 `null` 可能进入 generated schema payload；均已修复并重跑目标验证。复核结论 Approved，关闭成功。
- 无效评审尝试：`019f4fa4-0bd1-74d2-a5fb-b7ac6797fea1`（Copernicus）因外部 403 失败，关闭工具返回 `not found`；`019f503f-3e27-7343-91ae-42d70be7dfa8`（Anscombe）和 `019f5049-f8e6-7223-93a6-81c69c8f1dc6`（Lovelace）超时后主动关闭，均未作为有效评审证据。
