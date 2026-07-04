# DataSpec 数标

**AI 编程时代的数据字段标准系统**

DataSpec 用于统一数据库字段命名、数据类型、注释、枚举、表模板和建表规范。当前已形成个人/小团队可用的字段标准工作台，并提供任务式入口、统一前端数据状态、项目活动时间线、标准健康趋势、领域 Starter Kit、业务术语表、自然语言需求草案、Explain Trace、SQL 校验、DDL 生成、标准候选采纳、数据字典、Excel 导入导出、项目备份恢复、AI Context、AI 能力清单、AI 任务模式、AI 回放与反馈、AI 批量任务交付包、AI 执行证据包、API Token 安全基线与管理页、单机轻量幂等写保护、CLI、MCP 和 GitHub PR Review 等能力。

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Java 21 + Spring Boot 3.3 + MyBatis-Plus + PostgreSQL |
| 前端 | Vue 3 + Vite + TypeScript + Element Plus + Pinia |
| SQL 解析 | JSqlParser |
| 接口文档 | SpringDoc OpenAPI |
| 测试 | JUnit 5 |

## 功能概览

### 标准维护

- 项目空间管理，创建项目时可导入内置 standards，可选择用户账号、订单交易、支付金额、库存商品和审计日志等领域 Starter Kit，并支持一键创建演示项目。
- 标准字段库，支持别名、分类、代码集关联、敏感标记、生命周期状态、替代字段/说明、示例值、分组筛选、批量归组、批量维护和字段标准检索命中原因展示。
- 业务术语表，支持项目级术语、同义词、英文词根、拼音/历史缩写、禁用词、canonical 字段、适用范围和示例字段维护；冲突检测会提示重复术语、禁用词冲突和不可用 canonical 字段。
- 标准候选 Inbox，支持手动创建候选、按状态/来源/关键词筛选、采纳为新字段、合并到已有字段、忽略或延后处理。
- 字段质量评分，按注释、别名、示例、分类、敏感标识、代码集和废弃说明识别低质量字段。
- 标准健康趋势，按项目保存健康快照，聚合字段质量、覆盖率摘要、AI 反馈、候选状态和 fixedSql 机会，展示本周/月变化、Top actions 和可复制的 AI 改进计划。
- 字段冲突检测，识别别名冲突、显示名重复、语义疑似重复、关键属性不一致、SQL 保留字/危险命名、大小写碰撞和 alias 歧义。
- 字段影响分析，展示字段被模板、导入来源、历史 SQL 检查、标准快照和代码集引用的范围。
- 数据域、枚举字典、表模板、规则配置、规则基线套件和规则例外管理。
- 领域 Starter Kit 支持把内置领域字段、枚举和表模板幂等应用到项目；重复应用只补缺失项，不覆盖用户已修改标准，并保留安装摘要。
- 标准快照，支持为当前项目字段、枚举和规则生成版本号、内容 hash 和可追溯 payload。
- Excel `.xlsx` 模板下载、字段/代码集导出、导入预览和确认导入。
- 项目备份恢复，支持导出项目标准资产 JSON 包、恢复 dry-run、冲突预览、确认恢复和恢复摘要记录。
- 标准变更日志和 What-if 预览，记录字段、代码集、规则的新增、修改、删除、启停 before/after 和操作者；字段/规则保存前可预览 diff、影响、验证命令、当前快照和回退提示，字段库可基于最近字段变更执行确认回退。

### SQL 规范闭环

- SQL 粘贴校验，返回 error/warning/suggestion、结构化修复建议、请求级 fixedSql 策略和 dry-run 修复计划。
- 支持 PostgreSQL `COMMENT ON TABLE/COLUMN`，以及常见 MySQL `CREATE TABLE`、列内注释、表选项、索引定义、`UNSIGNED` 数值类型和 `tinyint(1)` 布尔习惯解析。
- SQL lint、fixedSql、DDL 生成和反向导入会返回 `dialectDiagnostics`，标明当前方言、能力维度、支持级别、稳定 code、说明和下一步建议。
- 表名/字段名 snake_case、禁用字段名、推荐字段名、必备列、金额类型、字段后缀/前缀类型和注释缺失等规则。
- 项目级规则例外，支持按规则编码、表名和字段名声明历史兼容原因；被豁免问题保留在结果中但不计入 active error/warning/suggestion。
- 修正 SQL 输出、复制、检查记录、分页历史和详情查看。
- SQL / 数据库直连反向导入预览，输出解析表、字段候选、缺注释项、非标准字段差异和字段级映射决策，并支持填写确认理由后导入数标候选；直连模式支持按表生成数据库现状与 DataSpec 标准字段的二次比对、项目级非敏感连接预设复用、连接健康与方言能力画像，并追踪确认导入后的字段来源批次和 mapping decision。
- 数据库 schema dump，支持把选定 PostgreSQL/MySQL 表结构 metadata 导出为离线 JSON，并从 dump 复现反向导入预览、标准差异比对和字段覆盖率报告。
- 字段覆盖率报告，支持基于 SQL/DDL、数据库直连 metadata 或 schema dump 统计标准命中、别名命中、未纳管、缺注释和疑似重复字段。

### 生成与报告

- 基于表模板生成 PostgreSQL DDL，并自动运行 lint 自检。
- Markdown/HTML 数据字典，包含概览统计、字段与数据域关系、枚举、模板、个人版字段元数据和 Mermaid 关系图。
- 个人工作台，展示任务入口、最近任务、字段数、代码集数、规则数、检查数、字段命中率、最近检查、问题趋势和项目活动时间线。
- 统一前端数据状态，Dashboard、AI 批量任务、覆盖率报告和 SQL 校验记录区已接入一致的项目缺失、空数据、请求失败和重试入口。
- 前端可复现操作链接，字段库、SQL 检查记录、AI 回放、覆盖率报告和反向导入支持把项目、筛选条件和详情 ID 写入安全 query 并复制链接；不会把 SQL 原文、数据库连接信息、token 或密码写入 URL。

### AI 与自动化

- AI Context zip 导出，包含 `.dataspec/` 目录、字段目录 JSON Schema、项目级业务术语表、规则、项目规则例外、prompt、workflow recipes、示例 SQL 和 `AGENTS.md.fragment`；支持按字段、数据域、标签、表、状态和关键词导出按需包，也支持按历史标准快照导出可复现上下文，并可写入业务仓库 `.dataspec/context/` 离线缓存。字段目录会导出生命周期状态和可选替代字段/说明；来自 Starter Kit 的字段会在字段目录中暴露 kit key/version 来源，方便 AI 判断领域模板出处。
- AI 能力清单，提供只读 `/api/capabilities` 和 `/api/capabilities/{id}`，稳定描述 API/CLI/MCP/前端入口、输入输出契约、preflightChecks、writeRisk、示例和 nextActions；能力清单不会执行任务，也不替代鉴权或 dry-run。
- AI Context manifest、字段目录和规则文件携带标准快照版本、hash 与来源 `source=current|snapshot|unversioned`；`rules.yaml` 还会标明当前规则基线 key/name/version/source/appliedAt；未创建快照时标记为 `unversioned`。
- AI 任务模式，内置 `create-table`、`sql-fix`、`reverse-import`、`pr-review`、`minimal-context`，用于给 AI/CLI/MCP 提供上下文范围、fixedSql 策略、输出格式和推荐命令的默认建议。
- AI 建表 Prompt 和 SQL 修正 Prompt 生成。
- AI 回放记录，支持查看 Prompt、SQL 检查修正和 DDL 预览的输入输出、promptVersion 与标准快照。
- AI 反馈报告，按项目聚合已有 AI job、SQL 检查记录、fixedSql、规则例外、反向导入来源和字段元数据，输出字段/规则/fixedSql/未纳管信号和下一步维护动作。
- 标准健康计划，复用字段质量、覆盖率摘要、AI 反馈和候选 Inbox 生成 Top actions 与 Markdown，供 AI 按优先级维护字段注释、别名、未纳管字段和规则问题。
- AI 批量任务，支持后端保存 SQL lint batch run、前端查看最近任务/分项结果/下一步动作并下载 JSON 交付包。
- AI 执行证据包，支持从 SQL 检查记录、AI job、AI 批量任务和当前覆盖率报告生成 JSON 或 zip，前端可复制/下载，CLI/MCP 可机器读取。
- AI/CLI 写入保护，标准快照、反向导入确认、AI 批量 SQL lint、项目恢复 apply 和 AI job 回放记录已接入单机轻量 idempotency key、项目级 operation lock 和可重试冲突诊断。
- 自然语言需求草案 API 和前端入口，基于字段推荐/检索、业务术语表和表模板，把建表描述拆成 matchedFields、missingCandidates、ambiguousTerms、recommendedTemplate、nextActions 和可复制 Prompt；字段、候选和模板会展示 Explain Trace 证据来源；第一版只读，不自动写入候选或字段库。
- 字段推荐与字段标准检索 API/CLI/MCP；启用的业务术语表会参与“会员手机号”“订单费用”等自然语言 query 的确定性匹配，并在命中原因中标记 `术语表`；推荐和检索结果包含轻量 evidence 数组，便于 AI 读取来源、置信度和文档引用。
- DDL 生成 API/CLI/MCP。
- 轻量 API Token 管理页，支持创建、禁用、授权范围查看、最近使用时间和一次性明文复制。
- CLI 支持业务仓库初始化 `init`、环境自检 `doctor`、capability catalog、workflow recipes、单文件 lint、批量 `lint-files`、变更感知 `changed/lint-changed`、AI 批量交付包文件输出、PR inline/汇总评论式 `review-pr`、AI Context 导出、历史快照 Context 导出、字段推荐、字段标准检索和 DDL 生成。
- MCP Server 暴露 DataSpec resources、capability catalog、workflow recipes、prompts、核心 tools 和 evidence package 导出 tool。
- GitHub Actions 示例支持 SQL 批量校验、PR diff inline 评论和 fallback 汇总评论。
- 本地 Docker Compose 一键启动和 demo smoke 验证，适合个人试用、演示和 AI agent 启动前检查。

