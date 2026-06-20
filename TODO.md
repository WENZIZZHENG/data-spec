# DataSpec 待办路线图

本文件记录当前仍需行动的产品与工程待办。优先级按用户可感知价值、核心链路阻断程度和后续开发解锁程度排序。

## 下一步顺序

1. 先完成反向导入来源与批次追踪，让直连导入的字段能追溯来源批次和原始 metadata。
2. 继续提升 AI 可消费质量：SQL 精准定位、字段推荐质量、规则测试语料库和 fixedSql golden fixtures。
3. 打磨个人/小团队日常体验：前端高频流程细节、轻量 API Token 管理页面；仍避免过早引入审批流、发布流程等重型治理模型。
4. P5 稳定后推进 P6：标准版本快照、字段覆盖率、AI 回放、业务仓库初始化向导、标准质量评分和轻量影响分析。

## 已完成能力摘要（P0-P4）

P0-P4 的详细背景、方案和验收已归档到 [docs/archive/todo-completed-p0-p4.md](docs/archive/todo-completed-p0-p4.md)。主待办只保留当前仍需行动的 P5 任务。

- P0 AI 可消费主线已完成第一版：AI Context zip、CLI、MCP、个人版字段模型、结构化命名规则和 AI Prompt 生成。
- P1 核心闭环已完成第一版：SQL 校验、OpenAPI 类型契约、COMMENT 解析、前端管理页、字段推荐、结构化修复建议、DDL 生成、检查记录和 fixedSql。
- P2 标准维护与生成能力已完成第一版：内置 standards 初始化、模板 DDL、业务项目 .dataspec/ 约定、数据字典、Excel 导入导出、变更日志和个人工作台。
- P3 自动化与反向导入已完成第一版：SQL 反向导入预览、MySQL DDL 解析、CI/GitHub Action 和 PR 评论式 SQL Review。
- P4 工程化与体验增强已完成第一版：SQL 定位、fixedSql diff、.dataspec/config.json、规则配置表单、OpenAPI 防漂移、Excel dry-run、HTML/ERD、MySQL 规则覆盖、安全基线、演示项目和数据库直连反向导入前端流程。
- 后续真实待办集中在 P5/P6：P5 已完成 dataspec doctor 和数据库二次比对，接下来补齐导入来源追踪、定位精度、字段推荐质量、fixtures、前端细节和轻量 token 管理；P6 再提升标准版本、覆盖率、AI 回放、初始化向导、质量评分和影响分析。

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
- 状态：待办。
- 为什么做：字段进入标准库后需要知道来源，否则后续清理、复盘和字段命中率分析会缺少上下文。
- 已有基础：标准字段已有 CRUD、变更日志和反向导入确认写入流程。
- 缺口：缺少导入批次、来源数据库类型、schema、table、column、导入时间、操作者和原始 metadata 快照。
- 落地产物：新增轻量导入批次记录和字段来源信息；反向导入确认时写入 batchId/source 信息；字段详情或列表可查看来源摘要。
- 验收标准：通过数据库直连导入的字段能追溯到导入批次、schema.table.column 和导入时间；变更日志中能关联本次导入操作；普通手工字段不受影响。
- 边界：不保存数据库密码，不长期保存连接串明文，不做跨项目来源合并。

### P5-5：SQL 定位精度升级与 GitHub inline comment 基础
- 状态：待办。
- 为什么做：当前 line/column/source span 是启发式定位，足够前端跳转和汇总评论，但要支持 GitHub inline review 与更稳定的 AI 修复，需要更可靠的位置映射。
- 已有基础：`LintIssue` 已有 `line/column/sourceStart/sourceEnd`，前端 SQL 校验页可点击跳转，CLI/GitHub Review 已能输出汇总评论。
- 缺口：解析器和规则还没有统一的 source map；fixedSql diff 与 issue 之间没有稳定关联；PR 评论还不能精准落到变更行。
- 落地产物：为表、列和规则 issue 建立统一 source span 解析/回填工具；增强 CLI review-pr 的文件级定位数据；为后续 GitHub inline comment 预留结构化输出。
- 验收标准：good/bad SQL fixture 中表名、字段名、COMMENT、类型问题的位置断言稳定通过；CLI JSON 能输出足够信息供 PR inline comment 使用。
- 边界：第一版不强制接入真实 GitHub inline API，不实现完整 SQL AST source map。

### P5-6：字段推荐质量增强
- 状态：待办。
- 为什么做：DataSpec 优先服务 AI，字段推荐质量直接决定 AI 建表时是否能少犯错；当前确定性匹配可用，但还不够懂个人命名习惯。
- 已有基础：字段推荐 API/CLI/MCP 已支持字段名、显示名、注释、别名、分类和标签匹配，并返回分数、原因和 fallback。
- 缺口：缺少中文同义词、拼音缩写、常见业务词库、泛化词惩罚、敏感字段提示和相近字段区分能力。
- 落地产物：增强字段推荐评分模型和内置词库；支持 `uid/user_id/account_id/member_id`、`phone/mobile/tel/mobile_no`、`amount/price/fee/amount_cent` 等常见语义区分；在推荐结果中输出更可解释的命中原因。
- 验收标准：常见中文业务描述能稳定命中标准字段；错误泛化词得到降权或提示；推荐结果通过单元测试覆盖典型样例。
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
