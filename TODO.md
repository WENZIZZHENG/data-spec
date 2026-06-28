# DataSpec 待办路线图

本文件记录当前仍需行动的产品与工程待办。优先级按用户可感知价值、核心链路阻断程度和后续开发解锁程度排序。

## 下一步顺序

1. P6-24 项目备份、恢复与迁移包已完成第一版，下一步推进 P6-25 数据库直连只读安全诊断与最小权限指引。
2. P6 后续继续补数据库只读安全诊断、AI 批量任务、标准候选采纳台、离线 Context、元数据适配、Prompt 评测、项目活动时间线、任务式前端导航、本地启动包、fixedSql 策略化、AI 使用画像、标准契约版本、执行证据包、统一前端状态、并发幂等保护、AI 能力清单、前端可复现链接、敏感信息脱敏、验证建议、TODO 到 OpenSpec 交接、业务术语表、自然语言标准候选、AI 引用证据、字段生命周期、变更感知扫描、健康趋势、数据库连接诊断、字段格式约束、命名保留字、反向导入映射、AI 任务重试、质量门禁、示例反例库、AI 会话启动包、AI 任务卡、数据库元数据浏览、大库扫描计划、标准合并向导、前端命令面板、交接证据看板、多项目标准复用包、AI 写入安全策略、规则调试器、元数据增量缓存、CLI/MCP 兼容握手、前端类型化 API Client、标准演练沙箱、MCP/CLI 工具契约验收、业务对象关系图、派生字段规则、fixedSql 文件补丁、标准问答入口、规则模板 diff 包、浏览器级 E2E、真实数据库集成测试、文档状态一致性、可访问性、本地数据清理和前端性能体验。
3. 新增优化建议已补为 P6-87 到 P6-98：数据库迁移计划、业务代码字段引用、MCP prompt/resource、AI 上下文预算、本地 pre-commit/IDE 检查、标准样例生成、多源契约导入、标准证据置信度、自定义规则 SDK、本地语义检索、标准使用热区和 AI 变更迁移说明。
4. P6 收束后再回看哪些能力需要从个人/小团队工具升级为团队协作能力。

## 已完成能力摘要（P0-P4）

P0-P4 的详细背景、方案和验收已归档到 [docs/archive/todo-completed-p0-p4.md](docs/archive/todo-completed-p0-p4.md)。主待办只保留当前仍需行动的 P5/P6 任务。

- P0 AI 可消费主线已完成第一版：AI Context zip、CLI、MCP、个人版字段模型、结构化命名规则和 AI Prompt 生成。
- P1 核心闭环已完成第一版：SQL 校验、OpenAPI 类型契约、COMMENT 解析、前端管理页、字段推荐、结构化修复建议、DDL 生成、检查记录和 fixedSql。
- P2 标准维护与生成能力已完成第一版：内置 standards 初始化、模板 DDL、业务项目 .dataspec/ 约定、数据字典、Excel 导入导出、变更日志和个人工作台。
- P3 自动化与反向导入已完成第一版：SQL 反向导入预览、MySQL DDL 解析、CI/GitHub Action 和 PR 评论式 SQL Review。
- P4 工程化与体验增强已完成第一版：SQL 定位、fixedSql diff、.dataspec/config.json、规则配置表单、OpenAPI 防漂移、Excel dry-run、HTML/ERD、MySQL 规则覆盖、安全基线、演示项目和数据库直连反向导入前端流程。
- 后续真实待办集中在 P6：P5 已完成 dataspec doctor、数据库二次比对、导入来源追踪、SQL 定位范围增强、字段推荐质量增强、核心 fixture/golden 基线、前端高频流程细节打磨和轻量 token 管理；P6-1 已完成标准版本快照第一版，P6-2 已完成字段覆盖率第一版，P6-3 已完成 AI 回放第一版，P6-4 已完成业务仓库初始化第一版，P6-5 已完成字段质量评分第一版，P6-6 已完成轻量字段影响分析第一版，P6-7 已完成 AI Context 按需裁剪第一版，P6-8 已完成字段冲突检测第一版，P6-9 已完成规则误报豁免第一版，P6-10 已完成数据库直连配置预设第一版，P6-11 已完成 MCP/CLI 任务化工作流模板第一版，P6-12 已完成 AI 输出契约稳定性第一版，P6-13 已完成 GitHub inline review 第一版，P6-14 已完成字段分组体验第一版，P6-15 已完成字段批量维护与回退第一版，P6-16 已完成性能基线第一版，P6-17 已完成前端冒烟门禁第一版，P6-18 已完成 AI 可读错误诊断第一版，P6-19 已完成字段标准检索第一版，P6-20 已完成 OpenSpec 归档收口第一版，P6-21 已完成历史快照导出与记录回放第一版，P6-22 已完成 SQL/DDL 多方言兼容矩阵与诊断第一版，P6-23 已完成规则模板库与项目基线套件第一版，P6-24 已完成项目备份恢复迁移包第一版，后续再提升 AI 使用画像、标准契约版本和执行证据包。

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
- 后续增强：多连接预设和无密复用已在 P6-10 完成第一版；重新比对上次来源快捷入口可结合 P6-57 导入映射继续增强，更完整的前端 E2E 门禁已放在 P6-17。
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
- 状态：已完成第一版，已新增规则豁免模型/API、SQL lint 抑制、AI Context 例外导出和前端“模板与规则 / 规则例外”页面。
- 为什么做：个人/小团队真实项目里会有少量历史表、第三方字段或框架约定无法完全满足 DataSpec 规则；如果没有结构化例外，AI 和 CI 会反复报告已知误报。
- 已有基础：已有规则配置、规则启停、SQL lint、CLI/GitHub Review、AI rules.yaml 导出和结构化修复建议。
- 已完成能力：新增 `ds_rule_exemption` 迁移和 `/api/rule-exemptions` API；豁免必须包含 ruleCode、原因和表名/字段名范围，可设置过期时间、禁用或删除；`SqlLintService` 会把命中项标记为 `suppressed=true` 并从 active 统计中排除；`fixedSql` 不再根据 suppressed issue 生成修复；AI Context 的 rules.yaml 和 DATABASE_RULES.md 会导出例外说明；前端支持列表、新建、禁用和删除。
- 落地产物：新增轻量 rule exemption 模型/API/前端入口；支持记录 ruleCode、scope、reason、expiresAt；SQL lint 输出中区分 suppressed issue 和 active issue；AI rules 导出包含项目例外说明。
- 验收标准：已知历史表可被豁免且不会导致 CI 失败；豁免项必须有原因；AI Context 能提醒 agent 这些例外不是推荐新建表继续沿用的标准。已通过 `mvn test`、`pnpm test` 和 `pnpm build` 验证。
- 边界：不做审批流，不做组织级例外审计，不允许无范围的全局静默。

### P6-10：数据库直连配置预设与无密复用
- 状态：已完成第一版，已新增 `ds_database_connection_preset`、项目级预设 API 和反向导入页选择/保存入口。
- 为什么做：数据库直连反向导入会成为高频入口，重复输入 host、port、database、schema 和表筛选很繁琐；但又不能为了方便保存密码或完整连接串。
- 已有基础：反向导入页已有连接测试、表列表、metadata 预览、二次比对、确认导入和来源批次追踪；P5-8 也规划了高频流程细节打磨。
- 已完成能力：新增数据库连接预设模型和前端选择器，只保存 databaseType、host、port、databaseName、schemaName、tableNames、别名和更新时间；服务端 request/response 不包含 username、password、token 或完整 JDBC URL；反向导入页可加载预设表选择，并继续使用原测试连接、加载表、比对和导入流程。
- 验收标准：用户可保存和复用多个非敏感连接预设；预设不会包含 password、token 或完整 JDBC URL；使用预设后仍能完成测试连接、加载表、比对和导入。已通过 `mvn test`、`pnpm test`、`pnpm build` 和 OpenSpec validate 验证。
- 边界：不做连接池，不做后台定时同步，不把密码写入数据库或浏览器持久存储。

### P6-11：MCP/CLI 任务化工作流模板
- 状态：已完成第一版，CLI 已新增 `workflow list/show`，MCP 已暴露 `workflow-recipes` resource，AI Context zip 已包含 `.dataspec/workflows.md`。
- 为什么做：DataSpec 已有 CLI/MCP 单点能力，但 AI agent 更需要“先自检 -> 取标准 -> lint -> 推荐字段 -> 生成/修复 -> 输出结果”的稳定工作流，减少每次靠 prompt 现场拼步骤。
- 已有基础：已有 `dataspec doctor`、`lint-files`、`review-pr`、字段推荐、DDL 生成、AI Prompt、AI Context zip 和 MCP resources/prompts/tools。
- 已完成能力：新增共享 workflow catalog，覆盖 `create-table`、`review-pr-sql`、`reverse-import-standards` 和 `export-min-context`；每个 recipe 定义输入、前置检查、步骤命令、产物、失败恢复和下一步建议；CLI 支持 text/json 输出和未知 id 诊断；MCP resource 无需访问后端即可读取 recipes；AI Context manifest 和 README 引用 workflows 文件。
- 验收标准：AI agent 按 recipe 能稳定完成建表前取标准、PR SQL review、数据库反向导入补标准和最小 Context 导出；失败时能读取下一步诊断建议。已通过 CLI/MCP 单测、后端测试、OpenSpec validate 和 diff 检查验证。
- 边界：不内置外部 LLM，不自动修改业务仓库，第一版以命令/文档/JSON 契约为主。

### P6-12：AI 输出契约稳定性与兼容测试
- 状态：已完成第一版，已新增 [docs/ai-contracts.md](docs/ai-contracts.md) 和后端/CLI/MCP contract fixture 测试。
- 为什么做：DataSpec 优先给 AI 使用，最怕的是字段目录、规则、lint 结果、推荐结果或 MCP 输出字段悄悄漂移，导致 agent 读错上下文或自动化脚本失效。
- 已有基础：已有 OpenAPI 契约检查、AI Context JSON Schema、CLI/MCP JSON 输出、字段推荐和 SQL lint 结构化结果。
- 已完成能力：第一版稳定字段清单覆盖 AI Context manifest/field-catalog/rules/workflows、`LintResult`/`LintIssue`/`fixedSql`、字段推荐、DDL 预览、CLI JSON 和 MCP resources/tools；后端测试锁定稳定字段路径、类型和核心枚举，Node 测试锁定 CLI/MCP JSON 结构并验证兼容新增字段不会破坏测试。
- 验收标准：改动 AI 可消费字段时，测试能明确显示契约变化；兼容字段新增不会破坏旧 fixtures；已通过目标后端契约测试、CLI/MCP 单测、OpenSpec validate 和 diff 检查验证。
- 边界：不冻结所有内部 DTO，不阻止向后兼容新增字段，不引入外部契约服务。

