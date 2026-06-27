# DataSpec 待办路线图

本文件记录当前仍需行动的产品与工程待办。优先级按用户可感知价值、核心链路阻断程度和后续开发解锁程度排序。

## 下一步顺序

1. P6-8 标准字段重复与冲突检测已完成第一版，下一步推进 P6-9 规则误报豁免与项目例外说明。
2. P6 后续继续补 AI contract fixtures、规则例外治理、GitHub inline 实接、性能基线、前端回归门禁、AI 可读诊断、字段检索、OpenSpec 收口、历史快照回放、多方言兼容矩阵、规则模板库、备份迁移包、数据库只读安全诊断、AI 批量任务、标准候选采纳台、离线 Context、元数据适配、Prompt 评测、项目活动时间线、任务式前端导航、本地启动包、fixedSql 策略化、AI 使用画像、标准契约版本、执行证据包、统一前端状态、并发幂等保护、AI 能力清单、前端可复现链接、敏感信息脱敏、验证建议、TODO 到 OpenSpec 交接、业务术语表、自然语言标准候选、AI 引用证据、字段生命周期、变更感知扫描和健康趋势。
3. P6 收束后再回看哪些能力需要从个人/小团队工具升级为团队协作能力。

## 已完成能力摘要（P0-P4）

P0-P4 的详细背景、方案和验收已归档到 [docs/archive/todo-completed-p0-p4.md](docs/archive/todo-completed-p0-p4.md)。主待办只保留当前仍需行动的 P5/P6 任务。

- P0 AI 可消费主线已完成第一版：AI Context zip、CLI、MCP、个人版字段模型、结构化命名规则和 AI Prompt 生成。
- P1 核心闭环已完成第一版：SQL 校验、OpenAPI 类型契约、COMMENT 解析、前端管理页、字段推荐、结构化修复建议、DDL 生成、检查记录和 fixedSql。
- P2 标准维护与生成能力已完成第一版：内置 standards 初始化、模板 DDL、业务项目 .dataspec/ 约定、数据字典、Excel 导入导出、变更日志和个人工作台。
- P3 自动化与反向导入已完成第一版：SQL 反向导入预览、MySQL DDL 解析、CI/GitHub Action 和 PR 评论式 SQL Review。
- P4 工程化与体验增强已完成第一版：SQL 定位、fixedSql diff、.dataspec/config.json、规则配置表单、OpenAPI 防漂移、Excel dry-run、HTML/ERD、MySQL 规则覆盖、安全基线、演示项目和数据库直连反向导入前端流程。
- 后续真实待办集中在 P6：P5 已完成 dataspec doctor、数据库二次比对、导入来源追踪、SQL 定位范围增强、字段推荐质量增强、核心 fixture/golden 基线、前端高频流程细节打磨和轻量 token 管理；P6-1 已完成标准版本快照第一版，P6-2 已完成字段覆盖率第一版，P6-3 已完成 AI 回放第一版，P6-4 已完成业务仓库初始化第一版，P6-5 已完成字段质量评分第一版，P6-6 已完成轻量字段影响分析第一版，P6-7 已完成 AI Context 按需裁剪第一版，P6-8 已完成字段冲突检测第一版，后续再提升 AI 契约稳定性、GitHub inline 实接、性能基线、前端回归、AI 可读诊断、字段检索、OpenSpec 收口、AI 使用画像、标准契约版本和执行证据包。

## P5：可用性与 AI 稳定性增强

### P5-1：TODO 路线图去噪与完成项归档
- 状态：已完成第一版，已把 P0-P4 已完成项归档到 `docs/archive/todo-completed-p0-p4.md`，主待办仅保留摘要和仍需行动的 P5 任务。
- 为什么做：当前多数 P0-P4 项已经完成第一版，但正文仍保留历史“缺口”描述，后续 AI 或人工排期容易把已完成能力误判为未实现。
- 已有基础：README 已维护当前功能概览，TODO 中每项已有状态行和验收描述。
- 已完成能力：P0-P4 已完成项不再使用会误导为未实现的旧 `缺口` 标签，详细历史已移动到归档文档；真正待办仍集中在 P5 中保留 `缺口`。
- 落地产物：将已完成条目改成“已完成能力 / 后续增强 / 不做边界”结构；把过长历史方案移动到归档或保留为简短摘要；顶部下一步顺序只指向仍需行动的任务。
- 验收标准：`TODO.md` 中已完成项不会再出现会误导为未实现的旧缺口；`rg "状态：待办|后续增强"` 能清晰定位真实待办；`git diff --check` 通过。
- 边界：不改 README 已实现能力描述，不重排历史编号，不删除有价值的参考项目链接。

### P5-2：`dataspec doctor` 自检命令
- 状态：已完成第一版，CLI 已新增 `doctor`，支持文本/JSON 输出、配置/服务/token/项目/defaultPaths/OpenAPI 状态检查，以及可选 `--check-openapi` 完整契约漂移检查。
- 为什么做：AI agent、CLI 和 MCP 使用 DataSpec 时，最常见的问题是服务未启动、项目 ID 错误、token 无权限、OpenAPI 契约漂移或 `.dataspec/config.json` 配置不完整；需要一个可机器读取的诊断入口。
- 已有基础：CLI/MCP 已支持 `.dataspec/config.json`、`DATASPEC_TOKEN`、项目默认配置、OpenAPI 契约检查脚本和统一 API wrapper。
- 已完成能力：已集中检查 server、projectId、token、默认路径、API docs 和本地 schema.ts 状态；完整 schema 漂移检查通过 `--check-openapi` 显式启用。
- 落地产物：新增 `dataspec doctor` CLI 命令，输出人类可读报告和 `--format json` 结构化结果；检查后端连通、当前项目存在性、token 当前身份、默认扫描路径、OpenAPI 契约状态和建议修复动作。
- 验收标准：在服务正常、服务未启动、projectId 不存在、token 无权限、OpenAPI 漂移等场景下返回明确诊断；JSON 输出可被 AI agent 稳定解析；相关 CLI 测试接入 `node --test`。
- 边界：不自动修复配置，不写入外部业务仓库，不绕过安全模式。

### P5-3：数据库直连反向导入二次比对
- 状态：已完成第一版，已新增只读 compare API 和反向导入页差异视图。
- 为什么做：反向导入第一版解决了从现有数据库生成数标候选，但日常维护还需要再次连接同一数据库，查看当前数据库 schema 与 DataSpec 标准之间的差异。
- 已有基础：已支持 PostgreSQL/MySQL 直连、表列表、metadata 预览、字段候选确认导入和前端步骤化流程。
- 已完成能力：同一项目再次连接数据库后，可按所选表输出字段差异摘要和明细；字段按 `MATCHED`、`CHANGED`、`NEW`、`MISSING_COMMENT` 和非标准原因展示；前端支持按状态筛选和按表展开。
- 验收标准：compare 本身只读，不修改源数据库，不自动写入字段库；需要导入新候选时仍通过原“生成预览 -> 勾选候选 -> 确认导入”流程完成。
- 边界：不做定时同步，不保存数据库密码，不自动删除 DataSpec 中已有标准字段，不做审批流。

### P5-4：反向导入来源与批次追踪
- 状态：已完成第一版，数据库直连确认导入的新字段会记录导入批次和字段来源。
- 为什么做：字段进入标准库后需要知道来源，否则后续清理、复盘和字段命中率分析会缺少上下文。
- 已有基础：标准字段已有 CRUD、变更日志和反向导入确认写入流程。
- 已完成能力：新增导入批次和字段来源记录；确认导入时写入 database/schema/table/column、导入统计、操作者和原始候选 metadata；字段库可查看来源摘要，无来源字段保持正常。
- 验收标准：通过数据库直连导入的字段能追溯到导入批次、schema.table.column 和导入时间；普通手工字段不受影响。
- 边界：不保存数据库密码，不长期保存连接串明文，不做跨项目来源合并。

### P5-5：SQL 定位精度升级与 GitHub inline comment 基础
- 状态：已完成第一版，已新增更稳定的 source range、定位类型和 CLI PR 评论行列范围展示。
- 为什么做：当前 line/column/source span 是启发式定位，足够前端跳转和汇总评论，但要支持 GitHub inline review 与更稳定的 AI 修复，需要更可靠的位置映射。
- 已有基础：`LintIssue` 已有 `line/column/sourceStart/sourceEnd`，前端 SQL 校验页可点击跳转，CLI/GitHub Review 已能输出汇总评论。
- 已完成能力：`LintIssue` 新增 `lineEnd/columnEnd/locationKind`；resolver 优先在匹配表定义和 COMMENT ON COLUMN 范围内定位；CLI `review-pr` Markdown 展示 `行 x:y-x2:y2`；前端位置列可展示范围并继续跳转起始位置。
- 验收标准：多表同名字段、schema/quoted/backtick/bracket 标识符、COMMENT ON COLUMN、不可定位 issue 和 CLI review 输出均有测试覆盖；CLI JSON 能输出足够信息供后续 PR inline comment 映射使用。
- 边界：第一版不强制接入真实 GitHub inline API，不实现完整 SQL AST source map。

### P5-6：字段推荐质量增强
- 状态：已完成第一版，字段推荐已支持轻量语义词库、泛化词降权、敏感字段提示和 canonical fallback。
- 为什么做：DataSpec 优先服务 AI，字段推荐质量直接决定 AI 建表时是否能少犯错；当前确定性匹配可用，但还不够懂个人命名习惯。
- 已有基础：字段推荐 API/CLI/MCP 已支持字段名、显示名、注释、别名、分类和标签匹配，并返回分数、原因和 fallback。
- 已完成能力：增强字段推荐评分模型和内置词库；支持 `uid/user_id/account_id/member_id`、`phone/mobile/tel/mobile_no`、`amount/price/fee/amount_cent`、`sfzh/id_card_no` 等常见语义区分；泛化词-only 命中会降权，敏感字段会在命中原因里提示，fallback 会优先输出 canonical snake_case 候选。
- 验收标准：常见中文业务描述、英文别名和拼音缩写能稳定命中标准字段；泛化词不会压过具体语义字段；推荐结果通过单元测试覆盖典型样例。
- 边界：仍不直接调用外部 LLM；不引入复杂向量数据库。