## 快速启动

### 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 18+ / pnpm 8+
- PostgreSQL 17+
- Docker Compose（可选，用于一键本地体验）

### 1. 一键本地启动（推荐体验）

适合新机器、演示环境或让 AI agent 先验证项目可用性：

```bash
docker compose -f docker-compose.local.yml up
```

Compose 会启动 PostgreSQL、后端和前端；本地默认端口分别为 `5432`、`8090`、`5173`，后端会通过 Flyway 自动迁移本地 PostgreSQL。默认数据库账号只用于个人本地开发，不是生产部署方案。

启动后可运行 smoke 验证：

```bash
node tools/dataspec-local-smoke.mjs
node tools/dataspec-local-smoke.mjs --json
```

smoke 会等待前端和 `/api-docs` 可访问，创建或复用演示项目，并检查工作台 summary 与 SQL lint 链路。安全模式开启时可传 `--token <token>` 或设置 `DATASPEC_TOKEN`；如只是检查服务可达，可加 `--skip-demo`。

端口冲突时可覆盖对外端口：

```powershell
$env:DATASPEC_DB_PORT="15432"
$env:DATASPEC_SERVER_PORT="18090"
$env:DATASPEC_WEB_PORT="15173"
docker compose -f docker-compose.local.yml up
node tools/dataspec-local-smoke.mjs --server http://localhost:18090 --web http://localhost:15173
```

停止和清理：

```bash
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml down -v  # 同时删除本地演示数据库和依赖缓存 volume
```

### 2. 手动开发启动：数据库

```bash
# 创建数据库，后端启动时会由 Flyway 自动执行迁移脚本
psql -U postgres -c "CREATE DATABASE dataspec;"
```

迁移脚本位于 `dataspec-server/src/main/resources/db/migration/`，后端启动会按版本顺序自动执行全部 `V*.sql`。
`dataspec-server/src/main/resources/db/schema.sql` 仅保留兼容说明，不再作为直接建表入口；如需手动初始化，请按版本号顺序执行迁移目录下的脚本。

### 3. 手动开发启动：后端

```bash
cd dataspec-server
export JAVA_HOME=/path/to/jdk-21  # Windows: set JAVA_HOME=...
mvn spring-boot:run
```

后端启动在 http://localhost:8090，API 文档：http://localhost:8090/swagger-ui.html

### 4. 手动开发启动：前端

```bash
cd dataspec-web
pnpm install
pnpm dev
```

前端启动在 http://localhost:5173

### 5. 首次体验

打开前端后进入“工作台”，点击“创建演示项目”即可生成 `DataSpec 演示项目`。演示项目会自动包含内置字段、个人默认规则基线、订单表模板和示例 SQL，可继续完成：

- “生成演示 DDL”：进入 DDL 生成页并预填 `user_order`。
- “校验示例 SQL”：进入 SQL 校验页并载入可修正的示例 SQL。
- “导出 AI Context”：预览并下载 `dataspec-ai-context.zip`。

选择项目后，工作台会直接展示“导入现有库”“检查 SQL”“生成覆盖率”“补标准字段”“生成 DDL”“导出给 AI”和“管理 Token”等任务入口；从工作台打开过的任务会按当前项目保存在浏览器本地最近任务中。顶部面包屑会显示“工作台 / 当前页”，便于从任务链路中回到总览。

## AI Context 导出包

DataSpec 可为 AI 编程工具导出完整上下文包，也可以按当前建表、修 SQL 或字段设计任务导出更小的按需包：

```bash
curl -L "http://localhost:8090/api/ai-context/package/download?projectId=1" -o dataspec-ai-context.zip
curl -L "http://localhost:8090/api/ai-context/package/download?projectId=1&scope=field&query=用户手机号&status=enabled&limit=20" -o dataspec-ai-context-field.zip
curl -L "http://localhost:8090/api/ai-context/package/download?projectId=1&snapshotId=42" -o dataspec-ai-context-snapshot-42.zip
```

解压后包含 `.dataspec/DATABASE_RULES.md`、`.dataspec/field-catalog.json`、`.dataspec/field-catalog.schema.json`、`.dataspec/schema-registry.json`、`.dataspec/capabilities.json`、`.dataspec/rules.yaml`、`.dataspec/prompts.md`、`.dataspec/workflows.md`、`.dataspec/examples/good.sql`、`.dataspec/examples/bad.sql` 和 `AGENTS.md.fragment`。可将这些文件复制到业务项目，让 Codex/Cursor/Claude Code 等 agent 在建表或评审 SQL 前先读取能力清单、字段标准、契约和规则。

导出包的 `.dataspec/manifest.json`、`.dataspec/field-catalog.json` 和 `.dataspec/rules.yaml` 会包含 `specVersion` / `specHash` / `source` 元数据。若项目尚未创建标准快照，版本显示为 `unversioned`，不阻断导出；创建快照后，后续 SQL 检查记录和 DDL 生成结果会记录当前快照 ID、版本和 hash。需要复现历史任务时，可传 `snapshotId` 或 `snapshotVersion` 从已保存 payload 导出历史字段目录、规则和 zip 包；CLI 等价命令为 `node tools/dataspec-cli.mjs export-context --project 1 --snapshot-id 42 --output dataspec-ai-context-snapshot-42.zip`。

按需导出支持 `scope=all|field|domain|tag|table|changed`，可叠加 `query`、`status` 和 `limit`。裁剪后的 `field-catalog.json` 会输出 `contextScope` 摘要和字段级 `matchReasons`，说明命中条件、字段总数、命中数量、返回数量和缺失或截断提示；`.dataspec/README.md` 会标明当前包是完整包还是按需包。前端“AI Context”页面也提供同样的范围、关键词、状态和上限筛选，并可选择“当前标准”或历史快照，预览与下载共用同一组条件。

字段目录中的每个字段可选输出 `format` 对象，包含 `type`、`pattern`、`unit`、`precision`、`timezone`、`nullPolicy`、`validExamples`、`invalidExamples` 和 `notes`。这让 AI 在生成 DDL、SQL、DTO 或校验建议前能直接读取“金额按分存储”“手机号正则”“时间戳使用 UTC”“JSON 结构说明”等约束；第一版只导出人工维护的格式元数据，不扫描真实业务数据行，也不执行正则或 JSON Schema 校验。

CLI 可把同一份 AI Context 写入业务仓库缓存，供服务不可用时 AI 只读使用：

```bash
node tools/dataspec-cli.mjs export-context --project 1 --cache
node tools/dataspec-cli.mjs export-context --project 1 --scope field --query "用户手机号" --cache --cache-ttl-days 3
node tools/dataspec-cli.mjs export-context --project 1 --output dataspec-ai-context.zip --cache
```

缓存目录固定为 `.dataspec/context/`，会包含 `manifest.json`、`capabilities.json`、`field-catalog.json`、`rules.yaml`、`prompts.md`、`AGENTS.md.fragment` 和 `cache-metadata.json` 等文件。`cache-metadata.json` 记录 projectId、server、导出参数、exportedAt、expiresAt、contentHash 和标准版本/hash/source，并会脱敏 token/password/Bearer/完整 JDBC URL。`doctor` 会报告 `context-cache` 检查项：无缓存、已过期、服务不可用但可离线读取，或远端标准 hash 与缓存不一致。离线缓存不写入 DataSpec 服务端状态，也不缓存数据库密码或业务数据行。

## AI 能力清单

能力清单是 AI agent 的只读自描述入口，用来回答“当前 DataSpec 能做什么、走哪个 API/CLI/MCP、需要哪些前置检查、是否有写入风险”。它不会执行 lint、导出 Context、连接数据库或写入标准；真实权限仍由 API Token、项目边界、具体 API 和 dry-run/人工确认负责。

```bash
curl "http://localhost:8090/api/capabilities?projectId=1"
curl "http://localhost:8090/api/capabilities/lint-sql?projectId=1"

node tools/dataspec-cli.mjs capability list --project 1 --format json
node tools/dataspec-cli.mjs capability show lint-sql --project 1 --format json
node tools/dataspec-cli.mjs capability check --project 1 --format json
```

第一版内置 `capability-catalog`、`doctor`、`export-ai-context`、`lint-sql`、`search-fields`、`suggest-fields`、`generate-ddl`、`reverse-import`、`coverage-report`、`schema-registry`、`export-evidence-package`、`workflow-recipes`、`ai-task-profiles` 和 `domain-starter-kits`。写入型能力会标记 `writeRisk` 和 `preflightChecks`，AI 应先运行 `doctor` 或读取相关 workflow/profile，再执行实际工具。

## AI 任务模式

AI task profile 用来描述“当前任务建议怎么用 DataSpec”，第一版内置 `create-table`、`sql-fix`、`reverse-import`、`pr-review` 和 `minimal-context`。每个 profile 会返回 `taskType`、`contextScope`、`ruleset`、`fixedSqlPolicy`、`outputFormat`、`maxContextFields`、`recommendedCommands`、`nextActions` 和 diagnostics。

```bash
curl "http://localhost:8090/api/ai-profiles?projectId=1"
curl "http://localhost:8090/api/ai-profiles/sql-fix?projectId=1"
```

前端“校验与生成 / AI 任务模式”可查看当前项目可用 profile、诊断、推荐命令和当前选择；选择结果只保存在当前浏览器本地，供 SQL 校验等高频页面读取。SQL 校验页会默认跟随当前 profile 的 fixedSql 策略，也可以切到手动策略显式覆盖；后端仍遵循“显式 `fixPolicy` 优先于 profile 默认值”。

Profile 是默认建议契约，不是权限、审批、外部模型或 provider 配置。真实写入安全仍由 API Token、项目边界、dry-run、只读数据库诊断和人工确认负责。

## 标准快照

前端“系统设置 / 标准快照”提供项目级快照入口。创建快照时，DataSpec 会把当前项目字段、枚举和规则生成确定性 JSON payload，并计算 SHA-256 hash；同一份标准内容会得到稳定 hash，方便 AI Context、SQL 检查记录和 DDL 生成结果追溯当时使用的标准版本。