### P6-13：GitHub inline review 实战接入
- 状态：已完成第一版，CLI `review-pr` 已支持 PR diff inline comment、重复 marker 跳过、fallback 汇总评论和 JSON 摘要输出。
- 为什么做：P5-5 已提供 SQL issue 文件内行列范围，但当前 `review-pr` 仍发布单条汇总评论；真正落到 PR diff inline 后，开发者和 AI 才能在具体 SQL 行旁边处理问题。
- 已有基础：已有 CLI `review-pr`、GitHub token 参数、PR 汇总评论 marker、文件级 source range 和“不直接发 inline”的边界说明。
- 已完成能力：新增 PR files patch 映射；当 issue 行号落在本次 diff hunk 内时发布 inline comment；无法映射时保留在汇总评论并统计 fallback reason；已有 `dataspec-inline-review` marker 的相同行规则会跳过；GitHub 权限错误会提示 token、repo、pr 或权限诊断。
- 验收标准：PR 中新增或修改 SQL 文件能收到对应行 inline comment；重复运行不会刷屏；不在 diff 内的问题进入汇总区；失败时 CLI 给出 token、权限或 diff 映射诊断；`--format json` 可输出 inline/fallback/skipped 统计。
- 边界：不做代码所有者审批，不改 GitHub Actions 示例以外的业务仓库文件，不要求所有历史 SQL 问题都能 inline。

### P6-14：项目内字段分组与数据域体验增强
- 状态：已完成第一版，字段库已支持数据域/category/tag/未分组筛选、批量归组和 AI Context 分组摘要。
- 为什么做：字段标准变多后，仅靠字段列表搜索不够，个人/小团队需要按项目内业务域、模块和标签组织数标，AI 也需要清楚“当前任务属于哪个字段范围”。
- 已有基础：字段已有 category、tags、数据域关系、字段库筛选、来源详情查看和 AI Context 导出。
- 已完成能力：后端提供字段分组摘要和批量归组接口；前端字段库左侧展示全部、数据域、分类、标签和未分组入口，表格支持多选后批量设置或清空 domain/category/tags；AI Context 的 `contextScope.groupSummary` 输出返回字段的分组计数和未分组 warning。
- 验收标准：用户能在一个项目内按业务分组管理标准字段；未分组字段可快速定位并批量补齐；AI 导出可携带分组摘要并说明裁剪条件。
- 边界：不做跨项目组织级目录，不引入审批/发布流，不把分组升级成复杂权限模型。

### P6-15：字段库批量维护与可撤销变更
- 状态：已完成第一版，字段库已支持批量维护常用属性、后端预览和基于变更日志的单条回退。
- 为什么做：反向导入、Excel 导入和质量检查会一次性暴露大量待修字段；如果只能逐条编辑，个人使用也会变慢，而且误操作后缺少快速恢复手段。
- 已有基础：已有字段 CRUD、Excel dry-run、变更日志 before/after、来源追踪和字段质量/冲突检测待办。
- 已完成能力：新增批量维护预览和提交接口，支持显式设置或清空 status/category/tags/sensitive/codeSetId/aliases；批量提交逐字段写入 update 变更日志；字段库可查看单字段最近变更并对带 beforeJson 的 update/undo 日志执行确认回退，回退会写入新的 undo 日志。
- 验收标准：用户可选择多条字段批量维护常用属性；批量操作写入变更日志；误改单个字段后可从日志恢复上一版关键属性。
- 边界：不做复杂事务审批，不自动合并冲突字段，不跨项目批量修改。

### P6-16：大字段库性能与可观测性基线
- 状态：已完成第一版，已新增合成性能基线测试和核心慢操作 warning。
- 为什么做：当标准字段、检查记录、导入批次和 AI Context 变多后，前端列表、字段推荐、Context 导出和 CLI/MCP 响应延迟会直接影响 AI 和人的日常使用。
- 已有基础：已有分页接口、字段推荐、AI Context 导出、工作台统计、CLI/MCP 和 Spring Boot Actuator 基础依赖可扩展空间。
- 已完成能力：新增 `PerformanceProbe`，字段分页/全量读取/分组/推荐、AI Context field catalog/package、SQL 检查记录分页和反向导入 compare 超过阈值时输出 operation、durationMs、thresholdMs 和 hint；新增 `PerformanceBaselineTest`，构造 5000 个标准字段和 2000 个 compare 列，输出字段分组、字段推荐、AI Context 字段目录和反向导入 compare 的本地 metric。
- 验收标准：大字段库场景下核心接口耗时有可重复测量结果；明显慢点有日志或诊断提示；前端列表不因大数据量明显卡顿；CLI/MCP 超时信息可读。
- 边界：不做分布式部署，不引入缓存集群，不为个人版过早上复杂监控平台。

### P6-17：前端关键流程 E2E 冒烟与回归门禁
- 状态：已完成第一版，已新增源码级前端关键流程冒烟测试并接入 `pnpm test`。
- 为什么做：前端页面已覆盖字段库、规则、SQL 校验、DDL、反向导入、AI Context 和工作台，但目前主要依赖构建和局部单测，核心流程仍容易在导航、项目切换或接口类型变化时悄悄回归。
- 已有基础：前端已有 Vue 3、Element Plus、Pinia、Axios、Monaco、`pnpm build` 和部分测试入口；后端已有 demo project 与核心接口。
- 已完成能力：新增 `dataspec-web/tests/frontendSmoke.test.ts`，覆盖关键路由与菜单、顶部项目选择、SQL 校验 fixedSql/检查记录、数据库反向导入与 compare、字段库筛选/分组/批量维护/回退、DDL 生成、AI Context、覆盖率报告、AI 回放的核心页面/API 耦合，以及关键按钮和空状态文案。
- 验收标准：本地 `pnpm test` 能跑完关键流程冒烟；破坏项目选择、接口字段、核心按钮或路由跳转时测试能失败；README 已说明该门禁不需要浏览器、后端服务或截图依赖。已通过 `pnpm test` 验证。
- 边界：第一版是源码级/组件集成式冒烟，不追求全页面像素级截图，不覆盖所有表单排列组合，不引入 Playwright 等重量级测试平台。

### P6-18：AI 可读错误码与下一步建议标准化
- 状态：已完成第一版，API/CLI/MCP 已输出结构化错误诊断。
- 为什么做：DataSpec 优先让 AI 使用，接口、CLI 和 MCP 失败时不能只给人类文本；AI 需要稳定的错误码、原因、可重试性和下一步动作，才能自动恢复或给出准确建议。
- 已有基础：已有 `dataspec doctor`、统一 API wrapper、CLI/MCP JSON 输出、OpenAPI 契约和安全基线。
- 已完成能力：后端失败响应保留 `code/message/data`，并新增可选 `error.code/category/retryable/suggestedAction/docsRef`；CLI API 失败会输出 `DataSpecError: {...}`；MCP JSON-RPC error 会在 `error.data.dataspecError` 中携带同类诊断；前端请求层会把诊断挂到 rejected `Error.dataspecError`。
- 验收标准：AI agent 可根据错误码判断是否需要运行 doctor、切换项目、重新生成 schema、补 token 或提示用户；错误契约已有后端、CLI 和 MCP 单测覆盖；README 已记录稳定字段。
- 边界：第一版使用集中分类器从现有 code/message 生成诊断，不逐个重写所有业务异常；不做复杂国际化，不改变 HTTP 状态语义，不把内部异常堆栈暴露给前端或 AI。

### P6-19：字段标准检索 API 与语义查询增强
- 状态：已完成第一版，已新增字段标准检索 API、CLI/MCP 入口、前端字段库复用和命中原因展示。
- 为什么做：字段推荐解决“给一个字段名推荐标准字段”，但 AI 和人还会问“支付金额相关字段有哪些”“订单域可用字段有哪些”“这个表建表前要参考哪些字段”；需要更像目录检索的入口。
- 已有基础：已有字段列表、字段推荐、category/tags/alias、AI Context、MCP resources 和按需裁剪待办。
- 已完成能力：新增 `/api/fields/search`，返回 `summary/items/nextActions`，结果项包含字段、分数、`matchReasons`、推荐使用范围和字段级下一步建议；支持关键词、中文描述、category、tag、status、sensitive 和 sourceBatchId 过滤，省略 query 且无过滤时返回校验错误。
- 落地产物：CLI 新增 `search-fields`，MCP 新增 `search_fields` tool，前端字段库在关键词或 category/tag 分组时复用检索 API 并显示命中原因；清空搜索条件后保留原字段列表体验。
- 验收标准：AI agent 能在建表或修 SQL 前检索最相关字段集合；搜索结果可被字段库页面复用；同义词、别名、拼音缩写、过滤参数和失败诊断已有后端与 CLI/MCP 测试覆盖，前端 smoke 覆盖字段库检索耦合。
- 边界：不引入向量数据库，不调用外部 LLM，不替代现有字段推荐接口。

### P6-20：OpenSpec 归档与主规格同步收口
- 状态：已完成第一版，已将已完成 change 归档到 `openspec/changes/archive/`，并把主规格同步到 `openspec/specs/`。
- 为什么做：项目已经持续使用 OpenSpec，但完成的 change 较多，如果不定期归档并同步主规格，后续 AI 会在 active changes、README、TODO 和实际能力之间读到过期上下文。
- 已有基础：已有多个 OpenSpec change、README 当前功能概览、TODO 完成项归档和 OpenSpec validate 流程。
- 已完成能力：归档 61 个已完成 OpenSpec change；主规格保留 57 个可验证 spec；补齐 `field-model`、`sql-lint-rules` 主规格，并把个人字段元数据和结构化命名规则出口同步到 `ai-context-package`。
- 落地产物：`openspec/changes/archive/2026-06-27-*` 保存历史 change；`docs/archive/openspec-p6-20-sync-2026-06-27.md` 记录归档范围、验证命令和遗留边界；README/TODO 已指向主规格与归档目录。
- 验收标准：已完成 change 不再干扰“下一步待办”判断；`openspec validate --all` 通过；归档记录能追溯关键验证命令、commit 和遗留边界。
- 边界：不在本任务中实现新产品能力，不重写历史方案，只处理已完成且证据清晰的变更。