### P5-7：规则测试语料库与 golden fixtures
- 状态：已完成第一版，已新增核心 SQL fixture、fixedSql golden 和反向导入 metadata fixture，并接入 `mvn test`。
- 为什么做：SQL parser、lint rule、fixedSql 和 reverse import 都已经成为核心链路，需要稳定样例库防止“修一个规则，坏另一个规则”。
- 已有基础：已有后端 parser/lint 单测、examples good/bad SQL、MySQL 兼容测试和 CLI/MCP 测试。
- 已完成能力：建立 `dataspec-server/src/test/resources/fixtures` 测试资源目录，覆盖 PostgreSQL good SQL、MySQL bad SQL、fixedSql 输入/期望输出和反向导入 metadata JSON；新增聚合 golden 测试入口。
- 验收标准：后端测试能一键跑完 fixture/golden 用例；修改 parser/rule/fixedSql 时能明确看到行为差异；`mvn test` 仍作为统一验证入口。
- 边界：不追求完整 SQL 方言覆盖，只收录项目真实会遇到的高价值样例。

### P5-8：前端高频流程细节打磨
- 状态：已完成第一版，反向导入页已按项目记住非敏感直连状态、表选择、搜索词和差异筛选，字段库支持关键词 query 跳转筛选。
- 为什么做：当前前端主流程已经可用，下一步应减少重复输入和上下文丢失，让个人日常使用更顺手。
- 已有基础：工作台、项目选择、SQL 校验、DDL 生成、反向导入、字段库、规则配置、AI Context 页面均已接后端。
- 已完成能力：新增前端 `reverseImportMemory` utility 和单测，使用浏览器本地存储保存 databaseType、host、port、databaseName、schemaName、username、tableNames、tableSearch、compareStatusFilter 和 activeMode；明确不保存 password、token、JDBC URL 或完整连接串；导入结果跳转字段库时会携带首个导入字段作为 `keyword`，字段库按 query 初始化筛选。
- 后续增强：多连接预设、无密复用和重新比对上次来源已放在 P6-10；更完整的前端 E2E 门禁已放在 P6-17。
- 验收标准：`pnpm test` 覆盖敏感字段剔除、项目隔离、损坏缓存兜底和字段库 query；`pnpm build` 通过。
- 边界：不做视觉大改版，不引入复杂新状态管理，不改变后端核心模型。

### P5-9：轻量 API Token 管理页面
- 状态：已完成第一版，已新增 token 管理 API、V8 迁移、lastUsedAt 跟踪和前端“系统设置 / API Token”页面。
- 为什么做：安全基线已有 API Token 模式，但目前 token 主要靠手写 SQL 配置；小团队和 AI agent 长期接入时，需要一个轻量页面来创建、禁用和查看授权范围。
- 已有基础：后端已有 token hash 存储、安全模式、项目级授权检查、前端 token 登录/退出、CLI/MCP token 透传和操作者记录。
- 已完成能力：新增 `/api/tokens` 管理接口，支持列表、创建和停用；创建时生成 `ds_` 前缀随机 token，后端仅保存 SHA-256 hash，明文只在创建响应中返回；认证成功会更新 `last_used_at`，列表不泄漏 `tokenHash`；前端页面支持选择全项目或指定项目、复制一次性明文、查看最近使用时间和禁用 token。
- 验收标准：安全模式开启后，用户无需手写 SQL 即可创建 CLI/MCP token；token 明文只在创建时显示；禁用后 CLI/MCP 请求被拒绝。
- 边界：不做复杂 RBAC、审批流、组织成员管理或 token 自动轮换；个人本地开发默认仍可关闭安全模式。

## P6：标准治理与 AI 协作增强

### P6-1：标准版本快照与 AI Context 可复现
- 状态：已完成第一版，已新增标准快照模型/API/前端入口，AI Context、SQL 检查记录和 DDL 生成结果会携带当前标准版本元数据。
- 为什么做：AI 使用字段标准生成 SQL、DDL 或修复建议时，需要明确“使用的是哪一版标准”，否则后续复盘很难判断结果来自规则问题、标准变更还是提示词变化。
- 已有基础：已有标准变更日志、AI Context zip、数据字典、CLI/MCP 和 OpenAPI 契约检查。
- 已完成能力：新增 `ds_standard_snapshot` 快照表和 `/api/projects/{projectId}/standard-snapshots` API；快照基于字段、枚举、规则生成确定性 payload JSON 和 SHA-256 hash；前端“系统设置 / 标准快照”支持查看当前快照、创建快照和查看历史列表；AI Context manifest/field-catalog/rules.yaml 携带 `specVersion` 与 `specHash`；SQL 检查记录和 DDL 生成结果引用当前快照。无快照时返回 `unversioned`，不阻断原有流程。
- 验收标准：用户能创建当前标准快照；导出的 AI Context 能稳定标明版本和 hash；同一份输入可按指定快照复现字段目录和规则上下文。
- 边界：第一版不做复杂发布审批、多人审核、语义化版本治理、按历史快照完整还原或跨项目标准合并；快照 payload 已保存，后续可继续发展回放能力。

### P6-2：字段使用覆盖率与未纳管字段盘点
- 状态：已完成第一版，已新增字段覆盖率报告后端 API、数据库直连 metadata 报告、SQL/DDL 报告和前端“数据管理 / 覆盖率报告”页面。
- 为什么做：个人/小团队维护数标时，最有价值的反馈是“真实数据库里哪些字段已经被标准覆盖，哪些还在野生生长”，这能直接指导下一轮补标准。
- 已有基础：已有数据库直连 metadata、SQL 反向导入、字段推荐、差异预览和字段来源追踪待办。
- 已完成能力：基于 SQL/DDL 或数据库直连选表生成即时覆盖率报告；输出项目级 summary、表级统计、字段明细和未纳管字段排行；字段状态区分标准命中、别名命中、缺注释、疑似重复和未纳管；前端支持连接、加载表、生成报告、按表/状态筛选，并可跳转字段库或反向导入。
- 验收标准：连接数据库后能输出字段覆盖率百分比和未纳管字段 Top 列表；用户可从报告跳转到反向导入或字段库补标准。
- 边界：第一版不扫描业务数据行，不做敏感数据采样，不做定时后台同步，不保存数据库密码，不自动导入或合并标准字段。

### P6-3：AI 生成与修复决策回放
- 状态：已完成第一版，已新增 AI 作业记录后端 API、Flyway 迁移、前端“校验与生成 / AI 回放”页面，并接入建表 Prompt、SQL 修正 Prompt、SQL lint/fixedSql 和 DDL preview。
- 为什么做：DataSpec 优先让 AI 使用时，除了给 AI 上下文，还需要能回看一次生成或修复使用了哪些字段、规则、prompt 模板和输入，方便定位“AI 为什么这么写”。
- 已有基础：已有 AI Prompt 生成、SQL 校验记录、fixedSql、DDL 生成、CLI/MCP 和标准变更日志。
- 已完成能力：新增 `ds_ai_job_record` 和 `/api/ai-jobs`；作业记录包含 jobType、promptVersion、输入/输出 payload、标准快照、关联 SQL 检查记录和 replay JSON/命令；前端支持按类型筛选、分页、详情查看、lint 摘要和复制。
- 落地产物：新增轻量 AI 作业记录，并在现有检查/生成记录中串联输入、输出、引用标准、规则结果和可复制的回放命令。
- 验收标准：一次 CLI/MCP 或前端生成后，能在记录详情看到本次使用的项目、标准版本、prompt 输入、输出 SQL 和 lint 结果；能复制命令或 JSON 复现本次请求。
- 边界：第一版不内置外部 LLM 调用，不保存第三方 API key，不做长文本会话管理；CLI/MCP 查询命令留给 P6-11 工作流模板继续增强。

### P6-4：业务仓库 `dataspec init` 初始化向导
- 状态：已完成第一版，CLI 已新增 `init`，支持生成 `.dataspec/config.json`、`.dataspec/README.md`、可选 `AGENTS.md` marker 片段，并在完成后运行轻量 `doctor`。
- 为什么做：让 AI 在真实业务仓库使用 DataSpec 时，第一步应该是把 `.dataspec/config.json`、默认扫描路径和 AGENTS 片段配置好；现在这些需要人工拼接。
- 已有基础：已有 `.dataspec/config.json` 读取、`dataspec doctor`、AI Context 导出、AGENTS fragment 和 CLI/MCP。
- 已完成能力：新增参数化 `dataspec init` CLI 命令；支持 `--project`、`--server`、多次 `--default-path`、`--with-agents`、`--force` 和 `--format text|json`；重复执行默认跳过已有文件，`--force` 仅覆盖 DataSpec 管理内容。
- 验收标准：在任意业务仓库执行初始化后，AI agent 能直接通过 CLI/MCP 读取项目标准；重复执行不会覆盖用户手写配置，除非显式使用 `--force`；初始化输出包含写入/跳过文件和 doctor 检查结果。
- 边界：不修改业务代码，不自动提交业务仓库，不把明文 token 写入可提交文件。