后端 API：

```bash
curl -X POST "http://localhost:8090/api/projects/1/standard-snapshots" \
  -H "Content-Type: application/json" \
  -d '{"version":"v2026.06.24","name":"AI Context 可复现基线"}'
```

第一版不做审批或发布流。快照 payload 可用于历史 AI Context 导出和 SQL 检查记录回放；更细粒度的字段级快照 diff、旧 DDL 完整重建和标准回滚仍保留为后续增强。

## 标准字段模型

标准字段支持 `aliases`、`category`、`domainId`、`tags`、`codeSetId`、`sensitive`、`status=draft|enabled|deprecated|disabled`、`replacementFieldId`、`replacementReason`、`exampleValue`、`formatType`、`formatPattern`、`formatUnit`、`formatPrecision`、`formatTimezone`、`formatNullPolicy`、`validExamplesJson`、`invalidExamplesJson` 和 `formatNotes` 等个人版元数据。字段库支持按数据域、分类、标签、生命周期状态和未分组状态浏览字段；编辑字段时可维护替代字段、替代说明和值格式约束，正例/反例在前端按“每行一个示例”编辑，保存为 JSON 字符串数组。批量维护支持状态、分类、标签、敏感标记、代码集和别名，提交前会先展示后端预览。每次批量维护和字段更新都会写入字段变更日志，字段行内“变更”入口可查看最近日志，并对带 before 快照的更新记录执行确认回退。AI 导出的 `field-catalog.json` 会把 `aliases` 转成数组，并输出敏感标记、字段状态、替代字段/说明、代码集关联、示例值、字段 `format` 对象和可选 `contextScope.groupSummary`，方便 AI 按业务语义复用标准字段、避开草稿/废弃/停用字段、识别未分组字段，并读取金额单位、手机号/邮箱格式、时间时区、JSON/状态码样例等值形态约束。

前端“基础数据 / 业务术语表”提供项目级 glossary 维护入口。每条术语可配置主术语、同义词、英文词根、拼音或历史缩写、禁用词、推荐 canonical 字段、适用范围、示例字段和状态；后端提供 `/api/glossary` 分页、`/api/glossary/all`、`/api/glossary/conflicts`、创建、更新和软删除接口。第一版只做确定性匹配和轻量冲突检测，不做企业级本体、知识图谱、向量检索或自动覆盖字段已有别名。

## 结构化命名规则

`rules.yaml` 会导出结构化 `naming:` 模型，包含表/字段 snake_case、必含列、禁用字段名、推荐替换、字段后缀/前缀类型规则。SQL lint 已支持 `field_suffix_type`，默认校验 `_id/_at/_no/_count` 和 `is_` 对应的数据类型；前端规则配置页已为必含列、禁用字段名、推荐替换、后缀/前缀类型提供结构化表单，并保留 JSON 预览和兜底编辑。

规则配置页还支持项目规则基线套件。内置 `personal_default`、`strict` 和 `legacy_compatible` 三套基线，可一键应用到当前项目；默认只新增缺失规则，不覆盖用户已调整的同编码规则，显式勾选覆盖后才会更新。项目规则可导出为稳定 JSON 包，也可导入到另一个项目；AI Context 的 `.dataspec/rules.yaml` 会输出 `baseline:` 节点，说明规则来自内置、导入还是自定义推断。

前端“模板与规则 / 规则例外”可为历史表、第三方字段或框架约定创建项目级豁免。豁免必须包含 `ruleCode`、原因，以及表名或字段名范围；禁用或过期后不再抑制 lint 问题。`LintIssue` 会保留 `suppressed/suppressionId/suppressionReason`，`LintResult` 的 active 统计会排除 suppressed issue，同时新增 `suppressedCount`。AI Context 的 `.dataspec/rules.yaml` 和 `.dataspec/DATABASE_RULES.md` 会导出规则例外，并说明这些例外不是新建表和新增字段的推荐标准。

## AI Prompt 生成

后端提供纯文本 Prompt 生成 API，不直接调用外部 LLM。建表、SQL 修正、SQL lint/fixedSql、DDL preview 和字段推荐解释模板已登记到 Prompt template registry，统一输出 `templateKey`、`promptVersion`、场景、输出格式、必备段落和变更说明；建表与 SQL 修正 Prompt 文本中会带上当前 `promptVersion`，AI 回放记录也使用同一版本。

- `POST /api/ai-context/prompts/create-table`：传入 `projectId` 和 `businessDescription`，生成建表 Prompt。
- `POST /api/ai-context/prompts/fix-sql`：传入 `projectId` 和 `sql`，先运行 DataSpec lint，再生成 SQL 修正 Prompt。
- `GET /api/prompt-templates`：查看已注册模板及其稳定输出约束。
- `POST /api/prompt-templates/evaluate`：传入 `templateKey` 和 `output`，本地检查必备段落、必备短语和版本标记，不调用外部 LLM。

Prompt golden fixture 位于 `dataspec-server/src/test/resources/fixtures/prompts/`，后端 `mvn test` 会覆盖 registry、评测和 create-table / fix-sql Prompt 契约；模板文本变更时，评测会输出可读 diff，便于审阅是否属于有意调整。

```bash
node tools/prompt-template-eval.mjs --format json
```

## AI 回放

前端“校验与生成 / AI 回放”展示当前项目最近的 AI 相关作业，包括建表 Prompt、SQL 修正 Prompt、SQL 检查修正和 DDL 预览。详情中可查看输入 payload、输出 payload、标准快照、prompt 模板版本、关联 SQL 检查记录，并复制回放 JSON 或查询命令。

后端 API：

```bash
curl "http://localhost:8090/api/ai-jobs?projectId=1&current=1&size=10"
curl "http://localhost:8090/api/ai-jobs/1"
```

第一版只记录 DataSpec 本地生成和检查链路，不内置外部 LLM 调用，不保存第三方 API key，不做长文本会话管理。

## AI 反馈

前端“校验与生成 / AI 反馈”展示当前项目的 AI 使用反馈报告，聚合最近 AI job、SQL 检查记录、规则例外、fixedSql、反向导入字段来源和字段元数据。页面展示 summary、字段信号、规则问题排行、修正 SQL 信号、未纳管/标准化信号、样本范围和下一步动作，并可跳转到字段库、字段质量、规则配置、规则例外、SQL 校验或 AI 回放。

后端 API：

```bash
curl "http://localhost:8090/api/ai-feedback/report?projectId=1"
```

第一版是只读聚合视图，不采集点击或停留等用户行为，不读取业务数据行，不保存 token/password/完整 JDBC URL，也不会自动创建字段、别名、规则例外或标准变更。现有记录无法证明字段推荐命中率时，报告会显式标记推荐历史不足，而不是伪造统计。

## AI 执行证据包

AI 执行证据包用于把一次 SQL 修复、AI job、AI 批量任务或覆盖率分析整理成可复制、可下载、可继续交给下游 AI 的只读交付物。JSON 包稳定包含 `kind`、`schemaVersion`、`packageId`、`projectId`、`generatedAt`、`source`、`standardSnapshot`、`inputsSummary`、`outputsSummary`、`validationSummary`、`artifacts`、`nextActions`、`suggestedCommands` 和 `diagnostics`；zip 包固定包含 `evidence.json`、`summary.md` 和 `README.md`。

后端 API：

```bash
curl -X POST "http://localhost:8090/api/evidence-packages" \
  -H "Content-Type: application/json" \
  -d '{"projectId":1,"sourceType":"SQL_CHECK","sourceId":42}'

curl -L -X POST "http://localhost:8090/api/evidence-packages/download" \
  -H "Content-Type: application/json" \
  -d '{"projectId":1,"sourceType":"AI_BATCH_RUN","sourceId":7}' \
  -o dataspec-ai-evidence.zip
```

前端入口：

- SQL 校验记录详情：复制证据 JSON、下载证据包。
- 覆盖率报告：对当前即时报告生成 payload 型证据包。
- AI 批量任务详情和列表：复制证据 JSON、下载 evidence zip。

证据包会对 token、password、Authorization、完整 JDBC URL 和自由文本 diagnostics 做统一脱敏；覆盖率与数据库 metadata 只包含 schema/字段/规则/count 摘要，不包含业务数据行。第一版不新增证据包表，不长期归档，不上传第三方，不做企业审计、审批、签名或防篡改系统。

## 标准候选 Inbox

前端“基础数据 / 标准候选”提供项目级候选采纳工作台。第一版支持手动创建候选字段，按状态、来源和关键词筛选，查看证据与置信度，并执行采纳、合并、忽略或延后。采纳会创建新的标准字段；合并只记录目标字段和决策原因，不会静默修改目标字段的别名、注释或类型。

后端 API：

```bash
curl "http://localhost:8090/api/standard-candidates?projectId=1&status=PENDING&current=1&size=10"
curl -X POST "http://localhost:8090/api/standard-candidates" \
  -H "Content-Type: application/json" \
  -d '{"projectId":1,"candidateName":"mobile","displayName":"手机号","dataType":"varchar","sourceType":"MANUAL","confidence":80}'
curl -X POST "http://localhost:8090/api/standard-candidates/1/accept" \
  -H "Content-Type: application/json" \
  -d '{"reason":"确认转正"}'
```

候选证据会做敏感信息脱敏，不保存 token/password/Bearer/完整 JDBC URL，不读取源数据库业务数据行。第一版不做审批流、不自动合并标准字段，也不把候选处理变成团队工单系统。

## 自然语言需求草案

前端“校验与生成 / 需求草案”支持输入业务描述、目标表名和可选分组提示，生成只读建表草案。草案会返回建议采用的已有标准字段、缺失候选字段、歧义词、推荐表模板、下一步动作和可复制 Prompt；推荐模板可带入 DDL 生成页，缺失候选可带关键词跳转到标准候选 Inbox。结果中的标准字段、缺失候选、歧义候选和推荐模板会展示 Explain Trace 证据来源，包含来源类型、来源 ID、命中原因、置信度和文档引用。