### P6-21：按历史标准快照导出与任务回放
- 状态：已完成第一版，已支持按历史标准快照导出 AI Context，并在 SQL 检查记录详情展示回放证据。
- 为什么做：P6-1 已能记录标准快照版本和 hash，但 AI 真正排查旧 SQL、旧 DDL 或旧检查记录时，需要按当时的标准快照重新导出上下文，而不是总使用最新标准。
- 已有基础：已有 `ds_standard_snapshot`、AI Context 标准元数据、SQL 检查记录快照引用、DDL 生成结果快照引用和 AI 回放待办。
- 已完成能力：新增快照 payload 只读加载和 hash 校验；AI Context `field-catalog.json`、`rules.yaml`、`DATABASE_RULES.md` 预览和 zip 下载支持 `snapshotId` / `snapshotVersion`；CLI `export-context` 支持 `--snapshot-id` / `--snapshot-version`；SQL 检查记录详情返回 recordedStandard、currentStandard、status、summary 和 nextActions，前端可查看并复制历史 Context 导出命令；AI Context 页面可选择当前标准或历史快照导出。
- 验收标准：给定一条历史 SQL 检查记录，用户或 AI 能按记录中的 snapshotId 导出当时上下文并复现主要诊断；无快照的历史记录仍走 `unversioned` 兼容路径。已通过后端目标测试、CLI 测试、`pnpm test` 和 `pnpm build` 验证。
- 边界：不做复杂审批发布，不自动回滚当前标准，不要求所有旧记录都能补齐历史快照；第一版只做轻量 hash/计数/命令级回放，不做字段级历史 diff。

### P6-22：SQL/DDL 多方言兼容矩阵与诊断
- 状态：已完成第一版，已新增 PostgreSQL/MySQL 方言诊断模型、运行时诊断输出和前端/CLI 展示。
- 为什么做：项目已支持 PostgreSQL/MySQL 的部分解析、校验和反向导入，但 AI 或 CLI 在不同业务仓库使用时，需要明确哪些方言能力可靠，哪些是降级或不支持。
- 已有基础：已有 JSqlParser、PostgreSQL/MySQL 反向导入、MySQL 规则覆盖、fixture/golden 基线和 SQL lint/fixedSql 流程。
- 已完成能力：新增 `DialectDiagnostic`、能力枚举、支持级别和 `SqlDialectCompatibilityService`；`/api/lint`、fixedSql、DDL 生成、SQL 反向导入和数据库直连反向导入会返回 `dialectDiagnostics`；前端 SQL 校验、DDL 生成和反向导入页展示方言摘要、降级原因和 nextAction；CLI `lint --format text` 会输出方言摘要，JSON 输出保留完整结构。
- 验收标准：新增或修改 SQL 方言行为时有 fixture 能防回归；不支持的语法能给出可读且 AI 可解析的诊断；前端和 CLI 都能显示当前方言及降级原因。已通过目标后端测试、`pnpm test`、`pnpm build` 和 CLI 测试验证。
- 边界：第一版不追求 Oracle/SQL Server 全量支持，不手写完整 SQL parser，不把未验证方言标成已支持。

### P6-23：规则模板库与项目基线套件
- 状态：已完成第一版，已新增内置规则基线、项目应用、导入导出、AI Context baseline 元数据和前端规则页入口。
- 为什么做：个人/小团队会反复配置相似的字段命名、注释、敏感字段、金额/时间/状态字段规则；AI 使用时也需要知道项目采用的是哪套规则基线。
- 已有基础：已有规则配置、规则启停、AI rules.yaml、模板 DDL、字段推荐和演示项目初始化。
- 已完成能力：新增 `ds_rule_baseline` 和 `/api/rule-baselines`；内置 `personal_default`、`strict`、`legacy_compatible` 三套规则基线；项目创建和演示项目会应用个人默认基线；规则配置页可查看当前基线、应用内置基线、导出 JSON、导入 JSON，默认不覆盖已有同编码规则；AI Context 的 `rules.yaml` 会输出 `baseline` 元数据。
- 验收标准：新项目可一键套用个人默认规则基线；规则模板变更有版本和说明；AI 读取 rules.yaml 能知道规则来自哪套基线。已通过 `mvn test`（235 tests）、`pnpm test`（56 tests）、`pnpm build`、CLI/MCP Node 测试（62 tests）、`npx.cmd openspec validate add-rule-template-baseline-suites` 和 `git diff --check` 验证。
- 边界：不做组织级发布审批，不强制所有项目统一规则，不自动覆盖用户已调整的规则。

### P6-24：项目备份、恢复与迁移包
- 状态：已完成第一版，已新增项目备份导出、恢复 dry-run、确认恢复、恢复摘要记录和前端“数据管理 / 项目备份”入口。
- 为什么做：项目优先个人/小团队使用，本地库、演示库或轻量部署都可能迁移机器；标准字段、规则、模板、快照和来源记录需要一个可恢复的交付包。
- 已有基础：已有数据字典导出、Excel 导入导出、AI Context zip、标准快照、规则配置、字段来源和项目模型。
- 已完成能力：备份 JSON 包包含项目元数据、数据域、字段、枚举、规则、规则基线、模板、标准快照、反向导入来源摘要和必要变更日志摘要；导出和恢复前均执行敏感信息剔除/扫描；恢复支持新项目或当前项目 dry-run，展示 CREATE/SKIP/UPDATE/CONFLICT/BLOCKED 明细，确认恢复后保存摘要记录且不保存完整备份包。
- 落地产物：新增 `projectbackup` 后端模块、`ds_project_restore_record` 迁移、`/api/project-backups` API、前端项目备份页、OpenAPI/TS 类型和前端 smoke 覆盖。
- 验收标准：一个项目可导出为不含 token/password/source rows 的迁移包；在新环境 dry-run 后可恢复主要标准资产；恢复过程保留审计摘要并避免误覆盖。已通过前端 `pnpm test` 和 `pnpm build`，完整后端/CLI/OpenSpec 验证随本任务提交记录保留。
- 边界：不备份源数据库数据行，不保存数据库密码或 API token 明文，不替代数据库级物理备份。

### P6-25：数据库直连只读安全诊断与最小权限指引
- 状态：已完成第一版，已增强数据库直连测试结果并在反向导入、覆盖率报告页展示只读安全诊断。
- 为什么做：数据库直连反向导入和二次比对是高价值能力，但个人使用也容易拿高权限账号直连；DataSpec 应主动提示只读账号、权限范围和敏感信息处理边界。
- 已有基础：已有数据库连接测试、metadata 读取、反向导入、二次比对、非敏感连接记忆和不保存密码的约束。
- 已完成能力：连接测试成功时返回数据库类型、当前用户、只读/写权限推断、可访问 schema/table 数、风险等级、warnings、推荐动作和 PostgreSQL/MySQL 最小权限 SQL；连接失败消息会脱敏 password、Bearer token 和完整 JDBC URL；诊断查询失败不会让可用连接失败。
- 落地产物：新增 `DatabaseConnectionSecurityDiagnostic` 响应模型、后端只读 metadata/方言查询诊断、前端 `databaseSecurityDiagnostic` 展示工具、反向导入页和覆盖率报告页诊断卡片，以及后端/前端测试覆盖。
- 验收标准：使用高权限账号连接时能看到风险提示；只读账号通过时显示“适合反向导入/比对”；日志、前端和记录中不泄漏 password 或完整连接串。已通过目标后端测试、前端 `pnpm test` 和 `pnpm build`，完整验证随本任务提交记录保留。
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

### P6-54：数据库连接健康探测与方言能力画像
- 状态：待办。
- 为什么做：数据库直连反向导入和二次比对已经成为核心入口，AI 在调用前需要知道连接是否可用、权限是否只读、当前库支持哪些 schema/comment/index 元数据能力，而不是只收到一个泛化失败。
- 已有基础：已有 PostgreSQL/MySQL 直连、表列表、metadata 预览、compare、只读安全诊断待办和数据库元数据适配层待办。
- 缺口：连接失败、权限不足、schema 不存在、方言不兼容等错误缺少结构化诊断；也没有把 dialect capability 以机器可读方式返回给前端、CLI 或 AI。
- 落地产物：新增连接诊断接口或扩展现有 metadata preview；输出 connectionStatus、latencyMs、databaseProduct、version、schemaSupport、commentSupport、readonlyCheck、requiredPrivileges、warnings 和 nextActions；前端连接页展示诊断结果。
- 验收标准：有效连接、错误密码、权限不足、schema 不存在、网络不可达等场景都有明确诊断；不持久化 password/token；AI 能据此判断是否可以继续反向导入或二次比对。
- 边界：不做长期监控，不保存敏感连接凭据，不替代数据库安全审计。

### P6-55：字段值格式与校验样例库
- 状态：待办。
- 为什么做：字段名和数据类型不足以让 AI 稳定生成正确 SQL，像金额单位、手机号格式、日期时区、JSON 结构、状态码取值都需要结构化表达。
- 已有基础：字段已有 dataType、exampleValue、sensitive、codeSetId、质量评分和 AI Context 导出。
- 缺口：缺少 field format/pattern 层；示例值只是自由文本，无法区分单位、正例、反例、正则、精度、时区和空值策略。
- 落地产物：新增轻量字段格式约束模型或字段扩展；支持 formatType、pattern、unit、precision、timezone、validExamples、invalidExamples 和 notes；AI Context、DDL 生成、字段质量评分和 lint 建议可读取。
- 验收标准：金额、手机号、邮箱、时间戳、JSON、状态码等字段能导出稳定格式说明；AI 生成 SQL/DDL 时能看到单位和格式约束；质量评分能提示关键字段缺少格式样例。
- 边界：不扫描真实业务数据行，不强制所有字段配置正则，不引入完整数据质量执行引擎。

### P6-56：标准字段别名冲突与命名保留字检测
- 状态：待办。
- 为什么做：字段冲突检测已能发现标准库内部重复，但 AI 真实建表还会遇到 SQL 方言保留字、跨字段别名歧义和大小写/引用规则差异，这些会导致生成结果不可执行或含义不清。
- 已有基础：已有字段冲突检测、字段推荐、SQL lint、多方言兼容矩阵待办和字段命名规则。
- 缺口：缺少按 PostgreSQL/MySQL 等方言维护的保留字/危险词清单；字段 alias 与 canonical name 的歧义也没有进入冲突报告。
- 落地产物：扩展冲突检测或新增命名风险报告；检测 reserved keyword、ambiguous alias、dialect unsafe name、case sensitive collision，并输出替代命名建议。
- 验收标准：`order`、`user`、`type` 等高风险命名能按方言给出提醒；同一个别名指向多个标准字段会提示 AI 不应直接采用；报告可在前端和 AI Context 中读取。
- 边界：不自动重命名已有字段，不追求覆盖所有数据库方言，不阻止用户保留历史兼容字段。

