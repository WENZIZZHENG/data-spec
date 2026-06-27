# DataSpec 待办路线图

本文件记录当前仍需行动的产品与工程待办。优先级按用户可感知价值、核心链路阻断程度和后续开发解锁程度排序。

## 下一步顺序

1. P6-2 字段使用覆盖率已完成第一版，下一步推进 P6-3 AI 生成与修复决策回放。
2. P6 后续继续补业务仓库初始化向导、标准质量评分、轻量影响分析、AI contract fixtures、按需 AI Context、规则例外治理、GitHub inline 实接、性能基线、前端回归门禁、AI 可读诊断、字段检索、OpenSpec 收口、历史快照回放、多方言兼容矩阵、规则模板库、备份迁移包、数据库只读安全诊断、AI 批量任务、标准候选采纳台、离线 Context、元数据适配、Prompt 评测、项目活动时间线、任务式前端导航、本地启动包和 fixedSql 策略化。
3. P6 收束后再回看哪些能力需要从个人/小团队工具升级为团队协作能力。

## 已完成能力摘要（P0-P4）

P0-P4 的详细背景、方案和验收已归档到 [docs/archive/todo-completed-p0-p4.md](docs/archive/todo-completed-p0-p4.md)。主待办只保留当前仍需行动的 P5/P6 任务。

- P0 AI 可消费主线已完成第一版：AI Context zip、CLI、MCP、个人版字段模型、结构化命名规则和 AI Prompt 生成。
- P1 核心闭环已完成第一版：SQL 校验、OpenAPI 类型契约、COMMENT 解析、前端管理页、字段推荐、结构化修复建议、DDL 生成、检查记录和 fixedSql。
- P2 标准维护与生成能力已完成第一版：内置 standards 初始化、模板 DDL、业务项目 .dataspec/ 约定、数据字典、Excel 导入导出、变更日志和个人工作台。
- P3 自动化与反向导入已完成第一版：SQL 反向导入预览、MySQL DDL 解析、CI/GitHub Action 和 PR 评论式 SQL Review。
- P4 工程化与体验增强已完成第一版：SQL 定位、fixedSql diff、.dataspec/config.json、规则配置表单、OpenAPI 防漂移、Excel dry-run、HTML/ERD、MySQL 规则覆盖、安全基线、演示项目和数据库直连反向导入前端流程。
- 后续真实待办集中在 P6：P5 已完成 dataspec doctor、数据库二次比对、导入来源追踪、SQL 定位范围增强、字段推荐质量增强、核心 fixture/golden 基线、前端高频流程细节打磨和轻量 token 管理；P6-1 已完成标准版本快照第一版，P6-2 已完成字段覆盖率第一版，后续再提升 AI 回放、初始化向导、质量评分、影响分析、AI 契约稳定性、GitHub inline 实接、性能基线、前端回归、AI 可读诊断、字段检索和 OpenSpec 收口。

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
- 状态：待办。
- 为什么做：DataSpec 优先让 AI 使用时，除了给 AI 上下文，还需要能回看一次生成或修复使用了哪些字段、规则、prompt 模板和输入，方便定位“AI 为什么这么写”。
- 已有基础：已有 AI Prompt 生成、SQL 校验记录、fixedSql、DDL 生成、CLI/MCP 和标准变更日志。
- 缺口：缺少 AI 任务级记录，无法把 prompt 模板版本、标准快照、输入 SQL/业务描述、输出 SQL、lint 结果和修复建议串起来。
- 落地产物：新增轻量 AI 作业记录或在现有检查/生成记录中扩展上下文字段；支持查看输入、输出、引用标准、规则结果和可复制的回放命令。
- 验收标准：一次 CLI/MCP 或前端生成后，能在记录详情看到本次使用的项目、标准版本、prompt 输入、输出 SQL 和 lint 结果；能复制命令或 JSON 复现本次请求。
- 边界：不内置外部 LLM 调用，不保存第三方 API key，不做长文本会话管理。

### P6-4：业务仓库 `dataspec init` 初始化向导
- 状态：待办。
- 为什么做：让 AI 在真实业务仓库使用 DataSpec 时，第一步应该是把 `.dataspec/config.json`、默认扫描路径和 AGENTS 片段配置好；现在这些需要人工拼接。
- 已有基础：已有 `.dataspec/config.json` 读取、`dataspec doctor`、AI Context 导出、AGENTS fragment 和 CLI/MCP。
- 缺口：缺少一条可交互或可参数化的初始化命令，把项目 ID、服务地址、默认路径、token 使用方式和推荐脚本一次性落到业务仓库。
- 落地产物：新增 `dataspec init` CLI 命令；生成或更新 `.dataspec/config.json`、`.dataspec/README.md`、可选 `AGENTS.md` 片段和常用命令示例；完成后自动运行 `dataspec doctor`。
- 验收标准：在任意业务仓库执行初始化后，AI agent 能直接通过 CLI/MCP 读取项目标准；重复执行不会覆盖用户手写配置，除非显式确认或使用 `--force`。
- 边界：不修改业务代码，不自动提交业务仓库，不把明文 token 写入可提交文件。