后端 API：

```bash
curl -X POST "http://localhost:8090/api/requirement-drafts" \
  -H "Content-Type: application/json" \
  -d '{"projectId":1,"description":"会员支付流水表，记录会员、支付金额、支付状态、第三方流水号","targetTableName":"pay_trade","groupHint":"payment","limit":10}'
```

第一版不调用外部 LLM，不新增持久化表，不自动创建标准候选或标准字段；它只做确定性检索、模板化草案和人工确认前的结构化准备。Explain Trace 仅引用 DataSpec 元数据和规则说明，不记录业务数据行，也不作为审批或写入授权。

## 字段推荐与字段标准检索

后端提供确定性字段推荐 API，不调用外部 LLM：

```bash
curl "http://localhost:8090/api/fields/suggest?projectId=1&query=用户手机号&limit=5"
```

推荐结果会返回已有标准字段、匹配分数、命中原因、推荐字段名、`existing` 标记和 `evidence[]`。当前推荐逻辑按字段名、显示名、注释、别名、分类、标签、业务术语表和内置语义词库匹配，覆盖 `uid/user_id`、`phone/mobile/tel/mobile_no`、`amount/price/fee/amount_cent`、`sfzh/id_card_no` 等常见叫法；默认只推荐 `enabled` 字段，草稿、废弃和停用字段不会作为 AI 既有字段推荐返回。泛化词会降权，敏感字段会在命中原因里提示。术语表命中 canonical 字段或示例字段时，命中原因会包含 `术语表：<术语> -> <字段>`；禁用词不会作为正向加权依据。未命中已有字段但命中已知语义组时，会优先生成 canonical snake_case fallback 字段名。

当 AI 或用户需要先查看一组相关标准字段时，可使用只读字段标准检索：

```bash
curl "http://localhost:8090/api/fields/search?projectId=1&query=用户手机号&limit=20"
curl "http://localhost:8090/api/fields/search?projectId=1&category=user&status=enabled&limit=20"
```

检索结果会返回 `summary`、`items[]` 和 `nextActions`。每个 item 包含标准字段、确定性分数、`matchReasons`、推荐使用范围、字段级下一步建议和 `evidence[]`；支持关键词、中文描述、别名、拼音缩写、业务术语表、category、tag、status、sensitive 和 sourceBatchId 过滤。默认检索只返回 `enabled` 字段；显式传 `status=draft|deprecated|disabled` 时，会返回对应历史字段并在 `recommendedUse`/`nextActions` 中说明状态、替代字段或替代原因。省略 query 且不提供任何结构化过滤时会返回校验错误，避免 AI 一次性拉取整个字段库。前端字段库的搜索框、生命周期状态筛选和 category/tag 分组会复用该检索结果并展示命中原因；清空搜索条件后仍使用原有字段列表。

## DDL 生成

后端提供基于表模板的 PostgreSQL DDL 生成 API，并在生成后复用 DataSpec lint 做自检：

```bash
curl "http://localhost:8090/api/generator/ddl/preview?projectId=1&templateId=1&tableName=user_order"
```

返回结果包含 `ddl`、`lintResult`、`standardSnapshot` 和 `dialectDiagnostics`。第一版只生成 PostgreSQL 风格 `CREATE TABLE` 与 `COMMENT ON` 文本，不执行数据库变更，也不生成迁移计划；如果目标库是 MySQL，诊断会提示需要先做类型、注释、自增和索引语法转换。

## SQL 校验记录与反向导入

`/api/lint` 会返回 lint 结果、结构化修复建议、`fixedSql`、`fixedSqlDiff`、`fixPolicy`、`fixChanges`、`fixSummary` 和 `dialectDiagnostics`，并保存 SQL 检查记录。`fixPolicy` 是请求级策略，支持 `GENERATE`、`DRY_RUN`、`DISABLED` 和最高风险等级；dry-run 只表示预览候选 SQL，不会写回业务仓库，应用前仍需人工确认 diff、风险和方言诊断。方言诊断第一版支持 PostgreSQL/MySQL：SQL 文本会根据 `COMMENT ON`、反引号、`AUTO_INCREMENT`、`ENGINE`、`DEFAULT CHARSET`、inline `COMMENT` 等特征做保守识别；混合或未知方言会返回稳定 code 与 nextAction，不会被静默标成已验证。前端 SQL 校验页支持查看修正 SQL、复制、修复策略摘要、变更风险、当前方言/降级提示、最近检查记录分页和详情；记录详情会展示当时标准、当前标准、回放状态、历史 Context 导出命令和下一步建议，并可复制只包含 `projectId`、页码和 `recordId` 的可复现链接。无快照的旧记录显示为 `unversioned`，仍保留原始 SQL 与问题列表。

反向导入页支持粘贴 SQL DDL，也支持 PostgreSQL/MySQL 数据库直连：填写连接信息后可测试连接、加载表、筛选并选择表、生成 metadata 预览、勾选字段候选并确认导入到当前项目字段库。SQL 文本和数据库直连预览都会展示方言诊断；PostgreSQL 直连会提示 schema 过滤，MySQL 直连会提示 databaseName 作为 catalog、schemaName 不参与过滤等边界。连接测试会返回 `security` 与 `health` 两组诊断：`security` 继续表达只读风险，`health` 输出连接状态、耗时、失败分类、retryable、数据库产品/版本、schema/comment/index capability、所需只读权限、warnings 和 nextActions；失败信息会统一脱敏，不返回 password、token 或完整 JDBC URL。直连模式还可以生成只读二次比对，按表展示已匹配、属性变化、新增、缺注释和非标准字段，并支持按状态筛选。页面会按项目在浏览器本地记住数据库类型、host、port、database、schema、username、表选择、搜索词和差异筛选，不保存数据库密码、token 或完整连接串。当前项目可保存多个数据库连接预设，服务端只持久化预设名、databaseType、host、port、databaseName、schemaName 和 tableNames；反向导入页可选择预设回填连接元数据和表选择，用户名和密码仍由用户当次输入。数据库直连预览会为字段输出 `EXISTING_MATCH`、`NEW_CANDIDATE` 等 mapping decision；确认导入时可填写 `confirmReason`，未勾选候选会记录默认忽略理由，导入结果返回 `batchId` 和 imported/skipped/ignored 决策摘要。确认导入创建的新字段会记录导入批次、来源 schema/table/column、原始 metadata 快照和字段级映射决策，字段库可查看来源摘要；从导入结果跳转字段库时会自动携带字段关键词并筛选当前页结果，带 `sourceBatchId` 的链接也能直达字段库来源筛选。直连模式不会修改源数据库，也不会做定时同步。

后端支持将直连 metadata 导出为离线 schema dump，并用同一份 dump 复现标准分析：

```bash
curl -X POST "http://localhost:8090/api/reverse-import/database/dump" \
  -H "Content-Type: application/json" \
  -d '{"projectId":1,"databaseType":"postgresql","host":"localhost","port":5432,"databaseName":"demo","schemaName":"public","username":"readonly","password":"<password>","tableNames":["user_order"]}'

curl -X POST "http://localhost:8090/api/reverse-import/dump/preview" \
  -H "Content-Type: application/json" \
  -d '{"projectId":1,"dump":{...}}'

curl -X POST "http://localhost:8090/api/reverse-import/dump/compare" \
  -H "Content-Type: application/json" \
  -d '{"projectId":1,"dump":{...}}'
```

schema dump 只包含 databaseType、databaseName、schemaName、表、列、类型、nullable、default、comment、tableType、generatedAt 和非敏感 source metadata；不会包含数据库密码、token、完整 JDBC URL 或业务数据行。

方言能力第一版边界：

| 方言 | 已验证能力 | 诊断边界 |
|---|---|---|
| PostgreSQL | `COMMENT ON TABLE/COLUMN`、`serial/bigserial`、schema 过滤、DDL 生成和 fixedSql 默认输出 | 未显式传方言时，非 MySQL 特征 SQL 会按 PostgreSQL/DataSpec 默认路径处理，并提示混合方言应显式确认 |
| MySQL | 常见 `CREATE TABLE`、反引号、inline `COMMENT`、表 `COMMENT`、`UNSIGNED`、`tinyint(1)`、`KEY`/`ENGINE`/`CHARSET` 解析兼容 | 索引模型、charset/collation、自增迁移和 fixedSql MySQL 原样输出仍标为 partial/warning |
| 其他 | 未验证 | 返回 unknown/unsupported 诊断，不标成已支持 |

数据库连接预设 API：

```bash
curl "http://localhost:8090/api/database-connection-presets?projectId=1"
curl -X POST "http://localhost:8090/api/database-connection-presets" \
  -H "Content-Type: application/json" \
  -d '{"projectId":1,"name":"本地只读库","databaseType":"postgresql","host":"localhost","port":5432,"databaseName":"dataspec_demo","schemaName":"public","tableNames":["users"]}'
```

## 字段覆盖率报告

前端“数据管理 / 覆盖率报告”可用 SQL DDL 或数据库直连生成即时覆盖率报告。报告包含项目级覆盖率、表级统计、字段明细和未纳管字段排行，并区分标准命中、别名命中、缺注释、疑似重复和未纳管状态。

后端 API：

```bash
curl -X POST "http://localhost:8090/api/coverage/sql" \
  -H "Content-Type: application/json" \
  -d '{"projectId":1,"sql":"CREATE TABLE user_order (id bigint NOT NULL);"}'
```

数据库直连报告使用 `/api/coverage/database`，请求体与反向导入直连请求一致；离线报告可使用 `/api/coverage/dump`，请求体为 `{"projectId":1,"dump":{...}}`。第一版只读取 metadata，不扫描业务数据行，不保存数据库密码，不自动导入未纳管字段。

## 字段质量评分

前端“基础数据 / 字段质量”按当前项目生成只读质量报告。报告会给每个标准字段输出分数、等级、问题和修复建议，并按低分优先展示；支持按质量等级和问题编码筛选，也可以跳转到字段库编辑具体字段。

后端 API：