### P6-57：反向导入字段映射策略与确认理由
- 状态：待办。
- 为什么做：数据库直连反向导入不仅要把新字段写进标准库，还应解释每个真实字段为什么匹配到某个标准字段、为什么成为候选、为什么被忽略，方便 AI 和用户复盘。
- 已有基础：已有反向导入预览、候选确认导入、字段来源与批次追踪、覆盖率报告和字段推荐原因。
- 缺口：当前导入确认偏结果写入，缺少结构化 mapping decision；被忽略字段、别名匹配字段和新候选字段的理由不能稳定回放。
- 落地产物：新增导入映射决策结构；为每个表字段记录 decisionType、matchedFieldId、matchReason、confidence、ignoreReason、confirmReason 和 batchId；前端预览页支持编辑确认理由。
- 验收标准：导入批次详情能解释 `mobile_no` 为什么映射到某个标准手机号字段；忽略字段不会反复出现在同一批次的待处理列表；AI 可读取映射历史避免重复建议。
- 边界：不做审批流，不强制每个字段都人工填写长说明，不自动覆盖已有标准字段定义。

### P6-58：AI 任务失败重试与断点续跑
- 状态：待办。
- 为什么做：AI 批量 lint、Context 导出、覆盖率扫描、反向导入比对和证据包生成都可能耗时或失败；缺少重试和断点续跑会让 agent 只能从头再跑，容易重复写入或浪费上下文。
- 已有基础：已有 AI 作业回放、批量任务待办、并发幂等待办、项目活动时间线待办和执行证据包待办。
- 缺口：任务状态、失败原因、可重试性、输入 hash、已完成步骤和恢复命令尚未形成统一模型。
- 落地产物：新增轻量 task run 模型或扩展 AI job；记录 stepStatus、inputHash、retryable、failedStep、resumeCommand、partialArtifacts 和 expiresAt；CLI/MCP/前端可查询最近失败任务并选择重试。
- 验收标准：一次批量操作中途失败后能看到失败步骤和可恢复命令；重复重试不会产生重复记录；AI 能根据 retryable 字段判断继续还是提示用户。
- 边界：不引入外部队列，不做分布式调度，不把所有同步 CRUD 都改造成异步任务。

### P6-59：标准质量门禁与阈值策略
- 状态：待办。
- 为什么做：字段质量、覆盖率和 lint 结果已经能被查看，但 CI/AI 自动化还缺少“低于什么阈值就阻断”的项目级策略，否则质量退化只能事后人工发现。
- 已有基础：已有字段质量评分、覆盖率报告、SQL lint、CI/GitHub Review、规则配置和验证建议待办。
- 缺口：缺少 project quality gate 配置；覆盖率下降、低质量字段新增、ERROR 级规则、敏感字段未标注等信号还不能组合成统一门禁结果。
- 落地产物：新增质量门禁配置和评估接口/CLI；支持 minCoverage、minAverageFieldScore、maxErrorIssues、maxNewUnmanagedFields、requiredSensitiveMarking 等阈值，并输出 pass/fail、failedChecks、nextActions。
- 验收标准：业务仓库 CI 可基于项目阈值失败或通过；前端能显示当前门禁状态；AI 能读取失败项并按优先级修复标准或 SQL。
- 边界：不做企业审批流，不默认阻断个人本地保存，不把门禁阈值硬编码到规则实现里。

### P6-60：标准字段使用示例与反例库
- 状态：待办。
- 为什么做：AI 更擅长从具体例子学习，只有字段说明和规则文本时仍可能误用字段；需要为关键字段、规则和表模板提供可裁剪的 good/bad examples。
- 已有基础：字段 exampleValue、SQL good/bad fixture、规则说明、Prompt 模板、AI Context 和 golden fixtures。
- 缺口：示例分散在测试或自由文本中，没有按字段/规则/模板结构化维护；也缺少明确反例告诉 AI 哪些历史写法不要模仿。
- 落地产物：新增示例/反例维护入口或配置文件；支持 exampleType、scope、input、expectedOutput、antiPattern、reason、tags 和 priority；AI Context 按 scope 裁剪导出最小示例集。
- 验收标准：AI Context 中能携带少量高价值字段使用例子和反例；DDL/Prompt 生成可以引用示例；新增示例有 fixture 或快照测试防止格式漂移。
- 边界：不采样真实业务数据，不导出敏感值，不把示例库扩成完整教程或大文档。

### P6-61：AI 会话启动包与当前上下文握手
- 状态：待办。
- 为什么做：AI agent 每次进入业务仓库时都需要重新判断 DataSpec 服务、项目、token、标准版本、可用工具和下一步命令；这些信息分散在 `doctor`、README、AI Context 和 MCP 中，容易漏读。
- 已有基础：已有 `dataspec init`、`doctor`、AI Context、MCP resources/tools、标准快照和 API Token。
- 缺口：缺少一份机器可读的 session bootstrap，把当前项目状态、能力清单、推荐入口、风险提示和最小下一步命令一次性给 AI。
- 落地产物：新增 CLI/MCP/API 任一轻量启动包入口，输出 projectId、server、authMode、specVersion、availableCapabilities、recommendedCommands、knownRisks 和 docsRefs；业务仓库 `.dataspec/README.md` 可引用该入口。
- 验收标准：AI 在新会话中一条命令即可知道当前能否 lint、导出 Context、反向导入或生成 DDL；服务未启动、token 无效、未选择项目等场景都有结构化 nextActions。
- 边界：不调用外部 LLM，不自动执行写操作，不把明文 token 或数据库密码写入启动包。

### P6-62：AI 任务卡与单步可恢复执行协议
- 状态：待办。
- 为什么做：P6-11 的工作流模板解决“应该怎么做”，但 AI 真正执行时还需要知道当前任务做到哪一步、缺哪些输入、失败后从哪一步恢复，否则长任务容易重复执行或越界。
- 已有基础：已有 AI 回放、执行证据包待办、AI 任务重试待办、CLI/MCP 工作流模板和 OpenSpec tasks 习惯。
- 缺口：缺少统一 task card 契约来表达 goal、inputs、currentStep、allowedActions、artifacts、resumeCommand、validationCommands 和 stopConditions。
- 落地产物：定义轻量 AI task card JSON/Markdown 模型；CLI/MCP/前端可展示任务卡，支持从工作流模板生成初始任务卡，并在关键步骤后更新状态。
- 验收标准：AI 执行建表、反向导入、PR SQL Review 或导出最小 Context 时，能用任务卡描述当前进度、下一条安全命令和恢复方式；失败重试不会重复写入。
- 边界：不实现企业审批流，不引入外部队列，不把所有同步接口强制改成异步任务。

### P6-63：数据库直连元数据浏览器与候选选择页
- 状态：待办。
- 为什么做：数据库直连反向导入当前以“连接 -> 选表 -> 预览 -> 导入”为主，用户和 AI 在导入前缺少一个只读浏览真实 schema/table/column/comment/index 的轻量视图。
- 已有基础：已有 PostgreSQL/MySQL 直连、表列表、metadata 预览、覆盖率报告、连接预设、字段来源追踪和反向导入前端页。
- 缺口：直连 metadata 主要服务导入流程，不能像数据库文档浏览器一样搜索表、查看列详情、对比标准命中和临时标记候选。
- 落地产物：新增前端元数据浏览入口或反向导入页子视图；支持按 schema/table/column/comment 搜索，展示字段标准匹配、缺注释、类型差异和可加入导入候选的勾选状态。
- 验收标准：连接数据库后无需导入即可浏览元数据；AI 可读取选中表的结构摘要并继续生成候选导入或覆盖率报告；全流程只读且不采样业务数据行。
- 边界：不做通用 SQL 客户端，不执行任意查询，不保存数据库密码。

### P6-64：大库扫描计划、分页预览与取消恢复
- 状态：待办。
- 为什么做：真实数据库可能有大量 schema、表和字段；一次性拉取所有 metadata 会慢、容易超时，也不利于 AI 在上下文有限时逐步处理。
- 已有基础：已有数据库直连表列表、metadata 预览、覆盖率报告、性能基线待办、数据库元数据适配层待办和 AI 任务重试待办。
- 缺口：缺少 scan plan、分页 cursor、进度摘要、取消和恢复机制；前端也没有把大库拆成可分批处理的选择体验。
- 落地产物：扩展直连 metadata 查询为可分页扫描；输出 scanId、estimatedTableCount、cursor、progress、partialSummary、resumeCommand 和 cancel 状态；前端按批次展示和筛选。
- 验收标准：上百张表的数据库可分批加载并生成部分预览；中途取消不会写入标准库；AI 能根据 cursor 继续下一批或停止。
- 边界：不引入分布式调度，不长期保存数据库连接凭据，不默认后台全库扫描。

### P6-65：标准字段智能合并向导
- 状态：待办。
- 为什么做：字段冲突检测能发现重复或疑似重复，但用户仍要手动判断哪个字段保留、别名如何合并、历史来源和模板引用如何处理；这一步对 AI 也容易出错。
- 已有基础：已有字段冲突报告、字段影响分析、字段来源批次、标准快照、变更日志和字段生命周期待办。
- 缺口：缺少可审阅的 merge preview，把保留字段、被合并字段、别名迁移、来源迁移、替代关系和风险提示放在一个确认流程中。
- 落地产物：新增标准字段合并预览与确认接口/页面；生成合并 diff、影响对象、推荐保留字段、迁移 aliases/tags/examples/source 的策略和可回滚的变更日志。
- 验收标准：用户能把两个重复字段合并为一个标准字段，并保留必要别名、来源和影响记录；AI Context 不再重复导出被合并字段；合并前可预览影响。
- 边界：不自动合并冲突字段，不删除历史审计记录，不跨项目强制统一字段定义。

### P6-66：前端命令面板与最近操作续跑
- 状态：待办。
- 为什么做：功能越来越多后，用户和 AI 驱动的人工操作都需要快速跳到“SQL 校验、反向导入、字段质量、覆盖率、AI Context、Token”等入口，并能续跑最近一次上下文。
- 已有基础：已有前端路由、个人工作台、项目活动时间线待办、URL 可复现链接待办、任务式导航待办和多页记录详情。
- 缺口：缺少全局 command palette、最近任务、最近记录和上下文恢复入口；现在需要用户记住各页面位置。
- 落地产物：新增全局命令面板，支持搜索页面、项目内资源、最近 SQL 检查、反向导入批次、AI 作业和常用动作；可带当前项目和必要 query 跳转。
- 验收标准：键盘或顶部入口可快速打开命令面板；选择最近 SQL 检查、导入批次或字段质量报告后能恢复对应页面状态；无项目时给出创建或选择项目建议。
- 边界：不重做整体信息架构，不引入复杂桌面化布局，不绕过页面原有权限和项目边界。

