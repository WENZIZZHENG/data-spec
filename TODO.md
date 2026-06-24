# DataSpec 待办路线图

本文件记录当前仍需行动的产品与工程待办。优先级按用户可感知价值、核心链路阻断程度和后续开发解锁程度排序。

## 下一步顺序

1. 先补规则测试语料库和 fixedSql golden fixtures，锁住 parser、lint、fixedSql 与反向导入核心行为。
2. 继续打磨 AI 可消费质量：AI contract fixtures、按需 AI Context 和 MCP/CLI workflow recipes。
3. 打磨个人/小团队日常体验：前端高频流程细节、轻量 API Token 管理页面；仍避免过早引入审批流、发布流程等重型治理模型。
4. P5 稳定后推进 P6：标准版本快照、字段覆盖率、AI 回放、业务仓库初始化向导、标准质量评分、轻量影响分析、按需 AI Context、规则例外治理、AI 契约稳定性、GitHub inline 实接和性能基线。

## 已完成能力摘要（P0-P4）

P0-P4 的详细背景、方案和验收已归档到 [docs/archive/todo-completed-p0-p4.md](docs/archive/todo-completed-p0-p4.md)。主待办只保留当前仍需行动的 P5 任务。

- P0 AI 可消费主线已完成第一版：AI Context zip、CLI、MCP、个人版字段模型、结构化命名规则和 AI Prompt 生成。
- P1 核心闭环已完成第一版：SQL 校验、OpenAPI 类型契约、COMMENT 解析、前端管理页、字段推荐、结构化修复建议、DDL 生成、检查记录和 fixedSql。
- P2 标准维护与生成能力已完成第一版：内置 standards 初始化、模板 DDL、业务项目 .dataspec/ 约定、数据字典、Excel 导入导出、变更日志和个人工作台。
- P3 自动化与反向导入已完成第一版：SQL 反向导入预览、MySQL DDL 解析、CI/GitHub Action 和 PR 评论式 SQL Review。
- P4 工程化与体验增强已完成第一版：SQL 定位、fixedSql diff、.dataspec/config.json、规则配置表单、OpenAPI 防漂移、Excel dry-run、HTML/ERD、MySQL 规则覆盖、安全基线、演示项目和数据库直连反向导入前端流程。
- 后续真实待办集中在 P5/P6：P5 已完成 dataspec doctor、数据库二次比对、导入来源追踪、SQL 定位范围增强和字段推荐质量增强，接下来补齐 fixtures、前端细节和轻量 token 管理；P6 再提升标准版本、覆盖率、AI 回放、初始化向导、质量评分、影响分析、AI 契约稳定性、GitHub inline 实接和性能基线。

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
- 状态：待办。
- 为什么做：SQL parser、lint rule、fixedSql 和 reverse import 都已经成为核心链路，需要稳定样例库防止“修一个规则，坏另一个规则”。
- 已有基础：已有后端 parser/lint 单测、examples good/bad SQL、MySQL 兼容测试和 CLI/MCP 测试。
- 缺口：缺少按方言、规则、修复 SQL、反向导入 metadata 组织的 fixtures；缺少 fixedSql golden 输出和差异断言。
- 落地产物：建立 `fixtures` 或等价测试资源目录，覆盖 PostgreSQL/MySQL good/bad SQL、COMMENT、UNSIGNED、索引、缺注释、字段推荐、fixedSql、反向导入样例；新增 golden 测试入口。
- 验收标准：后端测试能一键跑完 fixture/golden 用例；修改 parser/rule/fixedSql 时能明确看到行为差异；`mvn test` 仍作为统一验证入口。
- 边界：不追求完整 SQL 方言覆盖，只收录项目真实会遇到的高价值样例。