### P6-5：标准字段质量评分与修复建议
- 状态：已完成第一版，已新增字段质量评分后端 API 和前端“基础数据 / 字段质量”页面。
- 为什么做：字段标准越多，越需要识别哪些字段缺少别名、示例、枚举、敏感标识或注释，否则 AI 会得到看似完整但语义不足的字段目录。
- 已有基础：字段模型已有别名、标签、状态、敏感标识、示例值、代码集关联、变更日志和字段推荐原因。
- 已完成能力：实时生成项目级质量报告，输出 total/average/issue count、字段 score、level、issues 和 suggestions；检查缺注释、缺别名、缺示例值、缺分类/标签、疑似敏感未标记、枚举/状态字段未关联代码集、废弃/停用字段缺替代说明；前端支持等级和 issue 筛选并跳转字段库编辑。
- 后续增强：CLI/MCP 或 AI Context 消费质量报告、批量修复入口和质量趋势统计分别留给 P6-11、P6-15、P6-27 等后续任务。
- 验收标准：字段库能按质量分排序；低质量字段可一键定位编辑；质量检查结果结构化返回，后续可被 CLI/MCP 或 AI Context 消费。
- 边界：不自动改字段标准，不引入外部 LLM 自动补全，不做组织级质量 KPI。

### P6-6：轻量字段影响分析
- 状态：已完成第一版，已新增字段影响分析后端 API 和字段库“影响”入口。
- 为什么做：修改字段名、类型、枚举或状态前，用户需要知道它会影响哪些模板、DDL 生成、SQL 检查记录、AI Context 和反向导入来源。
- 已有基础：已有表模板、DDL 生成、数据字典、SQL 检查记录、变更日志、反向导入来源追踪待办和 AI Context。
- 已完成能力：新增 `/api/fields/{id}/impact?projectId=` 只读接口；实时汇总表模板引用、反向导入来源、最近 SQL 检查记录、标准快照和代码集影响；字段库支持查看影响弹窗，编辑字段名、类型、状态、代码集或敏感标记时展示非阻断提示。
- 后续增强：更完整的 AI Context 导出记录、DDL 生成历史和复杂血缘图谱留给 P6-7、P6-21、P6-38 或后续团队治理任务。
- 验收标准：打开字段库可查看当前字段的使用位置；修改字段类型/状态前能看到受影响模板或记录数量；不阻断个人快速编辑。
- 边界：不做复杂血缘图谱，不扫描生产查询日志，不实现审批或发布阻断。

### P6-7：AI Context 按需裁剪与检索模式
- 状态：已完成第一版，已新增 API/CLI/MCP/前端按需裁剪入口，默认完整导出保持兼容。
- 为什么做：标准字段、规则、模板和记录变多后，一次性把完整 AI Context 喂给 coding agent 会浪费上下文，也容易让 AI 读到与当前任务无关的标准。
- 已有基础：已有 AI Context zip、MCP resources、字段推荐、规则导出、项目 `.dataspec/config.json` 和 `dataspec doctor`。
- 已完成能力：`/api/ai-context/field-catalog`、`/database-rules`、`/package/download` 支持 `scope/query/status/limit`；`field-catalog.json` 输出 `contextScope` 和字段级 `matchReasons`，zip manifest 与 `.dataspec/README.md` 同步说明完整包/按需包；CLI `export-context` 支持按需参数；MCP `get_field_catalog` 支持裁剪参数并新增 `search_field_catalog`；前端 AI Context 页面支持范围、关键词、状态和上限筛选，预览与下载共用同一条件。
- 后续增强：`changed` 第一版为基于 query 的任务相关裁剪，真正基于 git diff/快照 diff 的变更感知扫描留给 P6-48；更完整字段检索 API、AI contract fixtures 和离线 Context 分别留给 P6-19、P6-12、P6-29。
- 落地产物：新增按需导出 API/CLI/MCP 参数，支持 `scope=table|domain|tag|field|changed` 等裁剪模式；输出包含命中原因、裁剪条件、字段/规则数量和缺失提示；在 `.dataspec/README.md` 中说明 AI 何时用完整包、何时用按需包。
- 验收标准：AI agent 能针对一个建表/修 SQL 任务获取最小可用字段标准；导出结果仍符合 JSON Schema；大项目下按需包体积明显小于完整包。
- 边界：不引入向量数据库，不依赖外部 LLM 检索，不改变完整 AI Context 的兼容格式。

### P6-8：标准字段重复与冲突检测
- 状态：已完成第一版，已新增字段冲突报告 API 和前端“基础数据 / 字段冲突”页面。
- 为什么做：标准字段长期维护后，容易出现同义字段重复、别名互相冲突、同名不同类型、敏感标识不一致等问题；这些会直接降低 AI 推荐和 SQL 修复质量。
- 已有基础：字段模型已有别名、分类、标签、敏感标记、状态、示例值和代码集关联；字段推荐已能输出命中原因。
- 已完成能力：新增 `/api/fields/conflicts?projectId=` 只读报告；按字段名、别名、显示名和内置语义词组检测重复/冲突；输出 `ERROR/WARNING/INFO`、涉及字段、证据、建议动作和摘要统计；前端支持按级别/类型筛选，并可跳转字段库编辑涉及字段。
- 后续增强：把冲突结果反馈给字段推荐排序、AI Context 提醒、CLI/MCP 输出和字段库筛选留给 P6-12、P6-19 或后续 AI 契约增强。
- 落地产物：新增标准冲突检查服务和前端结果页；按字段名、别名、显示名、语义词、类型、代码集、敏感标记输出疑似重复和冲突；提供合并建议、保留建议和跳转编辑入口。
- 验收标准：能发现 `user_id/uid/account_id`、`mobile/phone/tel` 等常见重复候选，并区分“可合并”和“语义相近但应保留”的场景；检查结果可被 CLI/MCP 或 AI Context 消费。
- 边界：不自动合并字段，不自动删除历史字段，不做跨项目统一标准治理。

### P6-9：规则误报豁免与项目例外说明
- 状态：待办。
- 为什么做：个人/小团队真实项目里会有少量历史表、第三方字段或框架约定无法完全满足 DataSpec 规则；如果没有结构化例外，AI 和 CI 会反复报告已知误报。
- 已有基础：已有规则配置、规则启停、SQL lint、CLI/GitHub Review、AI rules.yaml 导出和结构化修复建议。
- 缺口：缺少按项目、表、字段、规则编码或 SQL 文件路径声明例外的机制；例外原因也没有导出给 AI。
- 落地产物：新增轻量 rule exemption 模型/API/前端入口；支持记录 ruleCode、scope、reason、expiresAt/disabledAt；SQL lint 输出中区分 suppressed issue 和 active issue；AI rules 导出包含项目例外说明。
- 验收标准：已知历史表可被豁免且不会导致 CI 失败；豁免项必须有原因；AI Context 能提醒 agent 这些例外不是推荐新建表继续沿用的标准。
- 边界：不做审批流，不做组织级例外审计，不允许无范围的全局静默。

### P6-10：数据库直连配置预设与无密复用
- 状态：待办。
- 为什么做：数据库直连反向导入会成为高频入口，重复输入 host、port、database、schema 和表筛选很繁琐；但又不能为了方便保存密码或完整连接串。
- 已有基础：反向导入页已有连接测试、表列表、metadata 预览、二次比对、确认导入和来源批次追踪；P5-8 也规划了高频流程细节打磨。
- 缺口：缺少非敏感连接预设、最近使用 schema/tableNames、默认导入策略和“重新比对上次来源”的快捷入口。
- 落地产物：新增数据库连接预设模型和前端选择器，只保存 databaseType、host、port、databaseName、schemaName、tableNames、别名和更新时间；密码仍由用户当次输入或环境变量提供；反向导入页可一键加载上次表选择并进入 compare/import。
- 验收标准：用户可保存和复用多个非敏感连接预设；预设不会包含 password、token 或完整 JDBC URL；使用预设后仍能完成测试连接、加载表、比对和导入。
- 边界：不做连接池，不做后台定时同步，不把密码写入数据库或浏览器持久存储。

### P6-11：MCP/CLI 任务化工作流模板
- 状态：待办。
- 为什么做：DataSpec 已有 CLI/MCP 单点能力，但 AI agent 更需要“先自检 -> 取标准 -> lint -> 推荐字段 -> 生成/修复 -> 输出结果”的稳定工作流，减少每次靠 prompt 现场拼步骤。
- 已有基础：已有 `dataspec doctor`、`lint-files`、`review-pr`、字段推荐、DDL 生成、AI Prompt、AI Context zip 和 MCP resources/prompts/tools。
- 缺口：缺少面向常见任务的可复制 recipe，例如“新增建表 SQL”“修复 PR 中 SQL”“从数据库反向导入标准”“为某个模块导出最小上下文”。
- 落地产物：新增 CLI/MCP workflow recipes 文档和可选命令封装；为每个 recipe 定义输入、输出、失败处理、推荐下一步和机器可读 JSON 结果；AI Context 中附带 `AGENTS.md` 可直接引用的工作流片段。
- 验收标准：AI agent 按 recipe 能稳定完成至少三类任务：建表前取标准、PR SQL review、数据库反向导入后补标准；失败时能输出下一步诊断建议。
- 边界：不内置外部 LLM，不自动修改业务仓库，第一版以命令/文档/JSON 契约为主。

### P6-12：AI 输出契约稳定性与兼容测试
- 状态：待办。
- 为什么做：DataSpec 优先给 AI 使用，最怕的是字段目录、规则、lint 结果、推荐结果或 MCP 输出字段悄悄漂移，导致 agent 读错上下文或自动化脚本失效。
- 已有基础：已有 OpenAPI 契约检查、AI Context JSON Schema、CLI/MCP JSON 输出、字段推荐和 SQL lint 结构化结果。
- 缺口：缺少面向 AI 消费场景的契约 golden 测试，无法系统覆盖 `field-catalog.json`、`rules.yaml`、`LintIssue`、推荐结果、DDL 生成结果和 MCP tool/resource 返回结构。
- 落地产物：建立 AI contract fixtures；为 AI Context、CLI JSON、MCP resources/tools、字段推荐和 lint/fixedSql 输出增加 golden 断言；README 或 `.dataspec/README.md` 标明稳定字段与兼容策略。
- 验收标准：改动 AI 可消费字段时，测试能明确显示契约变化；兼容字段新增不会破坏旧 fixtures；`mvn test`、`node --test` 或前端契约检查能覆盖主要 JSON 输出。
- 边界：不冻结所有内部 DTO，不阻止向后兼容新增字段，不引入外部契约服务。