### P6-67：AI 交接证据看板
- 状态：待办。
- 为什么做：AI 完成一个任务后，用户需要快速看见它改了什么、用了哪些 DataSpec 标准、跑了哪些验证、还有哪些风险；这些证据目前分散在回放、检查记录、TODO、git log 和终端输出中。
- 已有基础：已有 AI 回放、SQL 检查记录、执行证据包待办、标准快照、验证建议待办和项目活动时间线待办。
- 缺口：缺少按任务聚合的交接视图，无法把输入、输出、引用标准、校验命令、结果摘要、commit 和剩余风险串起来。
- 落地产物：新增任务交接看板或证据包页面；展示 taskId、目标、关联 AI 作业、lint/DDL/Context 产物、验证结果、标准版本、相关 commit 和 nextActions。
- 验收标准：用户能打开一个任务交接记录，判断 AI 交付是否可继续使用；失败或未验证项会明确标红；记录不包含敏感连接凭据。
- 边界：不做团队审批流，不替代 GitHub PR，不采集业务数据行或第三方 LLM 对话全文。

### P6-68：多项目标准复用包与轻量继承
- 状态：待办。
- 为什么做：个人或小团队往往会有多个业务项目共享一套用户、订单、支付等通用字段标准；完全复制会造成漂移，过早做组织级治理又太重。
- 已有基础：已有项目模型、演示项目、领域 Starter Kit 待办、标准快照、导入导出、字段分组和项目模板能力。
- 缺口：缺少轻量的 shared standard pack，用于在多个项目间复用基础字段、枚举、规则和模板，同时允许项目局部覆盖。
- 落地产物：新增标准复用包导出/导入或轻量继承模型；支持 basePackVersion、includedFields、rules、templates、projectOverrides 和 driftReport。
- 验收标准：新项目可从共享包初始化通用标准；共享包升级后能看到本项目覆盖项和漂移项；AI Context 能说明字段来自共享包还是项目覆盖。
- 边界：不做企业组织层级、审批发布、跨团队权限或复杂包仓库；第一版只服务个人/小团队复用。

### P6-69：AI 写入安全策略与 dry-run 协议
- 状态：待办。
- 为什么做：DataSpec 越来越多入口会被 AI 通过 CLI/MCP 调用，个人工具也需要避免“AI 一次误写很多标准字段、规则或导入记录”；安全策略应偏产品内建约束，而不是企业审批流。
- 已有基础：已有 API Token、`doctor`、workflow recipes、AI 能力清单待办、AI 任务卡待办、幂等锁待办和敏感信息脱敏待办。
- 缺口：CLI/MCP/API 对写操作缺少统一 machine-readable safety metadata；AI 不容易判断哪些操作只读、哪些需要 dry-run、哪些必须带 idempotency key 或可撤销证据。
- 落地产物：为高风险写操作定义 safety metadata，包含 `readOnly`、`writesProject`、`requiresDryRun`、`supportsUndo`、`requiresIdempotencyKey`、`sensitiveInputs` 和 `nextActions`；CLI/MCP 输出并校验该协议，前端批量写入前展示 dry-run 摘要。
- 验收标准：AI 能先枚举安全等级再执行写操作；批量导入、批量维护、标准合并等写入默认可 dry-run；缺少必要幂等参数时返回结构化错误；日志不输出密码/token。
- 边界：不做组织审批、多人审核或复杂 RBAC；不阻塞单条低风险个人 CRUD。

### P6-70：SQL 规则调试器与可解释匹配面板
- 状态：待办。
- 为什么做：规则越来越多后，用户和 AI 需要知道某条 SQL 为什么被某个规则命中、命中的 AST/文本范围是什么、参数如何影响结果，否则只能靠猜。
- 已有基础：已有 SQL lint、source range、fixedSql、规则配置、golden fixtures、AI 可读错误码待办和多方言兼容矩阵待办。
- 缺口：当前 lint 结果面向最终问题展示，缺少 rule trace、匹配上下文、参数快照和“为什么没命中”的调试视图。
- 落地产物：新增规则调试 API 或 CLI 模式；返回 ruleCode、severity、enabled、paramsSnapshot、matchTrace、sourceRange、fixStrategy、suppressionStatus 和 debugNotes；前端 SQL 校验页提供调试面板。
- 验收标准：输入一段 SQL 后能查看每条规则的启用状态和命中理由；规则误报可直接跳转到豁免建议或规则参数；调试输出可被 AI 用来修规则或补标准。
- 边界：不暴露完整复杂 AST 编辑器，不要求所有规则第一版都有深度 trace；不改变现有 lint 结果兼容字段。

### P6-71：数据库元数据增量缓存与变更指纹
- 状态：待办。
- 为什么做：数据库直连反向导入、覆盖率报告和元数据浏览会反复读取同一批 schema；没有增量缓存时，大库会慢，AI 也无法判断“这次和上次相比变了什么”。
- 已有基础：已有数据库直连、连接预设、metadata 预览、覆盖率报告、字段来源批次、schema dump 待办、大库扫描计划待办和变更感知扫描待办。
- 缺口：缺少 metadata fingerprint、lastSeenAt、changeSummary 和缓存失效策略；每次扫描结果也没有稳定 hash 供 AI 判断是否需要重新生成 Context 或导入候选。
- 落地产物：新增只保存结构信息的 metadata cache；按连接预设、schema、table 计算 fingerprint；输出新增/删除/变更表字段摘要、缓存时间、刷新方式和源数据库版本。
- 验收标准：重复扫描同一数据库可复用缓存并提示是否过期；字段变化能生成差异摘要；缓存不保存密码、不保存业务数据行；AI 可根据 fingerprint 决定是否重跑反向导入。
- 边界：不做实时同步，不监听数据库 binlog，不默认后台扫描全库。

### P6-72：CLI/MCP 与服务端版本兼容握手
- 状态：待办。
- 为什么做：业务仓库里的 CLI/MCP 脚本可能落后于 DataSpec 服务端，OpenAPI 和 AI 契约也会演进；AI 遇到版本不兼容时应得到明确诊断，而不是运行到一半才失败。
- 已有基础：已有 `doctor`、OpenAPI 防漂移、AI 输出契约测试、workflow recipes、CLI/MCP 和 README 启动说明。
- 缺口：缺少统一 `capabilities/version` 握手；CLI 不知道服务端最小兼容版本、schema hash、功能开关和已废弃字段。
- 落地产物：新增服务端 capability endpoint 或复用现有健康检查扩展；CLI/MCP 在关键命令前读取 serverVersion、apiSchemaHash、minCliVersion、supportedCapabilities、deprecatedFields 和 upgradeHints。
- 验收标准：CLI 版本过旧、服务端未启用某能力、OpenAPI schema 漂移时都有明确中文诊断和修复命令；AI 可读取 JSON 结果决定升级、降级或停止。
- 边界：不做自动在线升级，不强制所有历史 CLI 版本兼容无限期；不引入远程遥测。

### P6-73：前端类型化 API Client 与请求状态收口
- 状态：待办。
- 为什么做：前端已有 OpenAPI 类型生成，但请求封装仍是手写薄层；随着页面增多，字段漂移、loading/error 重复处理和分页参数不一致会继续消耗维护成本。
- 已有基础：已有 `openapi-typescript` schema、`request.ts` 拦截器、项目 store、前端统一数据状态待办、OpenAPI 防漂移和关键流程 E2E 待办。
- 缺口：缺少以 OpenAPI paths 为核心的 typed client 约束；列表分页、项目 ID、错误码、空状态和重试提示仍散落在各页面。
- 落地产物：封装轻量 typed API client 或生成 request helper；统一 `PageResult`、项目参数、错误响应、取消请求、重试建议和 loading state；优先迁移高频页面。
- 验收标准：新增 API 调用默认能从 OpenAPI paths 推导请求/响应类型；前端 build 能发现路径或字段漂移；常见错误在页面显示一致 nextActions。
- 边界：不一次性重写所有页面，不引入大型运行时 SDK；保留现有 Axios 拦截器和 token 行为。

### P6-74：标准变更演练沙箱与样例回归
- 状态：待办。
- 为什么做：修改字段、规则或模板前，用户想知道会影响哪些 SQL、DDL、AI Prompt 和反向导入候选；需要一个轻量“先演练再保存”的入口。
- 已有基础：已有标准快照、What-if 预览待办、Prompt 评测、golden fixtures、SQL 检查记录、DDL 生成、AI 回放和标准质量门禁待办。
- 缺口：现有能力分散，缺少一次性把“改动草案 -> 样例集合 -> lint/DDL/prompt 结果 diff -> 保存建议”串起来的沙箱。
- 落地产物：新增标准草案演练入口；支持选择字段/规则/模板草案和样例集，运行 lint、DDL preview、Prompt preview、Context diff，并输出风险、收益和建议保存步骤。
- 验收标准：用户在保存规则或字段变更前能看到对 good/bad SQL、模板 DDL 和 AI Context 的影响；AI 可读取演练报告决定是否继续修改；演练不写入正式标准。
- 边界：不做多人审批发布，不替代完整 CI；第一版只覆盖项目内 fixture、历史检查记录和用户手动粘贴样例。

### P6-75：MCP/CLI 工具契约验收与示例调用库
- 状态：待办。
- 为什么做：DataSpec 优先给 AI 使用时，MCP tools 和 CLI 命令本身就是 AI 的“产品界面”；仅有 OpenAPI 类型还不够，工具入参、输出 schema、错误码、示例和安全边界都需要可回归验证。
- 已有基础：已有 CLI、MCP、OpenAPI 防漂移、AI 输出契约稳定性、AI 能力清单、doctor 和 workflow recipes。
- 缺口：MCP/CLI 的工具描述、示例调用、失败样例和安全 metadata 没有形成独立 fixture；工具变更后难以及时发现 AI prompt、AGENTS 片段或业务仓库脚本已不兼容。
- 落地产物：新增 MCP/CLI contract fixtures；覆盖核心 tools/commands 的 name、description、inputSchema、outputShape、example、errorExamples、safetyMetadata 和 recommendedNextActions；提供一条本地验收命令。
- 验收标准：修改 CLI/MCP 工具参数或输出字段时，契约测试能失败并提示更新示例；AI 可读取示例库选择正确工具；失败样例不包含 token/password/连接串。
- 边界：不实现完整 MCP 兼容性测试平台，不要求所有历史命令一次性补齐；优先覆盖 lint、Context、reverse import、DDL、doctor 和字段检索高频入口。