### P5-8：前端高频流程细节打磨
- 状态：待办。
- 为什么做：当前前端主流程已经可用，下一步应减少重复输入和上下文丢失，让个人日常使用更顺手。
- 已有基础：工作台、项目选择、SQL 校验、DDL 生成、反向导入、字段库、规则配置、AI Context 页面均已接后端。
- 缺口：缺少最近连接复用、表选择记忆、预览筛选、字段库跳转携带筛选条件、修正 SQL side-by-side diff、常用操作快捷入口等细节。
- 落地产物：围绕 SQL 校验、反向导入、字段库和 AI Context 增加小步体验优化：最近配置、筛选状态保留、只看冲突/新增、跳转字段库自动筛选、diff 视图切换。
- 验收标准：用户完成一次反向导入或 SQL 修复后，能顺畅跳转到字段库或相关结果，不需要重复选择项目和筛选条件；`pnpm build` 通过。
- 边界：不做视觉大改版，不引入复杂新状态管理，不改变后端核心模型。

### P5-9：轻量 API Token 管理页面
- 状态：待办。
- 为什么做：安全基线已有 API Token 模式，但目前 token 主要靠手写 SQL 配置；小团队和 AI agent 长期接入时，需要一个轻量页面来创建、禁用和查看授权范围。
- 已有基础：后端已有 token hash 存储、安全模式、项目级授权检查、前端 token 登录/退出、CLI/MCP token 透传和操作者记录。
- 缺口：缺少 token 列表、创建、禁用、项目范围授权、最后使用时间和复制一次性明文 token 的 UI/API。
- 落地产物：新增轻量 token 管理 API 和前端页面；支持创建 token、选择项目范围、复制一次性明文 token、禁用 token、查看最后使用时间和操作者。
- 验收标准：安全模式开启后，用户无需手写 SQL 即可创建 CLI/MCP token；token 明文只在创建时显示；禁用后 CLI/MCP 请求被拒绝。
- 边界：不做复杂 RBAC、审批流、组织成员管理或 token 自动轮换；个人本地开发默认仍可关闭安全模式。

## P6：标准治理与 AI 协作增强

### P6-1：标准版本快照与 AI Context 可复现
- 状态：待办。
- 为什么做：AI 使用字段标准生成 SQL、DDL 或修复建议时，需要明确“使用的是哪一版标准”，否则后续复盘很难判断结果来自规则问题、标准变更还是提示词变化。
- 已有基础：已有标准变更日志、AI Context zip、数据字典、CLI/MCP 和 OpenAPI 契约检查。
- 缺口：缺少命名快照、版本号、导出 hash、AI Context 版本标识和检查记录/生成记录对标准版本的引用。
- 落地产物：新增项目级标准快照模型和轻量创建入口；AI Context、数据字典、CLI/MCP 输出携带 `specVersion` 与内容 hash；SQL 校验、DDL 生成和字段推荐结果可记录使用的标准版本。
- 验收标准：用户能创建当前标准快照；导出的 AI Context 能稳定标明版本和 hash；同一份输入可按指定快照复现字段目录和规则上下文。
- 边界：不做复杂发布审批、多人审核、语义化版本治理或跨项目标准合并。

### P6-2：字段使用覆盖率与未纳管字段盘点
- 状态：待办。
- 为什么做：个人/小团队维护数标时，最有价值的反馈是“真实数据库里哪些字段已经被标准覆盖，哪些还在野生生长”，这能直接指导下一轮补标准。
- 已有基础：已有数据库直连 metadata、SQL 反向导入、字段推荐、差异预览和字段来源追踪待办。
- 缺口：缺少按项目/表/字段统计的标准覆盖率、未知字段清单、重复语义字段和高频未纳管字段排行。
- 落地产物：基于数据库直连或 SQL/DDL 输入生成覆盖率报告；展示标准命中、别名命中、未命中、疑似重复和缺注释字段；前端提供按表和状态筛选。
- 验收标准：连接数据库后能输出字段覆盖率百分比和未纳管字段 Top 列表；用户可从报告跳转到反向导入或字段库补标准。
- 边界：不扫描业务数据行，不做敏感数据采样，不做定时后台同步。

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
- [Model Context Protocol 规范](https://modelcontextprotocol.io/specification/2025-06-18)：AI 应用接入 resources、prompts、tools 的协议基础。
- [`modelcontextprotocol/servers`](https://github.com/modelcontextprotocol/servers)：MCP server 参考实现集合。
- [`agents.md`](https://agents.md/)：面向 coding agent 的项目指令文件约定。