```bash
curl "http://localhost:8090/api/fields/quality?projectId=1"
```

第一版实时计算，不新增质量评分表，不自动修改字段标准，不调用外部 LLM。当前检查项覆盖缺注释、缺别名、缺示例值、缺分类/标签、疑似敏感未标记、枚举/状态字段未关联代码集、废弃/停用字段缺少替代说明，以及金额、手机号、邮箱、时间戳、日期、JSON、状态/枚举/编码等格式敏感字段缺少格式约束或正例样例；若字段已配置结构化 `replacementFieldId` 或 `replacementReason`，不会再按“缺少替代说明”报问题。

## 字段冲突检测

前端“基础数据 / 字段冲突”按当前项目生成只读冲突报告。报告按冲突组展示字段名重复、别名冲突、显示名重复、语义疑似重复、SQL 保留字/危险命名、大小写碰撞、alias 歧义，以及数据类型、代码集、敏感标记和状态不一致等证据；支持按级别和类型筛选，并可跳转字段库编辑涉及字段。AI Context 的 `DATABASE_RULES.md` 会导出命名风险摘要，提醒 AI 生成新 DDL/SQL 时避让高风险字段名或歧义 alias。

后端 API：

```bash
curl "http://localhost:8090/api/fields/conflicts?projectId=1"
```

第一版实时计算，不新增冲突表，不自动合并字段，不删除历史字段，不做跨项目统一治理；保留字/危险词清单覆盖 PostgreSQL、MySQL 和通用 SQL 高频词，不追求完整方言字典。

## 字段影响分析

字段库每行提供“影响”入口，可查看字段在当前项目内被哪些表模板、数据库反向导入来源、最近 SQL 检查记录、标准快照和代码集引用。编辑字段名、类型、状态、代码集或敏感标记时，如果存在已知影响，前端会给出非阻断提示。

后端 API：

```bash
curl "http://localhost:8090/api/fields/1/impact?projectId=1"
```

第一版只读实时聚合，不新增影响表，不扫描生产查询日志，不做审批或发布阻断。

## 数据字典与导入导出

- Markdown 数据字典会输出项目概览、字段库、数据域、枚举字典、表模板和模板字段约束。
- HTML 数据字典支持浏览器离线打开；Mermaid ERD/关系图可展示字段、数据域、代码集和模板之间的关系。
- Excel 导入导出支持 `.xlsx` 模板下载、字段/代码集导出、导入预览、新增/更新/冲突统计、行级 dry-run 明细、字段级 before/after diff 和确认导入。

前端“数据管理 / 项目备份”可导出当前项目备份 JSON，也可以粘贴或上传备份包先执行恢复 dry-run。备份包包含项目元数据、数据域、标准字段、枚举、规则、规则基线、表模板、标准快照、反向导入来源摘要和必要变更日志摘要；导出与恢复流程会明确排除 password、API token、token hash、完整 JDBC URL 和源数据库业务数据行。

恢复默认写入新项目；也可选择恢复到当前项目。`overwrite=false` 时已有同名/同编码资产会跳过或标记冲突，显式开启覆盖后才更新支持覆盖的资产。确认恢复后会写入恢复摘要记录，记录 packageHash、来源项目、目标项目、覆盖模式、计数、warning 和操作者，但不保存完整备份包。

后端 API：

```bash
curl "http://localhost:8090/api/project-backups/export?projectId=1"
curl -X POST "http://localhost:8090/api/project-backups/restore/preview" \
  -H "Content-Type: application/json" \
  -d '{"overwrite":false,"backupPackage":{"schemaVersion":1,"assets":{},"packageHash":"..."}}'
curl "http://localhost:8090/api/project-backups/restore/records?projectId=1"
```

第一版面向个人/小团队迁移标准资产，不做源数据库物理备份、不保存数据库密码或 API token、不自动删除目标项目已有资产，也不做定时备份或远程对象存储。

## 工作台与变更记录

个人工作台提供项目级摘要：任务入口、最近任务、标准字段数、代码集数、规则数、禁用词数、SQL 检查数、字段命中率、最近检查、问题趋势和最近活动。任务入口覆盖数据库反向导入、SQL 校验、覆盖率报告、字段维护、DDL 生成、AI Context 导出和 API Token 管理；最近任务只保存在当前浏览器 localStorage，不写入后端。

项目活动时间线会按当前项目聚合字段变更、标准快照、反向导入批次、SQL 检查、AI job/DDL 记录和 API token 最近使用摘要，支持按动作类型筛选并跳转到相关详情页。活动 metadata 只返回安全摘要，不包含 SQL 原文、token hash/明文、数据库密码或连接串；token 使用摘要仅对全项目身份展示。

标准变更日志会记录字段、代码集/枚举值、规则配置的创建、更新、删除、启停操作，保留 before/after JSON 和操作者便于追溯。字段和规则配置支持保存前 What-if 预览：后端返回属性 diff、风险等级、影响项、建议验证命令、当前标准快照和回退提示；前端在字段编辑、规则编辑和规则启停前展示确认摘要。第一版只做非阻断预览和回退辅助，不做审批流，不自动回滚数据库，也不自动创建快照。

## 安全基线

个人本地开发默认不强制登录。小团队或 AI agent 长期接入时，可开启轻量 API Token 模式：

```yaml
dataspec:
  security:
    enabled: true
```

后端只保存 token 的 SHA-256 hash。安全模式关闭时可先在前端“系统设置 / API Token”创建首个 token，再开启安全模式；如果已经开启但没有全项目 token，可继续用 SQL bootstrap 写入一条全项目 token。示例：

```powershell
$token = "ds_your_token"
$hash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($token))).ToLower()
```

```sql
INSERT INTO ds_api_token(name, token_hash, operator_name, project_ids)
VALUES ('default-cli', '<hash>', 'alice', '1,2');
```

`project_ids` 使用逗号分隔项目 ID，`*` 表示全部项目。安全模式开启后，前端可在顶部 `API Token` 输入 token；CLI/MCP 可通过 `--dataspec-token`、`DATASPEC_TOKEN` 环境变量或 `.dataspec/config.json` 的 `apiToken` 传递 token。团队共享的 `.dataspec/config.json` 不建议提交明文 token，优先使用环境变量或本地忽略文件。

前端“系统设置 / API Token”提供日常管理入口：列表只展示 token 名称、操作者、项目范围、启停状态、创建时间、停用时间和最近使用时间，不返回 token hash；新建 token 时明文只显示一次，请立即复制保存。停用 token 后，后续 CLI/MCP/API 请求会被拒绝。

后端错误响应、数据库直连诊断、AI evidence package、项目备份安全扫描和 CLI 本地交付包已接入统一脱敏边界：`password`、`token`、`plainToken`、`tokenHash`、`apiKey`、`Authorization/Bearer`、完整 `jdbc:` URL 和 `connectionString` 会以 `[REDACTED]` 或 `***` 形式输出。允许持久化和展示的是项目 ID、databaseType、host、port、databaseName、schemaName、表名、字段名、规则统计和摘要计数等非敏感元数据；数据库密码、API token 明文、token hash、完整连接串和源数据库业务数据行不应进入日志、证据包、备份包或可复制错误消息。第一版不承诺识别所有自然语言隐私片段，遇到业务高敏样例仍应通过后续个人安全红线策略继续收紧。

## CLI

第一版 CLI 是 HTTP-backed wrapper，需要先启动 DataSpec 后端，默认连接 `http://localhost:8090`。在业务仓库中可运行 `init` 生成 `.dataspec/config.json`、`.dataspec/README.md`，并可选写入带 marker 的 `AGENTS.md` 片段：

```bash
# 初始化业务仓库接入配置，完成后会自动运行一次轻量 doctor
node tools/dataspec-cli.mjs init --project 1 --server http://localhost:8090 --default-path db/migrations --default-path sql --with-agents

# 输出 AI/CI 可解析的初始化结果；已有文件默认跳过
node tools/dataspec-cli.mjs init --project 1 --format json

# 明确需要覆盖 DataSpec 管理文件时使用 --force
node tools/dataspec-cli.mjs init --project 1 --force --with-agents
```

`init` 默认不覆盖已有 `.dataspec/config.json`、`.dataspec/README.md` 或 `AGENTS.md` 中的 DataSpec marker 片段，传 `--force` 才会覆盖 DataSpec 管理内容。初始化不会把明文 API token 写入可提交文件；安全模式下继续使用 `DATASPEC_TOKEN`、`--dataspec-token` 或本地忽略配置传递 token。

也可以手写 `.dataspec/config.json`，CLI 会从当前目录向上查找该文件；显式命令行参数优先于配置文件：

```json
{
  "projectId": 1,
  "server": "http://localhost:8090",
  "apiToken": "ds_optional_local_token",
  "defaultPaths": ["db/migrations", "sql"],
  "aiProfile": "sql-fix",
  "taskType": "SQL_FIX"
}
```