### P6-76：业务对象关系图与表模板依赖
- 状态：待办。
- 为什么做：AI 建表不仅需要字段标准，还需要知道用户、订单、支付、审计等业务对象之间的关系，否则容易生成孤立表、重复字段或错误外键。
- 已有基础：已有表模板、DDL 生成、领域 Starter Kit、字段分组、字段影响分析、AI Context 和多项目标准复用包待办。
- 缺口：字段标准和表模板之间缺少轻量关系模型；AI 无法稳定理解“订单表依赖用户表”“支付金额来自订单金额”“审计字段应应用到所有业务表”等模式。
- 落地产物：新增业务对象/表模板关系描述；支持 entityName、tablePattern、requiredFields、optionalFields、relations、foreignKeyHints、auditFields、commonPitfalls 和 contextExport；前端可展示简易关系图。
- 验收标准：生成订单、支付、用户等常见 DDL 时能引用对象关系和外键建议；AI Context 能按业务对象裁剪导出；关系图不要求依赖真实数据库外键。
- 边界：不做完整 ER 建模工具，不强制所有项目维护业务对象，不自动改写已有表结构。

### P6-77：派生字段、单位换算与口径规则
- 状态：待办。
- 为什么做：很多标准字段不是孤立存在的，例如 `amount_cent` 与 `amount_yuan`、`paid_at` 与 `paid_date`、`status_code` 与状态枚举；AI 如果不知道派生关系和单位口径，容易生成混用字段。
- 已有基础：已有字段值格式与校验样例库待办、枚举/码表、字段质量评分、AI Context、DDL 生成和字段推荐。
- 缺口：字段之间缺少 derivedFrom、unitConversion、aggregationRule、timeGranularity 和 sourceOfTruth 等结构化关系；质量检查也无法提示口径混用。
- 落地产物：新增派生字段规则模型或字段扩展；支持源字段、转换表达式说明、单位、精度、时间粒度、枚举映射、推荐使用场景和反例；导出到 AI Context 并纳入质量评分。
- 验收标准：AI 能区分金额分/元、日期/时间戳、编码/展示名、原始值/派生值；DDL/Prompt 生成会提示首选字段和转换口径；测试覆盖典型金额和时间字段。
- 边界：不执行真实数据计算，不替代数据血缘平台，不强制所有字段配置派生规则。

### P6-78：fixedSql 文件级补丁应用与人工确认
- 状态：待办。
- 为什么做：SQL 校验已经能输出 fixedSql，但真实业务仓库里 AI 还需要把修复从页面或 CLI 结果安全落到文件；直接覆盖文件风险高，需要 diff、dry-run、人工确认和回退提示。
- 已有基础：已有 fixedSql、fixedSql diff、SQL 定位范围、CLI lint-files/review-pr、AI 写入安全策略、任务卡和执行证据包待办。
- 缺口：fixedSql 主要停留在展示和复制，缺少按文件生成 patch、预览冲突、确认应用、记录证据和失败恢复的标准流程。
- 落地产物：新增 CLI 或前端文件补丁流程；输入 lint 结果和目标文件，输出 unified diff、conflictWarnings、applyCommand、dryRunResult、rollbackHint 和 evidenceRef；默认只 dry-run。
- 验收标准：AI 可以先生成补丁并展示 diff，再由用户或显式命令确认应用；行号漂移或文件变更时拒绝静默覆盖；应用结果进入检查记录或执行证据包。
- 边界：不自动提交业务仓库，不绕过用户确认，不处理所有复杂 SQL 重排；第一版只覆盖单文件或小批量文件。

### P6-79：标准问答只读入口与证据引用
- 状态：待办。
- 为什么做：用户和 AI 经常只想问“手机号标准字段叫什么”“订单金额应该用哪个单位”“这个字段是否已废弃”，不一定要进入候选草案或 DDL 生成流程。
- 已有基础：已有字段检索、业务术语表、AI 输出引用证据、生命周期状态、字段格式约束、AI Context 和 Prompt 生成。
- 缺口：缺少一个只读、短回答、带证据的标准问答入口；AI 需要自己拼接搜索和解释，容易把候选字段说成确定标准。
- 落地产物：新增标准问答 API/CLI/MCP 或前端搜索模式；输入自然语言问题，输出 answer、confidence、matchedFields、evidence、relatedRules、suggestedNextActions 和 unresolvedQuestions。
- 验收标准：常见字段命名、单位、状态、废弃替代、敏感字段标记问题能返回可读答案和证据；低置信度时明确提示需要人工确认或进入候选 Inbox。
- 边界：第一版不调用外部 LLM，不回答业务数据内容，不把问答结果直接写入标准库。

### P6-80：规则与模板变更 diff 包
- 状态：待办。
- 为什么做：规则、表模板和 Prompt 模板会持续调整；如果只看保存后的结果，AI 和用户很难判断这次变更会影响哪些 SQL 检查、DDL 生成和 Context 输出。
- 已有基础：已有规则配置、规则模板库、Prompt 模板版本化、标准变更演练沙箱、标准快照、执行证据包和 OpenSpec 归档待办。
- 缺口：字段快照相对清晰，但规则/模板/Prompt 的变更 diff、兼容性说明、影响样例和回滚信息还没有统一结构。
- 落地产物：新增规则/模板变更 diff 包；记录 before/after、changedParams、affectedRules、affectedTemplates、sampleResultDiff、compatibilityNotes、rollbackPlan 和 reviewChecklist。
- 验收标准：调整一条规则参数或模板后，能看到命中样例和生成结果差异；AI 可读取 diff 包决定是否需要补测试或更新 Context；回滚说明不依赖人工记忆。
- 边界：不做审批流，不替代 OpenSpec proposal，不要求所有历史模板补齐 diff；优先覆盖新变更。

### P6-81：浏览器级 E2E 验收与失败截图
- 状态：待办。
- 为什么做：P6-17 已有源码级冒烟门禁，但它不启动真实浏览器，也无法发现路由渲染、Monaco、Element Plus 弹层、响应式布局和真实用户操作中的问题。
- 已有基础：已有前端源码级冒烟测试、演示项目初始化、README 验证命令、SQL 校验、反向导入、字段库、AI Context 和覆盖率报告页面。
- 缺口：缺少 Playwright 等浏览器级用例；关键流程失败时没有截图、trace 或复现步骤，AI agent 很难判断是接口、页面还是浏览器交互问题。
- 落地产物：新增浏览器级 E2E 脚本和测试夹具；覆盖创建/选择项目、SQL 校验 fixedSql、检查记录详情、数据库直连预览、字段库筛选和 AI Context 预览；失败时保存截图、trace 和当前 URL。
- 验收标准：一条命令可在本地跑核心浏览器流程；破坏主导航、项目选择、关键按钮或结果渲染时测试失败并给出可复现证据；README 说明与源码级 smoke 的边界差异。
- 边界：不追求全量页面覆盖，不做像素级视觉回归，不要求普通 `pnpm test` 默认依赖浏览器；第一版可作为可选验证入口。

### P6-82：真实数据库 Testcontainers 集成测试矩阵
- 状态：待办。
- 为什么做：数据库直连反向导入、覆盖率和二次比对已经是核心能力，但当前主要依赖 fixture 和 H2/单测；真实 PostgreSQL/MySQL 元数据、COMMENT、schema、大小写和权限行为仍可能漂移。
- 已有基础：已有 Flyway、PostgreSQL/MySQL 直连 metadata、反向导入预览、数据库覆盖率、二次比对、metadata fixture 和多方言兼容矩阵待办。
- 缺口：缺少基于真实数据库容器的集成测试；无法稳定验证 COMMENT ON、MySQL 注释、schema/table 过滤、索引元数据、只读权限和大小写边界。
- 落地产物：新增 Testcontainers 测试 profile；启动 PostgreSQL/MySQL 容器，加载最小 schema fixture，验证表列表、metadata preview、compare、coverage 和连接诊断的关键字段。
- 验收标准：开发者可通过专门 profile 运行真实数据库集成测试；默认 `mvn test` 不强制依赖 Docker；失败信息能定位具体方言和 metadata 字段。
- 边界：不替代数据库供应商完整兼容认证，不把 Docker 作为所有开发环境的必需前提，不扫描业务数据行。

### P6-83：README/TODO/OpenSpec 状态一致性检查
- 状态：待办。
- 为什么做：项目能力迭代很快，README、TODO、OpenSpec active/archive 和实际代码容易出现“文档说已完成但入口不可用”或“待办仍写缺口但功能已实现”的漂移。
- 已有基础：已有 README 功能清单、TODO 状态行、OpenSpec change、OpenSpec validate、AI 输出契约测试和完成项归档习惯。
- 缺口：缺少自动检查文档状态一致性的脚本；AI agent 接手时仍需要人工比对多个入口，容易基于过期上下文继续开发。
- 落地产物：新增 docs/status-check 脚本或等价验证入口；扫描 TODO 状态、README 已完成功能清单、OpenSpec active/archive、关键 API/页面入口和参考链接，输出 mismatch、missingEvidence 和 suggestedFix。
- 验收标准：文档改动后能一条命令检查明显状态漂移；新增完成项时会提示补 README/TODO/OpenSpec 入口；检查结果不依赖外部网络。
- 边界：不做自然语言完美理解，不强制阻断所有文档变更；第一版只覆盖编号、状态、标题、链接和关键入口的确定性规则。

### P6-84：前端可访问性与键盘操作基线
- 状态：待办。
- 为什么做：DataSpec 会成为日常工作台，字段库、反向导入、SQL 校验和命令面板都需要键盘可操作、焦点清晰、表单标签明确；这也会让 AI/browser automation 更稳定。
- 已有基础：已有 Vue 3、Element Plus、Monaco、前端关键流程 smoke、命令面板待办、统一前端状态待办和浏览器级 E2E 待办。
- 缺口：当前页面主要关注功能可用，缺少焦点顺序、aria label、表格操作按钮名称、弹窗焦点恢复、键盘快捷入口和颜色对比度基线。
- 落地产物：为核心页面建立可访问性检查清单和少量自动化规则；补齐关键按钮/输入的可读名称、焦点状态、弹窗关闭/恢复逻辑和键盘路径；把检查接入可选前端验证。
- 验收标准：不用鼠标也能完成项目切换、SQL 校验、查看记录、字段库筛选和反向导入预览的核心路径；关键按钮对自动化工具有稳定名称；可访问性检查不出现高危问题。
- 边界：不一次性改完整视觉系统，不承诺 WCAG 全量认证；第一版聚焦高频工作流和自动化稳定性。