### P6-13：GitHub inline review 实战接入
- 状态：待办。
- 为什么做：P5-5 已提供 SQL issue 文件内行列范围，但当前 `review-pr` 仍发布单条汇总评论；真正落到 PR diff inline 后，开发者和 AI 才能在具体 SQL 行旁边处理问题。
- 已有基础：已有 CLI `review-pr`、GitHub token 参数、PR 汇总评论 marker、文件级 source range 和“不直接发 inline”的边界说明。
- 缺口：缺少 GitHub diff hunk position 映射、inline comment 去重、过期评论处理、fallback 汇总评论和失败诊断。
- 落地产物：新增 PR diff 映射模块；当 issue 行号落在本次 diff hunk 内时发布或更新 inline comment；无法映射时保留汇总评论；输出 JSON 说明 inline/fallback 数量和原因。
- 验收标准：PR 中新增或修改 SQL 文件能收到对应行 inline comment；重复运行不会刷屏；不在 diff 内的问题进入汇总区；失败时 CLI 给出 token、权限或 diff 映射诊断。
- 边界：不做代码所有者审批，不改 GitHub Actions 示例以外的业务仓库文件，不要求所有历史 SQL 问题都能 inline。

### P6-14：项目内字段分组与数据域体验增强
- 状态：待办。
- 为什么做：字段标准变多后，仅靠字段列表搜索不够，个人/小团队需要按项目内业务域、模块、标签和来源批次组织数标，AI 也需要清楚“当前任务属于哪个字段范围”。
- 已有基础：字段已有 category、tags、数据域关系、来源批次、字段库筛选和 AI Context 导出。
- 缺口：数据域/分组更像字段属性，缺少面向日常维护的分组视图、批量归组、未分组字段提示和按分组导出 AI Context 的入口。
- 落地产物：强化字段库分组视图；支持按数据域/category/tag/sourceBatch 分组浏览、批量设置分组、查看未分组字段；AI Context 和字段推荐可按分组裁剪或提示命中范围。
- 验收标准：用户能在一个项目内按业务分组管理标准字段；未分组字段可快速定位并批量补齐；AI 导出可只包含指定分组并说明裁剪条件。
- 边界：不做跨项目组织级目录，不引入审批/发布流，不把分组升级成复杂权限模型。

### P6-15：字段库批量维护与可撤销变更
- 状态：待办。
- 为什么做：反向导入、Excel 导入和质量检查会一次性暴露大量待修字段；如果只能逐条编辑，个人使用也会变慢，而且误操作后缺少快速恢复手段。
- 已有基础：已有字段 CRUD、Excel dry-run、变更日志 before/after、来源追踪和字段质量/冲突检测待办。
- 缺口：缺少字段状态、分类、标签、敏感标记、别名等常见属性的批量编辑；变更日志已有记录但还不能从 UI 一键回退单次变更。
- 落地产物：新增字段库批量操作入口和轻量撤销能力；支持批量设置状态/category/tags/sensitive/codeSetId，提交前展示影响预览；字段详情或变更日志可对最近单条变更执行可控回退。
- 验收标准：用户可选择多条字段批量维护常用属性；批量操作写入变更日志；误改单个字段后可从日志恢复上一版关键属性。
- 边界：不做复杂事务审批，不自动合并冲突字段，不跨项目批量修改。

### P6-16：大字段库性能与可观测性基线
- 状态：待办。
- 为什么做：当标准字段、检查记录、导入批次和 AI Context 变多后，前端列表、字段推荐、Context 导出和 CLI/MCP 响应延迟会直接影响 AI 和人的日常使用。
- 已有基础：已有分页接口、字段推荐、AI Context 导出、工作台统计、CLI/MCP 和 Spring Boot Actuator 基础依赖可扩展空间。
- 缺口：缺少大数据量 fixture、接口耗时基线、慢查询/慢导出诊断、前端大列表体验验证和 CLI/MCP 超时边界。
- 落地产物：新增性能基线测试或本地脚本，模拟千级/万级字段、百级规则和检查记录；记录字段列表、推荐、AI Context、lint records、反向导入 compare 的耗时；补充必要索引、分页和导出限流提示。
- 验收标准：大字段库场景下核心接口耗时有可重复测量结果；明显慢点有日志或诊断提示；前端列表不因大数据量明显卡顿；CLI/MCP 超时信息可读。
- 边界：不做分布式部署，不引入缓存集群，不为个人版过早上复杂监控平台。

### P6-17：前端关键流程 E2E 冒烟与回归门禁
- 状态：待办。
- 为什么做：前端页面已覆盖字段库、规则、SQL 校验、DDL、反向导入、AI Context 和工作台，但目前主要依赖构建和局部单测，核心流程仍容易在导航、项目切换或接口类型变化时悄悄回归。
- 已有基础：前端已有 Vue 3、Element Plus、Pinia、Axios、Monaco、`pnpm build` 和部分测试入口；后端已有 demo project 与核心接口。
- 缺口：缺少一组稳定的端到端冒烟用例，覆盖“选择项目 -> 校验 SQL -> 查看 fixedSql/记录 -> 反向导入预览 -> 字段库筛选 -> 导出 AI Context”等高频链路。
- 落地产物：新增前端 E2E 或组件集成测试入口，准备最小 demo 数据，覆盖关键导航、项目状态联动、核心按钮、空状态和错误提示；将命令接入统一验证说明。
- 验收标准：本地一条命令能跑完关键流程冒烟；破坏项目选择、接口字段、核心按钮或路由跳转时测试能失败；CI/本地验证文档清楚说明依赖。
- 边界：不追求全页面像素级截图，不覆盖所有表单排列组合，不引入重量级测试平台。

### P6-18：AI 可读错误码与下一步建议标准化
- 状态：待办。
- 为什么做：DataSpec 优先让 AI 使用，接口、CLI 和 MCP 失败时不能只给人类文本；AI 需要稳定的错误码、原因、可重试性和下一步动作，才能自动恢复或给出准确建议。
- 已有基础：已有 `dataspec doctor`、统一 API wrapper、CLI/MCP JSON 输出、OpenAPI 契约和安全基线。
- 缺口：不同入口的错误结构还不统一，部分失败只返回 message；缺少 `code`、`category`、`retryable`、`suggestedAction`、`docsRef` 等机器可读字段。
- 落地产物：定义轻量错误响应契约；统一核心 API、CLI 和 MCP 的错误输出；为服务未启动、projectId 无效、token 无权限、OpenAPI 漂移、SQL 解析失败、数据库连接失败等场景补稳定错误码和建议动作。
- 验收标准：AI agent 可根据错误码判断是否需要运行 doctor、切换项目、重新生成 schema、补 token 或提示用户；错误契约有 golden/单测覆盖；README 或 `.dataspec/README.md` 记录稳定字段。
- 边界：不做复杂国际化，不改变 HTTP 状态语义，不把内部异常堆栈暴露给前端或 AI。

### P6-19：字段标准检索 API 与语义查询增强
- 状态：待办。
- 为什么做：字段推荐解决“给一个字段名推荐标准字段”，但 AI 和人还会问“支付金额相关字段有哪些”“订单域可用字段有哪些”“这个表建表前要参考哪些字段”；需要更像目录检索的入口。
- 已有基础：已有字段列表、字段推荐、category/tags/alias、AI Context、MCP resources 和按需裁剪待办。
- 缺口：缺少面向 AI 的标准字段搜索 API，无法稳定返回 query 命中原因、作用域、字段数量、相近字段和缺失提示；前端字段库搜索也偏列表过滤。
- 落地产物：新增字段标准检索 API/CLI/MCP 工具；支持按关键字、中文描述、category、tag、表/模块、敏感标识、状态和来源批次查询；返回命中原因、推荐使用范围和下一步建议。
- 验收标准：AI agent 能在建表或修 SQL 前检索最相关字段集合；搜索结果可被字段库页面复用；同义词、别名和拼音缩写有核心测试覆盖。
- 边界：不引入向量数据库，不调用外部 LLM，不替代现有字段推荐接口。

### P6-20：OpenSpec 归档与主规格同步收口
- 状态：待办。
- 为什么做：项目已经持续使用 OpenSpec，但完成的 change 较多，如果不定期归档并同步主规格，后续 AI 会在 active changes、README、TODO 和实际能力之间读到过期上下文。
- 已有基础：已有多个 OpenSpec change、README 当前功能概览、TODO 完成项归档和 OpenSpec validate 流程。
- 缺口：完成 change 仍散落在 `openspec/changes` 中，主规格和归档证据缺少周期性收口规则；部分任务完成证据只存在 commit 和 TODO 摘要里。
- 落地产物：建立 OpenSpec 收口任务：确认已完成 change、同步 delta spec 到主 specs、归档 change、补 Verification Evidence、更新 README/TODO 入口和过期链接。
- 验收标准：已完成 change 不再干扰“下一步待办”判断；`openspec validate` 通过；归档记录能追溯关键验证命令、commit 和遗留边界。
- 边界：不在本任务中实现新产品能力，不重写历史方案，只处理已完成且证据清晰的变更。