```bash
# 校验 SQL 文件，发现 ERROR 时退出码为 1，参数错误/网络错误退出码为 2
node tools/dataspec-cli.mjs lint examples/bad-example.sql --project 1 --format json

# 在含 .dataspec/config.json 的业务仓库中，可省略 --project 和 --server
node tools/dataspec-cli.mjs lint examples/bad-example.sql --format json

# 安全模式开启后传递 DataSpec API token
node tools/dataspec-cli.mjs lint examples/bad-example.sql --project 1 --format json --dataspec-token "$DATASPEC_TOKEN"

# 从 stdin 校验
cat examples/good-example.sql | node tools/dataspec-cli.mjs lint - --project 1 --format json

# 批量校验 SQL 文件或目录，发现任一 ERROR 时退出码为 1
node tools/dataspec-cli.mjs lint-files examples --project 1 --format json

# 批量校验并写出 AI 可交付 JSON 包，stdout 仍保持原 summary/files 结构
node tools/dataspec-cli.mjs lint-files examples --project 1 --format json --delivery-package dataspec-ai-batch.json

# 未传路径时，lint-files 会使用 config.json 的 defaultPaths
node tools/dataspec-cli.mjs lint-files --format json

# 在业务仓库中基于 git 变更和 defaultPaths 输出 AI 可读的变更文件、最小 Context 建议和下一步命令
node tools/dataspec-cli.mjs changed --format json

# 只校验本次变更中的 SQL 文件；发现任一 ERROR 时退出码为 1，无 SQL 变更时返回可恢复诊断
node tools/dataspec-cli.mjs lint-changed --format json

# GitHub Actions 中发布 PR diff inline 评论，并更新 fallback 汇总评论；有 ERROR 时评论后退出码为 1
node tools/dataspec-cli.mjs review-pr . --project 1 --repo owner/repo --pr 123 --token "$GITHUB_TOKEN" --server http://localhost:8090

# 输出 CI/AI 可解析的 inline、fallback 和 lint 统计
node tools/dataspec-cli.mjs review-pr . --project 1 --repo owner/repo --pr 123 --token "$GITHUB_TOKEN" --format json --server http://localhost:8090

# 导出 AI Context zip 包
node tools/dataspec-cli.mjs export-context --project 1 --output dataspec-ai-context.zip

# 导出按需 AI Context zip 包
node tools/dataspec-cli.mjs export-context --project 1 --scope field --query "用户手机号" --status enabled --limit 20 --output dataspec-ai-context-field.zip

# 按 AI profile 默认上下文导出；显式 scope/query/status/limit 仍优先
node tools/dataspec-cli.mjs export-context --project 1 --profile minimal-context --output dataspec-ai-context-min.zip

# 刷新业务仓库离线 AI Context 缓存，写入 .dataspec/context/
node tools/dataspec-cli.mjs export-context --project 1 --cache

# 推荐标准字段
node tools/dataspec-cli.mjs suggest-field "用户手机号" --project 1 --format json

# 检索一组相关标准字段，支持 category/tag/status/sensitive/source-batch 过滤
node tools/dataspec-cli.mjs search-fields "用户手机号" --project 1 --limit 20 --format json
node tools/dataspec-cli.mjs search-fields --project 1 --category user --status enabled --format json

# 基于表模板生成 DDL，并返回 lint 自检结果
node tools/dataspec-cli.mjs generate-ddl --project 1 --template 1 --table user_order --format json

# 导出 AI 执行证据包 JSON 或 zip
node tools/dataspec-cli.mjs evidence export --project 1 --source-type SQL_CHECK --source-id 42 --format json
node tools/dataspec-cli.mjs evidence export --project 1 --source-type AI_BATCH_RUN --source-id 7 --format zip --output dataspec-ai-evidence.zip

# 自检本地 DataSpec CLI 环境；存在失败检查时退出码为 1，参数错误退出码为 2
node tools/dataspec-cli.mjs doctor --project 1

# 输出 AI/CI 可解析的 JSON 自检结果
node tools/dataspec-cli.mjs doctor --format json

# 执行完整 OpenAPI schema 漂移检查
node tools/dataspec-cli.mjs doctor --check-openapi --format json

# 列出 AI task profiles；profile show 遇到未知 profile 时返回参数错误退出码
node tools/dataspec-cli.mjs profile list --project 1 --format json
node tools/dataspec-cli.mjs profile show sql-fix --project 1 --format json

# 列出 AI/CLI/MCP 可读取的任务化 workflow recipes
node tools/dataspec-cli.mjs workflow list --format json

# 查看某个 recipe 的输入、前置检查、步骤、产物和失败恢复建议
node tools/dataspec-cli.mjs workflow show create-table --format json

# 列出、查看和检查 AI 能力清单
node tools/dataspec-cli.mjs capability list --project 1 --format json
node tools/dataspec-cli.mjs capability show lint-sql --project 1 --format json
node tools/dataspec-cli.mjs capability check --project 1 --format json

# 指定后端地址
node tools/dataspec-cli.mjs lint examples/bad-example.sql --project 1 --format json --server http://localhost:8090
```

`doctor` 会检查配置文件、DataSpec 服务、API token 身份、项目可访问性、`defaultPaths`、`aiProfile/taskType`、OpenAPI 状态和 `.dataspec/context/` AI Context 缓存；默认只做轻量 OpenAPI 检查，传 `--check-openapi` 时会复用前端契约校验逻辑做完整 schema 漂移检查。`capability` 只读取能力目录，`check` 会校验核心 capability 是否存在，不会执行这些能力；服务不可达时会输出 DataSpecError 并建议先运行 doctor。`profile` 只读取和诊断任务模式；`--profile/--task-type` 可用于 `lint`、`lint-files`、`export-context` 和 `doctor`，并且显式命令行参数优先于 `.dataspec/config.json`。`workflow` 只输出任务计划和命令建议，第一版包含 `create-table`、`review-pr-sql`、`reverse-import-standards` 和 `export-min-context`，不会自动执行步骤或调用外部 LLM。`changed` 会读取当前业务仓库 git 变更、`.dataspec/config.json` 和 `defaultPaths`，输出变更文件、SQL 子集、被配置范围外忽略的数量、`scope=changed` 的最小 AI Context 建议和下一步命令；无 git 仓库、未配置 `defaultPaths` 或无变更时返回 JSON 诊断，不自动扫描全仓。`lint-changed` 复用同一发现结果，只对变更 SQL 文件调用 `/api/lint`，无 SQL 变更时不调用服务端。`lint`、`lint-files`、`lint-changed` 和 `review-pr` 支持 `--idempotency-key`，也可用 `DATASPEC_IDEMPOTENCY_KEY` 兜底传递 `Idempotency-Key` header；多文件 lint 会按文件路径派生子 key，避免同一 key 误复用到不同 SQL 文件。后端当前使用单机内存缓存和项目级 operation lock，适合个人/小团队重复点击和 AI 自动重试保护，不等同于分布式队列或服务重启后的持久幂等。`evidence export` 只读取服务端 evidence package API；JSON 会写 stdout 或 `--output` 文件，zip 必须显式提供 `--output`，且拒绝写出当前工作目录之外的路径。`lint-files` 会递归扫描传入目录下的 `.sql` 文件，并跳过 `.git`、`node_modules`、`dist`、`build`、`target` 等常见缓存/构建目录。默认输出 JSON 包含 `summary` 和 `files[]`，适合 CI 或 AI agent 读取；传 `--delivery-package <json>` 或 `--batch-package <json>` 时，会额外写出 `ai-batch-delivery@1` 交付包，包含 batchId、summary、items、issueSummary、fixedSqlSummary、evidence 和 nextActions，并对 token、password、Bearer、完整 JDBC URL 做脱敏。`review-pr` 会在批量 lint 后读取 PR diff，把能映射到新增/修改行的 SQL 问题发布为 GitHub inline review comment；无法映射的问题会保留在包含 `<!-- dataspec-sql-review -->` marker 的汇总评论中，并统计 fallback reason。重复运行会通过 `dataspec-inline-review` marker 跳过已发布的相同行规则评论；`--format json` 会输出 `summary`、`inline` 和 `files[]`，评论成功后仍会按 ERROR 情况返回 0 或 1。GitHub Actions 示例见 `.github/workflows/dataspec-sql-lint.yml.example`；复制到业务仓库后改名为 `.github/workflows/dataspec-sql-lint.yml` 并按实际方式启动 DataSpec 后端即可启用。

## MCP Server

第一版 MCP Server 同样是 HTTP-backed stdio adapter，需要先启动 DataSpec 后端。启动时可显式指定默认项目，也可在业务仓库中依赖 `.dataspec/config.json`：

```bash
node tools/dataspec-mcp.mjs --project 1 --server http://localhost:8090 --profile sql-fix --dataspec-token "$DATASPEC_TOKEN"

# 在含 .dataspec/config.json 的业务仓库中
node tools/dataspec-mcp.mjs
```

可在 MCP client 中按本地 stdio server 配置。当前暴露能力：

- resources：`capability-catalog`、`field-catalog`、`database-rules`、`rules-yaml`、`workflow-recipes`、`ai-task-profiles`、`schema-registry`，URI 形如 `dataspec://project/1/capability-catalog`；也支持只读全局能力清单 `dataspec://capability-catalog`。
- prompts：`dataspec_create_table`、`dataspec_review_sql`、`dataspec_design_fields`，并提示 agent 先读取 capability catalog、schema registry 和 profile resource，再选择稳定字段名、兼容策略、上下文范围、fixedSql 模式和输出格式。
- tools：`lint_sql`、`get_field_catalog`、`search_field_catalog`、`suggest_fields`、`search_fields`、`generate_table_ddl`、`export_evidence_package`；`lint_sql`、`get_field_catalog` 和 `search_field_catalog` 可接收 `profileId/taskType` hint，显式工具参数仍优先于默认 profile；`get_field_catalog` 可传 `scope/query/status/limit`，`search_field_catalog` 默认按当前关键词读取较小字段目录，`search_fields` 调用 `/api/fields/search` 并返回字段、分数、命中原因和下一步建议；`lint_sql` 返回结构化 lint 结果，SQL 存在 ERROR 时仍视为工具调用成功；`export_evidence_package` 返回 `structuredContent` 与可解析 JSON text，用于交付前导出只读证据包。

## AI 输出契约

[docs/ai-contracts.md](docs/ai-contracts.md) 记录第一版 AI 可依赖的稳定字段，覆盖 Schema Registry、AI Context、SQL lint/fixedSql、字段推荐、字段检索、DDL 预览、CLI JSON 和 MCP resources/tools。兼容策略是：新增可选字段默认兼容；删除、改名、类型变化或语义变化需要同步更新契约测试和文档。

Schema Registry 是只读输出结构契约，不是鉴权、审批、发布流程或写入安全策略。服务端提供 `GET /api/contracts` 和 `GET /api/contracts/{contractId}`，返回 `kind/schemaVersion/registryVersion/compatibilityPolicy/contracts[]`、稳定字段、废弃字段、JSON Schema 和兼容窗口。AI Context zip 会额外包含 `.dataspec/schema-registry.json`，manifest 的 `contracts` 摘要会记录 registry schemaVersion、registryVersion、文件路径和 contractIds，离线 agent 可先读取它再消费字段目录、规则和 lint 结果。

