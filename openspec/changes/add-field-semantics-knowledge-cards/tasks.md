## 1. OpenSpec 与边界确认

- [x] 1.1 校验 proposal、design 和 spec delta 与 P6-77/P6-107/P6-161/P6-175/P6-184 范围一致：第一版只做字段语义、枚举生命周期、知识卡、指标口径和命名翻译的最小闭环；不执行真实计算、不写业务库、不采集业务数据行。
- [x] 1.2 确认本 change 按 SDD full 执行，commit 前需要独立子 agent 评审，并在 `Verification Evidence` 记录 agent id、用途、关闭状态和 findings 处理结论。

## 2. 后端数据模型与 API

- [x] 2.1 新增 additive Flyway migration：字段命名翻译扩展列、枚举值生命周期列、字段语义规则表、指标口径表和必要索引，保持旧数据/旧客户端兼容。
- [x] 2.2 扩展字段 entity/model/request/response/repository/service，使字段可读写命名翻译元数据，并保证普通更新不会误清空语义字段。
- [x] 2.3 扩展枚举值 entity/model/API/service/repository，使枚举值可读写 status、aliases、replacementValue、validFrom、validTo、sourceEvidence、mappingHints 和 aiUsageNotes。
- [x] 2.4 新增字段语义规则 model/entity/repository/service/controller，覆盖 list/detail/create/update/delete、项目归属校验、跨字段引用校验、secret-safe 校验和字段级说明。
- [x] 2.5 新增指标口径 model/entity/repository/service/controller，覆盖 list/detail/create/update/delete、字段引用归属校验、示例 SQL 只读说明、secret-safe 校验和字段级说明。
- [x] 2.6 新增 FieldKnowledgeCard 聚合服务/API，聚合字段、格式、usage contract、语义规则、枚举生命周期、使用示例、命名翻译、指标引用和风险证据，并支持 bounded list/detail。

## 3. AI Context、Schema Registry 与生成器

- [x] 3.1 扩展 AI Context：新增或等价导出 `.dataspec/field-knowledge-cards.json`、`.dataspec/field-semantics.json`、`.dataspec/metrics.json`，manifest 记录 schemaVersion、count、truncation 和 secret-safe 边界。
- [x] 3.2 扩展 `.dataspec/field-catalog.json` 和 `DATABASE_RULES.md`，添加 bounded 语义摘要、枚举生命周期、指标口径、禁用翻译和 source-of-truth guardrail，保持旧字段兼容。
- [x] 3.3 扩展 Markdown 数据字典，展示字段语义、枚举生命周期、知识卡摘要、命名翻译和指标口径摘要，并标记 example SQL 为只读说明。
- [x] 3.4 扩展 Schema Registry，登记 FieldSemanticRule、FieldKnowledgeCard、EnumValueLifecycle、MetricDefinitionMapping 和新增 AI Context artifact contracts。

## 4. 搜索、推荐、CLI 与 MCP

- [x] 4.1 扩展字段搜索/推荐逻辑，使 preferredEnglishName、translationAliases、forbiddenTranslations、semantic rules、enum hints 和 metric boundaries 进入 match reason、warning 或 nextActions。
- [x] 4.2 为 CLI 增加只读 field knowledge / field semantics / metric definitions 查询入口，或扩展既有 context/export 命令输出，并保持 JSON 稳定。
- [x] 4.3 为 MCP 增加只读 resources/tools 或扩展现有 resource descriptor，暴露字段知识卡、语义规则、指标口径和枚举生命周期，更新安全 metadata。
- [x] 4.4 更新 `tools/fixtures/cli-mcp-contracts.json`、contract check 和 CLI/MCP 单测，覆盖新增输出 shape、敏感信息脱敏和参数互斥/边界。

## 5. 前端最小闭环

- [x] 5.1 刷新 OpenAPI schema 并扩展前端 API wrapper/types，避免手工维护 generated schema。
- [x] 5.2 扩展字段库页面，支持查看/编辑命名翻译和字段语义规则摘要，并能打开字段知识卡只读视图。
- [x] 5.3 扩展枚举字典页面，支持枚举值生命周期、别名、替代值、有效期和 AI 使用说明。
- [x] 5.4 新增或扩展指标口径维护入口，支持维护 metricKey、definition、字段引用、过滤、聚合、时间粒度和示例 SQL 说明。
- [x] 5.5 扩展 AI Context / 数据字典 / 标准问答相关展示或 smoke 测试，确保用户能看到新增语义证据和导出提示。

## 6. 测试、文档、评审与提交