### P6-21：按历史标准快照导出与任务回放
- 状态：待办。
- 为什么做：P6-1 已能记录标准快照版本和 hash，但 AI 真正排查旧 SQL、旧 DDL 或旧检查记录时，需要按当时的标准快照重新导出上下文，而不是总使用最新标准。
- 已有基础：已有 `ds_standard_snapshot`、AI Context 标准元数据、SQL 检查记录快照引用、DDL 生成结果快照引用和 AI 回放待办。
- 缺口：当前快照 payload 主要用于记录和追溯，还缺少按 snapshotId/version 导出 field catalog、rules.yaml、DDL 上下文和检查记录详情回放的入口。
- 落地产物：新增按快照导出的 API/CLI 参数；AI Context、DDL 生成、SQL 检查记录详情可选择当前标准或指定历史快照；记录详情展示“当时标准”和“当前标准”的差异摘要。
- 验收标准：给定一条历史 SQL 检查记录，用户或 AI 能按记录中的 snapshotId 导出当时上下文并复现主要诊断；无快照的历史记录仍走 `unversioned` 兼容路径。
- 边界：不做复杂审批发布，不自动回滚当前标准，不要求所有旧记录都能补齐历史快照。

### P6-22：SQL/DDL 多方言兼容矩阵与诊断
- 状态：待办。
- 为什么做：项目已支持 PostgreSQL/MySQL 的部分解析、校验和反向导入，但 AI 或 CLI 在不同业务仓库使用时，需要明确哪些方言能力可靠，哪些是降级或不支持。
- 已有基础：已有 JSqlParser、PostgreSQL/MySQL 反向导入、MySQL 规则覆盖、fixture/golden 基线和 SQL lint/fixedSql 流程。
- 缺口：缺少方言能力矩阵、方言专属 fixture、错误码和降级提示；用户不知道 `COMMENT`、自增、索引、类型映射、schema/catalog、quoted identifier 等差异是否被完整覆盖。
- 落地产物：建立 PostgreSQL/MySQL 方言能力矩阵和测试集；在解析、lint、DDL 生成、反向导入、fixedSql 中输出方言诊断；README 标明已验证能力和已知边界。
- 验收标准：新增或修改 SQL 方言行为时有 fixture 能防回归；不支持的语法能给出可读且 AI 可解析的诊断；前端和 CLI 都能显示当前方言及降级原因。
- 边界：第一版不追求 Oracle/SQL Server 全量支持，不手写完整 SQL parser，不把未验证方言标成已支持。

### P6-23：规则模板库与项目基线套件
- 状态：待办。
- 为什么做：个人/小团队会反复配置相似的字段命名、注释、敏感字段、金额/时间/状态字段规则；AI 使用时也需要知道项目采用的是哪套规则基线。
- 已有基础：已有规则配置、规则启停、AI rules.yaml、模板 DDL、字段推荐和演示项目初始化。
- 缺口：规则仍偏单条配置，缺少可选择、可复制、可导出的规则模板套件；新项目缺少“轻量默认基线”“严格基线”“兼容历史库基线”等可落地起点。
- 落地产物：新增规则模板库和项目基线选择入口；支持从内置模板初始化规则、导出/导入规则基线、在 AI Context 中标明当前基线名称和版本。
- 验收标准：新项目可一键套用个人默认规则基线；规则模板变更有版本和说明；AI 读取 rules.yaml 能知道规则来自哪套基线。
- 边界：不做组织级发布审批，不强制所有项目统一规则，不自动覆盖用户已调整的规则。

### P6-24：项目备份、恢复与迁移包
- 状态：待办。
- 为什么做：项目优先个人/小团队使用，本地库、演示库或轻量部署都可能迁移机器；标准字段、规则、模板、快照和来源记录需要一个可恢复的交付包。
- 已有基础：已有数据字典导出、Excel 导入导出、AI Context zip、标准快照、规则配置、字段来源和项目模型。
- 缺口：当前导出更偏 AI 消费和字段交换，缺少覆盖项目完整配置的备份包、恢复 dry-run、版本兼容检查和敏感信息剔除规则。
- 落地产物：新增项目备份导出和恢复预览；备份包含项目元数据、字段、枚举、规则、模板、标准快照、来源批次和必要变更日志；恢复前展示冲突、覆盖范围和兼容性提示。
- 验收标准：一个项目可导出为不含 token/password 的迁移包；在新环境 dry-run 后可恢复主要标准资产；恢复过程保留审计摘要并避免误覆盖。
- 边界：不备份源数据库数据行，不保存数据库密码或 API token 明文，不替代数据库级物理备份。

### P6-25：数据库直连只读安全诊断与最小权限指引
- 状态：待办。
- 为什么做：数据库直连反向导入和二次比对是高价值能力，但个人使用也容易拿高权限账号直连；DataSpec 应主动提示只读账号、权限范围和敏感信息处理边界。
- 已有基础：已有数据库连接测试、metadata 读取、反向导入、二次比对、非敏感连接记忆和不保存密码的约束。
- 缺口：连接测试只验证可用性，缺少权限级别提示、只读账号建议、可访问 schema/table 范围诊断和连接信息脱敏展示。
- 落地产物：增强连接测试结果，输出数据库类型、当前用户、只读/写权限推断、可访问 schema/table 数、危险权限提示和推荐 SQL；前端用清晰状态展示，不阻塞个人快速使用。
- 验收标准：使用高权限账号连接时能看到风险提示；只读账号通过时显示“适合反向导入/比对”；日志、前端和记录中不泄漏 password 或完整连接串。
- 边界：不自动创建数据库账号，不执行写操作探测权限，不做企业级密钥托管。

### P6-26：AI 批量任务与结果交付包
- 状态：待办。
- 为什么做：AI agent 常见任务不是只检查一条 SQL，而是批量扫描多个 SQL 文件、多个表或一次反向导入结果；需要一个稳定的批处理输出，方便 AI 汇总、修复和交付。
- 已有基础：已有 CLI `lint-files`、PR review、SQL 检查记录、fixedSql、字段推荐、AI Context、数据库直连表选择和 JSON 输出。
- 缺口：批量任务缺少统一任务 ID、进度摘要、分文件/分表结果、可下载结果包和失败重试建议；前端也缺少批量结果视图。
- 落地产物：新增轻量 batch run 模型或文件级结果聚合；CLI/API/前端输出统一的任务摘要、结果明细、fixedSql 汇总、未纳管字段列表和机器可读交付包。
- 验收标准：对一个业务仓库或一批表执行检查后，AI 能拿到完整 JSON 结果包并继续修复；用户能在前端查看批量任务结果和失败原因。
- 边界：不做后台长任务平台，不接外部队列，不自动提交业务仓库修改。

### P6-27：AI 使用反馈与标准改进闭环
- 状态：待办。
- 为什么做：字段推荐、SQL 修复和 DDL 生成会暴露“AI 总是选错哪个字段”“哪些规则最常误报”“哪些标准字段总被补充别名”等问题；这些反馈应反哺标准维护。
- 已有基础：已有字段推荐原因、SQL 检查记录、fixedSql、规则配置、字段来源、标准质量评分和冲突检测待办。
- 缺口：AI 使用结果目前更多是单次输出，缺少按字段/规则/任务聚合的反馈指标，也缺少从反馈跳转到字段质量修复或规则例外的入口。
- 落地产物：新增轻量反馈聚合视图；统计推荐命中/未命中、fixedSql 常见修复、误报豁免、未纳管字段转正率和字段被 AI 引用次数；提供“补别名、补注释、调整规则、加入例外”的下一步动作。
- 验收标准：用户能看到标准系统被 AI 使用后的高频问题；能从反馈直接跳转到字段库、规则配置、质量检查或例外管理；统计不采集业务数据行。
- 边界：不做用户行为监控，不调用外部分析服务，不把反馈自动写成标准变更。

### P6-28：标准候选 Inbox 与采纳工作台
- 状态：待办。
- 为什么做：覆盖率报告、反向导入、字段推荐未命中和 AI 使用反馈都会产生“可能应该进入标准库”的候选项；如果散落在各页面，用户和 AI 都很难形成持续改进闭环。
- 已有基础：已有反向导入预览、字段覆盖率报告、字段推荐、导入来源批次、字段变更日志和 AI 使用反馈待办。
- 缺口：候选字段、疑似重复字段、待补别名和待修质量问题缺少统一入口；接受、忽略、合并、延后这些决策也没有结构化记录。
- 落地产物：新增轻量标准候选 Inbox；聚合反向导入、覆盖率、字段推荐和质量检查产生的候选；支持接受为新字段、合并到已有字段、忽略并填写原因、延后处理；决策结果写入来源和变更日志。
- 验收标准：用户能在一个页面处理“未纳管字段 -> 标准字段”的采纳流程；AI 能读取候选状态和用户决策，避免反复推荐已忽略项。
- 边界：不做审批流，不自动合并标准字段，不把 Inbox 变成团队工单系统。

### P6-29：离线 AI Context 与业务仓库缓存模式
- 状态：待办。
- 为什么做：AI agent 在业务仓库中工作时，不一定随时能连上 DataSpec 服务；需要一个可提交或可缓存的离线上下文，让 AI 至少能按最近标准进行 lint、检索和生成提示。
- 已有基础：已有 AI Context zip、标准快照、`.dataspec/config.json`、`dataspec doctor`、CLI/MCP 和按历史快照导出待办。
- 缺口：当前 AI Context 更像一次性下载包，缺少业务仓库内缓存目录、过期提示、离线读取优先级和缓存与服务端快照的一致性诊断。
- 落地产物：新增 `dataspec context export --cache` 或等价命令；把 field catalog、rules、snapshot metadata 和 AGENTS 片段写入 `.dataspec/context/`；CLI/MCP 支持在服务不可用时读取缓存并提示 stale 状态。
- 验收标准：后端服务未启动时，AI 仍能读取最近一次缓存的标准上下文；`dataspec doctor` 能报告缓存版本、过期时间和与远端快照的差异。
- 边界：不缓存 token、数据库密码或业务数据行；离线模式不允许写入 DataSpec 服务端状态。