### P6-85：本地数据清理、重置与演示项目重建
- 状态：待办。
- 为什么做：个人使用和 AI 开发过程中会频繁生成项目、字段、检查记录、AI 回放和导入批次；没有安全的清理/重置入口时，本地库会越来越乱，复现问题也更困难。
- 已有基础：已有演示项目初始化、Flyway、标准快照、SQL 检查记录、AI 回放、反向导入来源批次、API Token 和本地启动包待办。
- 缺口：缺少项目级清理、演示项目重建、过期记录清理和 dry-run 摘要；开发者只能手动删库或写 SQL，容易误删 token、连接预设或重要标准。
- 落地产物：新增 CLI/API 或前端设置入口；支持 dry-run 展示将删除的 SQL 检查记录、AI jobs、导入批次、快照和演示数据；支持重建演示项目，默认保留 token 和非敏感连接预设。
- 验收标准：用户可以安全重置演示项目或清理旧记录；所有破坏性操作先输出 dry-run 摘要并要求显式确认；README 说明不会删除源数据库数据。
- 边界：不做企业级数据保留策略，不自动清理用户标准字段，不绕过备份/恢复待办。

### P6-86：前端性能体验指标与慢页面提示
- 状态：待办。
- 为什么做：后端已有大字段库性能基线，但前端字段库、反向导入、覆盖率、AI 回放和 Monaco 页面在数据量变大后仍可能卡顿；用户和 AI automation 需要知道页面是在加载、计算还是异常。
- 已有基础：已有性能基线、字段分页、SQL 检查记录分页、前端 smoke、统一请求状态、元数据增量缓存和大库扫描计划待办。
- 缺口：缺少前端渲染耗时、列表数量、虚拟滚动阈值、慢请求提示和长任务定位；页面卡顿时没有可复制的诊断摘要。
- 落地产物：新增前端 performance markers 和慢页面提示；对字段库、反向导入 preview/compare、覆盖率报告和 AI 回放记录输出 renderMs、rowCount、requestMs、slowReason 和 recommendedAction；必要处引入分页或虚拟列表。
- 验收标准：大字段库或大 metadata 结果下页面给出明确加载/慢查询提示；性能摘要可复制给 AI 分析；前端构建和 smoke 测试覆盖关键状态文案。
- 边界：不引入复杂 APM，不上传用户数据，不为了性能一次性重写所有表格组件。

### P6-87：数据库 Schema 变更计划与迁移脚本预览
- 状态：待办。
- 为什么做：DataSpec 已能生成 DDL、检查 SQL 和反向导入现有数据库，但真实落地还需要回答“从当前数据库变到目标标准要改哪些表/字段、风险是什么、怎么回退”；AI 不能只输出一段不可审计的 ALTER SQL。
- 已有基础：已有表模板、DDL 生成、数据库直连 metadata、二次比对、标准快照、fixedSql diff、备份迁移包待办和 Atlas/Terraform 风格 plan/apply 参考。
- 缺口：缺少以 DataSpec 标准和当前数据库 metadata 为输入的 schema change plan；不能稳定输出 add/alter/rename/drop 的风险、依赖、dry-run SQL、回滚提示和人工确认点。
- 落地产物：新增只读 schema plan API/CLI/前端预览；输出 currentSchemaHash、targetSpecHash、changeSet、riskLevel、migrationSql、rollbackHint、manualChecks、blockedReasons 和 nextActions；默认只生成预览，不执行数据库写入。
- 验收标准：连接数据库后可生成“当前库 -> DataSpec 标准”的变更计划；高风险 drop/rename 默认标红且不自动执行；AI 可读取 JSON 计划并决定补标准、生成迁移文件或停止等待人工确认。
- 边界：第一版不直接执行迁移，不替代 Flyway/Liquibase/Atlas 等迁移工具，不自动推断所有字段重命名；只服务个人/小团队的迁移草案和风险说明。

### P6-88：业务代码字段引用索引与重命名风险分析
- 状态：待办。
- 为什么做：修改字段名、废弃字段或合并标准前，用户和 AI 需要知道业务仓库里哪些 SQL、迁移文件、ORM 模型、报表或配置正在引用该字段，否则标准变更容易造成代码与数据库脱节。
- 已有基础：已有 `.dataspec/config.json` 默认扫描路径、CLI 批量 lint、PR review、字段影响分析、变更感知扫描、fixedSql 文件补丁和业务仓库初始化。
- 缺口：字段影响分析主要聚焦 DataSpec 内部对象，缺少对业务仓库文件的字段引用索引、引用类型、置信度、文件位置和重命名风险。
- 落地产物：新增 `dataspec index-refs` 或等价 API/CLI；扫描 SQL、DDL、常见 ORM/配置文件，输出 fieldName、referenceKind、file、line、confidence、suggestedAction 和 renameRisk；前端字段影响弹窗可读取摘要。
- 验收标准：给定一个标准字段，能列出业务仓库内主要引用位置；准备重命名或停用字段时能看到风险清单；扫描只读且遵守 defaultPaths/ignorePatterns。
- 边界：不做完整代码智能平台，不解析所有语言 AST，不自动改业务代码；第一版优先覆盖 SQL、迁移文件和常见 schema/model 文件。

### P6-89：MCP Prompt/Resource 一等化与 Agent 引导包
- 状态：待办。
- 为什么做：当前 MCP 重点是 tools/resources，但 AI agent 很多时候需要先拿到“该怎么问、先读什么、什么不能做”的 prompt 引导；如果这些只散落在 README 和 AGENTS 片段里，工具调用仍容易绕远。
- 已有基础：已有 MCP server、AI Context package、workflow recipes、AI 能力清单、CLI/MCP 契约验收、AI 会话启动包和 `AGENTS.md.fragment`。
- 缺口：缺少稳定的 MCP prompts/resource templates，把建表、SQL review、反向导入、字段检索、标准问答等常用任务封装成可枚举、可复用、可版本化的 agent 引导。
- 落地产物：扩展 MCP 暴露 prompts 和更细的 resources；提供 `create_table_with_dataspec`、`review_sql_with_dataspec`、`reverse_import_standards`、`answer_field_standard_question` 等 prompt 模板，包含 requiredInputs、safeDefaults、toolSequence、stopConditions 和 evidenceRequirements。
- 验收标准：MCP 客户端能枚举 DataSpec prompt/resource；AI 选择任务模板后能按推荐顺序读取标准、执行工具和返回证据；模板变更纳入 MCP/CLI 契约测试。
- 边界：不绑定单一 IDE 或 agent 产品，不调用外部 LLM，不把 prompts 做成复杂审批流。

### P6-90：AI 上下文预算评估与自动裁剪策略
- 状态：待办。
- 为什么做：字段标准、规则、模板、样例和历史记录越来越多后，AI Context 很容易过大；仅靠手动 scope/query/limit 不够，AI 需要知道不同预算下应该保留哪些标准、舍弃哪些上下文以及风险是什么。
- 已有基础：已有 AI Context 按需裁剪、字段检索、标准快照、业务术语表、AI 会话启动包、上下文握手和 prompt 评测待办。
- 缺口：缺少 token/context budget 估算、裁剪策略说明、召回质量指标和低预算降级提示；AI 也无法判断“当前包够不够完成这个任务”。
- 落地产物：新增 context budget planner；输入任务类型、query、目标表/文件、预算上限，输出 selectedArtifacts、estimatedTokens、droppedArtifacts、qualityRisk、fallbackSteps 和 recommendedNextActions；前端 AI Context 页面展示预算预估。
- 验收标准：同一项目可生成完整包、标准包和极简包；AI 能解释为什么保留某些字段/规则并标出缺失风险；裁剪策略有 fixture 或快照测试防漂移。
- 边界：不依赖特定模型的精确 tokenizer，不上传标准内容到外部服务，不保证一次裁剪覆盖所有复杂任务。

### P6-91：本地 pre-commit 与 IDE 保存前 SQL 标准检查
- 状态：待办。
- 为什么做：CI/PR review 发现问题已经偏晚；个人使用时更希望在本地提交前或保存 SQL/迁移文件时就看到 DataSpec 诊断，让 AI 和开发者少走返工。
- 已有基础：已有 CLI `lint-files`、PR review、`.dataspec/config.json`、GitHub Action 示例、质量门禁、changed-file 扫描和 fixedSql 文件补丁待办。
- 缺口：缺少官方 pre-commit hook、轻量 IDE/编辑器任务配置示例和本地失败输出规范；不同业务仓库要自己拼命令。
- 落地产物：新增 `dataspec install-hook` 或模板文档；生成 pre-commit 配置、VS Code task/Problem Matcher 示例和 `lint-changed` 快捷命令；输出 file/line/rule/severity/suggestion 便于 IDE 跳转。
- 验收标准：业务仓库执行初始化后可一键启用本地 SQL 标准检查；提交前能只检查变更 SQL/DDL 文件；失败输出不泄漏 token/password，且可被 AI 读取继续修复。
- 边界：不强制所有项目安装 hook，不绕过用户本地 Git 配置，不替代 CI/GitHub Review。

### P6-92：标准样例自动生成与合成业务场景库
- 状态：待办。
- 为什么做：规则、字段推荐、DDL 生成和 Prompt 评测都依赖高质量样例；手写少量 good/bad SQL 容易覆盖不足，AI 也缺少“典型业务场景下标准如何使用”的可复用素材。
- 已有基础：已有演示项目、golden fixtures、字段使用示例与反例库、领域 Starter Kit、Prompt 评测、规则模板库和标准变更演练沙箱待办。
- 缺口：缺少从标准字段、模板、代码集和业务对象关系自动生成 SQL/DDL/Prompt 样例的能力，也缺少按场景组织的 synthetic cases。
- 落地产物：新增样例生成器；支持用户、订单、支付、审计等场景，生成 good SQL、bad SQL、DDL preview 输入、字段推荐问题、标准问答案例和预期诊断；样例带 specHash 与生成参数。
- 验收标准：新规则或字段变更后可快速生成一组覆盖样例；样例可接入现有后端 fixture、前端 smoke 或 Prompt 评测；生成内容不包含真实业务数据行。
- 边界：不替代人工维护的高价值真实样例，不引入外部 LLM 自动造数据，不生成可直接写入生产库的数据。