- [x] 6.1 补后端单测/controller 测试，覆盖 additive 字段读写、跨项目引用拒绝、secret-safe 拒绝、知识卡稀疏数据、枚举生命周期和指标口径边界。
- [x] 6.2 补 tools 测试，覆盖 CLI/MCP 新契约、contract fixture、脱敏输出和冲突参数。
- [x] 6.3 补前端单测/smoke，覆盖字段知识卡、枚举 lifecycle、指标口径维护、typed API wrapper 和 AI Context scoped 参数。
- [x] 6.4 更新 README、TODO、docs/ai-contracts.md 和候选/归档文档，记录本主题包第一版能力、边界、命令和剩余风险。
- [x] 6.5 运行 OpenSpec strict、后端受影响测试、tools 测试、前端测试/build、OpenAPI drift 检查、`git diff --check` 和必要 secrets 扫描。
- [x] 6.6 启动独立子 agent 做只读代码评审，修复或明确记录 findings，关闭 agent 并记录生命周期。
- [x] 6.7 在本文件追加 `Verification Evidence`，列明关键验证命令、结果、评审证据、未覆盖风险和 commit 前状态。
- [x] 6.8 按项目 Git 规则精确 stage 本次变更，检查 staged diff、敏感项和 `git diff --cached --check` 后创建本地 commit；不主动 push，不自动 archive OpenSpec。

## Verification Evidence

- OpenSpec：`openspec validate add-field-semantics-knowledge-cards --strict` 通过；`openspec validate --all` 通过，129 passed、0 failed。
- 后端：先补失败用例验证评审问题可复现，再修复；最终 `cd dataspec-server && mvn "-Dtest=EnumDictServiceImplTest,FieldServiceImplTest,FieldSemanticRuleServiceImplTest,FieldSemanticRuleRepositoryImplTest,MetricDefinitionServiceImplTest,MetricDefinitionRepositoryImplTest,FieldKnowledgeCardServiceImplTest,AiContextExportServiceTest" test` 通过，123 tests、0 failures/errors；`cd dataspec-server && mvn test` 通过，663 tests、0 failures/errors。
- Tools：`node --test tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，261 total、259 pass、2 skipped。
- 前端：`cd dataspec-web && pnpm test` 通过，184 pass；`cd dataspec-web && pnpm build` 通过，保留既有 Rolldown `INVALID_ANNOTATION` 与 chunk size warning。
- OpenAPI drift：临时后端使用端口 `18092`，连接用户授权的一次性 PostgreSQL `localhost:5432/ai_test` 且 `SPRING_FLYWAY_ENABLED=false`，仅读取 `/api-docs`；先发现 `dataspec-web/src/api/schema.ts` 过期，随后执行 `pnpm exec openapi-typescript http://localhost:18092/api-docs -o src/api/schema.ts` 重新生成，`cd dataspec-web && pnpm check:api -- --source http://localhost:18092/api-docs` 通过；临时后端 PID 4976 已停止并确认端口无监听。
- 通用检查：`git diff --check` 通过，仅输出 Git LF/CRLF 提示；生产 diff 与全量 diff 敏感项扫描命中 `secretSafe` 文案、脱敏测试假值、CLI `dataspec-token` 参数和 sanitizer 调用，未发现新增真实凭据、JDBC URL、DSN、Authorization 或 token。
- 评审生命周期：子 agent `019f51e0-f8d1-7941-8095-8d3a796f7a99`（Fermat，用途：首次只读复评）因工具侧 503 未形成有效 findings，已关闭。
- 评审生命周期：子 agent `019f51f5-ff48-7431-92f0-b6478c93fc76`（Kierkegaard，用途：只读代码复评）发现 3 个 P2：受控字符串错误回显 raw secret、FieldKnowledgeCard bounded/scoped evidence 缺口、AI Context scoped export 先截断后过滤；已修复 secret-safe 错误消息、repository/service 层 related-to-fields 查询、知识卡候选查询下推和 scoped AI Context 相关查询，并补回归测试；该 agent 已关闭。
- 评审生命周期：子 agent `019f520e-2f26-7793-9781-b7da36e691e7`（Jason，用途：Kierkegaard findings 修复后复评）结论 With fixes，发现 2 个 P2：枚举 lifecycle status 敏感值仍可能回显、知识卡多字段 scope 仍可能被前置字段耗尽全局 evidence limit；已关闭。
- Jason findings 处理：`EnumDictServiceImpl.normalizeStatus()` 先检测敏感文本 / private key 并抛固定消息，新增 `EnumDictServiceImplTest.createValue_rejectsSensitiveLifecycleStatusWithoutEchoingSecret`；`FieldKnowledgeCardServiceImpl` 多字段 semantic/metric evidence 改为逐字段 bounded 查询并去重，新增 `FieldKnowledgeCardServiceImplTest.list_preservesEvidenceForLaterScopedFieldsWhenFirstFieldWouldExhaustGlobalLimit`。
- 评审生命周期：子 agent `019f5216-bc3e-7082-93c0-3b5009c2573b`（Franklin，用途：Jason findings 修复复评）结论 Ready，无 Critical/Important/P2/Minor；已运行 `mvn "-Dtest=EnumDictServiceImplTest,FieldKnowledgeCardServiceImplTest" test` 通过，11 tests、0 failures/errors；该 agent 已关闭。
- commit 前状态：工作区仍未 stage；下一步执行 6.8，按项目 Git 规则精确 stage 本 change 相关文件、检查 staged diff 与敏感项后创建本地 commit；不主动 push，不自动 archive OpenSpec。
- 剩余风险：知识卡、语义规则和指标口径第一版为 metadata guidance，不执行真实计算、不连接业务库统计枚举分布、不自动改生产 SQL；多字段知识卡 evidence 为逐字段 bounded 查询，单字段内仍按服务层 limit 裁剪以控制 AI Context 体积。