### P6-5：标准字段质量评分与修复建议
- 状态：待办。
- 为什么做：字段标准越多，越需要识别哪些字段缺少别名、示例、枚举、敏感标识或注释，否则 AI 会得到看似完整但语义不足的字段目录。
- 已有基础：字段模型已有别名、标签、状态、敏感标识、示例值、代码集关联、变更日志和字段推荐原因。
- 缺口：缺少字段健康度评分、低质量字段筛选、推荐补全动作和批量修复入口。
- 落地产物：新增标准质量检查服务和前端质量视图；按字段输出缺注释、缺别名、缺示例、疑似敏感未标记、枚举未关联、废弃字段仍推荐等问题；提供修复建议和跳转编辑。
- 验收标准：字段库能按质量分排序；低质量字段可一键定位编辑；质量检查结果能被 CLI/MCP 或 AI Context 消费。
- 边界：不自动改字段标准，不引入外部 LLM 自动补全，不做组织级质量 KPI。

### P6-6：轻量字段影响分析
- 状态：待办。
- 为什么做：修改字段名、类型、枚举或状态前，用户需要知道它会影响哪些模板、DDL 生成、SQL 检查记录、AI Context 和反向导入来源。
- 已有基础：已有表模板、DDL 生成、数据字典、SQL 检查记录、变更日志、反向导入来源追踪待办和 AI Context。
- 缺口：字段详情页还不能展示“这个标准字段在哪里被使用”，编辑前也没有影响提示。
- 落地产物：新增字段影响查询 API 和前端详情区域；汇总关联模板、生成记录、检查记录、导入来源、枚举引用和 AI Context 最近导出时间；编辑关键字段时展示轻量影响提示。
- 验收标准：打开字段详情可看到当前字段的使用位置；修改字段类型/状态前能看到受影响模板或记录数量；不阻断个人快速编辑。
- 边界：不做复杂血缘图谱，不扫描生产查询日志，不实现审批或发布阻断。

### P6-7：AI Context 按需裁剪与检索模式
- 状态：待办。
- 为什么做：标准字段、规则、模板和记录变多后，一次性把完整 AI Context 喂给 coding agent 会浪费上下文，也容易让 AI 读到与当前任务无关的标准。
- 已有基础：已有 AI Context zip、MCP resources、字段推荐、规则导出、项目 `.dataspec/config.json` 和 `dataspec doctor`。
- 缺口：AI Context 目前偏完整导出，缺少按表、数据域、标签、状态、字段名和任务类型裁剪的稳定入口；MCP 侧也缺少面向“当前 SQL 文件/当前需求”的检索式资源。
- 落地产物：新增按需导出 API/CLI/MCP 参数，支持 `scope=table|domain|tag|field|changed` 等裁剪模式；输出包含命中原因、裁剪条件、字段/规则数量和缺失提示；在 `.dataspec/README.md` 中说明 AI 何时用完整包、何时用按需包。
- 验收标准：AI agent 能针对一个建表/修 SQL 任务获取最小可用字段标准；导出结果仍符合 JSON Schema；大项目下按需包体积明显小于完整包。
- 边界：不引入向量数据库，不依赖外部 LLM 检索，不改变完整 AI Context 的兼容格式。

### P6-8：标准字段重复与冲突检测
- 状态：待办。
- 为什么做：标准字段长期维护后，容易出现同义字段重复、别名互相冲突、同名不同类型、敏感标识不一致等问题；这些会直接降低 AI 推荐和 SQL 修复质量。
- 已有基础：字段模型已有别名、分类、标签、敏感标记、状态、示例值和代码集关联；字段推荐已能输出命中原因。
- 缺口：缺少项目级重复/冲突扫描，也没有把冲突结果反馈给字段推荐、AI Context 或字段库筛选。
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
- [Model Context Protocol 规范](https://modelcontextprotocol.io/specification/2025-06-18)：AI 应用接入 resources、prompts、tools 的协议基础。
- [`modelcontextprotocol/servers`](https://github.com/modelcontextprotocol/servers)：MCP server 参考实现集合。
- [`agents.md`](https://agents.md/)：面向 coding agent 的项目指令文件约定。