### P6-93：多源契约反向导入到标准候选
- 状态：待办。
- 为什么做：很多字段标准并不只存在于数据库，还散落在 OpenAPI、JSON Schema、Protobuf、事件 schema 和前端类型里；AI 建表或修 SQL 时如果只看数据库来源，会遗漏接口层已经稳定下来的业务命名。
- 已有基础：已有 OpenAPI 类型生成、字段推荐、标准候选 Inbox、数据库反向导入、字段来源追踪、业务代码引用索引和 AI Context。
- 缺口：缺少从 API/Schema 契约反向抽取字段候选的统一入口；无法记录字段来自哪个接口、消息、版本和属性路径，也无法和数据库字段候选合并比对。
- 落地产物：新增契约导入预览 API/CLI；支持 OpenAPI、JSON Schema 和 Protobuf descriptor 或 `.proto` 文件输入，输出 candidateFields、sourceKind、sourcePath、schemaVersion、confidence、conflictReasons 和 recommendedAction；前端复用候选采纳台确认写入。
- 验收标准：给定一份接口契约，能抽取字段名、类型、描述、必填性、枚举和示例值作为标准候选；候选可与已有字段、数据库反向导入结果去重；AI Context 能标明字段来源于 API 契约还是数据库。
- 边界：第一版不做全语言类型系统解析，不直接写入正式标准，不替代数据库反向导入；复杂 oneOf/allOf/泛型先保守降级为人工确认。

### P6-94：标准来源可信度与 AI 置信度标记
- 状态：待办。
- 为什么做：AI 使用数标时需要知道哪些字段是人工确认的核心标准、哪些是数据库反向导入候选、哪些是样例生成或低置信度推断；否则容易把“疑似标准”当成“强制标准”使用。
- 已有基础：已有字段来源批次、变更日志、标准快照、标准候选 Inbox、字段质量评分、冲突检测、AI 输出证据和反向导入映射待办。
- 缺口：字段与规则缺少统一 provenance/confidence 结构；AI Context、字段推荐和标准问答无法稳定表达“建议使用但需确认”“已废弃但仍被引用”“仅来自样例”的差异。
- 落地产物：为字段、别名、枚举、规则和模板补充来源证据模型；输出 sourceType、sourceRef、verifiedBy、verifiedAt、confidenceLevel、evidenceCount、lastSeenAt 和 warning；前端字段详情和 AI Context 展示可信度摘要。
- 验收标准：AI 能区分 confirmed、imported、generated、deprecated、conflicting 等来源状态；低置信度字段不会被推荐为首选；标准快照包含可信度摘要且可回放。
- 边界：不做复杂组织认证流程，不引入人工审批；可信度只辅助决策，不自动删除或隐藏已有标准。

### P6-95：可插拔规则 SDK 与项目内自定义规则
- 状态：待办。
- 为什么做：内置 SQL/字段规则能覆盖通用标准，但个人或小团队会有项目特有命名、枚举、审计字段、分库分表和框架约束；如果每次都改核心代码，规则迭代会变慢，也不利于 AI 针对项目补规则。
- 已有基础：已有规则配置、规则模板库、误报豁免、规则调试器、多方言兼容矩阵、golden fixtures 和 AI 输出契约测试。
- 缺口：缺少稳定的 custom rule SDK、规则包目录、测试夹具和加载边界；项目规则无法用声明式配置或轻量脚本扩展。
- 落地产物：定义项目内规则包格式；支持 declarative rule、fixture、metadata、severity、paramsSchema、safeFixStrategy 和 AI readable description；CLI 提供 validate-rule/test-rule，前端规则页可加载和调试自定义规则。
- 验收标准：用户能在业务仓库或 DataSpec 项目中新增一条自定义命名规则并跑通测试；规则错误不会拖垮核心 lint；AI 可读取规则描述、示例和失败原因后补充 SQL。
- 边界：第一版不执行不可信远程代码，不做规则市场，不允许自定义规则访问数据库密码或业务数据行。

### P6-96：本地语义检索索引与相似字段发现
- 状态：待办。
- 为什么做：字段数量变多后，纯关键词检索难以发现“手机号、联系电话、mobile_no”“支付金额、订单实付金额”这类语义相近字段；AI 在上下文有限时也需要一个本地、可重建、可解释的相似字段检索入口。
- 已有基础：已有字段标准检索、字段推荐、AI Context 裁剪、业务术语表、同义词词根库、上下文预算评估和字段冲突检测。
- 缺口：缺少离线语义索引、相似度解释、索引版本和重建命令；当前只能依赖手写同义词和确定性评分。
- 落地产物：新增可选本地 semantic index；记录 indexVersion、sourceSnapshotHash、embeddingProvider/localTokenizer、similarFields、score、explainTerms 和 rebuildCommand；CLI/MCP/前端字段检索可选择语义模式。
- 验收标准：无需上传标准内容即可生成本地索引；语义检索能发现命名不同但含义接近的字段并给出解释；索引过期时能提示基于当前标准快照重建。
- 边界：不强依赖外部向量数据库或云模型，不把语义分数作为唯一排序依据，不改变现有确定性检索的默认可用性。

### P6-97：标准使用热区与清理优先级报告
- 状态：待办。
- 为什么做：标准字段、规则和候选越来越多后，用户需要知道哪些标准被 SQL、DDL、数据库表、AI 任务和业务代码频繁使用，哪些长期无人使用或冲突高，才能优先清理真正影响 AI 输出质量的部分。
- 已有基础：已有字段影响分析、字段覆盖率、业务代码引用索引、AI 使用画像、字段质量评分、冲突检测、检查记录和 AI 回放。
- 缺口：缺少跨来源 usage heatmap 和 cleanup priority；字段列表只能看静态属性，无法按“高使用低质量”“高冲突高影响”“长期未命中”排序。
- 落地产物：新增标准使用热区报告；聚合 fieldUsageCount、lastReferencedAt、sourceKinds、qualityScore、conflictCount、aiJobHits、lintHits、cleanupPriority 和 suggestedNextAction；前端提供可筛选列表和跳转。
- 验收标准：用户能一眼看到最值得优先修的字段、规则和模板；AI 可读取报告先处理高影响标准，而不是随机优化；报告生成不读取业务数据行。
- 边界：不做团队 KPI，不上传使用统计，不以使用次数自动删除低频字段；第一版只聚合 DataSpec 已有记录和用户指定扫描结果。

### P6-98：面向 AI 的标准变更发布说明与迁移指令
- 状态：待办。
- 为什么做：标准字段、规则、模板和 Prompt 变化后，AI agent 需要知道“这次标准变了什么、旧写法如何迁移、哪些任务要重跑”，而不只是看到一份 diff 或快照 hash。
- 已有基础：已有标准快照、变更日志、规则与模板 diff 包、执行证据包、AI 会话启动包、TODO 到 OpenSpec 交接、MCP/CLI 工作流模板和 OpenSpec 归档。
- 缺口：缺少面向 AI 的 release note/migration guide；变更完成后没有统一输出 breakingChanges、deprecatedFields、replacementHints、requiredChecks 和 recommendedWorkflow。
- 落地产物：新增标准变更发布说明生成器；按快照 diff 或变更日志生成 machine-readable changelog，包含 changeType、affectedArtifacts、migrationSteps、beforeAfterExamples、verificationCommands、rollbackHint 和 agentInstructions；CLI/MCP/AI Context 可读取。
- 验收标准：标准升级后 AI 能解释本次变化并按迁移指令修 SQL、DDL 或字段引用；破坏性变更会明确标记并要求人工确认；发布说明可随备份包或执行证据包归档。
- 边界：不做复杂发布审批，不自动修改业务仓库；第一版只生成说明和建议命令，实际改动仍走 dry-run 或人工确认。

## 参考项目索引

- [`sqlfluff/sqlfluff`](https://github.com/sqlfluff/sqlfluff)：模块化、可配置、多方言 SQL linter。
- [`eslint/eslint`](https://github.com/eslint/eslint)：可插拔规则、fixture 测试和规则元数据设计参考。
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
- [`bufbuild/buf`](https://github.com/bufbuild/buf)：Protobuf schema lint、breaking change 检查和契约管理参考。
- [`glideapps/quicktype`](https://github.com/glideapps/quicktype)：从 JSON/Schema 推导类型与结构的契约反向提取参考。
- [`backstage/backstage`](https://github.com/backstage/backstage)：项目模板、开发者入口和脚手架体验参考。
- [`dbeaver/dbeaver`](https://github.com/dbeaver/dbeaver)：数据库连接配置、metadata 浏览和多方言体验参考。
- [`reviewdog/reviewdog`](https://github.com/reviewdog/reviewdog)：基于 diff 的代码审查评论、诊断聚合和 PR 反馈参考。
- [`pre-commit/pre-commit`](https://github.com/pre-commit/pre-commit)：本地变更钩子、按文件质量门禁和轻量开发工作流参考。
- [`hashicorp/terraform`](https://github.com/hashicorp/terraform)：plan/apply、状态记录和 dry-run 风格的写入前演练参考。
- [`changesets/changesets`](https://github.com/changesets/changesets)：变更集、版本发布说明和迁移提示组织参考。
- [`sourcegraph/sourcegraph`](https://github.com/sourcegraph/sourcegraph)：代码引用检索、搜索索引和仓库级影响分析参考。
- [`Redocly/redocly-cli`](https://github.com/Redocly/redocly-cli)：OpenAPI lint、bundle 和契约治理参考。
- [`Schemathesis/schemathesis`](https://github.com/schemathesis/schemathesis)：基于 OpenAPI 的契约测试和接口行为回归参考。
- [`sqlmesh/sqlmesh`](https://github.com/TobikoData/sqlmesh)：数据模型依赖、plan/apply 和变更影响分析参考。
- [`microsoft/playwright`](https://github.com/microsoft/playwright)：浏览器级 E2E、trace、截图和稳定选择器参考。
- [`testcontainers/testcontainers-java`](https://github.com/testcontainers/testcontainers-java)：Java 集成测试中启动真实 PostgreSQL/MySQL 容器的参考。
- [`TanStack/query`](https://github.com/TanStack/query)：前端 server state、请求缓存、重试和错误状态收口参考。
- [`dequelabs/axe-core`](https://github.com/dequelabs/axe-core)：前端可访问性自动检查规则参考。
- [`GoogleChrome/lighthouse`](https://github.com/GoogleChrome/lighthouse)：页面性能、可访问性和最佳实践审计参考。
- [`gitleaks/gitleaks`](https://github.com/gitleaks/gitleaks)：敏感信息检测、日志脱敏和 secret 防泄漏参考。
- [`pgvector/pgvector`](https://github.com/pgvector/pgvector)：本地或自托管向量检索索引的设计参考。
- [Model Context Protocol 规范](https://modelcontextprotocol.io/specification/2025-06-18)：AI 应用接入 resources、prompts、tools 的协议基础。
- [`modelcontextprotocol/servers`](https://github.com/modelcontextprotocol/servers)：MCP server 参考实现集合。
- [`agents.md`](https://agents.md/)：面向 coding agent 的项目指令文件约定。