### P6-30：数据库元数据适配层与离线 schema dump
- 状态：待办。
- 为什么做：数据库直连反向导入、二次比对和覆盖率报告都依赖 metadata 读取；后续支持更多方言或离线排查时，需要把 JDBC 读取、方言映射和标准分析解耦。
- 已有基础：已有 PostgreSQL/MySQL 直连 metadata、反向导入、二次比对、覆盖率报告、多方言兼容矩阵待办和数据库只读安全诊断待办。
- 缺口：metadata 提取逻辑还偏连接时流程，缺少统一 schema snapshot 数据结构、适配器测试和从离线 dump 生成报告的入口。
- 落地产物：抽象数据库 metadata adapter；支持把选定 schema/table 导出为 DataSpec schema dump JSON；覆盖率、反向导入和比对可以从直连或 dump 两种输入运行。
- 验收标准：同一份数据库 metadata dump 可复现反向导入候选、覆盖率和差异报告；PostgreSQL/MySQL 适配器有 fixture 覆盖；离线 dump 不包含数据行。
- 边界：不扫描业务数据，不执行写操作，不一次性承诺所有数据库方言。

### P6-31：Prompt 模板版本化与效果评测
- 状态：待办。
- 为什么做：AI Prompt、DDL 生成和 SQL 修复提示会持续调整；如果没有模板版本和样例评测，prompt 改动可能让 AI 输出格式、字段选择或修复策略悄悄退化。
- 已有基础：已有 AI Prompt 生成、DDL 生成、fixedSql、标准快照、AI 输出契约稳定性待办和 AI 生成回放待办。
- 缺口：Prompt 模板缺少版本号、变更说明、样例输入输出 fixture 和可重复评测入口；AI 回放记录也难以说明“当时用的是哪版 prompt”。
- 落地产物：建立 Prompt template registry；为建表、修 SQL、字段推荐解释等模板记录版本、适用场景和稳定输出约束；新增 prompt fixture/eval 脚本，对关键样例做输出结构和差异检查。
- 验收标准：修改 prompt 后能看到样例输出差异；AI 作业记录能引用 promptVersion；破坏 JSON/Markdown 契约时验证失败。
- 边界：第一版不强制调用外部 LLM，不做复杂在线实验平台，不保存第三方 API key。

### P6-32：项目活动时间线与轻量审计视图
- 状态：待办。
- 为什么做：个人/小团队虽然不需要重审批，但仍需要知道最近谁或哪个 AI/CLI 做了标准快照、导入、字段修改、SQL 检查、token 使用和 Context 导出，便于回滚和排查。
- 已有基础：已有字段变更日志、导入来源批次、标准快照、SQL 检查记录、DDL 生成记录、token lastUsedAt 和项目工作台。
- 缺口：活动信息分散在不同页面，缺少按项目聚合的时间线、操作者来源、动作类型和跳转详情。
- 落地产物：新增项目活动 API 和前端时间线；聚合字段变更、快照创建、导入批次、SQL 检查、DDL 生成、AI Context 导出和 token 使用摘要；支持按动作类型筛选。
- 验收标准：打开项目工作台能看到最近关键活动并跳转详情；AI/CLI 操作能留下可读来源；排查“标准什么时候变了”不再需要翻多张表。
- 边界：不做企业级审计留存，不引入复杂权限模型，不记录业务数据内容。

### P6-33：前端任务式导航与空状态收口
- 状态：待办。
- 为什么做：功能增多后，左侧按模块导航对熟手可用，但新项目或 AI 辅助操作更需要按任务入口开始，例如“接入项目”“导入现有库”“检查 SQL”“补标准”“导出给 AI”。
- 已有基础：已有工作台、项目选择、字段库、SQL 校验、DDL 生成、反向导入、覆盖率报告、AI Context 和 token 管理页面。
- 缺口：跨页面任务链路缺少统一入口、面包屑、空项目引导、最近使用动作和失败后的下一步建议；部分页面的空状态还偏孤立。
- 落地产物：重整工作台任务入口和关键页面空状态；补充任务式快捷入口、面包屑、最近使用项目/动作、空项目引导和错误后的可执行跳转。
- 验收标准：用户从首页能直接进入导入数据库、生成覆盖率、校验 SQL、补字段和导出 AI Context；新项目没有数据时能按引导完成第一条标准闭环。
- 边界：不做大规模视觉改版，不替换 Element Plus 体系，不引入复杂工作流引擎。

### P6-34：本地部署与演示数据一键启动包
- 状态：待办。
- 为什么做：项目优先个人/小团队使用，安装启动成本会直接影响采用；同时 AI agent 和回归测试也需要一个可重复的本地演示环境。
- 已有基础：已有 Flyway、README 启动说明、演示项目初始化、后端/前端验证命令和 Docker 化可扩展空间。
- 缺口：启动后端、前端、数据库和 demo 数据仍需要手动串命令；演示数据与验收路径没有形成一键 smoke 流程。
- 落地产物：新增本地启动脚本或 docker compose；包含 PostgreSQL、后端、前端和 demo seed；提供 health check、端口冲突提示和一键 smoke 验证命令。
- 验收标准：新机器能通过一条命令启动可用环境；demo 项目包含字段、规则、SQL 样例和覆盖率样例；README 明确个人本地与开发模式差异。
- 边界：不做生产级部署方案，不引入 Kubernetes，不替代正式数据库备份。

### P6-35：fixedSql 修复策略配置与 dry-run 解释
- 状态：待办。
- 为什么做：fixedSql 对 AI 很有用，但自动修复天然有风险；用户需要知道每一次修改来自哪个规则、是否安全、能否关闭某类修复，以及是否只做 dry-run 解释。
- 已有基础：已有 SQL lint、fixedSql、diff 展示、规则配置、检查记录、AI 可读 lint 输出和结构化修复建议。
- 缺口：修复策略还不够可配置，fixedSql 与 issue、规则、变更片段之间的映射说明不稳定；AI 难以只请求“低风险格式/注释修复”。
- 落地产物：新增修复策略配置和 dry-run 输出；为每个 fixer 标注 ruleCode、riskLevel、enabled、explain；fixedSql 返回变更列表、原片段、目标片段和不可自动修复原因。
- 验收标准：用户可关闭高风险修复；AI 能请求 safe-only 修复并获得机器可读解释；前端 diff 能显示每个变更对应的规则和风险级别。
- 边界：不自动写回业务仓库，不承诺所有 SQL issue 都能自动修复，不跳过人工确认。

### P6-36：AI 使用画像与任务模式配置
- 状态：待办。
- 为什么做：DataSpec 的使用者既有人，也有 CLI/MCP/coding agent；不同任务需要不同上下文大小、规则严格度、输出格式和修复策略，如果都靠 prompt 临时说明，AI 很容易拿错上下文。
- 已有基础：已有 `.dataspec/config.json`、AI Context、MCP resources/prompts/tools、workflow recipes 待办、按需 Context 待办和 fixedSql 策略化待办。
- 缺口：缺少项目级 `aiProfile` 或任务模式配置，无法稳定声明“建表生成”“SQL 修复”“反向导入补标准”“PR review”分别该用哪些标准范围、规则基线和输出契约。
- 落地产物：新增轻量 AI profile 配置；支持 taskType、contextScope、ruleset、fixedSqlPolicy、outputFormat、maxContextFields、推荐后续命令等字段；CLI/MCP/API 能读取默认 profile，前端可查看和切换。
- 验收标准：AI agent 可根据 profile 自动选择最小上下文和输出格式；`dataspec doctor` 能诊断 profile 缺失或引用不存在的规则/分组；配置字段有 JSON Schema 或契约测试防漂移。
- 边界：不做复杂角色权限，不替代用户 prompt，不保存外部 LLM provider 配置。

### P6-37：标准系统 Schema Registry 与字段契约版本
- 状态：待办。
- 为什么做：项目优先给 AI 使用后，字段、枚举、规则、模板、快照和 Context 输出本身就是一套数据契约；如果契约没有版本和兼容策略，AI/CLI/MCP 会在字段改名或结构调整时读错。
- 已有基础：已有 OpenAPI 类型生成、AI Context JSON Schema、标准快照、AI contract fixtures 待办和 Prompt 模板版本化待办。
- 缺口：当前契约散落在 OpenAPI、前端类型和导出 JSON 中，缺少统一的 schemaVersion、兼容说明、废弃字段策略和变更检查入口。
- 落地产物：建立标准契约 registry；为 Field、Enum、Rule、Template、Snapshot、LintResult、AI Context 等 AI 消费结构声明 schemaVersion、JSON Schema、废弃字段和兼容窗口；导出物统一携带契约版本。
- 验收标准：新增或删除 AI 可消费字段时必须更新契约；契约变更能通过 golden 测试或 schema diff 看出来；README 或 `.dataspec/README.md` 说明兼容策略。
- 边界：不引入重型 schema registry 服务，不要求历史所有导出包完全补齐版本，只从新增入口开始收敛。

### P6-38：AI 执行证据包与交付归档
- 状态：待办。
- 为什么做：AI 完成一次建表、修 SQL、导入标准或覆盖率分析后，用户需要一份可交付、可复盘的证据包，而不是只看一次页面结果或聊天摘要。
- 已有基础：已有 AI 回放待办、SQL 检查记录、fixedSql、标准快照、覆盖率报告、DDL 生成记录和项目活动时间线待办。
- 缺口：任务输入、标准版本、命令、输出、校验结果、跳过项和后续建议没有形成统一归档，AI 无法稳定把“我做了什么、依据是什么、还剩什么”交给用户。
- 落地产物：新增执行证据包导出；支持按 AI job、SQL check、coverage report 或 batch run 生成 JSON/zip，包含 replay payload、snapshot metadata、验证摘要、关键输出和脱敏后的错误信息。
- 验收标准：完成一次 SQL 修复或覆盖率报告后，用户能下载或复制证据包；AI 可把证据包作为交付附件继续传给下游任务；包内不包含数据库密码、token 或业务数据行。
- 边界：不做长期对象存储，不上传第三方服务，不把证据包变成企业审计系统。