CLI 可用下面命令读取或检查 registry：

```bash
node tools/dataspec-cli.mjs contract list --format json --server http://localhost:8090
node tools/dataspec-cli.mjs contract show lint-result --format json --server http://localhost:8090
node tools/dataspec-cli.mjs contract check --format json --server http://localhost:8090
```

`contract check` 只做轻量 invariants 检查，例如核心 contract id、schemaVersion、stableFields、deprecatedFields 和 compatibilityPolicy 是否存在；它不替代 OpenAPI 漂移检查，也不表示当前 token 拥有任何项目写入权限。

## AI 可读错误诊断

失败的 JSON API 响应会保留既有 `code/message/data`，并额外返回可选 `error` 诊断对象，方便 AI、CLI 和 MCP 判断下一步动作：

```json
{
  "code": 400,
  "message": "projectId 参数无效: abc",
  "error": {
    "code": "PROJECT_ID_INVALID",
    "category": "VALIDATION",
    "retryable": true,
    "suggestedAction": "提供有效 projectId；不确定时先运行 dataspec doctor --format json 查看当前项目状态。",
    "docsRef": "README.md#验证"
  }
}
```

第一版覆盖 token、项目权限、projectId、资源不存在、SQL 输入、数据库连接、参数校验和内部错误等高频场景。CLI 在保留 `错误: ...` 人类可读行的同时，会额外输出 `DataSpecError: {...}`；MCP 会把同类诊断放到 JSON-RPC `error.data.dataspecError`。该对象用于诊断和恢复建议，不暴露内部异常堆栈，也不改变原有 HTTP 状态语义。

## 验证

```bash
# 根据当前 git 改动推荐最小验证命令；JSON 输出适合 AI agent 读取
node tools/dataspec-verify-advisor.mjs --changed
node tools/dataspec-verify-advisor.mjs --changed --format json

# 从 TODO 条目生成 OpenSpec change 草稿；先 dry-run 再写入
node tools/dataspec-todo-openspec-handoff.mjs --item P6-48 --dry-run --format json
node tools/dataspec-todo-openspec-handoff.mjs --item P6-48

# 后端单元测试
cd dataspec-server
mvn test

# 前端类型检查与生产构建
cd ../dataspec-web
pnpm test
pnpm build

# OpenAPI 类型契约防漂移；默认读取 http://localhost:8090/api-docs
pnpm check:api

# CI 或离线场景可指定 OpenAPI JSON/YAML 文件
pnpm check:api -- --source ./api-docs.json

# CLI/MCP 单元测试
cd ..
node --test tools/dataspec-config.test.mjs tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs tools/dataspec-verify-advisor.test.mjs tools/dataspec-todo-openspec-handoff.test.mjs

# 本地启动包与 smoke 脚本契约测试
node --test tools/dataspec-local-smoke.test.mjs

# 已安装 Docker 时，可验证 Compose 配置解析
docker compose -f docker-compose.local.yml config

# OpenSpec 主规格与 active change 验证
npx openspec validate --all
```

`node tools/dataspec-verify-advisor.mjs --changed --format json` 会按变更路径推荐最小验证集，输出命令、原因、工作目录、预计耗时和下一步动作；它只给建议，不自动执行命令。`node tools/dataspec-todo-openspec-handoff.mjs --item P6-48 --dry-run --format json` 会从 TODO 条目生成 OpenSpec 草稿计划，确认后去掉 `--dry-run` 写入 `openspec/changes/<change-id>/`；它只生成 proposal/design/spec/tasks 草稿，不实现代码、不提交、不归档。后端 `mvn test` 已包含核心 fixture/golden 回归测试、Prompt 模板 registry/eval、AI contract fixtures、AI evidence package、AI 可读错误诊断、幂等写保护、标准变更 What-if 预览和合成性能基线，覆盖 PostgreSQL/MySQL SQL 样例、fixedSql golden 输出、Prompt golden 输出、反向导入 metadata 预览摘要、Schema Registry、AI Context、lint/fixedSql、字段推荐、字段检索、DDL 预览、执行证据包稳定字段与脱敏、写入重复 key/任务锁冲突/AI job 去重、字段/规则变更预览风险与回退提示，以及千级字段库下的字段分组、字段推荐、AI Context 字段目录和反向导入 compare。前端 `pnpm test` 已包含关键流程源码级冒烟门禁，覆盖路由导航、项目选择、统一请求状态、SQL 校验 fixedSql/记录/evidence 入口、数据库反向导入、字段库检索命中原因、标准变更预览确认、标准候选、筛选与批量维护、DDL 生成、AI Context、覆盖率报告 evidence 入口、AI 回放、AI 反馈、AI 批量任务 evidence 入口和项目备份恢复的核心页面/API 耦合，以及关键按钮、空状态和失败重试入口；它不需要浏览器、后端服务或截图依赖。`node --test` 覆盖 CLI/MCP JSON 契约、Prompt fixture 脚本、本地 smoke 脚本、验证建议工具和 TODO 到 OpenSpec 交接助手，包括 contract list/show/check、evidence export、schema-registry resource、workflow recipes、resource/tool `structuredContent`、字段标准检索、可解析文本内容、Prompt golden marker、Idempotency-Key 透传、API 失败时的 `DataSpecError` / `error.data.dataspecError` 诊断透传、变更路径到验证命令的推荐规则、TODO 条目到 OpenSpec 草稿的字段保留和覆盖保护，以及本地启动包的参数解析、输出结构、敏感信息脱敏和 compose/Vite 代理契约。`npx openspec validate --all` 用于校验当前 `openspec/specs/` 主规格和仍处于 active 状态的 change；已完成 change 应归档到 `openspec/changes/archive/`，主规格作为后续开发的权威入口。

## 性能基线

本地可单独运行合成大字段库性能基线：

```bash
cd dataspec-server
mvn -Dtest=PerformanceBaselineTest test
```

该测试会构造 5000 个标准字段和 2000 个 compare 列，输出 `[dataspec-perf-baseline]` metric 行，便于修改字段推荐、AI Context 或反向导入逻辑后做本地对比。它不是生产容量承诺，只用宽松阈值拦截明显退化。后端核心入口还会在字段分页/全量读取/分组/推荐、AI Context 字段目录/zip、SQL 检查记录分页和反向导入 compare 超过保守阈值时输出 `DataSpec slow operation` warning，并附带诊断 hint。

## 项目结构

```
data-spec/
├── dataspec-server/          # Spring Boot 后端
│   └── src/main/java/com/dataspec/
│       ├── common/           # 通用：响应封装、异常处理、配置
│       ├── aiprofile/        # AI task profile 与任务模式建议
│       ├── aireplay/         # AI 作业回放记录
│       ├── coverage/         # 字段覆盖率报告
│       ├── project/          # 项目空间
│       ├── field/            # 标准字段库
│       ├── domain/           # 数据域
│       ├── enumdict/         # 枚举字典
│       ├── template/         # 表模板
│       ├── rule/             # 规则配置
│       ├── standards/        # 内置标准初始化
│       ├── lint/             # SQL 校验引擎 + 规则实现
│       ├── aicontext/        # AI 规则导出
│       ├── dashboard/        # 个人工作台
│       ├── generator/        # Markdown 数据字典与 DDL 生成
│       ├── requirementdraft/  # 自然语言需求到标准候选草案
│       ├── importexport/     # 导入导出
│       ├── projectbackup/    # 项目备份恢复
│       ├── changelog/        # 标准变更日志
│       ├── standard/         # 标准版本快照
│       ├── standardhealth/   # 标准健康快照、趋势和改进计划
│       ├── dbpreset/         # 数据库直连非敏感连接预设
│       ├── reverseimport/    # SQL 反向导入
│       └── security/         # API Token 认证与管理
├── dataspec-web/             # Vue 3 前端
├── standards/                # 内置标准 YAML/JSON
├── tools/                    # CLI 与 MCP adapter
├── openspec/                 # OpenSpec 主规格、active change 与 archive 历史记录
└── examples/                 # 示例 SQL
```

## 后端模块

每个业务模块遵循 **Entity → Mapper → Repository → Service → Controller** 五层架构：

| 模块 | 路径 | 说明 |
|------|------|------|
| project | /api/projects | 项目空间管理 |
| aiprofile | /api/ai-profiles | AI task profile 与任务模式建议 |
| aireplay | /api/ai-jobs | AI 生成与修复回放 |
| aibatch | /api/ai-batches | AI 批量任务交付包 |
| evidence | /api/evidence-packages | AI 执行证据包 JSON/zip |
| coverage | /api/coverage | 字段覆盖率报告 |
| field | /api/fields | 标准字段库 CRUD |
| domain | /api/domains | 数据域管理 |
| enumdict | /api/enums | 枚举字典 + 枚举值 |
| template | /api/templates | 表模板 + 模板字段 |
| rule | /api/rules | 规则配置管理 |
| rulebaseline | /api/rule-baselines | 规则基线套件、应用、导入导出 |
| ruleexemption | /api/rule-exemptions | 项目级规则例外管理 |
| lint | /api/lint | SQL 粘贴校验 |
| dashboard | /api/dashboard | 个人工作台统计 |
| auth | /api/auth | API token 当前身份 |
| tokens | /api/tokens | API token 创建、列表与停用 |
| generator | /api/generator | Markdown 数据字典与 DDL 生成 |
| requirementdraft | /api/requirement-drafts | 自然语言需求草案 |
| aicontext | /api/ai-context | AI 规则导出 |
| importexport | /api/import-export | 字段导入导出 |
| projectbackup | /api/project-backups | 项目备份、恢复 dry-run 和恢复摘要 |
| changelog | /api/change-logs | 标准变更日志 |
| standard-snapshots | /api/projects/{projectId}/standard-snapshots | 标准版本快照 |
| standard-health | /api/standard-health | 标准健康快照、趋势和 AI 可复制改进计划 |
| dbpreset | /api/database-connection-presets | 数据库直连非敏感连接预设 |
| reverse-import | /api/reverse-import | SQL 与数据库直连反向导入 |

