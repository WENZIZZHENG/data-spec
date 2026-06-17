# DataSpec 待办路线图

本文件记录当前仍需行动的产品与工程待办。优先级按用户可感知价值、核心链路阻断程度和后续开发解锁程度排序。

## 下一步顺序

1. 先让 DataSpec 变成个人/小团队可自用、AI 可消费的规范上下文和可调用工具，而不是只做给人看的后台。
2. 优先交付 AI Context 导出包、CLI 和 MCP Server，让 Codex/Cursor/Claude Code/CI 能直接使用字段标准。
3. 再打通 SQL 校验端到端闭环和字段推荐、DDL 生成、自动修复建议，让 AI 能“写表前查标准，写表后自检”。
4. 最后补齐项目、字段库、规则配置页面，以及数据库反向导入、CI PR 评论等团队化能力。

## P0：AI 可消费主线

### P0-1：升级 AI Context 导出包
- 状态：已完成，已新增 `dataspec-ai-context.zip` 导出能力，包含 `.dataspec/` 上下文、JSON Schema、Prompt、示例 SQL 和 `AGENTS.md.fragment`。
- 类型：AI 上下文、文档生成、规范导出。
- 背景/问题：当前已有 `DATABASE_RULES.md`、`field-catalog.json`、`rules.yaml` 单项导出，但 AI 编程工具更需要一次性、结构化、可放进业务仓库的上下文包。
- 已有基础：后端已有 `AiContextExportService`，可生成规则文档、字段目录和规则 YAML。
- 缺口：缺少 zip 打包、`.dataspec/` 目录约定、`AGENTS.md` 片段、JSON Schema、few-shot 示例和 AI prompt 模板。
- 建议方案：新增 `dataspec-ai-context.zip` 导出，包含 `.dataspec/DATABASE_RULES.md`、`.dataspec/field-catalog.json`、`.dataspec/field-catalog.schema.json`、`.dataspec/rules.yaml`、`.dataspec/prompts.md`、`.dataspec/examples/good.sql`、`.dataspec/examples/bad.sql` 和 `AGENTS.md.fragment`。
- 涉及文件/模块：`dataspec-server/src/main/java/com/dataspec/aicontext`、`standards/`、`examples/`，后续可补前端 `AiExport.vue`。
- 验收标准：下载包解压后可直接复制到业务项目；AI agent 能从 `AGENTS.md.fragment` 和 `.dataspec/field-catalog.json` 理解字段命名、类型、注释和规则。
- 参考项目/资料：[`agents.md`](https://agents.md/)、[OpenAI Codex AGENTS.md 文档](https://developers.openai.com/codex/guides/agents-md)。
- 不做/边界：本任务只生成上下文文件，不自动修改外部业务仓库。

### P0-2：提供 DataSpec CLI 给 AI 和 CI 调用
- 状态：已完成第一版，已提供 HTTP-backed CLI，支持 `lint <path|-> --format json` 和 `export-context --output <zip>`；`generate-ddl`、`suggest-field` 待对应能力成熟后接入。
- 类型：命令行工具、自动化入口、AI 工具调用。
- 背景/问题：AI agent 和 CI 更适合调用命令行；如果只有 Web API/后台页面，DataSpec 很难进入真实编码工作流。
- 已有基础：后端已有 lint、AI context、generator 等 service；示例 SQL 已在 `examples/`。
- 缺口：缺少批量文件扫描、stdin 输入、JSON 输出、退出码约定和本地命令入口。
- 建议方案：提供 `dataspec lint <path|-> --project <id> --format json`、`dataspec export-context --project <id> --target codex`、`dataspec generate-ddl --template <id>`、`dataspec suggest-field "用户手机号"` 等命令。
- 涉及文件/模块：后端可先新增 Spring Boot CLI profile 或独立 command 模块；后续再评估是否拆成轻量 native/Node wrapper。
- 验收标准：AI 或 CI 可通过命令校验 SQL；发现 ERROR 时返回非 0 退出码；`--format json` 输出可被 agent 稳定解析。
- 参考项目/资料：[`sqlfluff/sqlfluff`](https://github.com/sqlfluff/sqlfluff) 的 lint CLI、[`ariga/atlas`](https://github.com/ariga/atlas) 的 schema 工作流。
- 不做/边界：第一阶段不发布包管理器，不做完整安装器。

### P0-3：建设 DataSpec MCP Server
- 状态：已完成第一版，已提供 HTTP-backed stdio MCP adapter，支持 resources/prompts，以及 `lint_sql`、`get_field_catalog` 两个核心工具；`suggest_fields`、`generate_table_ddl`、`explain_lint_issue` 待对应后端能力成熟后接入。
- 类型：MCP 集成、AI 工具协议、新架构边界。
- 背景/问题：如果 DataSpec 要优先服务 AI，最自然的形态是 MCP Server：暴露规范资源、标准 prompt 和可执行工具。
- 已有基础：后端已有项目、字段、规则、lint、生成器、AI context 服务。
- 缺口：缺少 MCP resources/prompts/tools 映射、鉴权边界、项目选择和错误输出协议。
- 建议方案：先实现只读 resources 和核心 tools：`lint_sql`、`get_field_catalog`、`suggest_fields`、`generate_table_ddl`、`explain_lint_issue`；再补 prompts：`按 DataSpec 创建表`、`评审 SQL`、`把业务需求转字段设计`。
- 涉及文件/模块：可新增 `dataspec-mcp` 子模块，或在后端新增 MCP adapter 层；复用现有 service，不复制业务逻辑。
- 验收标准：MCP client 能列出 DataSpec resources；AI 能调用 `lint_sql` 得到结构化问题；能读取字段目录并用于生成 SQL。
- 参考项目/资料：[Model Context Protocol 规范](https://modelcontextprotocol.io/specification/2025-06-18)、[`modelcontextprotocol/servers`](https://github.com/modelcontextprotocol/servers)。
- 不做/边界：第一阶段不做多租户权限模型和远程 SaaS 托管。

### P0-4：补齐个人版标准字段模型
- 状态：已完成第一版，`ds_field` 已扩展 `aliases/category/codeSetId/sensitive/status/exampleValue`，AI 字段目录和 schema 已同步输出；字段库前端表单重做留到 P1 体验专项。
- 类型：数据模型、标准字段库、AI 上下文基础。
- 背景/问题：当前 `ds_field` 已支持字段名、显示名、类型、长度、注释、数据域和 tags，但附件中的个人版字段标准还需要别名、敏感标记、启用/停用/废弃状态、代码集关联、示例值等信息。
- 已有基础：已有标准字段 CRUD、数据域、枚举字典和 `standards/fields/standard-fields.yaml`。
- 缺口：缺少字段别名表或结构化别名字段；缺少 `sensitive`、`enabled/deprecated`、`example`、`code_set_id`；当前 tags 不能替代可检索别名。
- 建议方案：在保持个人版简单性的前提下扩展字段模型：`aliases`、`category/domain`、`codeSetId`、`sensitive`、`status(enabled/disabled/deprecated)`、`example`；字段搜索、AI 导出和 lint 都读取同一模型。
- 涉及文件/模块：`dataspec-server/src/main/resources/db/schema.sql`、`field`、`domain`、`enumdict`、`aicontext`、`standards/fields`。
- 验收标准：能维护 `mobile_no` 的别名 `phone/mobile/tel/user_phone`；AI 导出的字段目录包含别名、敏感标记、代码集关联和状态。
- 参考项目/资料：附件中的个人版字段 JSON 结构。
- 不做/边界：不引入草稿、审核、发布等生命周期流程。

### P0-5：结构化命名规则配置
- 类型：规则配置、SQL lint、个人规范沉淀。
- 背景/问题：当前已有 `RuleConfig.paramsJson` 和若干固定 lint 规则，但附件要求的 field/table case、后缀规则、禁用词、拼音缩写、泛化词等还没有形成个人可维护的结构化配置。
- 已有基础：`ds_rule_config`、`ForbiddenFieldNameRule`、`RecommendedFieldNameRule`、`RequiredColumnsRule`、`FieldNamingSnakeCaseRule`。
- 缺口：缺少统一 `naming-rules.yaml/json` 导出模型；缺少后缀规则、拼音/无意义词检测、表名前缀提示、字段类型/长度规则配置页面。
- 建议方案：先用内置默认规则 + `paramsJson` 配置支撑个人使用，再逐步提供 UI：基础命名、后缀规则、禁用词、推荐替换、公共字段。
- 涉及文件/模块：`rule`、`lint/rules`、`aicontext`、前端 `RuleConfig.vue`。
- 验收标准：可配置 `created_at/updated_at/is_deleted`、`_id/_at/_no/_count/is_`、`tmp/test/flag1/type1` 等规则，并导出给 AI。
- 参考项目/资料：附件中的 `naming`、`suffix_rules`、`forbidden_words` 示例。
- 不做/边界：第一版不做复杂规则 DSL 或图形化规则编排器。

### P0-6：AI 建表 Prompt 生成器
- 类型：AI 辅助、Prompt 生成、个人工作流。
- 背景/问题：个人版第一阶段可以不接大模型 API，但需要根据当前字段标准和命名规则生成可复制给 Codex/ChatGPT 的建表提示词。
- 已有基础：`AiContextExportService` 能生成规则文档、字段目录和规则 YAML。
- 缺口：缺少面向“建表任务”的 prompt 模板、按项目标准填充字段清单、SQL 检查后生成修正 prompt 的能力。
- 建议方案：新增 `generateCreateTablePrompt(projectId, businessDescription)` 和 `generateFixSqlPrompt(projectId, sql, issues)`，输出纯文本 prompt；后续再接 CLI/MCP。
- 涉及文件/模块：`aicontext`、`lint`、前端 `AiExport.vue` 或新增 AI 助手页。
- 验收标准：用户输入“订单模块”或粘贴一段 SQL 后，可生成包含字段规则、禁用词、代码集、标准字段和修正要求的 prompt。
- 参考项目/资料：附件中的 AI 建表 Prompt 和 AI 修正建议示例。
- 不做/边界：不直接调用外部 LLM API。

## P1：核心闭环

### P1-1：打通前端 SQL 校验页
- 为什么做：当前 `SqlLint.vue` 仍是占位逻辑，用户无法从前端使用后端 `/api/lint`。
- 已有基础：后端已有 `SqlLintService`、`LintController` 和规则单测；前端已有 Monaco SQL 编辑器和请求封装。
- 缺口：前端未发起真实请求，结果字段与后端返回结构不一致。
- 落地产物：SQL 校验按钮调用 `/api/lint`，展示解析表、error/warning/suggestion 数量和问题列表。
- 验收标准：输入 `examples/bad-example.sql` 后能看到后端返回的问题列表；空 SQL、解析失败、网络失败都有明确反馈。
- 边界：本任务不新增规则，只完成现有规则的端到端使用。
- 参考项目：[`sqlfluff/sqlfluff`](https://github.com/sqlfluff/sqlfluff)
- 借鉴点：把 lint 结果作为用户首要反馈，按 rule、severity、message 呈现。
- 不照搬：不引入 SQLFluff 作为运行时依赖，当前仍保留 Java/JSqlParser 规则引擎。
- 落地方式：更新 `dataspec-web/src/views/SqlLint.vue` 和前端类型定义。

### P1-2：统一前后端 API 契约和类型
- 为什么做：前端类型中存在 `fieldName/fieldType/totalIssues/line/column` 等字段，但后端实际返回 `name/dataType/errorCount/warningCount/suggestionCount/tableName/columnName` 等结构。
- 已有基础：后端统一使用 `R<T>` 和 `PageResult<T>`；前端已有 `request.ts` 解包。
- 缺口：缺少统一 API 类型来源，页面实现容易继续偏离后端。
- 落地产物：补齐前端 API service 层和类型定义；至少覆盖 project、field、rule、lint。
- 验收标准：`pnpm build` 通过，SQL 校验页和首批 CRUD 页面不再使用与后端不一致的字段名。
- 边界：本任务不要求生成 OpenAPI client；如引入生成式 client，需单独评估。
- 参考项目：[`bytebase/bytebase`](https://github.com/bytebase/bytebase)
- 借鉴点：数据库 DevOps 工具中前后端围绕项目、环境、规则等对象形成稳定契约。
- 不照搬：不引入 Bytebase 的完整组织/环境/审批模型。
- 落地方式：先用手写轻量 API client，后续再评估从 OpenAPI 生成 TypeScript 类型。

### P1-3：增强 SQL 解析与 lint 准确性
- 为什么做：当前 parser 不解析 `COMMENT ON TABLE/COLUMN`，示例中预期的表名 snake_case 规则也尚未实现，容易产生误报或漏报。
- 已有基础：已有 `SqlParserService`、`TableDef`、`ColumnDef`、`LintRule` 插件式规则模型和单测。
- 缺口：缺少表注释/列注释解析、表名规则、定位信息、更多 PostgreSQL 约束识别。
- 落地产物：解析 `COMMENT ON`；新增表名 snake_case 规则；必要时为 `LintIssue` 增加 line/column 或 source span。
- 验收标准：`examples/good-example.sql` 不再因独立注释语句误报缺注释；`examples/bad-example.sql` 能报出表名不规范。
- 边界：本阶段优先 PostgreSQL，不做完整跨方言兼容。
- 参考项目：[`sqlfluff/sqlfluff`](https://github.com/sqlfluff/sqlfluff)、[`ariga/atlas`](https://github.com/ariga/atlas)
- 借鉴点：SQLFluff 的规则化 lint 思路；Atlas 的 schema lint 和迁移检查边界。
- 不照搬：不做自动格式化和完整 migration planner。
- 落地方式：围绕现有 `lint` 包补解析器能力、规则类和单元测试。

### P1-4：补齐项目、字段库、规则配置页面
- 为什么做：后端 CRUD 已有，但前端多数页面仍是“开发中”，用户无法维护项目标准。
- 已有基础：路由、侧边栏、Element Plus、Pinia 当前项目状态、后端 CRUD Controller。
- 缺口：项目列表、当前项目选择、字段库 CRUD、规则配置 CRUD 未接后端。
- 落地产物：项目列表/创建/编辑/删除；顶部项目选择真实可用；字段库和规则配置可增删改查。
- 验收标准：本地启动前后端后，可以创建项目、切换项目、维护字段、维护规则，并影响 `/api/lint` 的项目规则过滤。
- 边界：枚举、模板、导入导出页面可排在下一批。
- 参考项目：[`bytebase/bytebase`](https://github.com/bytebase/bytebase)
- 借鉴点：以项目空间承载规则和数据库资产的工作台模式。
- 不照搬：不做多租户、审批流、环境隔离等重型能力。
- 落地方式：优先实现最小可用的管理表格和表单。

### P1-5：字段推荐 API
- 为什么做：AI 写 SQL 前最常见的问题是“这个业务含义应该用什么字段名和类型”，只做 lint 会让 AI 先犯错再修。
- 已有基础：标准字段库、数据域、枚举字典和内置 `standards/fields`。
- 缺口：缺少按业务描述查找标准字段、推荐字段名、别名匹配和置信度输出。
- 落地产物：提供 `suggestField` 能力，输入“用户手机号”“支付金额”等描述，输出标准字段候选、推荐字段名、类型、默认值、注释、是否已有标准字段。
- 验收标准：常见字段描述能命中或推荐 `phone`、`amount_cent`、`created_at`、`is_deleted` 等标准字段；结果可被 CLI/MCP/API 复用。
- 边界：第一版先做规则、别名、关键词和 fuzzy match，不直接引入 LLM。
- 参考项目：[`bytebase/bytebase`](https://github.com/bytebase/bytebase)
- 借鉴点：围绕数据库资产提供结构化检索和工作台能力。
- 不照搬：不引入完整数据库实例管理。
- 落地方式：新增字段搜索/推荐 service，并让 CLI/MCP 调用同一能力。

### P1-6：SQL 自动修复建议结构化
- 为什么做：AI agent 需要可执行的修改建议，而不是只读自然语言错误信息。
- 已有基础：`LintIssue` 已有 severity、ruleCode、message、tableName、columnName。
- 缺口：缺少 `suggestion`、`replacement`、`before`、`after`、`confidence` 等结构化修复字段。
- 落地产物：扩展 lint issue 模型和规则输出，让 snake_case、禁用字段、推荐字段名、必备列等规则能提供修复建议。
- 验收标准：AI 调用 lint 后可根据结构化字段生成修复后的 SQL 或补充字段列表。
- 边界：第一阶段只给建议，不自动改写用户文件。
- 参考项目：[`sqlfluff/sqlfluff`](https://github.com/sqlfluff/sqlfluff)
- 借鉴点：lint issue 和 fixable rule 的分层思路。
- 不照搬：不做完整 SQL formatter 或自动格式化。
- 落地方式：先扩展 Java model 和核心规则单测，再接 CLI/MCP JSON 输出。

### P1-7：DDL 生成能力作为 AI tool
- 为什么做：理想流程不是让 AI 直接手写建表 SQL，而是 AI 提供业务语义，DataSpec 匹配字段/模板并生成规范 DDL，再自检。
- 已有基础：模板、模板字段、标准字段和 Markdown generator。
- 缺口：缺少面向 AI 调用的 DDL 生成接口和 lint 自检闭环。
- 落地产物：提供 `generateTableDdl` 能力，支持按模板或字段候选生成 PostgreSQL `CREATE TABLE` 和 `COMMENT ON` 语句。
- 验收标准：生成 SQL 能通过 DataSpec lint；CLI/MCP/API 均能调用同一生成逻辑。
- 边界：第一阶段只支持 PostgreSQL，不做自动迁移应用。
- 参考项目：[`ariga/atlas`](https://github.com/ariga/atlas)
- 借鉴点：从期望 schema 生成数据库变更的思路。
- 不照搬：不实现完整 migration planner。
- 落地方式：复用 P2-2 的 DDL generator，但优先暴露给 CLI/MCP。

### P1-8：SQL 检查记录与修正 SQL 输出
- 为什么做：附件里的 SQL 检查不只是列问题，还要能输出建议版 SQL，并保留最近检查记录用于个人复盘和命中率统计。
- 已有基础：后端已有 `SqlParserService`、`SqlLintService` 和 lint issue 统计；前端已有 SQL 编辑器骨架。
- 缺口：缺少 `sql_check_record` 表；缺少 `fixedSql`；缺少基于别名、标准字段、类型长度、公共字段和注释生成修正 SQL 的服务。
- 落地产物：SQL 检查返回 `issues`、`suggestions`、`fixedSql`；保存原 SQL、修正 SQL、问题数量、结构化检查结果和创建时间。
- 验收标准：输入包含 `phone/create_time/update_time` 的建表 SQL 后，结果能建议 `mobile_no/created_at/updated_at`，并输出一份可复制的修正 SQL。
- 边界：第一版只生成建议 SQL，不自动覆盖源文件或直接执行数据库变更。
- 参考项目：[`sqlfluff/sqlfluff`](https://github.com/sqlfluff/sqlfluff)
- 借鉴点：lint 输出与可修复建议分离。
- 不照搬：不做完整 formatter。
- 落地方式：在 P1-6 的结构化修复建议基础上增加 SQL 生成和记录表。

## P2：产品增强

### P2-1：创建项目时导入内置 standards
- 为什么做：`standards/fields` 和 `standards/domains` 已有 YAML，但当前没有自动初始化入口，新项目会是空数据。
- 已有基础：内置 YAML 文件和字段/数据域 service。
- 缺口：缺少读取、预览、导入、去重和错误反馈。
- 落地产物：创建项目后可选择导入内置标准字段和数据域；或提供“初始化标准”按钮。
- 验收标准：新项目初始化后能看到 `id`、`created_at`、`updated_at`、`is_deleted` 等内置字段。
- 边界：暂不做复杂标准版本迁移。

### P2-2：基于表模板生成 DDL
- 为什么做：DataSpec 不只应该检查 SQL，也应能基于标准字段和模板产出规范 SQL。
- 已有基础：后端已有 template、template_field、field 模块。
- 缺口：缺少 DDL 生成服务、预览页面和下载能力。
- 落地产物：选择模板后生成 PostgreSQL `CREATE TABLE` 和 `COMMENT ON` 语句。
- 验收标准：生成的 SQL 通过本项目 lint，且包含必备字段、类型、默认值和注释。
- 边界：优先 PostgreSQL；MySQL 等方言后置。
- 参考项目：[`ariga/atlas`](https://github.com/ariga/atlas)
- 借鉴点：schema-as-code 和从期望结构产出数据库变更的思路。
- 不照搬：不实现自动迁移应用、状态管理或 Atlas Cloud 类能力。
- 落地方式：新增 generator service 方法和前端模板生成页。

### P2-3：业务项目 `.dataspec/` 落地约定
- 为什么做：AI agent 在业务项目中需要稳定入口读取字段标准，不能每次依赖人工复制散落文件。
- 已有基础：P0-1 会生成 AI Context 包；当前仓库已有 `standards/`、`examples/` 和导出服务。
- 缺口：缺少业务项目目录约定、更新策略、README 和 agent 指令片段。
- 落地产物：定义 `.dataspec/` 目录结构，包含规则、字段目录、prompt、示例和版本信息；生成项目级 `AGENTS.md.fragment`。
- 验收标准：业务仓库引入 `.dataspec/` 后，AI agent 可按目录约定找到 DataSpec 上下文并执行 lint 命令。
- 边界：只定义和导出目录结构，不托管业务项目文件。
- 参考项目：[`agents.md`](https://agents.md/)
- 借鉴点：用项目内约定文件指导 coding agent 的工作方式。
- 不照搬：不把所有项目开发规范都塞进 DataSpec，只聚焦数据库字段标准。

### P2-4：数据字典生成增强
- 为什么做：当前 Markdown 数据字典较基础，缺少模板、枚举、字段域关系、索引/约束等可读信息。
- 已有基础：`MarkdownGeneratorService` 已生成数据域、字段库、枚举字典。
- 缺口：缺少更完整的数据库文档结构和可视化关系。
- 落地产物：增强 Markdown 结构；后续可导出 HTML 或 ERD。
- 验收标准：生成文档能清晰回答“有哪些字段、属于哪个域、关联哪个枚举、用于哪些模板”。
- 边界：不在本阶段做复杂在线文档站。
- 参考项目：[`k1LoW/tbls`](https://github.com/k1Low/tbls)
- 借鉴点：CI-friendly 数据库文档和 GFM 输出方式。
- 不照搬：不直接扫描数据库生成全量 schema 文档，先基于 DataSpec 内部模型生成。
- 落地方式：扩展 generator service 和前端预览/下载体验。

### P2-5：Excel 导入导出标准字段和代码集
- 为什么做：个人/小团队维护字段标准时，Excel 批量编辑比逐条表单更高效。
- 已有基础：已有字段 JSON 导入导出和标准字段 CRUD。
- 缺口：缺少 `.xlsx` 模板、字段别名/分类/代码集的批量导入导出、导入预览和错误报告。
- 落地产物：导出标准字段 Excel 模板；导入字段、别名、分类、代码集；导入前预览新增/更新/冲突。
- 验收标准：可用 Excel 批量维护 `mobile_no`、`order_status` 等字段及别名，导入后字段推荐和 AI 导出立即可用。
- 边界：第一版只支持手动上传文件，不做在线协同编辑。

### P2-6：轻量变更记录
- 为什么做：第一版不做审核发布，但字段标准和命名规则修改仍然需要可追溯，后续多人协作也能复用。
- 已有基础：所有主表已有 `created_at/updated_at`；后端 service 层集中处理创建和更新。
- 缺口：缺少 `standard_change_log` 或等价记录表；缺少 before/after JSON 自动记录。
- 落地产物：记录标准字段、代码集、命名规则的新增/修改/停用/废弃操作。
- 验收标准：修改字段类型、别名、状态后，后台能查到 target、before、after、changedAt。
- 边界：不做审批流、不做页面复杂 diff；先后台自动记录。

### P2-7：个人工作台与标准命中率报告
- 为什么做：附件建议首页展示标准字段数量、代码集数量、禁用词数量、最近检查 SQL 数量和字段命中率，这有助于自用时快速判断规范沉淀效果。
- 已有基础：已有字段、枚举、规则和 lint 结果统计模型。
- 缺口：缺少检查记录、命中率计算、轻量首页。
- 落地产物：首页展示标准字段数、代码集数、命名规则数、最近检查次数、SQL 问题趋势和字段标准命中率。
- 验收标准：完成几次 SQL 检查后，首页能看到最近检查记录和命中率摘要。
- 边界：不做企业治理驾驶舱和复杂资产目录。

## P3：高级能力

### P3-1：数据库反向导入与差异分析
- 为什么做：团队已有数据库时，需要从现有 schema 生成字段库和规范问题报告。
- 已有基础：已有 SQL parser、字段导入导出、数据字典生成。
- 缺口：缺少连接数据库或导入 dump 的流程、schema introspection、差异模型。
- 落地产物：上传 SQL 或连接 PostgreSQL 后生成字段候选、缺注释清单和标准差异报告。
- 验收标准：能从一个真实 PostgreSQL schema 生成可人工确认的导入预览。
- 边界：初期只做只读导入，不自动修改数据库。
- 参考项目：[`k1LoW/tbls`](https://github.com/k1Low/tbls)、[`ariga/atlas`](https://github.com/ariga/atlas)
- 借鉴点：数据库 introspection、schema 文档和 schema diff 思路。
- 不照搬：不实现完整跨数据库迁移和部署。
- 落地方式：先支持 SQL 文件解析，再评估 JDBC 直连读取 metadata。

### P3-1a：MySQL CREATE TABLE 支持
- 为什么做：附件中的个人版示例以 MySQL DDL 为主，但当前 README 和 parser 主要面向 PostgreSQL。
- 已有基础：JSqlParser 已用于解析 CREATE TABLE；现有规则大部分与方言无关。
- 缺口：缺少 MySQL 方言下的类型、长度、COMMENT、AUTO_INCREMENT、tinyint、datetime、反引号等兼容测试。
- 落地产物：支持 MySQL `CREATE TABLE` 基础解析，并能复用字段标准、别名、命名规则和注释检查。
- 验收标准：附件中的 `t_user` 示例能被解析并输出规范建议。
- 边界：不做完整多数据库迁移；仅补个人常用 MySQL DDL 检查能力。

### P3-2：CI/GitHub Action 校验入口
- 为什么做：DataSpec 如果能在 PR 中检查 SQL，就能从本地工具升级为团队规范门禁。
- 已有基础：后端 lint service 和示例 SQL。
- 缺口：缺少 CLI、批量文件扫描、退出码、GitHub Actions 示例。
- 落地产物：提供命令行或 Maven task 批量校验 SQL 文件；补 `.github/workflows` 示例文档。
- 验收标准：在 CI 中对 SQL 文件执行 lint，发现 ERROR 时以非 0 退出码失败。
- 边界：先做示例和本地命令，不直接发布 Marketplace Action。
- 参考项目：[`ariga/atlas-action`](https://github.com/ariga/atlas-action)、[`bytebase/example-gitops-github-flow`](https://github.com/bytebase/example-gitops-github-flow)
- 借鉴点：数据库变更在 GitHub Flow 中的自动校验与反馈。
- 不照搬：不做完整数据库发布流水线或审批系统。
- 落地方式：优先实现批量 lint 命令，再补 GitHub Actions 使用示例。

### P3-3：PR 评论式 SQL Review
- 为什么做：CI 只失败不够友好，AI 和开发者需要在 PR 中看到可定位、可修复的字段规范反馈。
- 已有基础：P0-2 CLI、P1-6 修复建议和 P3-2 GitHub Action 校验入口。
- 缺口：缺少文件级定位、Markdown 评论格式、重复评论更新策略和 GitHub token 权限边界。
- 落地产物：在 PR 中评论 SQL lint 结果，包含文件、表、字段、规则、建议替换和修复示例。
- 验收标准：Pull Request 中新增或修改 SQL 文件时，Action 能发布或更新 DataSpec Review 评论。
- 边界：第一阶段只做 GitHub Actions，不做 GitLab/Gitea 集成。
- 参考项目：[`bytebase/example-gitops-github-flow`](https://github.com/bytebase/example-gitops-github-flow)、[`ariga/atlas-action`](https://github.com/ariga/atlas-action)
- 借鉴点：数据库变更在 PR 流程中的自动反馈。
- 不照搬：不做完整审批流和数据库发布。
- 落地方式：先基于 CLI JSON 输出生成 Markdown Summary，再扩展为 PR comment。

## P4：暂缓探索

### P4-1：多方言规则体系
- 目标：支持 PostgreSQL 之外的 MySQL、SQL Server 等方言。
- 触发条件：出现明确用户场景或项目需要。
- 边界：当前 MVP 继续聚焦 PostgreSQL，避免规则体系过早复杂化。

### P4-2：权限、审批和审计日志
- 目标：面向团队协作时支持角色、审批、变更历史和审计。
- 触发条件：项目从单人/小团队工具转向多人协作平台。
- 边界：当前先不做登录态和权限模型，以免拖慢核心规范能力闭环。

## 参考项目索引

- [`sqlfluff/sqlfluff`](https://github.com/sqlfluff/sqlfluff)：模块化、可配置、多方言 SQL linter。
- [`ariga/atlas`](https://github.com/ariga/atlas)：schema-as-code、schema lint 和迁移规划。
- [`ariga/atlas-action`](https://github.com/ariga/atlas-action)：数据库 schema 变更的 GitHub Actions lint 入口。
- [`bytebase/bytebase`](https://github.com/bytebase/bytebase)：数据库 DevOps 工作台、SQL Review、数据库 CI/CD。
- [`bytebase/example-gitops-github-flow`](https://github.com/bytebase/example-gitops-github-flow)：Bytebase + GitHub Flow 数据库发布示例。
- [`k1LoW/tbls`](https://github.com/k1Low/tbls)：CI-friendly 数据库文档生成工具。
- [Model Context Protocol 规范](https://modelcontextprotocol.io/specification/2025-06-18)：AI 应用接入 resources、prompts、tools 的协议基础。
- [`modelcontextprotocol/servers`](https://github.com/modelcontextprotocol/servers)：MCP server 参考实现集合。
- [`agents.md`](https://agents.md/)：面向 coding agent 的项目指令文件约定。