### P6-39：前端统一数据状态与可恢复错误体验
- 状态：待办。
- 为什么做：功能页越来越多后，项目未选择、接口失败、空数据、加载中、无权限和后端未启动等状态如果各写各的，用户和 AI 自动化都很难判断下一步该点哪里。
- 已有基础：已有工作台、项目选择、字段库、反向导入、覆盖率报告、SQL 校验、AI Context、token 管理和任务式导航待办。
- 缺口：页面级 loading/empty/error/retry/project guard 缺少统一模式；有些页面空状态缺少可执行跳转，有些错误只显示原始接口文本。
- 落地产物：新增前端 `useRequestState`、`ProjectRequired` 或等价组件/组合函数；统一空状态、错误动作、重试按钮、项目选择提示和 API 错误码展示；关键页面逐步迁移。
- 验收标准：未选择项目时所有业务页都有一致提示和跳转；接口失败时显示可操作建议；前端测试覆盖项目缺失、空数据和失败重试。
- 边界：不做大视觉改版，不替换 Element Plus，不一次性重写所有页面。

### P6-40：AI/CLI 并发写入幂等与任务锁
- 状态：待办。
- 为什么做：当多个 coding agent、CLI 或前端页面同时操作一个项目时，可能重复创建快照、重复导入候选、重复生成记录或覆盖彼此的配置；个人工具也需要基础幂等保护。
- 已有基础：已有标准快照、导入批次、SQL 检查记录、AI 作业回放、token 管理和项目活动时间线待办。
- 缺口：写接口缺少 idempotency key、批量操作缺少任务锁或重复提交保护；AI 自动重试时可能制造重复记录。
- 落地产物：为高风险写入引入轻量 idempotency key 和项目级操作锁；至少覆盖标准快照创建、反向导入确认、批量 lint/coverage、AI job 记录和备份恢复；错误响应提示可重试性。
- 验收标准：同一个 key 重复提交不会产生重复数据；并发导入同一批候选时能得到明确冲突或排队提示；相关 API/CLI 有幂等测试。
- 边界：不引入外部队列，不做分布式事务，不阻塞普通单条 CRUD。

### P6-41：标准变更 What-if 预览与回滚辅助
- 状态：待办。
- 为什么做：字段、规则、模板或分组变更会影响 AI Context、DDL 生成、SQL lint 和字段推荐；用户在保存前应该能预览影响，保存后也要知道如何回退到上一个可信状态。
- 已有基础：已有字段变更日志、标准快照、轻量字段影响分析待办、备份恢复待办和规则配置。
- 缺口：编辑标准时缺少 what-if diff；变更日志能记录 before/after，但还不能把影响范围、建议验证命令和回退动作串起来。
- 落地产物：新增标准变更预览接口和前端保存前摘要；展示受影响字段/规则/模板/Context 范围、推荐验证命令、可回退快照或变更日志入口。
- 验收标准：修改字段类型、状态、别名或规则前能看到影响摘要；保存后可跳转到变更日志和相关快照；AI 能读取预览结果判断是否需要用户确认。
- 边界：不做强审批流，不自动回滚数据库，不阻止个人快速保存。

### P6-42：领域 Starter Kit 与项目模板
- 状态：待办。
- 为什么做：新项目从空字段库开始成本高，尤其是用户想快速让 AI 写订单、用户、支付、库存、审计等常见表时，需要可选的领域初始标准。
- 已有基础：已有内置 standards 初始化、演示项目、规则模板库待办、项目备份迁移包待办和字段推荐。
- 缺口：当前初始化偏基础字段和演示数据，缺少按业务领域选择的字段集、枚举、规则和表模板组合。
- 落地产物：新增领域 starter kit；提供电商/订单、用户账号、支付金额、审计日志等小型可组合模板；创建项目或导入时可选择，所有字段标记来源和版本。
- 验收标准：新建项目可选择一个或多个 starter kit 快速生成可用标准；AI Context 能标明字段来自哪个 kit；重复安装不会覆盖用户修改。
- 边界：不追求行业全量模型，不强制用户采用模板，不把 starter kit 当作企业主数据标准。

### P6-43：AI 能力清单与自描述入口
- 状态：待办。
- 为什么做：DataSpec 的 API、CLI、MCP、前端页面越来越多，AI agent 需要先知道“当前项目有哪些可用能力、参数、前置条件和失败恢复方式”，而不是每次从 README 或源码里猜。
- 已有基础：已有 OpenAPI、CLI help、MCP resources/prompts/tools、`dataspec doctor`、工作流模板待办和 AI 可读错误码待办。
- 缺口：缺少一个机器可读的 capability catalog，稳定描述支持的任务类型、命令/API、输入输出 schema、权限要求、示例和推荐下一步。
- 落地产物：新增 `/api/capabilities` 或 CLI/MCP 等价入口；输出能力清单、版本、依赖检查、示例命令、docsRef、稳定字段和不支持能力说明；README 与 `.dataspec/README.md` 引导 AI 先读取能力清单。
- 验收标准：AI agent 在未知项目中可先调用能力清单，再决定运行 doctor、导出 Context、lint、推荐字段或反向导入；清单与 OpenAPI/CLI/MCP 的关键入口保持一致并有契约测试。
- 边界：不替代完整文档，不自动执行任务，不把实验性内部接口声明为稳定能力。

### P6-44：前端 URL 状态与可复现操作链接
- 状态：待办。
- 为什么做：前端已覆盖多条工作流，但页面状态多靠本地内存或 localStorage；用户和 AI 需要可复制的链接来复现一次筛选、检查记录、导入批次、覆盖率结果或 AI 回放详情。
- 已有基础：已有路由、项目选择、SQL 检查记录、反向导入来源批次、覆盖率报告、AI 回放、字段库 query 筛选和前端状态统一待办。
- 缺口：列表筛选、详情抽屉、项目 ID、记录 ID、表名、状态筛选等状态没有统一映射到 query/hash，刷新或分享后容易丢失上下文。
- 落地产物：为关键页面补 URL 状态协议和复制链接入口；覆盖字段库筛选、SQL 检查记录详情、AI 回放详情、覆盖率表筛选和反向导入批次详情；入口参数无效时给出可恢复提示。
- 验收标准：复制链接到新标签页可复现同一个项目、筛选条件和详情记录；AI/browser automation 可通过 URL 直达目标状态；前端测试覆盖 query 解析和无效参数兜底。
- 边界：不把敏感连接信息、SQL 原文、token 或密码写入 URL；不重做整体路由体系。

### P6-45：敏感信息脱敏与日志输出边界
- 状态：待办。
- 为什么做：数据库直连、token、AI 回放、执行证据包和错误诊断都会处理连接信息或输入 payload；需要统一保证日志、前端提示、证据包和记录详情不泄漏密码、token、完整连接串或高敏业务片段。
- 已有基础：已有 API Token hash 存储、不保存数据库密码约束、反向导入本地记忆剔除敏感字段、备份/证据包不含敏感信息的待办边界和安全基线。
- 缺口：脱敏规则散落在各功能中，缺少统一 sanitizer、测试夹具、日志字段白名单和“允许记录/禁止记录”清单。
- 落地产物：新增敏感信息 sanitizer 工具和跨模块使用规范；覆盖 JDBC URL、password、token、Authorization、SQL literal、连接表单和 AI payload；日志、错误响应、回放记录和导出包统一脱敏。
- 验收标准：核心 API 和 CLI 错误路径不会输出明文密码/token；含敏感字段的 fixture 通过脱敏测试；README 或安全文档明确记录哪些字段可持久化、哪些只能内存使用。
- 边界：不做企业级密钥托管，不扫描业务数据行，不承诺识别所有自然语言敏感内容。

### P6-46：按变更范围推荐验证命令
- 状态：待办。
- 为什么做：当前验证命令分散在 README、OpenSpec tasks 和开发习惯里；AI 改完代码后需要根据触碰模块自动知道该跑 `mvn test`、`pnpm test/build`、`node --test`、OpenSpec validate 还是契约检查。
- 已有基础：已有 README 验证小节、OpenSpec change tasks、后端/前端/CLI 测试入口、AI contract fixtures 待办和执行证据包待办。
- 缺口：缺少“文件变更 -> 推荐验证命令 -> 结果证据”的轻量规则，导致 AI 容易漏跑某条关键门禁，或为小文档改动跑过重命令。
- 落地产物：新增验证策略文档和可选脚本；根据变更路径推荐最小验证集，输出命令、原因、预计耗时和失败后的下一步；执行证据包可引用验证结果。
- 验收标准：修改后端、前端、CLI、OpenSpec、README/TODO 等不同路径时能得到合理验证建议；脚本或文档被 README 和 AGENTS 片段引用；关键规则有测试或快照覆盖。
- 边界：不替代 CI，不强制所有改动跑全量测试，不执行破坏性命令。

### P6-47：TODO 到 OpenSpec 的实施交接助手
- 状态：待办。
- 为什么做：主待办已经积累大量 P6 任务，真正开工时仍需要把“为什么做、已有基础、缺口、产物、验收、边界”手动转成 OpenSpec proposal/design/spec/tasks；AI 很容易漏掉边界或重复造需求。
- 已有基础：已有结构化 TODO、OpenSpec-first 流程、多个已完成 change、OpenSpec 收口待办和 AGENTS/SDD 规则。
- 缺口：缺少从单个 TODO 条目生成 OpenSpec change 草稿的稳定模板、命名规则、任务拆分和验证清单；也没有检查 TODO 与 OpenSpec 状态是否一致。
- 落地产物：新增轻量 `openspec draft-from-todo` 文档或脚本；读取指定 P6 条目，生成 change_id、proposal、design、spec 草稿和 tasks 初稿，并提示需要人工确认的开放问题。
- 验收标准：选择一个 P6 待办后，可快速生成符合项目格式的 OpenSpec 草稿；生成内容保留原待办边界和验收标准；OpenSpec validate 能通过基础格式检查。
- 边界：不自动实现代码，不自动归档 change，不把模糊待办强行变成无需确认的需求。