## 规则引擎

规则引擎采用插件式设计，每条规则实现 `LintRule` 接口，Spring 自动发现和注册：

| 规则编码 | 说明 | 级别 |
|----------|------|------|
| table_naming_snake_case | 表命名必须 snake_case | ERROR |
| field_naming_snake_case | 字段命名必须 snake_case | ERROR |
| forbidden_field_name | 禁用字段名（uid、create_time 等） | ERROR |
| recommended_field_name | 推荐字段名（create_time → created_at） | SUGGESTION |
| required_columns | 业务表必含列（id、created_at、updated_at、is_deleted） | ERROR |
| field_suffix_type | 字段后缀/前缀应匹配推荐类型 | WARNING |
| amount_field_type | 金额字段应使用 bigint/numeric | WARNING |
| comment_missing | 字段/表注释缺失 | SUGGESTION |

`LintIssue` 除 `severity/ruleCode/message/tableName/columnName` 外，还会输出可定位时的 `line/column/lineEnd/columnEnd/sourceStart/sourceEnd/locationKind`，并在可确定修复时输出 `suggestion`、`replacement`、`before`、`after`、`confidence`。这些字段会通过 API、CLI 和 MCP 原样返回，供 AI agent 生成修正 SQL、修复说明或文件级 review 定位。

## 已完成功能清单

- [x] 项目空间、顶部当前项目联动和内置 standards 初始化
- [x] 演示项目与首次使用入口，串联 DDL 生成、SQL 校验和 AI Context 导出
- [x] 标准字段、数据域、枚举字典、表模板、规则配置、规则基线、规则例外 CRUD 与常见规则结构化参数表单
- [x] 标准快照、内容 hash、AI Context 版本标识、SQL 检查记录和 DDL 生成结果快照引用
- [x] 个人版字段模型：别名、数据域、分类、标签、代码集、敏感标记、状态、示例值
- [x] 字段库分组视图与批量归组，支持按数据域、分类、标签和未分组字段筛选维护
- [x] 字段库批量维护和单条变更回退，支持状态、分类、标签、敏感标记、代码集、别名的预览式批量更新
- [x] 字段值格式与校验样例库，支持格式类型、pattern、单位、精度、时区、空值策略、正例/反例和 AI Context `format` 导出
- [x] 业务术语表与同义词词根库，支持项目级术语维护、冲突检测、字段推荐/检索增强和 AI Context glossary 导出
- [x] 标准候选 Inbox，支持候选新建、筛选、采纳、合并、忽略、延后和决策记录
- [x] 自然语言需求草案，支持从建表描述输出标准字段、缺失候选、歧义点、推荐模板、下一步和可复制 Prompt
- [x] 前端关键流程源码级冒烟门禁，覆盖路由、项目选择、SQL 校验记录、反向导入、字段库、DDL、AI Context、覆盖率、AI 回放入口和关键按钮/空状态
- [x] 结构化命名规则导出和 `field_suffix_type` lint 规则
- [x] SQL 粘贴校验、结构化 issue、修复建议、`fixedSql`、请求级修复策略、dry-run 和变更风险解释
- [x] SQL 检查记录、最近记录分页、详情和标准快照回放提示
- [x] SQL issue source range，支持表/字段/COMMENT 定位、前端跳转和 PR 汇总评论行列范围展示
- [x] PostgreSQL/MySQL 方言诊断，覆盖 lint/fixedSql、DDL 生成、SQL/数据库反向导入和 CLI 文本摘要
- [x] PostgreSQL `COMMENT ON` 解析和常见 MySQL `CREATE TABLE` / `UNSIGNED` / 表选项解析
- [x] 字段覆盖率报告，支持 SQL/DDL、数据库直连 metadata 和 schema dump 生成覆盖率与未纳管字段排行
- [x] 字段质量评分，支持低质量字段筛选、问题编码和跳转字段库编辑
- [x] 标准健康趋势，支持创建项目级健康快照、查看本周/月变化、Top actions 和复制 AI 改进计划
- [x] 字段冲突检测，支持别名冲突、语义疑似重复、属性不一致、SQL 保留字/危险命名、大小写碰撞、alias 歧义和跳转字段库编辑
- [x] 字段影响分析，支持模板、导入来源、SQL 检查记录、标准快照和代码集影响提示
- [x] 大字段库性能基线和慢操作 warning，覆盖字段分组/推荐、AI Context 字段目录和反向导入 compare
- [x] 字段推荐与字段标准检索 API/CLI/MCP，字段库可展示检索命中原因和下一步建议，并输出 Explain Trace evidence
- [x] DDL 生成 API/CLI/MCP 和前端预览下载
- [x] AI Context zip 导出、按需裁剪、历史快照导出、离线 `.dataspec/context/` 缓存、分组摘要、workflow recipes 和业务项目 `.dataspec/` 约定
- [x] AI task profiles，支持 `create-table`、`sql-fix`、`reverse-import`、`pr-review`、`minimal-context` 默认建议，前端可查看切换，CLI/MCP/doctor 可读取诊断
- [x] AI 输出契约文档与 contract fixtures，覆盖 AI Context、lint/fixedSql、字段推荐、字段检索、DDL 预览、CLI/MCP JSON 稳定字段
- [x] AI 可读错误诊断，API 失败响应、CLI stderr 和 MCP JSON-RPC error 可输出 code/category/retryable/suggestedAction/docsRef
- [x] `dataspec init` 业务仓库初始化向导，生成 `.dataspec` 配置、README、可选 AGENTS 片段并运行 doctor
- [x] AI 建表 Prompt、SQL 修正 Prompt、Prompt template registry 和本地评测
- [x] AI 回放记录，支持查看 Prompt、lint/fixedSql、DDL 预览的输入输出和标准快照
- [x] AI 反馈报告，按项目聚合字段、规则、fixedSql、未纳管信号和下一步维护动作
- [x] AI 批量任务交付包，支持后端保存 SQL lint batch run、CLI 写出同构 package、前端查看详情并下载 JSON
- [x] AI 执行证据包，支持 SQL_CHECK、AI_JOB、AI_BATCH_RUN 和 COVERAGE_REPORT 生成 JSON/zip，前端复制/下载，CLI/MCP 机器读取，并默认脱敏
- [x] AI 输出引用证据与 Explain Trace 第一版，覆盖字段推荐、字段检索和自然语言需求草案的 evidence 来源、置信度和文档引用
- [x] AI/CLI 并发写入幂等与任务锁第一版，覆盖标准快照、反向导入确认、AI 批量 SQL lint、项目恢复 apply、AI job 回放记录和 CLI Idempotency-Key 透传
- [x] 标准变更 What-if 预览与回滚辅助第一版，覆盖字段编辑、规则编辑、规则启停的 diff、影响、验证命令、当前快照和回退提示
- [x] Markdown/HTML 数据字典增强和 Mermaid ERD 输出
- [x] Excel `.xlsx` 字段/代码集导入导出与 dry-run 明细预览
- [x] 项目备份恢复，支持项目标准资产 JSON 导出、恢复 dry-run、冲突预览、确认恢复和恢复摘要记录
- [x] 标准变更日志和操作者记录
- [x] 个人/小团队 API Token 安全基线、管理页面、项目边界和 CLI/MCP token 透传
- [x] 个人工作台和字段命中率报告
- [x] 项目活动时间线，聚合字段变更、快照、反向导入、SQL 检查、AI job 和全项目可见的 token 使用摘要
- [x] SQL 反向导入预览与差异分析
- [x] 数据库直连反向导入与前端确认导入流程
- [x] 数据库直连二次比对，按表展示标准命中、属性变化、新增、缺注释和非标准字段
- [x] 数据库直连导入来源、批次追踪和字段映射决策，字段库可查看来源摘要，反向导入结果可回看确认/忽略理由
- [x] 前端反向导入高频流程记忆，按项目恢复非敏感连接信息、表选择、筛选状态和字段库关键词跳转
- [x] 数据库直连非敏感连接预设，支持项目级保存、选择复用和表选择恢复，不持久化用户名、密码、token 或 JDBC URL
- [x] 数据库连接健康探测与方言能力画像，连接测试返回健康状态、失败分类、重试建议、metadata capability、只读权限提示和前端诊断展示
- [x] DataSpec CLI：`doctor`、`profile list/show`、`workflow list/show`、`contract list/show/check`、`evidence export`、`lint`、`lint-files`、`changed`、`lint-changed`、`review-pr`、`export-context`、`suggest-field`、`search-fields`、`generate-ddl`，支持 `.dataspec/config.json` 默认项目配置、AI profile 默认值、业务仓库 git 变更感知、AI batch delivery package 文件输出、按需/历史快照 Context 导出、AI evidence package 导出和 PR diff inline/fallback SQL Review
- [x] DataSpec MCP Server：resources、`ai-task-profiles`、`workflow-recipes`、`schema-registry`、prompts、`lint_sql`、`get_field_catalog`、`search_field_catalog`、`search_fields`、`suggest_fields`、`generate_table_ddl`、`export_evidence_package`，支持 `.dataspec/config.json` 默认项目配置和 profile hint
- [x] GitHub Actions 示例和 PR inline/fallback 评论式 SQL Review
- [x] 本地 Docker Compose 一键启动包和 demo smoke 验证，支持 PostgreSQL/后端/前端联动、端口覆盖、依赖缓存、text/json 输出和敏感信息脱敏
- [x] 前端统一数据状态第一版，Dashboard、AI 批量任务、覆盖率报告和 SQL 校验记录区支持一致的项目缺失、空数据、失败建议和重试入口

## 暂缓探索

- 多方言完整规则体系：当前已覆盖 PostgreSQL/MySQL 能力矩阵和诊断，Oracle/SQL Server 等其他方言后续按实际场景补充。
- 审批流、发布流和复杂 RBAC：当前定位个人/小团队优先，只保留轻量 API Token 与项目边界。