### P6-48：业务术语表与同义词词根库
- 状态：待办。
- 为什么做：字段别名散落在单个字段上后，AI 很难稳定理解“用户/账号/会员”“手机号/电话/mobile”“金额/费用/price”等项目级术语关系；需要一层轻量术语表来提升推荐、检索和 Context 裁剪质量。
- 已有基础：字段已有 alias、category、tags、字段推荐原因、字段检索待办和 AI Context 导出。
- 缺口：缺少项目级 glossary，把中文术语、英文词根、拼音缩写、禁用词、推荐 canonical 字段和适用范围统一管理。
- 落地产物：新增术语表模型/API/前端维护入口；支持术语、同义词、英文词根、适用分组、禁用说明和示例字段；字段推荐、检索、AI Context 和 Prompt 可引用术语命中原因。
- 验收标准：AI 查询“会员手机号”“订单费用”等自然语言时能稳定映射到对应标准字段集合；术语冲突可被检测并提示；导出的 Context 包含精简 glossary。
- 边界：不做企业级本体/知识图谱，不引入向量数据库，不自动覆盖字段已有别名。

### P6-49：自然语言需求到标准候选草案
- 状态：待办。
- 为什么做：用户和 AI 常从“我要建一个订单表/会员表/支付流水表”这类自然语言开始；系统应先把需求拆成可选标准字段、缺失候选和歧义点，再生成 DDL 或 Prompt。
- 已有基础：已有字段推荐、字段检索待办、DDL 生成、AI Prompt、表模板和 AI Context。
- 缺口：缺少面向自然语言需求的结构化草案入口；当前需要 AI 自己拼接检索、推荐、模板和 DDL 生成，容易漏字段或误选泛化字段。
- 落地产物：新增需求草案 API/CLI/MCP 或前端入口，输入业务描述、目标表名和可选分组，输出 matchedFields、missingCandidates、ambiguousTerms、recommendedTemplate、nextActions 和可复制 Prompt。
- 验收标准：输入一段建表需求后，系统能列出建议采用的标准字段、需要新增的候选字段和不确定问题；结果可继续进入 DDL 预览或标准候选 Inbox。
- 边界：第一版不调用外部 LLM，不自动写入字段库，不承诺完整领域建模，只做确定性检索和模板化草案。

### P6-50：AI 输出引用证据与 Explain Trace
- 状态：待办。
- 为什么做：AI 采用某个字段、规则或 fixedSql 修复时，用户需要知道依据来自哪个标准字段、规则、快照、术语或质量诊断；没有证据链时，AI 输出很难复盘和信任。
- 已有基础：已有标准快照、字段推荐原因、SQL 检查记录、fixedSql、AI 回放、字段质量评分和执行证据包待办。
- 缺口：推荐、lint、DDL、Prompt 和 Context 输出的 evidence 结构不统一，无法稳定追踪“这个建议为什么出现”。
- 落地产物：定义轻量 explain trace 契约；核心输出附带 evidence 数组，包含 sourceType、sourceId、snapshotVersion、matchReason、confidence、ruleCode 和 docsRef；前端详情页展示证据来源。
- 验收标准：AI 生成或修复结果中的关键字段和规则都有可读证据；回放记录能展示当时使用的标准版本和命中原因；契约有 golden 测试防漂移。
- 边界：不引入完整分布式 tracing 平台，不记录业务数据行，不把 evidence 作为强审批依据。

### P6-51：标准字段生命周期状态机
- 状态：待办。
- 为什么做：字段已有状态，但 AI 使用时更需要明确哪些字段是草稿、可用、废弃、停用，以及废弃字段应替换成什么；否则 AI 可能继续推荐历史字段。
- 已有基础：字段模型已有 status、变更日志、标准快照、质量评分中的废弃说明检查和字段推荐。
- 缺口：字段状态语义还不够统一，缺少结构化 replacementFieldId/replacementReason、状态流转校验和导出给 AI 的稳定说明。
- 落地产物：定义轻量生命周期状态和流转规则；为废弃/停用字段增加结构化替代字段或替代说明；字段推荐、AI Context、DDL 生成和质量评分统一读取生命周期约束。
- 验收标准：废弃字段不会被默认推荐给 AI；需要保留历史兼容时能说明原因和替代字段；字段状态变更进入变更日志和快照。
- 边界：不做审批流，不做组织级发布治理，不阻止个人快速维护标准。

### P6-52：业务仓库变更感知扫描与最小上下文
- 状态：待办。
- 为什么做：AI 在业务仓库中工作时，通常只需要处理本次 git diff 中的 SQL、迁移文件或模型文件；如果每次都扫描全仓和导出完整 Context，会浪费上下文并增加误报。
- 已有基础：已有 `dataspec init`、`.dataspec/config.json` 默认路径、`doctor`、`lint-files`、`review-pr`、AI Context 导出和按需裁剪待办。
- 缺口：CLI 还缺少基于 git diff/defaultPaths 的 changed-file 发现、只对变更文件 lint、并按变更内容导出最小 Context 的稳定入口。
- 落地产物：新增 `dataspec changed`、`lint-changed` 或等价工作流；读取 `.dataspec/config.json`、git diff 和默认路径，输出变更 SQL/DDL 文件、推荐 Context scope、lint 摘要和下一步命令。
- 验收标准：在业务仓库改动少量 SQL 文件后，AI 可一条命令拿到变更文件列表、对应 lint 结果和最小标准上下文；无 git 仓库或无变更时有可恢复提示。
- 边界：不自动修改业务代码，不自动提交，不扫描未配置的大型目录。

### P6-53：标准健康趋势与改进计划
- 状态：待办。
- 为什么做：字段质量、覆盖率、AI 使用反馈和规则误报都是持续改进信号；只看单次报告无法判断标准系统是否真的越来越好。
- 已有基础：已有字段质量评分、覆盖率报告、AI 回放、检查记录、候选 Inbox、项目活动时间线和执行证据包待办。
- 缺口：缺少按时间保存的健康快照、趋势对比、Top 改进项和 AI 可读的下一步维护计划。
- 落地产物：新增标准健康快照和趋势视图；按项目记录质量均分、低质量字段数、覆盖率、未纳管 Top、规则误报、AI 推荐未命中和候选采纳率；生成可复制的改进计划摘要。
- 验收标准：用户能看到本周/本月标准质量和覆盖率变化；AI 可读取 Top actions 并按优先级补字段、修别名或调整规则；趋势数据不包含业务数据行。
- 边界：不做组织 KPI，不接外部 BI，不采集用户行为监控。

## 参考项目索引

- [`sqlfluff/sqlfluff`](https://github.com/sqlfluff/sqlfluff)：模块化、可配置、多方言 SQL linter。
- [`ariga/atlas`](https://github.com/ariga/atlas)：schema-as-code、schema lint 和迁移规划。
- [`ariga/atlas-action`](https://github.com/ariga/atlas-action)：数据库 schema 变更的 GitHub Actions lint 入口。
- [`bytebase/bytebase`](https://github.com/bytebase/bytebase)：数据库 DevOps 工作台、SQL Review、数据库 CI/CD。
- [`bytebase/example-gitops-github-flow`](https://github.com/bytebase/example-gitops-github-flow)：Bytebase + GitHub Flow 数据库发布示例。
- [`k1LoW/tbls`](https://github.com/k1Low/tbls)：CI-friendly 数据库文档生成工具。
- [`dbt-labs/dbt-core`](https://github.com/dbt-labs/dbt-core)：项目化数据模型、文档和可复现构建的参考。
- [`great-expectations/great_expectations`](https://github.com/great-expectations/great_expectations)：数据质量规则、验证结果和文档化体验参考。
- [`datahub-project/datahub`](https://github.com/datahub-project/datahub)：数据目录、字段影响分析和元数据关系参考。
- [`open-metadata/OpenMetadata`](https://github.com/open-metadata/OpenMetadata)：元数据采集、数据质量和资产视图参考。
- [`schemacrawler/SchemaCrawler`](https://github.com/schemacrawler/SchemaCrawler)：数据库 metadata 抽取、schema 快照和文档化参考。
- [`prisma/prisma`](https://github.com/prisma/prisma)：schema introspection、开发期数据库工具和本地工作流参考。
- [`promptfoo/promptfoo`](https://github.com/promptfoo/promptfoo)：prompt 输出评测、回归样例和批量评估参考。
- [`langfuse/langfuse`](https://github.com/langfuse/langfuse)：AI trace、prompt 版本和生成任务观测参考。
- [`OpenLineage/OpenLineage`](https://github.com/OpenLineage/OpenLineage)：作业运行、输入输出和血缘事件模型参考，可借鉴执行证据包结构。
- [`OpenAPITools/openapi-generator`](https://github.com/OpenAPITools/openapi-generator)：契约优先、代码生成和版本兼容策略参考。
- [`backstage/backstage`](https://github.com/backstage/backstage)：项目模板、开发者入口和脚手架体验参考。
- [`dbeaver/dbeaver`](https://github.com/dbeaver/dbeaver)：数据库连接配置、metadata 浏览和多方言体验参考。
- [`reviewdog/reviewdog`](https://github.com/reviewdog/reviewdog)：基于 diff 的代码审查评论、诊断聚合和 PR 反馈参考。
- [`pre-commit/pre-commit`](https://github.com/pre-commit/pre-commit)：本地变更钩子、按文件质量门禁和轻量开发工作流参考。
- [Model Context Protocol 规范](https://modelcontextprotocol.io/specification/2025-06-18)：AI 应用接入 resources、prompts、tools 的协议基础。
- [`modelcontextprotocol/servers`](https://github.com/modelcontextprotocol/servers)：MCP server 参考实现集合。
- [`agents.md`](https://agents.md/)：面向 coding agent 的项目指令文件约定。
