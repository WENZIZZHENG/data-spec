# DataSpec 已完成待办归档（P5/P6）

归档日期：2026-07-13

本文件从根 TODO.md 机械迁移已完成的 P5/P6 待办详情，根待办只保留入口和当前候选池。每个条目保留原始状态、已完成能力、验证证据、产物、后续增强和边界；原条目未记录 commit 的地方不补猜。

- 完成项数量：136
- 当前未完成主题：9，详见 [P6 候选池](../todo-p6-candidates.md)
- P0-P4 已完成归档：[todo-completed-p0-p4.md](todo-completed-p0-p4.md)

## 历史追加记录（已降级为背景）

1. P6-31 Prompt 模板版本化与效果评测、P6-32 项目活动时间线与轻量审计视图、P6-33 前端任务式导航与空状态收口、P6-34 本地部署与演示数据一键启动包、P6-35 fixedSql 修复策略配置与 dry-run 解释、P6-36 AI 使用画像与任务模式配置、P6-37 标准系统 Schema Registry 与字段契约版本、P6-38 AI 执行证据包与交付归档、P6-39 前端统一数据状态与可恢复错误体验、P6-40 AI/CLI 并发写入幂等与任务锁、P6-41 标准变更 What-if 预览与回滚辅助、P6-42 领域 Starter Kit 与项目模板、P6-43 AI 能力清单与自描述入口、P6-44 前端 URL 状态与可复现操作链接、P6-45 敏感信息脱敏与日志输出边界、P6-46 按变更范围推荐验证命令、P6-47 TODO 到 OpenSpec 的实施交接助手已完成第一版，P6-48 业务术语表与同义词词根库已完成第一版，P6-49 自然语言需求到标准候选草案已完成第一版，P6-50 AI 输出引用证据与 Explain Trace 已完成第一版，P6-51 标准字段生命周期状态机已完成第一版，P6-52 业务仓库变更感知扫描与最小上下文已完成第一版，P6-53 标准健康趋势与改进计划已完成第一版，P6-54 数据库连接健康探测与方言能力画像已完成第一版，P6-55 字段值格式与校验样例库已完成第一版，P6-56 标准字段别名冲突与命名保留字检测已完成第一版，P6-57 反向导入字段映射策略与确认理由已完成第一版，P6-58 AI 任务失败重试与断点续跑已完成第一版，P6-59 标准质量门禁与阈值策略已完成第一版，P6-60 标准字段使用示例与反例库已完成第一版，P6-61 AI 会话启动包与当前上下文握手已完成第一版，P6-62 AI 任务卡与单步可恢复执行协议已完成第一版，P6-63 数据库直连元数据浏览器与候选选择页已完成第一版，P6-64 大库扫描计划、分页预览与取消恢复已完成第一版，P6-65 标准字段智能合并向导已完成第一版，P6-66 前端命令面板与最近操作续跑已完成第一版，P6-67 AI 交接证据看板已完成第一版，P6-68 多项目标准复用包与轻量继承已完成第一版，下一步推进 P6-69 AI 写入安全策略与 dry-run 协议。
2. P6 后续继续补验证建议、TODO 到 OpenSpec 交接、业务术语表、自然语言标准候选、AI 引用证据、字段生命周期、变更感知扫描、健康趋势、数据库连接诊断、命名保留字、反向导入映射、AI 任务重试、质量门禁、示例反例库、AI 会话启动包、AI 任务卡、数据库元数据浏览、大库扫描计划、标准合并向导、AI 写入安全策略、规则调试器、元数据增量缓存、CLI/MCP 兼容握手、前端类型化 API Client、标准演练沙箱、MCP/CLI 工具契约验收、业务对象关系图、派生字段规则、fixedSql 文件补丁、标准问答入口、规则模板 diff 包、浏览器级 E2E、真实数据库集成测试、文档状态一致性、可访问性、本地数据清理和前端性能体验。
3. 新增优化建议已补为 P6-87 到 P6-98：数据库迁移计划、业务代码字段引用、MCP prompt/resource、AI 上下文预算、本地 pre-commit/IDE 检查、标准样例生成、多源契约导入、标准证据置信度、自定义规则 SDK、本地语义检索、标准使用热区和 AI 变更迁移说明。
4. 追加优化建议已补为 P6-99 到 P6-104：只读标准文档站、标准资产依赖图、环境配置漂移检测、数据源连接器注册、本地运行观测诊断和标准决策理由库。
5. 本轮新增 AI 使用优化建议已补为 P6-105 到 P6-110：AI 一页式工作台、表级约束与索引标准、枚举生命周期、业务仓库迁移交付包、AI 能力边界模拟和文档反向提取候选。
6. 本次追加优化建议已补为 P6-111 到 P6-116：标准候选来源管道、候选批量决策、采纳前质量门禁、AI 任务推荐队列、跨来源证据视图和前端端到端引导。
7. 继续追加优化建议已补为 P6-117 到 P6-122：统一变更 Diff、全链路 Trace、版本兼容降级、AI 场景样例、数据模型契约和前端反馈转任务。
8. 最新追加优化建议已补为 P6-123 到 P6-128：标准向 API/DTO Schema 导出、ORM/代码模型候选提取、前端组件状态样例库、规则变异回归、Context 增量更新包和前端包体性能预算。
9. 本次新增优化建议已补为 P6-129 到 P6-134：标准包 Lockfile、SQL 格式化 Profile、脱敏样例采样、标准消费清单、规则覆盖率和前端 mock 演示模式。
10. 本次继续新增优化建议已补为 P6-135 到 P6-140：AI 任务预检、反向导入 plan/apply、配置 Schema、凭据复用、标准变更迁移 recipe 和统一任务结果协议。
11. 本次补充优化建议已补为 P6-141 到 P6-146：AI 输出验证沙箱、多 schema 反向导入合并、Agent 启动包、质量异常归因、前端修复 Action 和个人健康摘要。
12. 本次继续补充优化建议已补为 P6-147 到 P6-152：Schema Registry 可视化、可复用工作流 recipe、标准包同步巡检、业务仓库合规分、字段库密集编辑体验和待办里程碑收束。
13. 本次最新优化建议已补为 P6-153 到 P6-158：AI Context 注入防护、标准消费端 SDK、规则依赖冲突诊断、示例契约快照、单机分发预案和字段可见性策略。
14. 本次补充优化建议已补为 P6-159 到 P6-164：AI 任务断点续跑、标准变更影响预演、字段知识卡、规则/标准 A/B 评测、连接器能力探测和个人安全红线配置。
15. 本次追加优化建议已补为 P6-165 到 P6-170：标准对象稳定标识、AI 输出后置校验、标准查询 DSL、MCP 资源游标分页、前端操作录制和标准维护工作量估算。
16. 本次新增工程化优化建议已补为 P6-171 到 P6-176：数据质量测试导出、标准变更事件流、IDE 提示、跨协议 Schema 导出、指标口径映射和消费端兼容套件。
17. 本次新增优化建议已补为 P6-177 到 P6-182：OpenSpec 准备度评分、MCP 会话状态记忆、业务代码 Patch Plan、数据库采集断点续扫、维护 Inbox 可执行工作流和前端页面对象测试层。
18. 本次继续新增优化建议已补为 P6-183 到 P6-188：数据库 COMMENT 回写计划、中英文命名映射、标准驱动测试数据包、AI Context 质量预算、字段使用契约和标准问答可采纳度。
19. 本次功能探索建议已合并到既有候选：P6-76 数据域与表模板设计器闭环、P6-133 规则/fixture 覆盖报告、P6-140 TaskResult/Evidence 信封、P6-166 AI 输出后置校验、P6-170/P6-181 标准维护工作流、P6-183 COMMENT 回写计划、P6-185 测试数据包、P6-186 Context 可用性评分、P6-187 字段使用契约、P6-176 消费端兼容套件和 P6-46 验证证据闭环；当时不新增 P6-189。2026-07-12 第三轮功能评审发现新的命名解析缺口后，P6-189 已重新用于“确定性数标命名解析与缩写治理”。
20. P6 收束后再回看哪些能力需要从个人/小团队工具升级为团队协作能力。

## 已完成 P5/P6 条目

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
- 状态：已完成第一版，已新增 AI batch run 后端 API、CLI `lint-files` 交付包输出和前端“AI 批量任务”页面。
- 为什么做：AI agent 常见任务不是只检查一条 SQL，而是批量扫描多个 SQL 文件、多个表或一次反向导入结果；需要一个稳定的批处理输出，方便 AI 汇总、修复和交付。
- 已有基础：已有 CLI `lint-files`、PR review、SQL 检查记录、fixedSql、字段推荐、AI Context、数据库直连表选择和 JSON 输出。
- 已完成能力：新增 `ds_ai_batch_run`、`/api/ai-batches/sql-lint`、列表、详情和下载 API；后端同步聚合多条 SQL lint 结果，逐项保留失败并输出 summary、items、issueSummary、fixedSqlSummary、evidence 和 nextActions；CLI `lint-files --delivery-package <json>` 可在保持原 stdout JSON 和退出码兼容的同时写出同构交付包；前端支持查看最近任务、分项结果、问题、fixedSql、下一步动作和下载 JSON。
- 落地产物：新增 `aibatch` 后端模块、V15 Flyway 迁移、前端 `AiBatch.vue` 页面/API/类型/显示工具和 smoke 测试、CLI delivery package builder 与 Node 测试。
- 验收标准：对一个业务仓库执行批量 SQL 检查后，AI 能拿到不含 token/password/Bearer/完整 JDBC URL 的完整 JSON 结果包并继续修复；用户能在前端查看批量任务结果、失败原因和下载交付包。已通过后端目标测试、CLI Node 测试、前端 `pnpm test` 和 `pnpm build`，完整验证随本任务提交记录保留。
- 边界：不做后台长任务平台，不接外部队列，不自动提交业务仓库修改。

### P6-27：AI 使用反馈与标准改进闭环
- 状态：已完成第一版，已新增只读反馈报告 API 和前端“AI 反馈”页面。
- 为什么做：字段推荐、SQL 修复和 DDL 生成会暴露“AI 总是选错哪个字段”“哪些规则最常误报”“哪些标准字段总被补充别名”等问题；这些反馈应反哺标准维护。
- 已有基础：已有字段推荐原因、SQL 检查记录、fixedSql、规则配置、字段来源、标准质量评分和冲突检测待办。
- 已完成能力：基于 AI job、SQL 检查记录、规则例外、反向导入来源和字段元数据聚合 summary、fieldSignals、ruleSignals、fixedSqlSignals、unmanagedSignals、nextActions、sampleSize 和 generatedAt；推荐历史无法可靠统计时显式标记缺口，不伪造命中率；前端支持跳转字段库、字段质量、规则配置、规则例外、SQL 校验和 AI 回放。
- 落地产物：新增 `aifeedback` 后端模块、`/api/ai-feedback/report?projectId=` 只读 API、前端 `AiFeedback.vue` 页面/API/类型/显示工具和 smoke 测试。
- 验收标准：用户能看到标准系统被 AI 使用后的高频问题；能从反馈直接跳转到字段库、规则配置、质量检查或例外管理；统计不采集业务数据行。已通过后端目标测试、前端 `pnpm test` 和 `pnpm build`，完整验证随本任务提交记录保留。
- 边界：不做用户行为监控，不调用外部分析服务，不把反馈自动写成标准变更。

### P6-28：标准候选 Inbox 与采纳工作台
- 状态：已完成第一版，已新增候选持久化表、后端决策 API 和前端“标准候选”工作台。
- 为什么做：覆盖率报告、反向导入、字段推荐未命中和 AI 使用反馈都会产生“可能应该进入标准库”的候选项；如果散落在各页面，用户和 AI 都很难形成持续改进闭环。
- 已有基础：已有反向导入预览、字段覆盖率报告、字段推荐、导入来源批次、字段变更日志和 AI 使用反馈待办。
- 已完成能力：新增 `ds_standard_candidate` 候选表和 `/api/standard-candidates` API；支持手动创建候选、分页筛选、采纳为新字段、合并到已有字段、忽略和延后；候选证据、来源、置信度、状态、目标字段和决策原因会持久化。
- 落地产物：新增 `standardcandidate` 后端模块、V16 Flyway 迁移、前端 `StandardCandidate.vue` 页面/API/类型/显示工具和 smoke 测试。
- 验收标准：用户能在一个页面处理“未纳管字段 -> 标准字段”的采纳流程；AI 能读取候选状态和用户决策，避免反复推荐已忽略项。已通过后端目标测试、前端 `pnpm test` 和 `pnpm build`，完整验证随本任务提交记录保留。
- 边界：不做审批流，不自动合并标准字段，不把 Inbox 变成团队工单系统。

### P6-29：离线 AI Context 与业务仓库缓存模式
- 状态：已完成第一版，CLI 已支持 `export-context --cache` 写入 `.dataspec/context/`，`doctor` 已输出 `context-cache` 诊断。
- 为什么做：AI agent 在业务仓库中工作时，不一定随时能连上 DataSpec 服务；需要一个可提交或可缓存的离线上下文，让 AI 至少能按最近标准进行 lint、检索和生成提示。
- 已有基础：已有 AI Context zip、标准快照、`.dataspec/config.json`、`dataspec doctor`、CLI/MCP 和按历史快照导出待办。
- 已完成能力：`export-context --cache` 会复用现有 AI Context zip，安全解包到业务仓库 `.dataspec/context/`，写入 `cache-metadata.json`，记录导出参数、contentHash、expiresAt 和标准版本/hash/source；`doctor` 能报告 missing/fresh/stale/unreadable/remote-different，并在服务不可用时提示离线只读边界。
- 落地产物：扩展 `tools/dataspec-cli.mjs`、CLI 单测、README 和 OpenSpec 规格；支持 `--output --cache` 并存、`--cache-ttl-days`、危险 zip path 拒绝和 metadata 脱敏。
- 验收标准：后端服务未启动时，AI 仍能读取最近一次缓存的标准上下文；`dataspec doctor` 能报告缓存版本、过期时间和与远端快照的差异。已通过 CLI 目标测试，完整验证随本任务提交记录保留。
- 边界：不缓存 token、数据库密码或业务数据行；离线模式不允许写入 DataSpec 服务端状态。

### P6-30：数据库元数据适配层与离线 schema dump
- 状态：已完成第一版，后端已新增 schema dump 模型、JDBC metadata adapter、dump preview/compare/coverage API 和 PostgreSQL/MySQL fixture。
- 为什么做：数据库直连反向导入、二次比对和覆盖率报告都依赖 metadata 读取；后续支持更多方言或离线排查时，需要把 JDBC 读取、方言映射和标准分析解耦。
- 已有基础：已有 PostgreSQL/MySQL 直连 metadata、反向导入、二次比对、覆盖率报告、多方言兼容矩阵待办和数据库只读安全诊断待办。
- 已完成能力：直连 PostgreSQL/MySQL metadata 会先规范化为 `dataspec-database-schema-dump`，现有直连 preview/compare/coverage 内部复用 dump 转 `TableDef` 路径；新增 `/api/reverse-import/database/dump`、`/api/reverse-import/dump/preview`、`/api/reverse-import/dump/compare` 和 `/api/coverage/dump`。
- 落地产物：抽象 `DatabaseMetadataAdapter` 与 `JdbcDatabaseMetadataAdapter`；新增 dump 模型、PostgreSQL/MySQL fixture、adapter/converter 测试、服务测试和 Controller 委托测试。
- 验收标准：同一份数据库 metadata dump 可复现反向导入候选、覆盖率和差异报告；PostgreSQL/MySQL 适配器有 fixture 覆盖；离线 dump 不包含数据行。已通过后端目标测试，完整验证随本任务提交记录保留。
- 边界：不扫描业务数据，不执行写操作，不一次性承诺所有数据库方言。

### P6-31：Prompt 模板版本化与效果评测
- 状态：已完成第一版，已新增 Prompt template registry、本地评测 API、prompt golden fixture，并让建表 Prompt、SQL 修正 Prompt、SQL lint/fixedSql 和 DDL preview 回放记录引用统一 promptVersion。
- 为什么做：AI Prompt、DDL 生成和 SQL 修复提示会持续调整；如果没有模板版本和样例评测，prompt 改动可能让 AI 输出格式、字段选择或修复策略悄悄退化。
- 已有基础：已有 AI Prompt 生成、DDL 生成、fixedSql、标准快照、AI 输出契约稳定性待办和 AI 生成回放待办。
- 已完成能力：Prompt 模板统一登记 `templateKey`、`promptVersion`、场景、输出格式、必备段落、必备短语和变更说明；`/api/prompt-templates` 可查看模板元数据，`/api/prompt-templates/evaluate` 可本地评测输出；建表/修 SQL Prompt 文本包含版本标记，AI job 记录从 registry 取版本。
- 落地产物：新增 `com.dataspec.prompt` 后端模型、registry、评测服务和 controller；新增 create-table/fix-sql prompt golden fixture；新增 registry/eval 单测，并扩展 Prompt、SQL lint 和 DDL 生成回放版本断言。
- 验收标准：修改 prompt 后能通过 golden diff 看到样例输出差异；AI 作业记录能引用 registry promptVersion；破坏 JSON/Markdown 契约时 `mvn test` 失败。完整验证随本任务提交记录保留。
- 边界：第一版不强制调用外部 LLM，不做复杂在线实验平台，不保存第三方 API key。

### P6-32：项目活动时间线与轻量审计视图
- 状态：已完成第一版，已新增项目活动 API、Dashboard 最近活动时间线、动作类型筛选和详情跳转。
- 为什么做：个人/小团队虽然不需要重审批，但仍需要知道最近谁或哪个 AI/CLI 做了标准快照、导入、字段修改、SQL 检查、token 使用和 Context 导出，便于回滚和排查。
- 已有基础：已有字段变更日志、导入来源批次、标准快照、SQL 检查记录、DDL 生成记录、token lastUsedAt 和项目工作台。
- 完成内容：已按项目聚合字段变更、标准快照、反向导入批次、SQL 检查、AI job/DDL 记录和全项目身份可见的 token 最近使用摘要；metadata 只包含安全摘要，不返回 SQL 原文、token hash/明文或数据库密码。
- 落地产物：新增 `GET /api/projects/{projectId}/activities`、活动 DTO/聚合服务、后端单测、前端 Dashboard 时间线、类型筛选、详情跳转、OpenAPI schema 和前端 smoke 覆盖。当前不会伪造未持久化的 AI Context 导出活动，后续如需展示可独立补轻量导出记录。
- 验收标准：打开项目工作台能看到最近关键活动并跳转详情；AI/CLI 操作能留下可读来源；排查“标准什么时候变了”不再需要翻多张表。已通过 `mvn test`、`pnpm test`、`pnpm build` 和 OpenSpec validate。
- 边界：不做企业级审计留存，不引入复杂权限模型，不记录业务数据内容。

### P6-33：前端任务式导航与空状态收口
- 状态：已完成第一版，已新增工作台任务入口、按项目最近任务记忆、顶部轻量面包屑和源码级 smoke 覆盖。
- 为什么做：功能增多后，左侧按模块导航对熟手可用，但新项目或 AI 辅助操作更需要按任务入口开始，例如“接入项目”“导入现有库”“检查 SQL”“补标准”“导出给 AI”。
- 已有基础：已有工作台、项目选择、字段库、SQL 校验、DDL 生成、反向导入、覆盖率报告、AI Context 和 token 管理页面。
- 已完成能力：工作台按任务展示导入现有库、检查 SQL、生成覆盖率、补标准字段、生成 DDL、导出给 AI 和管理 Token；点击入口会写入浏览器 localStorage 最近任务，按当前项目展示并支持清空；App 顶部基于 route meta 展示“工作台 / 当前页”面包屑；无项目状态保留创建演示项目和项目列表入口。
- 落地产物：更新 `Dashboard.vue`、`App.vue`、`frontendSmoke.test.ts`、README 和 OpenSpec 规格；最近任务只保存 task key、route、projectId、title 和 usedAt，不写入后端。
- 验收标准：用户从首页能直接进入导入数据库、生成覆盖率、校验 SQL、补字段、生成 DDL、导出 AI Context 和管理 Token；新项目没有数据时能按引导创建演示项目或进入项目列表；源码级 smoke 防止入口、localStorage 和面包屑漂移。已通过 `pnpm test`、`pnpm build` 和 OpenSpec validate。
- 边界：不做大规模视觉改版，不替换 Element Plus 体系，不引入复杂工作流引擎。

### P6-34：本地部署与演示数据一键启动包
- 状态：已完成第一版，已新增本地 Docker Compose 启动包和 demo smoke 验证脚本。
- 为什么做：项目优先个人/小团队使用，安装启动成本会直接影响采用；同时 AI agent 和回归测试也需要一个可重复的本地演示环境。
- 已有基础：已有 Flyway、README 启动说明、演示项目初始化、后端/前端验证命令和 Docker 化可扩展空间。
- 已完成能力：`docker-compose.local.yml` 覆盖 PostgreSQL、后端和前端，支持端口覆盖、Flyway、依赖缓存 volume 和本地安全默认值；`tools/dataspec-local-smoke.mjs` 支持 text/json、timeout、server/web/token 参数，等待 web/API、创建或复用演示项目，并验证 dashboard summary 和 SQL lint。
- 落地产物：新增 compose 启动包、smoke 脚本与脚本级测试；README 说明一键启动、手动开发启动、smoke、端口覆盖和本地清理边界。
- 验收标准：新机器可通过 `docker compose -f docker-compose.local.yml up` 启动本地体验环境；`node tools/dataspec-local-smoke.mjs` 能输出可复制的本地 smoke 结果；源码级测试覆盖参数解析、输出结构、敏感信息脱敏和 compose/Vite 代理契约。
- 边界：不做生产级部署方案，不引入 Kubernetes，不替代正式数据库备份。

### P6-35：fixedSql 修复策略配置与 dry-run 解释
- 状态：已完成第一版，已新增请求级 fixedSql 策略、dry-run 预览和机器可读修复计划。
- 为什么做：fixedSql 对 AI 很有用，但自动修复天然有风险；用户需要知道每一次修改来自哪个规则、是否安全、能否关闭某类修复，以及是否只做 dry-run 解释。
- 已有基础：已有 SQL lint、fixedSql、diff 展示、规则配置、检查记录、AI 可读 lint 输出和结构化修复建议。
- 已完成能力：`/api/lint` 支持可选 `fixPolicy`，包含 `GENERATE`、`DRY_RUN`、`DISABLED`、最高风险等级和解释开关；`LintResult` 返回 `fixPolicy`、`fixDryRun`、`fixChanges`、`fixExplanations`、`fixSummary` 和 `fixNextActions`；`LintIssue` 记录 `fixRiskLevel`、`fixChangeType`、`fixStatus`、`fixExplain` 和 `fixReasonCode`，并随检查记录 issues JSON 保存。
- 落地产物：重构 `FixedSqlGenerator` 为计划式输出；命名类 fixer 标记低风险，必备列补齐标记中风险，类型/语义类修复只解释不自动改写；SQL 校验页增加修复模式、最高风险、dry-run 标识、策略摘要和变更列表。
- 验收标准：用户可关闭 fixedSql 或仅请求低风险修复；AI 能读取 safe-only/dry-run 的机器可读解释；前端 fixedSql 区域能显示每个变更对应的规则、风险、before/after 和跳过原因。已通过后端目标测试、前端 `pnpm test` 和 `pnpm build`。
- 边界：不自动写回业务仓库，不承诺所有 SQL issue 都能自动修复，不跳过人工确认。

### P6-36：AI 使用画像与任务模式配置
- 状态：已完成第一版，已新增内置 AI task profiles、API、CLI/MCP/doctor 接入、前端查看切换和 SQL 校验 profile 默认策略。
- 为什么做：DataSpec 的使用者既有人，也有 CLI/MCP/coding agent；不同任务需要不同上下文大小、规则严格度、输出格式和修复策略，如果都靠 prompt 临时说明，AI 很容易拿错上下文。
- 已有基础：已有 `.dataspec/config.json`、AI Context、MCP resources/prompts/tools、workflow recipes 待办、按需 Context 待办和 fixedSql 策略化待办。
- 已完成能力：后端 `/api/ai-profiles` 返回 `create-table`、`sql-fix`、`reverse-import`、`pr-review`、`minimal-context`；`.dataspec/config.json` 支持 `aiProfile/taskType`；CLI `profile list/show`、`lint`、`lint-files`、`export-context` 和 `doctor` 可读取 profile；MCP 暴露 `ai-task-profiles` resource 并支持 profile hint；前端“AI 任务模式”页可查看 profiles、diagnostics、推荐命令和当前选择，SQL 校验页默认跟随当前 profile 的 fixedSql 策略并可手动覆盖。
- 验收标准：AI agent 可根据 profile 自动选择最小上下文和输出格式；`dataspec doctor` 能诊断未知 profile/taskType；OpenAPI/TS 类型、前端 smoke、后端测试和 CLI/MCP 测试覆盖核心契约。
- 边界：不做复杂角色权限，不替代用户 prompt，不保存外部 LLM provider 配置；profile 只是默认建议，不是鉴权或审批策略。

### P6-37：标准系统 Schema Registry 与字段契约版本
- 状态：已完成第一版，已新增只读 Schema Registry API、AI Context registry 文件、CLI/MCP 读取入口、前端 OpenAPI 类型、契约文档和测试覆盖。
- 为什么做：项目优先给 AI 使用后，字段、枚举、规则、模板、快照和 Context 输出本身就是一套数据契约；如果契约没有版本和兼容策略，AI/CLI/MCP 会在字段改名或结构调整时读错。
- 已有基础：已有 OpenAPI 类型生成、AI Context JSON Schema、标准快照、AI contract fixtures 待办和 Prompt 模板版本化待办。
- 已完成能力：服务端 `/api/contracts` 和 `/api/contracts/{contractId}` 返回 registry catalog/detail；内置 `field`、`enum-dict`、`rule-config`、`template`、`standard-snapshot`、`lint-result`、`ai-context-manifest`、`ai-context-field-catalog` 和 `ai-task-profile`；AI Context zip 增加 `.dataspec/schema-registry.json`，manifest 增加 `contracts` 摘要；CLI 支持 `contract list/show/check`，MCP 增加 `schema-registry` resource；前端 `schema.ts` 和 `types/index.ts` 已导出相关类型。
- 验收标准：新增或删除 AI 可消费字段时必须更新契约；契约变更能通过后端 contract tests、AI Context tests、CLI/MCP Node tests 和 OpenAPI TS 类型看出来；README、`docs/ai-contracts.md` 和 `.dataspec/README.md` 已说明兼容策略和非权限边界。
- 边界：不引入重型 schema registry 服务，不要求历史所有导出包完全补齐版本，只从新增入口开始收敛。

### P6-38：AI 执行证据包与交付归档
- 状态：已完成第一版，已新增后端 evidence package JSON/zip API、CLI `evidence export`、MCP `export_evidence_package`、前端复制/下载入口、Schema Registry contract 和契约文档。
- 为什么做：AI 完成一次建表、修 SQL、导入标准或覆盖率分析后，用户需要一份可交付、可复盘的证据包，而不是只看一次页面结果或聊天摘要。
- 已有基础：已有 AI 回放、SQL 检查记录、fixedSql、标准快照、覆盖率报告、DDL 生成记录和项目活动时间线。
- 已完成能力：支持 `SQL_CHECK`、`AI_JOB`、`AI_BATCH_RUN` 和 payload 型 `COVERAGE_REPORT` 生成 `AiEvidencePackage`；zip 固定包含 `evidence.json`、`summary.md` 和 `README.md`；前端 SQL 记录详情、覆盖率报告和 AI 批量任务页可复制 JSON 或下载 zip；CLI/MCP 可机器读取同一结构；Schema Registry 增加 `ai-evidence-package`。
- 验收标准：完成一次 SQL 修复、覆盖率报告或 AI 批量任务后，用户能下载或复制证据包；AI 可把证据包作为交付附件继续传给下游任务；包内不包含数据库密码、token、Authorization、完整 JDBC URL 或业务数据行；相关后端、前端、CLI/MCP、OpenSpec 验证随提交记录保留。
- 边界：不做长期对象存储，不上传第三方服务，不把证据包变成企业审计系统。

### P6-39：前端统一数据状态与可恢复错误体验
- 状态：已完成第一版，已新增 `useRequestState`、`StateBlock`、`ProjectRequired`，并迁移 Dashboard、AI 批量任务、覆盖率报告和 SQL 校验记录区的项目缺失、空数据、失败建议和重试入口。
- 为什么做：功能页越来越多后，项目未选择、接口失败、空数据、加载中、无权限和后端未启动等状态如果各写各的，用户和 AI 自动化都很难判断下一步该点哪里。
- 已有基础：已有工作台、项目选择、字段库、反向导入、覆盖率报告、SQL 校验、AI Context、token 管理和任务式导航待办。
- 已完成能力：统一请求状态会记录 loading、error、suggestedAction、docsRef、retryable 和 lastUpdatedAt；共享状态组件负责项目缺失、空数据、失败重试和建议文案展示；前端 smoke 和 request state 单测已覆盖迁移入口。
- 验收标准：Dashboard、AI 批量任务、覆盖率报告和 SQL 校验记录区在未选择项目、空结果或接口失败时有一致提示与重试入口；后续页面可按同一模式渐进迁移。
- 边界：不做大视觉改版，不替换 Element Plus，不一次性重写所有页面。

### P6-40：AI/CLI 并发写入幂等与任务锁
- 状态：已完成第一版，已新增单机 `WriteGuardService`、`Idempotency-Key` header 接入、项目级 operation try-lock、AI job 稳定指纹去重和 CLI `--idempotency-key` / `DATASPEC_IDEMPOTENCY_KEY` 透传。
- 为什么做：当多个 coding agent、CLI 或前端页面同时操作一个项目时，可能重复创建快照、重复导入候选、重复生成记录或覆盖彼此的配置；个人工具也需要基础幂等保护。
- 已有基础：已有标准快照、导入批次、SQL 检查记录、AI 作业回放、token 管理和项目活动时间线待办。
- 已完成能力：标准快照创建、数据库反向导入确认、AI 批量 SQL lint、项目恢复 apply 已支持幂等 key 和项目级锁；AI job 回放记录按项目、jobType、promptVersion、输入输出 payload、标准快照和关联 SQL 检查记录生成稳定指纹去重；锁冲突返回 retryable 的 AI/CLI 可读诊断。
- 验收标准：同一个 key 重复提交不会产生重复数据；并发同项目同 operation 写入会得到明确 retryable 冲突；`mvn test` 与 CLI 测试覆盖 guard、目标服务接入和 header 透传。
- 边界：第一版为单 JVM 内存缓存，服务重启后不保留历史幂等；不引入外部队列，不做分布式事务，不阻塞普通单条 CRUD。

### P6-41：标准变更 What-if 预览与回滚辅助
- 状态：已完成第一版，已新增标准变更 what-if 预览 API，字段编辑、规则编辑和规则启停会在保存前展示 diff、风险、影响项、验证命令、当前快照和回退提示。
- 为什么做：字段、规则、模板或分组变更会影响 AI Context、DDL 生成、SQL lint 和字段推荐；用户在保存前应该能预览影响，保存后也要知道如何回退到上一个可信状态。
- 已有基础：已有字段变更日志、标准快照、轻量字段影响分析待办、备份恢复待办和规则配置。
- 已完成能力：字段更新预览复用字段影响分析，输出属性变更、模板/SQL/快照/代码集影响、风险等级、`dataspec-cli` 验证命令和字段变更日志回退提示；规则更新/启停预览输出 SQL lint、AI Context 和规则基线影响；前端字段库和规则配置页已接入保存前确认。
- 验收标准：修改字段类型、状态、别名或规则前能看到影响摘要；保存后可通过变更日志和相关快照辅助回退；AI 能读取预览结果判断是否需要用户确认。已通过后端单测、前端测试、前端 build 和 OpenSpec change 校验。
- 边界：不做强审批流，不自动回滚数据库，不阻止个人快速保存。

### P6-42：领域 Starter Kit 与项目模板
- 状态：已完成第一版，已新增内置领域 Starter Kit、项目应用 API、前端创建/补装入口、安装记录和 AI Context 来源导出。
- 为什么做：新项目从空字段库开始成本高，尤其是用户想快速让 AI 写订单、用户、支付、库存、审计等常见表时，需要可选的领域初始标准。
- 已有基础：已有内置 standards 初始化、演示项目、规则基线、字段/枚举/模板 CRUD、项目备份迁移包、字段推荐和 AI Context。
- 已完成能力：内置 `user_account`、`order_trade`、`payment_amount`、`inventory_catalog` 和 `audit_log` 五个领域包；应用时创建缺失的数据域、枚举、标准字段和表模板，已有对象会跳过；字段 tags 会记录 `starter:<kitKey>@<version>`，AI Context 字段目录输出 `starterKitSources`。
- 落地产物：新增领域 starter kit；提供电商/订单、用户账号、支付金额、审计日志等小型可组合模板；创建项目或导入时可选择，所有字段标记来源和版本。
- 验收标准：新建项目可选择一个或多个 starter kit 快速生成可用标准；AI Context 能标明字段来自哪个 kit；重复安装不会覆盖用户修改。
- 边界：不追求行业全量模型，不强制用户采用模板，不把 starter kit 当作企业主数据标准。

### P6-43：AI 能力清单与自描述入口
- 状态：已完成第一版，已新增只读 capability catalog API、CLI `capability list/show/check`、MCP `capability-catalog` resource，并把 `.dataspec/capabilities.json` 写入 AI Context zip 与离线 cache。
- 为什么做：DataSpec 的 API、CLI、MCP、前端页面越来越多，AI agent 需要先知道“当前项目有哪些可用能力、参数、前置条件和失败恢复方式”，而不是每次从 README 或源码里猜。
- 已有基础：已有 OpenAPI、CLI help、MCP resources/prompts/tools、`dataspec doctor`、工作流模板待办和 AI 可读错误码待办。
- 已完成能力：能力清单覆盖 capability-catalog、doctor、export-ai-context、lint-sql、字段检索/推荐、DDL 生成、反向导入、覆盖率、Schema Registry、证据包、workflow/profile 和 Starter Kit；每项包含 API/CLI/MCP/前端入口、输出契约、示例、preflightChecks、writeRisk 和 nextActions。
- 后续增强：能力清单仍是代码内置 registry；MCP prompt/resource 引导已由 P6-89 补齐，若后续要做动态插件、前端能力页或任务预检，可分别承接 P6-105、P6-109 和 P6-135。
- 验收标准：AI agent 在未知项目中可先调用能力清单，再决定运行 doctor、导出 Context、lint、推荐字段或反向导入；清单与 OpenAPI/CLI/MCP 的关键入口保持一致并有后端、CLI/MCP 和 AI Context cache 测试覆盖。
- 边界：不替代完整文档，不自动执行任务，不把实验性内部接口声明为稳定能力。

### P6-44：前端 URL 状态与可复现操作链接
- 状态：已完成第一版，已新增 URL 状态工具、全局 `projectId` 联动、字段库/SQL 检查记录/AI 回放/覆盖率/反向导入页面复制链接和无效参数兜底。
- 为什么做：前端已覆盖多条工作流，但页面状态多靠本地内存或 localStorage；用户和 AI 需要可复制的链接来复现一次筛选、检查记录、导入批次、覆盖率结果或 AI 回放详情。
- 已有基础：已有路由、项目选择、SQL 检查记录、反向导入来源批次、覆盖率报告、AI 回放、字段库 query 筛选和前端状态统一待办。
- 已完成能力：列表筛选、详情抽屉、项目 ID、记录 ID、表名、状态筛选等安全状态已统一映射到 query；复制链接到新标签页可恢复同一项目、筛选条件和详情记录。
- 落地产物：已为关键页面补 URL 状态协议和复制链接入口；覆盖字段库筛选、SQL 检查记录详情、AI 回放详情、覆盖率表筛选和反向导入批次详情；入口参数无效时给出可恢复提示。
- 验收标准：复制链接到新标签页可复现同一个项目、筛选条件和详情记录；AI/browser automation 可通过 URL 直达目标状态；前端测试覆盖 query 解析和无效参数兜底。
- 边界：不把敏感连接信息、SQL 原文、token 或密码写入 URL；不重做整体路由体系。

### P6-45：敏感信息脱敏与日志输出边界
- 状态：已完成第一版，已新增后端公共 `SensitiveDataSanitizer`，并接入错误响应、AI evidence package、数据库直连诊断、项目备份扫描和 CLI 本地交付包脱敏测试。
- 为什么做：数据库直连、token、AI 回放、执行证据包和错误诊断都会处理连接信息或输入 payload；需要统一保证日志、前端提示、证据包和记录详情不泄漏密码、token、完整连接串或高敏业务片段。
- 已有基础：已有 API Token hash 存储、不保存数据库密码约束、反向导入本地记忆剔除敏感字段、备份/证据包不含敏感信息的待办边界和安全基线。
- 已完成能力：脱敏规则已从多处局部正则收敛到统一后端 sanitizer；CLI 侧递归清洗敏感 key；README 已明确允许持久化的非敏感元数据和禁止输出字段。
- 落地产物：新增敏感信息 sanitizer 工具和跨模块使用规范；覆盖 JDBC URL、password、token、Authorization、连接表单、AI payload、错误响应、回放摘要和导出包。
- 验收标准：核心 API 和 CLI 错误路径不会输出明文密码/token；含敏感字段的 fixture 通过脱敏测试；README 或安全文档明确记录哪些字段可持久化、哪些只能内存使用。
- 边界：不做企业级密钥托管，不扫描业务数据行，不承诺识别所有自然语言敏感内容。

### P6-46：按变更范围推荐验证命令
- 状态：已完成第一版，已新增本地验证建议脚本、JSON/text 输出、README 入口和 AGENTS 片段提示。
- 为什么做：当前验证命令分散在 README、OpenSpec tasks 和开发习惯里；AI 改完代码后需要根据触碰模块自动知道该跑 `mvn test`、`pnpm test/build`、`node --test`、OpenSpec validate 还是契约检查。
- 已有基础：已有 README 验证小节、OpenSpec change tasks、后端/前端/CLI 测试入口、AI contract fixtures 待办和执行证据包待办。
- 已完成能力：`tools/dataspec-verify-advisor.mjs` 支持 `--path`、`--changed`、`--format text|json`，根据后端、前端、CLI/MCP、OpenSpec、文档、Docker/local smoke 等路径推荐命令、原因、工作目录、预计耗时和下一步；`dataspec init --with-agents` 生成片段会提示该入口。
- 落地产物：新增验证建议脚本和单测，README 验证小节已加入建议命令，AGENTS 片段保留 doctor/lint/context 指引并补充验证建议入口。
- 验收标准：修改后端、前端、CLI、OpenSpec、README/TODO 等不同路径时能得到合理验证建议；脚本和文档被 README 与 AGENTS 片段引用；关键规则有 Node 测试覆盖。
- 后续增强：把验证建议推进到证据包闭环，生成 validation evidence manifest，记录命令、cwd、推荐原因、预期结果、执行结果占位或结果摘要、失败 nextAction；不自动伪造通过结果。
- 边界：不替代 CI，不强制所有改动跑全量测试，不执行破坏性命令。

### P6-47：TODO 到 OpenSpec 的实施交接助手
- 状态：已完成第一版，已新增本地 TODO 到 OpenSpec 草稿生成脚本、JSON/text 输出和覆盖保护。
- 为什么做：主待办已经积累大量 P6 任务，真正开工时仍需要把“为什么做、已有基础、缺口、产物、验收、边界”手动转成 OpenSpec proposal/design/spec/tasks；AI 很容易漏掉边界或重复造需求。
- 已有基础：已有结构化 TODO、OpenSpec-first 流程、多个已完成 change、OpenSpec 收口待办和 AGENTS/SDD 规则。
- 已完成能力：`tools/dataspec-todo-openspec-handoff.mjs` 可读取指定 P6 条目，提取标题、状态、为什么做、已有基础、缺口、落地产物、验收标准和边界；支持 `--dry-run`、`--format json`、`--change`、`--capability`、`--force` 和自定义输出目录。
- 落地产物：生成 `.openspec.yaml`、`proposal.md`、`design.md`、`specs/<capability>/spec.md` 和 `tasks.md` 草稿，输出 openQuestions 和 nextActions；默认拒绝覆盖已有 change。
- 验收标准：选择一个 P6 待办后，可快速生成符合项目格式的 OpenSpec 草稿；生成内容保留原待办边界和验收标准；关键路径已有 Node 测试覆盖。
- 边界：不自动实现代码，不自动归档 change，不把模糊待办强行变成无需确认的需求。

### P6-48：业务术语表与同义词词根库
- 状态：已完成第一版，已新增项目级业务术语表模型/API/前端维护页，并接入字段推荐、字段检索和 AI Context glossary 导出。
- 为什么做：字段别名散落在单个字段上后，AI 很难稳定理解“用户/账号/会员”“手机号/电话/mobile”“金额/费用/price”等项目级术语关系；需要一层轻量术语表来提升推荐、检索和 Context 裁剪质量。
- 已完成能力：新增 `ds_business_glossary` Flyway 迁移、后端 CRUD/list/conflicts/match/contextExport 服务与 `/api/glossary` 接口；字段推荐和字段标准检索会用启用术语表增强自然语言命中，并输出 `术语表` 命中原因；AI Context `field-catalog.json` 会导出 bounded glossary，超限时写入 warning；前端“基础数据 / 业务术语表”支持列表、筛选、新建、编辑、启停、删除和冲突摘要。
- 验收标准：AI 查询“会员手机号”“订单费用”等自然语言时能稳定映射到对应标准字段集合；术语冲突可被检测并提示；导出的 Context 包含精简 glossary。已通过后端目标测试和前端 `pnpm build`，完整验证随本任务提交记录保留。
- 边界：不做企业级本体/知识图谱，不引入向量数据库，不自动覆盖字段已有别名。

### P6-49：自然语言需求到标准候选草案
- 状态：已完成第一版，已新增只读需求草案 API 和前端“校验与生成 / 需求草案”入口，可从自然语言建表描述输出标准字段、缺失候选、歧义点、推荐模板、下一步和可复制 Prompt。
- 为什么做：用户和 AI 常从“我要建一个订单表/会员表/支付流水表”这类自然语言开始；系统应先把需求拆成可选标准字段、缺失候选和歧义点，再生成 DDL 或 Prompt。
- 已完成能力：新增 `requirementdraft` 后端模块和 `/api/requirement-drafts`；服务复用字段推荐、字段检索和表模板，输出 `matchedFields`、`missingCandidates`、`ambiguousTerms`、`recommendedTemplate`、`nextActions` 和 `copyablePrompt`；前端页面支持输入业务描述/目标表名/分组提示，展示草案、复制 Prompt/候选 Payload，并跳转 DDL 预览或标准候选 Inbox。
- 验收标准：输入一段建表需求后，系统能列出建议采用的标准字段、需要新增的候选字段和不确定问题；结果可继续进入 DDL 预览或标准候选 Inbox。已通过目标后端测试和前端 `pnpm build` 验证。
- 边界：第一版不调用外部 LLM，不新增持久化表，不自动写入字段库或标准候选 Inbox，不承诺完整领域建模，只做确定性检索和模板化草案。

### P6-50：AI 输出引用证据与 Explain Trace
- 状态：已完成第一版，字段推荐、字段标准检索和自然语言需求草案已输出轻量 `ExplainTrace` evidence，前端需求草案页可展示证据来源。
- 为什么做：AI 采用某个字段、规则或 fixedSql 修复时，用户需要知道依据来自哪个标准字段、规则、快照、术语或质量诊断；没有证据链时，AI 输出很难复盘和信任。
- 已有基础：已有标准快照、字段推荐原因、SQL 检查记录、fixedSql、AI 回放、字段质量评分和执行证据包待办。
- 已完成能力：新增 `ExplainTrace` 契约，字段推荐和字段检索输出 `evidence[]`，需求草案的标准字段/歧义候选/模板输出 `evidence[]`，缺失候选保留字符串 `evidence` 并新增 `evidenceTrace[]`；前端需求草案页展示来源类型、来源 ID、置信度和命中原因。
- 后续增强：SQL lint/fixedSql、DDL、Prompt、Context 和 AI 回放的全量 evidence 接入仍留给后续专项。
- 验收标准：字段推荐、字段检索和需求草案中的关键字段都有可读证据；缺失候选能说明来源和置信度；契约有测试防漂移。
- 边界：不引入完整分布式 tracing 平台，不记录业务数据行，不把 evidence 作为强审批依据。

### P6-51：标准字段生命周期状态机
- 状态：已完成第一版，已定义 `draft/enabled/deprecated/disabled` 轻量生命周期，新增结构化 `replacementFieldId/replacementReason`，并接入字段推荐、字段检索、AI Context、标准快照、质量评分和前端字段库维护。
- 为什么做：字段已有状态，但 AI 使用时更需要明确哪些字段是草稿、可用、废弃、停用，以及废弃字段应替换成什么；否则 AI 可能继续推荐历史字段。
- 已有基础：字段模型已有 status、变更日志、标准快照、质量评分中的废弃说明检查和字段推荐。
- 已完成能力：新增 Flyway V19、字段模型/API/前端表单与类型字段；保存时校验替代字段同项目且不能指向自身；字段推荐和默认检索只返回 enabled 字段；显式 status 检索非 enabled 字段时输出替代提示；AI Context、Schema Registry、标准快照和质量评分读取生命周期字段；字段库支持状态筛选、草稿状态、替代字段和替代说明维护。
- 后续增强：DDL 生成器当前以表模板字段为直接来源，未在本轮强制读取模板字段关联标准字段的生命周期；若后续做“模板字段引用标准字段”的硬约束，应作为字段使用契约专项补充。
- 验收标准：废弃字段不会被默认推荐给 AI；需要保留历史兼容时能说明原因和替代字段；字段状态变更进入变更日志和快照。已通过 OpenSpec 校验、后端全量测试、前端 smoke/build 和本地结构化评审。
- 边界：不做审批流，不做组织级发布治理，不阻止个人快速维护标准；第一版不强制每个非 enabled 字段必须填写替代字段。

### P6-52：业务仓库变更感知扫描与最小上下文
- 状态：已完成第一版，新增 `changed` 与 `lint-changed` CLI 工作流，可基于业务仓库 git 变更和 `.dataspec/config.json` 的 `defaultPaths` 输出 AI 可读的变更文件、SQL 子集、最小 Context 建议、lint 汇总和可恢复诊断。
- 为什么做：AI 在业务仓库中工作时，通常只需要处理本次 git diff 中的 SQL、迁移文件或模型文件；如果每次都扫描全仓和导出完整 Context，会浪费上下文并增加误报。
- 已有基础：已有 `dataspec init`、`.dataspec/config.json` 默认路径、`doctor`、`lint-files`、`review-pr`、AI Context 导出和按需裁剪待办。
- 已完成能力：`changed --format json|text` 合并 tracked diff、staged diff 和 untracked 文件，按 `defaultPaths` 过滤，输出 `files.sql/files.other/files.ignored`、summary、`scope=changed` 的 `export-context --cache` 建议和 nextActions；`lint-changed --format json` 只 lint changed SQL 文件，复用现有 `/api/lint`、profile、token 和 idempotency-key 机制；无 git 仓库、无 `defaultPaths`、无变更或无 SQL 变更时返回结构化诊断且不调用服务端。
- 后续增强：可再把 changed workflow 暴露到 MCP/workflow recipe，或增加按文件 diff 内容提取 table/field query 的更精准策略。
- 验收标准：在业务仓库改动少量 SQL 文件后，AI 可一条命令拿到变更文件列表、对应 lint 结果和最小标准上下文；无 git 仓库或无变更时有可恢复提示。已通过 OpenSpec 校验、CLI 契约测试和本地结构化评审。
- 边界：不自动修改业务代码，不自动提交，不扫描未配置的大型目录；第一版不自动导出 Context 包，只输出推荐命令。

### P6-53：标准健康趋势与改进计划
- 状态：已完成第一版，新增项目级标准健康快照、趋势 API、AI 可复制改进计划和前端“标准健康”页面。
- 为什么做：字段质量、覆盖率、AI 使用反馈和规则误报都是持续改进信号；只看单次报告无法判断标准系统是否真的越来越好。
- 已有基础：已有字段质量评分、覆盖率报告、AI 回放、检查记录、候选 Inbox、项目活动时间线和执行证据包待办。
- 已完成能力：新增 `ds_standard_health_snapshot`，可手动创建快照；快照聚合字段质量、AI 反馈、候选状态、可选覆盖率摘要、fixedSql 机会和 Top actions；趋势接口返回最近快照、本周/月 delta 和空状态 nextActions；计划接口输出 Markdown 与结构化动作；前端支持创建快照、查看趋势、填写覆盖率摘要、打开改进目标和复制计划。
- 验收标准：用户能看到本周/本月标准质量和覆盖率变化；AI 可读取 Top actions 并按优先级补字段、修别名或调整规则；趋势 payload 只保存统计、字段名和动作，不保存 SQL 原文、数据库连接串、token、密码或业务数据行。已通过后端目标测试、前端 smoke/build、OpenSpec 校验和本地结构化评审。
- 边界：不做组织 KPI，不接外部 BI，不采集用户行为监控；第一版不自动调度采集，覆盖率摘要由用户或 AI 在创建快照时传入。

### P6-54：数据库连接健康探测与方言能力画像
- 状态：已完成第一版，扩展数据库连接测试响应，新增连接健康诊断、方言能力画像和前端诊断展示。
- 为什么做：数据库直连反向导入和二次比对已经成为核心入口，AI 在调用前需要知道连接是否可用、权限是否只读、当前库支持哪些 schema/comment/index 元数据能力，而不是只收到一个泛化失败。
- 已有基础：已有 PostgreSQL/MySQL 直连、表列表、metadata 预览、compare、只读安全诊断待办和数据库元数据适配层待办。
- 已完成能力：`/api/reverse-import/database/test` 保留 `security`，新增 `health`；成功连接返回 connectionStatus、latencyMs、databaseProduct、version、dialect、schema/comment/index capability、readonlyCheck、requiredPrivileges、warnings 和 nextActions；失败连接返回稳定 failureCategory、retryable、脱敏 message 和恢复建议；反向导入页和覆盖率页展示连接健康、失败分类、方言能力、所需权限和下一步动作。
- 落地产物：新增连接健康/方言能力 DTO、后端分类逻辑、OpenAPI 类型、前端诊断工具、两处直连页面展示和前端 smoke 覆盖。
- 验收标准：有效连接、错误密码、权限不足、schema 不存在、网络不可达等场景都有明确诊断；不持久化 password/token/JDBC URL；AI 能据此判断是否可以继续反向导入或二次比对。已通过后端全量测试、前端测试/build、OpenAPI schema 生成、OpenSpec 校验和本地结构化评审。
- 边界：不做长期监控，不保存敏感连接凭据，不替代数据库安全审计。

### P6-55：字段值格式与校验样例库
- 状态：已完成第一版，已新增字段格式约束列、AI Context `format` 导出、字段质量提示和前端字段库维护入口。
- 为什么做：字段名和数据类型不足以让 AI 稳定生成正确 SQL，像金额单位、手机号格式、日期时区、JSON 结构、状态码取值都需要结构化表达。
- 已有基础：字段已有 dataType、exampleValue、sensitive、codeSetId、质量评分和 AI Context 导出。
- 已完成能力：新增 `ds_field` 格式约束扩展，支持 `formatType`、`formatPattern`、`formatUnit`、`formatPrecision`、`formatTimezone`、`formatNullPolicy`、`validExamplesJson`、`invalidExamplesJson` 和 `formatNotes`；字段创建/编辑/回退、标准变更预览、标准快照、备份恢复和 OpenAPI 类型已接入；AI Context 的 `DATABASE_RULES.md`、`field-catalog.json` 和 schema 会导出字段 `format` 对象；字段质量评分会对金额、手机号、邮箱、时间戳、日期、JSON、状态/枚举/编码等格式敏感字段提示 `format_examples_missing`；前端字段库可维护值格式、正例和反例。
- 验收标准：金额、手机号、邮箱、时间戳、JSON、状态码等字段能导出稳定格式说明；AI 生成 SQL/DDL 时能看到单位和格式约束；质量评分能提示关键字段缺少格式样例。已通过相关后端测试、前端测试/build、OpenAPI schema 生成/检查和 OpenSpec 校验。
- 边界：不扫描真实业务数据行，不强制所有字段配置正则，不引入完整数据质量执行引擎。

### P6-56：标准字段别名冲突与命名保留字检测
- 状态：已完成第一版，已扩展字段冲突报告和 AI Context 命名风险摘要。
- 为什么做：字段冲突检测已能发现标准库内部重复，但 AI 真实建表还会遇到 SQL 方言保留字、跨字段别名歧义和大小写/引用规则差异，这些会导致生成结果不可执行或含义不清。
- 已有基础：已有字段冲突检测、字段推荐、SQL lint、多方言兼容矩阵待办和字段命名规则。
- 已完成能力：字段冲突报告新增 `RESERVED_WORD`、`DANGEROUS_SQL_NAME`、`CASE_COLLISION` 和 `AMBIGUOUS_ALIAS`，内置 PostgreSQL/MySQL/通用 SQL 高频保留字和危险词，输出方言证据、涉及字段和替代命名建议；前端字段冲突页支持新增类型筛选和命名风险计数；AI Context 的 `DATABASE_RULES.md` 会导出命名风险摘要。
- 验收标准：`order`、`user`、`type` 等高风险命名能按方言给出提醒；同一个别名指向多个标准字段会提示 AI 不应直接采用；报告可在前端和 AI Context 中读取。已通过字段冲突/AiContext 后端测试、OpenAPI schema 生成/检查和前端测试。
- 边界：不自动重命名已有字段，不追求覆盖所有数据库方言，不阻止用户保留历史兼容字段。

### P6-57：反向导入字段映射策略与确认理由
- 状态：已完成第一版，已新增反向导入字段级 mapping decision、确认/忽略理由和批次决策查询。
- 为什么做：数据库直连反向导入不仅要把新字段写进标准库，还应解释每个真实字段为什么匹配到某个标准字段、为什么成为候选、为什么被忽略，方便 AI 和用户复盘。
- 已有基础：已有反向导入预览、候选确认导入、字段来源与批次追踪、覆盖率报告和字段推荐原因。
- 已完成能力：新增 `ds_reverse_import_decision` 迁移和 `/api/reverse-import/decisions` 只读查询；数据库直连预览会输出 `EXISTING_MATCH`/`NEW_CANDIDATE` 映射决策，确认导入会记录 `IMPORTED`、`SKIPPED_EXISTING` 和 `IGNORED`；前端候选表支持填写确认理由，导入结果展示 mapping decision 摘要。
- 验收标准：导入批次详情能解释 `mobile_no` 为什么映射到某个标准手机号字段；忽略字段不会反复出现在同一批次的待处理列表；AI 可读取映射历史避免重复建议。已通过反向导入后端测试、OpenAPI schema 生成、前端测试和前端构建。
- 边界：不做审批流，不强制每个字段都人工填写长说明，不自动覆盖已有标准字段定义。

### P6-58：AI 任务失败重试与断点续跑
- 状态：已完成第一版，已新增 AI task run 持久化、AI batch 首批接入、CLI/MCP 查询和前端可恢复任务入口。
- 为什么做：AI 批量 lint、Context 导出、覆盖率扫描、反向导入比对和证据包生成都可能耗时或失败；缺少重试和断点续跑会让 agent 只能从头再跑，容易重复写入或浪费上下文。
- 已有基础：已有 AI 作业回放、AI 批量任务、执行证据包、AI 可读错误诊断、CLI/MCP 和单机幂等写保护。
- 已完成能力：新增 `ds_ai_task_run`、`/api/ai-task-runs`、`/recent-failures` 和详情 API；记录 taskType、status、source、inputHash、idempotencyKey、stepStatus、retryable、failedStep、resumeCommand、nextAction、partialArtifacts、expiresAt 和脱敏 metadata；AI batch SQL lint 会在 delivery package 中返回 taskRun 摘要；CLI 新增 `task list/failures/show`；MCP 新增 `ai-task-runs` resource 和 `get_ai_task_run` tool；AI 批量任务页展示最近可恢复任务、绑定 task run 和复制恢复命令。
- 验收标准：一次批量操作中途失败后能看到失败步骤和可恢复命令；重复重试不会产生重复 task run；AI 能根据 retryable 字段判断继续还是提示用户。已通过后端 task run/AI batch/evidence 定向测试、CLI/MCP node tests、OpenAPI 生成和前端冒烟测试。
- 边界：不引入外部队列，不做分布式调度，不把所有同步 CRUD 都改造成异步任务。

### P6-59：标准质量门禁与阈值策略
- 状态：已完成第一版，已新增项目级质量门禁配置/评估 API、CLI `quality-gate check` 和前端“标准健康”状态入口。
- 为什么做：字段质量、覆盖率和 lint 结果已经能被查看，但 CI/AI 自动化还缺少“低于什么阈值就阻断”的项目级策略，否则质量退化只能事后人工发现。
- 已有基础：已有字段质量评分、覆盖率报告、SQL lint、CI/GitHub Review、规则配置和验证建议待办。
- 已完成能力：新增 `ds_standard_quality_gate` 项目级配置表，支持 `minCoverage`、`minAverageFieldScore`、`maxErrorIssues`、`maxNewUnmanagedFields`、`requiredSensitiveMarking` 等阈值；评估结果输出 `PASS/FAIL/DISABLED`、summary、checks、failedChecks、nextActions 和 evaluatedAt。
- 落地产物：后端 `/api/quality-gate/config` 与 `/api/quality-gate/evaluate`；CLI `node tools/dataspec-cli.mjs quality-gate check --project 1 --format json`，PASS=0、FAIL=1、参数/服务错误=2；前端“标准健康”页展示门禁状态、失败/告警/通过/跳过数量、阈值实际值和修复跳转。
- 验收标准：业务仓库 CI 可基于项目阈值失败或通过；前端能显示当前门禁状态；AI 能读取失败项并按优先级修复字段质量、覆盖率或 SQL。
- 边界：不做企业审批流，不默认阻断个人本地保存，不把门禁阈值硬编码到规则实现里。

### P6-60：标准字段使用示例与反例库
- 状态：已完成第一版。
- 为什么做：AI 更擅长从具体例子学习，只有字段说明和规则文本时仍可能误用字段；需要为关键字段、规则和表模板提供可裁剪的 good/bad examples。
- 已有基础：字段 exampleValue、SQL good/bad fixture、规则说明、Prompt 模板、AI Context 和 golden fixtures。
- 已完成能力：新增项目级 `ds_standard_usage_example` 表、后端 `/api/usage-examples` CRUD、前端“示例与反例库”维护页和 OpenAPI 类型；支持 `FIELD/RULE/TEMPLATE/GENERAL` scope、`GOOD/BAD` exampleType、input、expectedOutput、antiPattern、reason、tags、priority 和启停状态；保存和导出前会拒绝明显 secret、Bearer、password 或 JDBC URL。
- AI Context：`.dataspec/usage-examples.json` 和 `field-catalog.json` 会导出按优先级裁剪的启用示例，完整包包含字段/规则/模板/通用示例，按需包优先保留匹配字段示例，并只带入命中 query 的通用、规则或模板示例；`README.md` 和 `AGENTS.md.fragment` 会提示 AI 优先模仿 `GOOD`、避开 `BAD`。
- 验收标准：AI Context 中能携带少量高价值字段使用例子和反例；DDL/Prompt 生成可以引用示例；新增示例有 service/controller/export/repository wrapper 测试和前端 smoke 防止格式漂移。已通过后端全量测试、前端测试/build、OpenAPI schema 检查、OpenSpec strict 校验和独立 agent review。
- 边界：不采样真实业务数据，不导出敏感值，不把示例库扩成完整教程或大文档。

### P6-61：AI 会话启动包与当前上下文握手
- 状态：已完成第一版，已新增 `/api/bootstrap/session`、CLI `bootstrap`、MCP `session-bootstrap` resource 和 `get_session_bootstrap` tool；业务仓库 `dataspec init` 生成的 `.dataspec/README.md` 和 AGENTS 片段会提示 AI 新会话先读取启动包。
- 为什么做：AI agent 每次进入业务仓库时都需要重新判断 DataSpec 服务、项目、token、标准版本、可用工具和下一步命令；这些信息分散在 `doctor`、README、AI Context 和 MCP 中，容易漏读。
- 已有基础：已有 `dataspec init`、`doctor`、AI Context、MCP resources/tools、标准快照和 API Token。
- 已完成能力：启动包输出 `projectId/server/authMode/specVersion/standardSnapshot/availableCapabilities/recommendedCommands/knownRisks/docsRefs/checks/nextActions`；服务不可达时 CLI 返回本地 `BLOCKED` fallback JSON；未选择项目、未版本化标准快照等场景会给结构化 nextActions。
- 落地产物：新增后端 bootstrap DTO/service/controller、OpenAPI TS 类型、CLI/MCP 入口和测试；能力清单新增 `session-bootstrap` 条目；README 记录 AI 新会话第一跳。
- 验收标准：AI 在新会话中一条命令即可知道当前能否 lint、导出 Context、反向导入或生成 DDL；服务未启动、token 无效、未选择项目等场景都有结构化 nextActions。已通过后端定向测试、CLI/MCP Node 测试和 OpenSpec strict 校验；完整验证见本次 OpenSpec 归档记录。
- 边界：不调用外部 LLM，不自动执行写操作，不把明文 token 或数据库密码写入启动包。

### P6-62：AI 任务卡与单步可恢复执行协议
- 状态：已完成第一版，已新增本地 `dataspec-ai-task-card` JSON/Markdown 协议、CLI `task-card create/show/update`、MCP `create_task_card/render_task_card` tool 和前端 `taskCardDisplay` 展示工具。
- 为什么做：P6-11 的工作流模板解决“应该怎么做”，但 AI 真正执行时还需要知道当前任务做到哪一步、缺哪些输入、失败后从哪一步恢复，否则长任务容易重复执行或越界。
- 已有基础：已有 AI 回放、执行证据包待办、AI 任务重试待办、CLI/MCP 工作流模板和 OpenSpec tasks 习惯。
- 已完成能力：任务卡可从 `create-table/review-pr-sql/reverse-import-standards/export-min-context/standard-evidence-review/standard-maintenance` workflow recipe 生成，包含 `goal/inputs/currentStep/steps/allowedActions/artifacts/resumeCommand/validationCommands/stopConditions/risks/nextActions`；缺必填输入时返回 `BLOCKED` 和 `PROVIDE_REQUIRED_INPUT`；更新步骤只改本地任务卡文件，不执行 workflow。
- 落地产物：新增 `tools/dataspec-task-card.mjs` 共享模块和测试；CLI 支持创建、展示、更新本地 JSON/Markdown 任务卡并限制输出路径；MCP 支持本地创建/渲染任务卡；前端提供摘要与 Markdown 展示工具并接入 smoke gate。
- 验收标准：AI 执行建表、反向导入、PR SQL Review 或导出最小 Context 时，能用任务卡描述当前进度、下一条安全命令和恢复方式；失败重试不会重复写入。已通过 task card 共享测试、CLI/MCP Node 测试和前端统一测试。
- 边界：不实现企业审批流，不引入外部队列，不把所有同步接口强制改成异步任务。

### P6-63：数据库直连元数据浏览器与候选选择页
- 状态：已完成第一版。
- 为什么做：数据库直连反向导入当前以“连接 -> 选表 -> 预览 -> 导入”为主，用户和 AI 在导入前缺少一个只读浏览真实 schema/table/column/comment/index 的轻量视图。
- 已有基础：已有 PostgreSQL/MySQL 直连、表列表、metadata 预览、覆盖率报告、连接预设、字段来源追踪和反向导入前端页。
- 已完成能力：第一版已补只读 metadata browser；后续大库分页、scan plan 和取消恢复归入 P6-64。
- 落地产物：新增 `/api/reverse-import/database/browser` 聚合接口和反向导入页元数据浏览子视图；支持按 schema/table/column/comment/type/index/标准匹配搜索，展示字段标准匹配、缺注释、属性差异、未纳管和可加入导入候选的勾选状态；schema dump 增加 index metadata；浏览结果提供 AI schema-only 摘要并复用 preview/compare/coverage 结果。
- 验收标准：连接数据库后无需导入即可浏览元数据；AI 可读取选中表的结构摘要并继续生成候选导入或覆盖率报告；全流程只读且不采样业务数据行。第一版已覆盖。
- 边界：不做通用 SQL 客户端，不执行任意查询，不保存数据库密码。

### P6-64：大库扫描计划、分页预览与取消恢复
- 状态：已完成第一版。
- 为什么做：真实数据库可能有大量 schema、表和字段；一次性拉取所有 metadata 会慢、容易超时，也不利于 AI 在上下文有限时逐步处理。
- 已有基础：已有数据库直连表列表、metadata 预览、覆盖率报告、性能基线待办、数据库元数据适配层待办和 AI 任务重试待办。
- 已完成能力：新增数据库 metadata scan plan，支持 pageSize、cursor、scanId、progress、partialSummary、resumeCommand 和 cancel 状态；前端反向导入页可按批次扫描、继续下一批、取消扫描，并把当前页表合并到候选表选择中。
- 落地产物：新增 `/api/reverse-import/database/scan` 和反向导入页分页扫描控件；后端测试覆盖上百张表分页、取消不写入标准库、当前页驱动 metadata browser 部分预览，前端测试覆盖批次表名合并、进度摘要和恢复命令脱敏。
- 验收标准：上百张表的数据库可分批加载并生成部分预览；中途取消不会写入标准库；AI 能根据 cursor 继续下一批或停止。第一版已覆盖。
- 边界：不引入分布式调度，不长期保存数据库连接凭据，不默认后台全库扫描；第一版 cursor 是同一连接上下文内的短期偏移量，不作为持久采集作业游标。

### P6-65：标准字段智能合并向导
- 状态：已完成第一版。
- 为什么做：字段冲突检测能发现重复或疑似重复，但用户仍要手动判断哪个字段保留、别名如何合并、历史来源和模板引用如何处理；这一步对 AI 也容易出错。
- 已有基础：已有字段冲突报告、字段影响分析、字段来源批次、标准快照、变更日志和字段生命周期待办。
- 已完成能力：新增 `/api/fields/merge/preview` 和 `/api/fields/merge/apply`；字段冲突页和字段库可打开合并向导；preview 展示 target/source、aliases/tags 安全迁移、examples/source 人工审阅项、风险、影响对象和 rollbackHints；apply 要求合并原因，更新目标字段 aliases/tags，废弃来源字段并写入变更日志。
- 落地产物：标准字段合并预览与确认接口/页面；AI capability catalog 和 Schema Registry 增加 `merge-standard-fields` / `standard-field-merge`；README 和 AI contract 文档同步记录第一版边界。
- 验收标准：用户能把两个重复字段合并为一个标准字段，并保留必要别名、来源和影响记录；AI Context 能通过 deprecated/replacement 关系识别被合并字段；合并前可预览影响。
- 边界：不自动合并冲突字段，不删除历史审计记录，不跨项目强制统一字段定义。

### P6-66：前端命令面板与最近操作续跑
- 状态：已完成第一版，已新增顶部入口和 `Ctrl/⌘+K` 全局命令面板，可搜索页面入口、项目动作和最近 SQL/反向导入/AI 作业续跑命令。
- 为什么做：功能越来越多后，用户和 AI 驱动的人工操作都需要快速跳到“SQL 校验、反向导入、字段质量、覆盖率、AI Context、Token”等入口，并能续跑最近一次上下文。
- 已有基础：已有前端路由、个人工作台、项目活动时间线待办、URL 可复现链接待办、任务式导航待办和多页记录详情。
- 已完成能力：命令面板会在有项目时拉取最近 SQL 检查、反向导入决策批次和 AI job；无项目时显示选择项目/创建演示项目建议，并禁用项目内命令。
- 落地产物：新增 `CommandPaletteDialog`、`commandPalette` 工具和源码级测试；最近命令只持久化安全的 DataSpec route/query，不保存 SQL 原文、token、payload 或连接凭据。
- 验收标准：键盘或顶部入口可快速打开命令面板；选择最近 SQL 检查、导入批次或字段质量报告后能恢复对应页面状态；无项目时给出创建或选择项目建议。
- 边界：不重做整体信息架构，不引入复杂桌面化布局，不绕过页面原有权限和项目边界。

### P6-67：AI 交接证据看板
- 状态：已完成第一版，已新增前端“校验与生成 / AI 交接证据”看板。
- 为什么做：AI 完成一个任务后，用户需要快速看见它改了什么、用了哪些 DataSpec 标准、跑了哪些验证、还有哪些风险；这些证据目前分散在回放、检查记录、TODO、git log 和终端输出中。
- 已有基础：已有 AI 回放、SQL 检查记录、AI 批量任务、AI task run、执行证据包、标准快照和统一前端状态。
- 已完成能力：按项目聚合最近 AI task run、AI job、SQL 检查和 AI 批量任务；失败、部分失败、等待和未验证项会进入“失败或未验证项”区域并明确标色；可对任意持久化来源生成或下载 evidence package。
- 落地产物：新增 `AiHandoff.vue`、`handoffEvidenceDisplay` 工具、路由/侧栏/命令面板入口和前端测试；看板展示任务交接记录、关联证据源、lint / DDL / Context 产物、验证结果、标准版本、相关 commit 摘要和 nextActions。
- 验收标准：用户能打开交接看板判断 AI 交付是否可继续使用；失败或未验证项会明确标红；复制 JSON 和页面摘要会再次脱敏 Authorization、token、password、JDBC URL、DSN 和连接串。第一版已覆盖。
- 边界：不做团队审批流，不替代 GitHub PR，不采集业务数据行或第三方 LLM 对话全文。

### P6-68：多项目标准复用包与轻量继承
- 状态：已完成第一版。
- 为什么做：个人或小团队往往会有多个业务项目共享一套用户、订单、支付等通用字段标准；完全复制会造成漂移，过早做组织级治理又太重。
- 已有基础：已有项目模型、演示项目、领域 Starter Kit 待办、标准快照、导入导出、字段分组和项目模板能力。
- 已完成能力：已新增轻量 shared standard pack，可从源项目创建版本化标准复用包，并在目标项目预览、确认应用和查看漂移；应用默认只创建缺失资产，不覆盖项目本地资产。
- 落地产物：新增复用包与应用记录模型、`/api/standard-reuse-packs` API、前端“标准复用包”页面、字段来源 `pack:<packKey>@<basePackVersion>` 标记和 AI Context 来源摘要。
- 验收标准：新项目可从共享包初始化通用标准；共享包升级后能看到本项目覆盖项和漂移项；AI Context 能说明字段来自共享包还是项目覆盖；复用包 payload 默认脱敏，不打包 raw secret、JDBC URL、DSN 或 Authorization。
- 边界：不做企业组织层级、审批发布、跨团队权限或复杂包仓库；第一版只服务个人/小团队复用。

### P6-69：AI 写入安全策略与 dry-run 协议
- 状态：已完成第一版，已新增 AI capability `safety` metadata、CLI/MCP safety 展示与校验、缺幂等 key 的结构化诊断、签名 dry-run 证据和前端 dry-run 摘要。
- 为什么做：DataSpec 越来越多入口会被 AI 通过 CLI/MCP 调用，个人工具也需要避免“AI 一次误写很多标准字段、规则或导入记录”；安全策略应偏产品内建约束，而不是企业审批流。
- 已有基础：已有 API Token、`doctor`、workflow recipes、AI 能力清单、AI 任务卡、幂等写保护和敏感信息脱敏。
- 已完成能力：CLI/MCP/API 已补统一 machine-readable safety metadata；AI 可判断哪些操作只读、哪些需要 dry-run、哪些必须带 idempotency key 或可撤销证据。
- 落地产物：为高风险写操作定义 safety metadata，包含 `readOnly`、`writesProject`、`requiresDryRun`、`supportsUndo`、`requiresIdempotencyKey`、`sensitiveInputs` 和 `nextActions`；反向导入和项目恢复确认写入要求预览返回的签名 `dryRunToken`，CLI/MCP 输出并校验该协议，前端批量写入前展示 dry-run 摘要。
- 验收标准：AI 能先枚举安全等级再执行写操作；反向导入和项目恢复等高风险确认写入必须先 dry-run，AI 批量 SQL lint 等重复写操作必须带幂等 key；标准复用等仅建议预览的能力不伪装成服务端强校验；缺少必要安全参数时返回结构化错误；日志不输出 password/token/Authorization/JDBC URL/DSN。已通过后端、CLI/MCP、前端和 OpenSpec 目标验证，完整证据见本次 OpenSpec 变更记录。
- 边界：不做组织审批、多人审核或复杂 RBAC；不阻塞单条低风险个人 CRUD。

### P6-70：SQL 规则调试器与可解释匹配面板
- 状态：已完成第一版，commit `9bd40c9`；OpenSpec change `add-sql-rule-debugger` 已于 2026-07-05 归档并同步主规格。
- 为什么做：规则越来越多后，用户和 AI 需要知道某条 SQL 为什么被某个规则命中、命中的 AST/文本范围是什么、参数如何影响结果，否则只能靠猜。
- 已有基础：已有 SQL lint、source range、fixedSql、规则配置、golden fixtures、AI 可读错误码待办和多方言兼容矩阵待办。
- 已完成能力：新增只读 `/api/lint/debug`、CLI `lint-debug`、AI capability `sql-rule-debugger`、Schema Registry contract `sql-rule-debug-result` 和前端规则调试面板；debug 不保存 SQL 检查记录、不创建 AI replay，参数快照复用统一脱敏。
- 后续增强：更深 AST trace、规则参数快速跳转、从调试结果直接创建豁免或规则建议可放入后续规则体验任务。
- 边界：不暴露完整复杂 AST 编辑器，不要求所有规则第一版都有深度 trace；不改变现有 lint 结果兼容字段。

### P6-71：数据库元数据增量缓存与变更指纹
- 状态：已完成第一版，本地 commit `c993ee2`；OpenSpec change `add-db-metadata-incremental-cache` 已于 2026-07-07 归档到 `openspec/changes/archive/2026-07-07-add-db-metadata-incremental-cache`。
- 为什么做：数据库直连反向导入、覆盖率报告和元数据浏览会反复读取同一批 schema；没有增量缓存时，大库会慢，AI 也无法判断“这次和上次相比变了什么”。
- 已有基础：已有数据库直连、连接预设、metadata 预览、覆盖率报告、字段来源批次、schema dump 待办、大库扫描计划待办和变更感知扫描待办。
- 已完成能力：新增只保存结构信息的 metadata cache；按连接预设或脱敏连接来源、schema、table 计算 fingerprint；scan/browser/dump/preview/compare/coverage 返回 cache status、lastSeenAt、expiresAt、refreshMode、metadataFingerprint 和变化摘要；前端展示缓存状态并提供 `REFRESH` 手动刷新入口。
- 验证证据：提交前已完成 OpenSpec strict、后端/前端验证、`git diff --check`、敏感词扫描和独立子 agent 只读评审；后续已完成 OpenSpec archive，未主动 push。
- 验收标准：重复扫描同一数据库可复用缓存并提示是否过期；字段变化能生成差异摘要；缓存不保存密码、不保存业务数据行；AI 可根据 fingerprint 决定是否重跑反向导入。
- 边界：不做实时同步，不监听数据库 binlog，不默认后台扫描全库。

### P6-72：CLI/MCP 与服务端版本兼容握手
- 状态：已完成第一版，OpenSpec change 已归档到 `openspec/changes/archive/2026-07-05-add-version-compatibility-handshake`。
- 为什么做：业务仓库里的 CLI/MCP 脚本可能落后于 DataSpec 服务端，OpenAPI 和 AI 契约也会演进；AI 遇到版本不兼容时应得到明确诊断，而不是运行到一半才失败。
- 已有基础：已有 `doctor`、OpenAPI 防漂移、AI 输出契约测试、workflow recipes、CLI/MCP 和 README 启动说明。
- 已完成能力：新增只读 `/api/capabilities/version` 握手，CLI `compat check` 与 `doctor` 兼容检查、MCP `dataspec://version-compatibility` resource 均可读取 serverVersion、apiSchemaHash、minCliVersion、supportedCapabilities、deprecatedFields 和 upgradeHints。
- 落地产物：新增服务端 capability endpoint 或复用现有健康检查扩展；CLI/MCP 在关键命令前读取 serverVersion、apiSchemaHash、minCliVersion、supportedCapabilities、deprecatedFields 和 upgradeHints。
- 验收标准：CLI 版本过旧、服务端未启用某能力、OpenAPI schema 漂移时都有明确中文诊断和修复命令；AI 可读取 JSON 结果决定升级、降级或停止。
- 边界：不做自动在线升级，不强制所有历史 CLI 版本兼容无限期；不引入远程遥测。

### P6-73：前端类型化 API Client 与请求状态收口
- 状态：已完成第一版，按常规前端模式交付，未新增 OpenSpec change。
- 为什么做：前端已有 OpenAPI 类型生成，但请求封装仍是手写薄层；随着页面增多，字段漂移、loading/error 重复处理和分页参数不一致会继续消耗维护成本。
- 已有基础：已有 `openapi-typescript` schema、`request.ts` 拦截器、项目 store、前端统一数据状态待办、OpenAPI 防漂移和关键流程 E2E 待办。
- 已完成能力：新增 `typedGet` / `typedPost` 和 OpenAPI path 渲染 helper，GET/POST path、query、body 与响应 data 可从 `paths` 推导；AI 批量任务与 AI 任务运行列表/详情等高频调用已迁移；`useRequestState` 统一暴露 `nextActions`，`StateBlock` 可展示一致的下一步动作。
- 验证证据：`node --test dataspec-web\tests\typedApiClient.test.ts dataspec-web\tests\requestState.test.ts` 通过；`node --test dataspec-web\tests\frontendSmoke.test.ts` 通过；前端 build 已验证 typed client 可参与类型检查。
- 后续增强：保留现有 Axios 拦截器和 token 行为；后续新增 API 优先走 typed helper，历史页面按改动机会逐步迁移，不一次性重写所有页面。
- 边界：不一次性重写所有页面，不引入大型运行时 SDK；保留现有 Axios 拦截器和 token 行为。

### P6-75：MCP/CLI 工具契约验收与示例调用库
- 状态：已完成第一版，OpenSpec change `add-cli-mcp-contract-fixtures` 已归档到 `openspec/changes/archive/2026-07-05-add-cli-mcp-contract-fixtures`。
- 为什么做：DataSpec 优先给 AI 使用时，MCP tools 和 CLI 命令本身就是 AI 的“产品界面”；仅有 OpenAPI 类型还不够，工具入参、输出 schema、错误码、示例和安全边界都需要可回归验证。
- 已有基础：已有 CLI、MCP、OpenAPI 防漂移、AI 输出契约稳定性、AI 能力清单、doctor 和 workflow recipes。
- 已完成能力：新增 `tools/fixtures/cli-mcp-contracts.json` 和 `tools/dataspec-cli-mcp-contract-check.mjs`，覆盖核心 CLI commands、MCP tools/resources/prompts 的名称、输入边界、输出 shape、成功/失败示例、安全 metadata 和 recommendedNextActions；校验会对齐本地 MCP descriptors、拒绝 secret-like 示例，并接入验证建议和 Node tests。
- 落地产物：新增 MCP/CLI contract fixtures、本地验收命令、fixture checker 单测、验证建议规则、README 与 AI 契约文档说明，以及主规格 `openspec/specs/cli-mcp-contract-fixtures/spec.md`。
- 验收标准：修改 CLI/MCP 工具参数、输出字段、resource/prompt 描述或安全 metadata 时，契约测试能失败并提示更新示例；AI 可读取示例库选择正确工具；失败样例不包含 token/password/连接串。
- 边界：不实现完整 MCP 兼容性测试平台，不要求所有历史命令一次性补齐；优先覆盖 lint、Context、reverse import、DDL、doctor 和字段检索高频入口。

### P6-78：fixedSql 文件级补丁应用与人工确认
- 状态：已完成第一版，CLI 已新增 `fixed-sql patch` dry-run 补丁计划和显式确认 apply。
- 为什么做：SQL 校验已经能输出 fixedSql，但真实业务仓库里 AI 还需要把修复从页面或 CLI 结果安全落到文件；直接覆盖文件风险高，需要 diff、dry-run、人工确认和回退提示。
- 已有基础：已有 fixedSql、fixedSql diff、SQL 定位范围、CLI lint-files/review-pr、AI 写入安全策略、任务卡和执行证据包待办。
- 已完成能力：`fixed-sql patch --lint-result <json> --target <file.sql> --format json` 默认只生成补丁计划，输出 `unifiedDiff`、`conflictWarnings`、`dryRunResult`、`planHash`、`applyCommand`、`rollbackHint`、`evidenceRef` 和安全 metadata；写入必须显式传 `--apply --confirm <planHash>`。
- 落地产物：新增 `buildFixedSqlPatchPlan`、目标路径 cwd 内校验、内容漂移检测、确认 hash 校验、脱敏 diff 输出、CLI help 和 CLI/MCP contract fixture。
- 验收标准：AI 可以先生成补丁并展示 diff，再由用户或显式命令确认应用；目标路径越界、缺少 fixedSql、内容漂移、缺少确认或确认 hash 不匹配都会拒绝写入；相关 Node 测试覆盖 dry-run、apply、阻断和脱敏。
- 边界：不自动提交业务仓库，不绕过用户确认，不处理所有复杂 SQL 重排；第一版只覆盖单文件或小批量文件。

### P6-79：标准问答只读入口与证据引用
- 状态：已完成第一版，前端已新增 `/standard-qa` 只读入口，可基于字段检索、启用术语和规则配置生成短答案、置信度、字段证据和下一步动作。
- 为什么做：用户和 AI 经常只想问“手机号标准字段叫什么”“订单金额应该用哪个单位”“这个字段是否已废弃”，不一定要进入候选草案或 DDL 生成流程。
- 已有基础：已有字段检索、业务术语表、AI 输出引用证据、生命周期状态、字段格式约束、AI Context 和 Prompt 生成。
- 已完成能力：新增只读、短回答、带证据的标准问答入口；规则证据只展示与问题或匹配字段相关的规则，低置信度时不会把候选字段说成确定标准。
- 落地产物：新增前端标准问答页面、答案构建纯函数和可复制 Markdown；输入自然语言问题，输出 answer、confidence、matchedFields、evidence、relatedRules、suggestedNextActions 和 unresolvedQuestions，并接入路由、侧边栏、命令面板和前端测试。
- 验收标准：常见字段命名、单位、状态、废弃替代、敏感字段标记问题能返回可读答案和证据；低置信度时明确提示需要人工确认或进入候选 Inbox；连续查询时旧请求不会覆盖新答案。
- 边界：第一版不调用外部 LLM，不回答业务数据内容，不把问答结果直接写入标准库。

### P6-81：浏览器级 E2E 验收与失败截图
- 状态：已完成第一版，按常规前端模式交付，未新增 OpenSpec change。
- 为什么做：P6-17 已有源码级冒烟门禁，但它不启动真实浏览器，也无法发现路由渲染、Monaco、Element Plus 弹层、响应式布局和真实用户操作中的问题。
- 已有基础：已有前端源码级冒烟测试、演示项目初始化、README 验证命令、SQL 校验、反向导入、字段库、AI Context 和覆盖率报告页面。
- 已完成能力：新增 Playwright 浏览器级 E2E 配置、脚本和测试夹具；覆盖创建/选择项目、SQL 校验 fixedSql、检查记录详情、数据库直连预览、字段库筛选和 AI Context 三类预览；失败时保留截图、trace、video 和当前 URL。
- 落地产物：新增 `dataspec-web/playwright.config.ts`、`dataspec-web/tests/e2e/core-workflows.spec.ts`、`pnpm test:e2e` / `pnpm test:e2e:headed` 脚本，README 已说明源码级 smoke 与浏览器级 E2E 的边界和失败 artifacts。
- 验证证据：`pnpm test` 148 pass；`pnpm build` exit 0（仅依赖 pure annotation 和 chunk size 既有 warning）；`pnpm test:e2e` 1 passed。
- 验收标准：一条命令可在本地跑核心浏览器流程；破坏主导航、项目选择、关键按钮或结果渲染时测试失败并给出可复现证据；README 说明与源码级 smoke 的边界差异。
- 边界：不追求全量页面覆盖，不做像素级视觉回归，不要求普通 `pnpm test` 默认依赖浏览器；第一版可作为可选验证入口。

### P6-82：真实数据库 Testcontainers 集成测试矩阵
- 状态：已完成第一版，OpenSpec change `add-testcontainers-db-integration-tests` 已于 2026-07-07 归档到 `openspec/changes/archive/2026-07-07-add-testcontainers-db-integration-tests`。
- 为什么做：数据库直连反向导入、覆盖率和二次比对已经是核心能力，但当前主要依赖 fixture 和 H2/单测；真实 PostgreSQL/MySQL 元数据、COMMENT、schema、大小写和权限行为仍可能漂移。
- 已有基础：已有 Flyway、PostgreSQL/MySQL 直连 metadata、反向导入预览、数据库覆盖率、二次比对、metadata fixture 和多方言兼容矩阵待办。
- 已完成能力：新增基于 Testcontainers 的真实数据库集成测试矩阵，覆盖 PostgreSQL/MySQL schema-only fixture、表列表、metadata dump/browser、compare、coverage、只读连接诊断和敏感信息不外泄断言；同时补充 PostgreSQL 只读账号在 JDBC 未声明 readOnly 时的权限诊断回归测试。
- 落地产物：新增 Testcontainers 测试 profile；启动 PostgreSQL/MySQL 容器，加载最小 schema fixture，验证表列表、metadata preview、compare、coverage 和连接诊断的关键字段。
- 验证证据：`mvn test` 476 pass，确认默认后端测试不运行 `*IT.java` 且不依赖 Docker；`mvn test -Pdb-integration` 已进入 Testcontainers 启动阶段，但当前环境无可用 Docker，报 `Could not find a valid Docker environment`，真实容器矩阵需在 Docker 可用环境补跑。
- 本地真实自测记录（2026-07-07）：用户提供一次性 PostgreSQL 测试库 `localhost:5432/ai_test`，用户 `postgres`；密码仅保留在当前会话上下文，不写入待办。已使用本地 PostgreSQL JDBC 42.7.11 执行只读 `select current_database(), current_user, version()`，连接成功，返回数据库 `ai_test`、用户 `postgres`、版本 PostgreSQL 17.9。用户已确认 `ai_test` 可作为可写、可丢弃的一次性测试库；后续按待办执行真实闭环自测时，可在该库内启动后端 Flyway、创建 DataSpec 测试数据并运行 smoke，授权边界仅限 `ai_test`，不得扩展到其他数据库或真实业务库。
- 本地真实闭环执行记录（2026-07-07）：已在端口 `18090/15173` 启动后端和前端，后端显式连接用户授权的一次性 `ai_test` 测试库并启用 Flyway；`node tools/dataspec-local-smoke.mjs --server http://localhost:18090 --web http://localhost:15173 --json` 通过，创建演示项目 `projectId=1`，web、api-docs、demo-project、dashboard-summary、sql-lint 全部 pass，SQL lint 返回 22 个 active issues；随后调用 `/api/reverse-import/database/tables` 只读读取 `ai_test`，返回 32 张表，并调用 `/api/reverse-import/database/browser` 浏览 `public.ds_project`，summary 为 1 张表、7 列、1 个索引、3 个候选、7 个缺注释、4 个 changed、3 个 unmanaged，coverageRate 57.1%。本轮启动的 `18090/15173` 进程已停止。
- 验收标准：开发者可通过专门 profile 运行真实数据库集成测试；默认 `mvn test` 不强制依赖 Docker；失败信息能定位具体方言和 metadata 字段。
- 边界：不替代数据库供应商完整兼容认证，不把 Docker 作为所有开发环境的必需前提，不扫描业务数据行。

### P6-83：README/TODO/OpenSpec 状态一致性检查
- 状态：已完成第一版，新增 `tools/dataspec-status-check.mjs` 与目标单测，README 已补验证入口。
- 为什么做：项目能力迭代很快，README、TODO、OpenSpec active/archive 和实际代码容易出现“文档说已完成但入口不可用”或“待办仍写缺口但功能已实现”的漂移。
- 已有基础：已有 README 功能清单、TODO 状态行、OpenSpec change、OpenSpec validate、AI 输出契约测试和完成项归档习惯。
- 已完成能力：状态检查工具可扫描 TODO 近期队列、完成态残留“缺口”、OpenSpec active/archive/main spec、README 工具入口和 Markdown 相对链接，并输出 `severity/code/message/file/line/suggestedFix` 等机器可读问题。
- 验收标准：文档改动后能运行 `node tools/dataspec-status-check.mjs --format json` 检查明显状态漂移；新增完成项时会提示补 README/TODO/OpenSpec 入口；检查结果不依赖外部网络。
- 边界：不做自然语言完美理解，不强制阻断所有文档变更；第一版只覆盖编号、状态、标题、链接和关键入口的确定性规则。

### P6-87：数据库 Schema 变更计划与迁移脚本预览
- 状态：已完成第一版，已完成验证、独立评审和本地 commit 收口。
- 为什么做：DataSpec 已能生成 DDL、检查 SQL 和反向导入现有数据库，但真实落地还需要回答“从当前数据库变到目标标准要改哪些表/字段、风险是什么、怎么回退”；AI 不能只输出一段不可审计的 ALTER SQL。
- 已有基础：已有表模板、DDL 生成、数据库直连 metadata、二次比对、标准快照、fixedSql diff、备份迁移包待办和 Atlas/Terraform 风格 plan/apply 参考。
- 已完成能力：新增只读 schema plan API `/api/reverse-import/database/schema-plan`、CLI `schema-plan` 和反向导入页预览区；计划复用数据库 metadata dump 与 compare 语义，输出 `currentSchemaHash`、`targetSpecHash`、`changeSet`、`riskLevel`、`migrationSql`、`rollbackHint`、`manualChecks`、`blockedReasons` 和 `nextActions`。
- 落地产物：API/CLI/前端均只生成 dry-run 草案和风险说明；注释修正可给出 `COMMENT ON` 草案，结构属性只给出 `-- REVIEW` 人工确认文本，不拼接可执行 `ALTER TABLE`；前端展示整体风险、阻塞原因、人工检查、按表分组的 changeSet 和 SQL 草案；CLI 推荐 `--password-env` 读取数据库密码。
- 验收标准：连接数据库后可生成“当前库 -> DataSpec 标准”的变更计划；高风险 drop/rename 默认标红且不自动执行；AI 可读取 JSON 计划并决定补标准、生成迁移文件或停止等待人工确认。
- 边界：第一版不直接执行迁移，不替代 Flyway/Liquibase/Atlas 等迁移工具，不自动推断所有字段重命名；只服务个人/小团队的迁移草案和风险说明。

### P6-88：业务代码字段引用索引与重命名风险分析
- 状态：已完成第一版，OpenSpec change `add-code-field-reference-index` 已于 2026-07-07 归档到 `openspec/changes/archive/2026-07-07-add-code-field-reference-index`。
- 为什么做：修改字段名、废弃字段或合并标准前，用户和 AI 需要知道业务仓库里哪些 SQL、迁移文件、ORM 模型、报表或配置正在引用该字段，否则标准变更容易造成代码与数据库脱节。
- 已有基础：已有 `.dataspec/config.json` 默认扫描路径、CLI 批量 lint、PR review、字段影响分析、变更感知扫描、fixedSql 文件补丁和业务仓库初始化。
- 已完成能力：新增 CLI `index-refs`，在业务仓库本地只读扫描显式 `--path` 或 `.dataspec/config.json` 的 `defaultPaths`，输出字段引用、引用类型、文件行列、置信度、重命名风险、建议动作和 nextActions；未配置扫描路径时返回 `DATASPEC_DEFAULT_PATHS_MISSING`，不会全仓误扫。
- 落地产物：CLI/MCP 契约 fixture 已记录 `index-refs` 的输入、输出、安全 metadata 和示例；后端字段影响模型新增 `CODE_REFERENCE` 与 `codeReferenceImpactCount` 摘要字段；前端字段影响弹窗可展示业务代码引用计数。
- 验收标准：给定一个标准字段，能列出业务仓库内主要 SQL、DDL、迁移、模型和配置引用位置；准备重命名或停用字段时能看到风险清单；扫描只读、限制在业务仓库路径内，并跳过常见生成目录；输出片段和诊断会脱敏 password、token、Authorization、JDBC URL、DSN 和连接串。
- 边界：不做完整代码智能平台，不解析所有语言 AST，不自动改业务代码，不新增持久化引用索引表，也不让后端根据任意路径读取服务器文件系统；第一版优先覆盖 SQL、迁移文件和常见 schema/model/config 文件。

### P6-89：MCP Prompt/Resource 一等化与 Agent 引导包
- 状态：已完成第一版，OpenSpec change 已归档到 `openspec/changes/archive/2026-07-05-add-mcp-agent-guidance-pack`。
- 为什么做：当前 MCP 重点是 tools/resources，但 AI agent 很多时候需要先拿到“该怎么问、先读什么、什么不能做”的 prompt 引导；如果这些只散落在 README 和 AGENTS 片段里，工具调用仍容易绕远。
- 已有基础：已有 MCP server、AI Context package、workflow recipes、AI 能力清单、CLI/MCP 契约验收、AI 会话启动包和 `AGENTS.md.fragment`。
- 已完成能力：MCP 已新增 7 个 `resources/templates/list` 项目级模板和本地 `agent-guidance-pack` resource；新增 `create_table_with_dataspec`、`review_sql_with_dataspec`、`reverse_import_standards`、`answer_field_standard_question` 一等化 prompt，descriptor 暴露 safety 与完整 `dataspecGuidance`，旧 prompt 名称继续兼容。
- 验证证据：契约 fixture 覆盖 resource templates、prompt safety、参数 required/description 和完整 guidance 漂移；已通过目标 Node 测试、全量 tools 测试、OpenSpec strict/all、状态检查和独立子 agent 评审。
- 后续增强：若后续需要 agent session memory、跨 IDE prompt 包或更多任务模板，可从 P6-178、P6-173 或候选池中单独开 OpenSpec。
- 边界：不绑定单一 IDE 或 agent 产品，不调用外部 LLM，不把 prompts 做成复杂审批流。

### P6-90：AI 上下文预算评估与自动裁剪策略
- 状态：已完成第一版，OpenSpec change `add-ai-context-budget-planner` 已于 2026-07-07 归档到 `openspec/changes/archive/2026-07-07-add-ai-context-budget-planner`。
- 为什么做：字段标准、规则、模板、样例和历史记录越来越多后，AI Context 很容易过大；仅靠手动 scope/query/limit 不够，AI 需要知道不同预算下应该保留哪些标准、舍弃哪些上下文以及风险是什么。
- 已有基础：已有 AI Context 按需裁剪、字段检索、标准快照、业务术语表、AI 会话启动包、上下文握手和 prompt 评测待办。
- 已完成能力：新增只读 `/api/ai-context/budget/plan`、CLI `context-budget plan`、CLI/MCP contract fixture 和前端 AI Context 预算预览；输入任务类型、query、目标表/文件、预算上限或 scoped export 参数，输出 selectedArtifacts、estimatedTokens、droppedArtifacts、qualityRisk、fallbackSteps、recommendedExportParams 和 recommendedNextActions。
- 落地产物：预算 planner 采用确定性本地估算，不调用外部 LLM/tokenizer，不生成 zip、不写 `.dataspec/context/` 缓存、不修改项目状态；前端只展示推荐参数，必须点击“一键填充”才会应用。
- 验收标准：同一项目可生成完整包、标准包和极简包；AI 能解释为什么保留某些字段/规则并标出缺失风险；裁剪策略有服务层测试、CLI 测试、fixture 校验和前端 smoke/显示工具测试防漂移。
- 边界：不依赖特定模型的精确 tokenizer，不上传标准内容到外部服务，不保证一次裁剪覆盖所有复杂任务。

### P6-91：本地 pre-commit 与 IDE 保存前 SQL 标准检查
- 状态：已完成第一版。
- 为什么做：CI/PR review 发现问题已经偏晚；个人使用时更希望在本地提交前或保存 SQL/迁移文件时就看到 DataSpec 诊断，让 AI 和开发者少走返工。
- 已有基础：已有 CLI `lint-files`、PR review、`.dataspec/config.json`、GitHub Action 示例、质量门禁、changed-file 扫描和 fixedSql 文件补丁待办。
- 已完成能力：新增 CLI `install-hook --hook pre-commit --with-vscode --format json`，可在业务 git 仓库内写入 DataSpec 管理的 `.git/hooks/pre-commit`，并可选生成 `.vscode/tasks.json` 与 `.vscode/dataspec-problem-matcher.json`；hook 默认运行 `lint-changed --format json`，IDE task 使用 `lint-changed --format text`。
- 落地产物：`lint-changed --format text` 输出 `file:line:column: severity rule - message suggestion: ...` 行格式，便于 VS Code Problem Matcher 跳转；`install-hook` 输出 writtenFiles/skippedFiles/diagnostics/safety/nextActions，CLI/MCP contract fixture 已补 `install-hook` 安全契约。
- 验收标准：业务仓库执行初始化后可显式启用本地 SQL 标准检查；提交前只检查变更 SQL/DDL 文件；失败输出不泄漏 token/password，且 JSON 可被 AI 读取继续修复，text 可被 IDE 解析。
- 边界：不强制所有项目安装 hook，不覆盖非 DataSpec marker 管理的用户 hook 或编辑器配置，不绕过用户本地 Git 配置，不替代 CI/GitHub Review。

### P6-92：标准样例自动生成与合成业务场景库
- 状态：已完成第一版。
- 为什么做：规则、字段推荐、DDL 生成和 Prompt 评测都依赖高质量样例；手写少量 good/bad SQL 容易覆盖不足，AI 也缺少“典型业务场景下标准如何使用”的可复用素材。
- 已有基础：已有演示项目、golden fixtures、字段使用示例与反例库、领域 Starter Kit、Prompt 评测、规则模板库和标准变更演练沙箱待办。
- 已完成能力：新增只读 `/api/synthetic-examples/generate` 和 CLI `synthetic-examples generate`，支持 `user/order/payment/audit` 场景，输出 good SQL、bad SQL、DDL preview 输入、字段推荐问题、标准问答案例、预期诊断、`specHash`、生成参数、sourceSummary、safety 和 nextActions。
- 落地产物：生成器会读取项目标准字段和模板摘要；素材不足时使用内置场景字段补齐并输出 fallback diagnostics；后端 contract fixture、CLI/MCP contract fixture、OpenSpec delta 和 README 已同步。
- 验收标准：标准字段或模板摘要变化会改变 `specHash`；样例包可接入后端 fixture、Prompt 评测或前端 smoke；生成内容不包含真实业务数据行，且敏感值会脱敏。
- 边界：不替代人工维护的高价值真实样例，不引入外部 LLM 自动造数据，不自动写入标准使用示例库，不生成可直接写入生产库的数据。

### P6-93：多源契约反向导入到标准候选
- 状态：已完成第一版，OpenSpec change `add-contract-candidate-import` 已于 2026-07-07 归档到 `openspec/changes/archive/2026-07-07-add-contract-candidate-import`。
- 为什么做：很多字段标准并不只存在于数据库，还散落在 OpenAPI、JSON Schema、Protobuf、事件 schema 和前端类型里；AI 建表或修 SQL 时如果只看数据库来源，会遗漏接口层已经稳定下来的业务命名。
- 已有基础：已有 OpenAPI 类型生成、字段推荐、标准候选 Inbox、数据库反向导入、字段来源追踪、业务代码引用索引和 AI Context。
- 已完成能力：新增只读 `POST /api/contract-import/preview` 和 CLI `contract-import preview`，支持 OpenAPI、JSON Schema、Protobuf `.proto` 文本或 descriptor 风格 JSON 第一版字段抽取；输出 `candidateFields`、`contractHash`、`diagnostics`、`safety`、`nextActions` 和兼容现有候选创建语义的 `inboxPayload`。
- 落地产物：新增后端 `contractimport` controller/service/model、后端 service/controller/contract fixture 测试、CLI 命令与测试、CLI/MCP contract fixture 校验、OpenSpec delta 和 README 说明；候选会与当前项目已有标准字段及同包重复字段做确定性去重，命中已有字段时建议 `MERGE_EXISTING`，复杂契约结构建议 `REVIEW_REQUIRED`。
- 验收标准：给定一份接口契约，能抽取字段名、类型、描述、必填性、枚举和示例值作为标准候选；输出稳定字段和安全 metadata；契约内容、sourcePath、diagnostics、CLI stdout/stderr 默认脱敏 token、password、Authorization、API key、完整 JDBC URL、DSN 和连接串；验证证据记录在 `openspec/changes/add-contract-candidate-import/tasks.md` 的 `Verification Evidence`。
- 边界：第一版只读，不自动写入候选 Inbox 或正式字段，不调用外部 LLM，不访问外部 URL，不读取真实业务数据行，不新增数据库表或迁移；复杂 `oneOf`、`anyOf`、`allOf`、`$ref`、泛型或深层嵌套先保守降级为人工确认。

### P6-152：P6 待办里程碑收束与实施队列
- 状态：已完成快速收束第一版；顶部“下一步顺序”已压缩为近期优先队列，已完成 active OpenSpec change 按需归档，P6-73、P6-89、P6-71、P6-81 和 P6-82 状态已同步。
- 为什么做：P6 待办已经覆盖大量增强方向，如果只按编号线性追加，AI 和用户都容易在“下一个最该做什么”上迷路；需要把待办转成更可执行的 Now/Next/Later 和 OpenSpec 输入队列。
- 已有基础：已有 TODO 路线图、OpenSpec change 流程、归档记录、README 当前功能摘要、执行证据包和 TODO 到 OpenSpec 交接助手待办。
- 已完成能力：主 TODO 顶部不再把 P6-71 到 P6-188 当作默认线性实施顺序；近期任务、暂缓池、active change 按需保留/归档边界和 SDD 触发边界已显式写入。
- 参考项目：`backstage/backstage` 的开发者入口、`changesets/changesets` 的变更组织和 OpenSpec tasks 的可验证清单；只借鉴规划方式，不引入外部项目管理系统。
- 后续增强：完整拆分 P6-71 到 P6-188 到子路线、归档已完成 P5/P6 长段内容、自动状态检查脚本由 P6-83 承接。
- 验收标准：AI 打开项目后能先看到近期队列和效率优先顺序；已完成 active OpenSpec change 按当前任务边界保留或归档；新增建议能先归并到已有主题而不是继续追加新编号。
- 边界：不删除历史任务，不改变已完成事实，不要求一次性重写所有 P6 内容；第一版先收束当前 P6 队列和新增任务入口。

### P6-177：OpenSpec Change 准备度评分与缺口检查
- 状态：已完成第一版，OpenSpec change `add-openspec-readiness-check` 已于 2026-07-08 归档到 `openspec/changes/archive/2026-07-08-add-openspec-readiness-check`。
- 为什么做：TODO 到 OpenSpec 草稿生成后，真正开工前还需要判断 proposal、design、spec、tasks 是否已经足够明确；如果缺影响范围、验证命令或边界，AI 很容易边做边猜。
- 已有基础：已有 TODO 到 OpenSpec 交接助手、OpenSpec validate、验证命令推荐工具、P6 里程碑收束待办和多次归档经验。
- 已完成能力：新增 `tools/dataspec-openspec-readiness.mjs`，支持 `--change <change-id>` 和 `--format text|json`，只读扫描 active change 的 proposal/design/spec/tasks，输出 `readinessScore`、`readinessLevel`、`missingFacts`、`affectedSpecs`、`validationPlan`、`reviewBoundary`、`riskFlags`、`humanQuestions`、`checks` 和 `nextActions`；缺验收标准、边界、有效 spec delta、Impact 内容、验证命令、占位内容或人工确认点时会降分并给出可执行诊断；validationPlan 会脱敏 token、password、JDBC URL、DSN 和 URL userinfo。
- 参考项目：`stoplightio/spectral` 的规则化 lint、`open-policy-agent/conftest` 的策略校验和 `Redocly/redocly-cli` 的契约检查；只借鉴可配置规则与诊断输出，不引入复杂治理流程。
- 落地产物：新增 OpenSpec change 准备度检查脚本、Node 单测、README 验证入口和 OpenSpec change `add-openspec-readiness-check`。
- 验收标准：对一个待实施 change 能输出可读报告和 JSON；缺少验收标准、边界、影响规格或验证命令时给出明确诊断；通过准备度检查不自动实现、不自动归档。已通过定点 Node 测试、tools 全量测试、OpenSpec 全量校验、状态检查和独立子 agent 评审。
- 验证证据：`node --test tools/dataspec-openspec-readiness.test.mjs` 10 pass；`node --test tools/dataspec-verify-advisor.test.mjs` 30 pass；`node --test tools/*.test.mjs` 358 pass、2 skipped（当前平台无法创建部分 symlink）；`openspec validate add-openspec-readiness-check --strict` valid；归档前 readiness JSON 为 `readinessScore=100`、`readinessLevel=READY`、`missingFacts=[]`；独立评审 agent `019f3d97-f09d-7872-808e-fa3d0d11ffa8` 的 4 个 Important 和 1 个 Minor finding 均已修复并关闭 agent；归档后 `openspec validate --all` 120 passed、0 failed，`openspec list --json` 为 `changes=[]`，`node tools/dataspec-status-check.mjs --format json` status pass、0 issues。
- 边界：不替代人工判断，不把所有低分 change 阻塞掉；第一版作为本地开工前提示和 AI 自检入口。

### P6-178：MCP 会话状态与当前项目记忆
- 状态：已完成第一版，OpenSpec change 已归档到 `openspec/changes/archive/2026-07-08-add-mcp-session-state-memory`。
- 为什么做：AI 通过 MCP 使用 DataSpec 时，经常需要重复确认 currentProjectId、标准快照、最近导出范围、上一轮任务结果和下一步建议；缺少会话状态会增加上下文浪费和误操作概率。
- 已有基础：已有 MCP/CLI 工作流模板、AI 会话启动包、AI 能力清单、AI 任务状态机、统一任务结果协议和敏感信息脱敏边界。
- 已完成能力：MCP 新增 `dataspec://project/<id>/session-state` resource、`dataspec://project/{projectId}/session-state` resource template 和只读 `get_session_state` tool；输出 `currentProject`、`currentSnapshot`、`lastTaskResult`、`toolCursor`、`safeDefaults`、`redactedMemory`、`diagnostics` 和 `nextActions`。
- 参考项目：`modelcontextprotocol/servers` 的 resource/tool 组织、`langchain-ai/langgraph` 的状态化 agent 流程和 `temporalio/temporal` 的可恢复任务状态；只借鉴状态模型，不接入远程编排服务。
- 落地产物：新增 MCP `session-state` resource 和 `get_session_state` tool；第一版只读聚合本地 `.dataspec/config.json`、`.dataspec/context/cache-metadata.json`、当前 profile、最近 task run 入口、safeDefaults、redactedMemory 和 nextActions，不自动写会话状态文件。
- 验证证据：`node --test tools/dataspec-mcp.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 68 pass；`node tools/dataspec-cli-mcp-contract-check.mjs --format json` `ok=true` 且 0 diagnostics；`node --test tools/*.test.mjs` 362 pass、2 skipped；`openspec validate add-mcp-session-state-memory --strict` valid；独立评审 agent `019f3db6-49ae-71f2-b7a2-d5462b1f9c61` 的 1 个 Critical 和 3 个 Important finding 均已修复并关闭 agent。
- 边界：不做云端长期记忆，不跨用户同步，不把会话状态当权限依据。

### P6-179：标准字段到业务代码 Patch Plan
- 状态：已完成第一版，已新增本地只读 `code-patch plan` CLI 和 Patch Plan 工具 API。
- 为什么做：字段标准变更后，仅知道影响哪些文件还不够，AI 需要一个可审查的 Patch Plan 来判断哪些实体、DTO、SQL、迁移脚本和测试可能要改。
- 已有基础：已有业务代码字段引用索引、标准变更迁移 Recipe、编辑器提示、业务仓库合规分、字段影响分析和 fixedSql 文件补丁待办。
- 已完成能力：`code-patch plan` 支持字段重命名、类型变化和枚举变化，输出 `patchPlan`、`candidateEdits[]`、`fileRef`、`riskLevel`、`dryRunDiff`、`manualSteps[]`、`verificationCommands[]`、`rollbackHint`、`safety`、`diagnostics[]` 和 `nextActions[]`；默认只读 dry-run，不写业务文件，未配置 `defaultPaths` 且未传 `--path` 时返回 `DATASPEC_DEFAULT_PATHS_MISSING`。
- 参考项目：`openrewrite/rewrite` 的迁移 recipe、`codemod-com/codemod` 的代码修改计划和 `ast-grep/ast-grep` 的结构化匹配；只借鉴 patch planning，不默认改写业务仓库。
- 落地产物：新增字段变更到业务代码 Patch Plan 的 CLI 和本地工具 API；基于引用索引和规则生成候选修改、风险等级、验证命令和人工确认点；可导出 Markdown/JSON；CLI/MCP 契约 fixture 已记录 `code-patch-plan` 的输出 shape 与安全 metadata。
- 验证证据：`node --test tools/*.test.mjs` 370 tests、368 pass、2 skipped；`node tools/dataspec-cli-mcp-contract-check.mjs --format json` `ok=true` 且 0 diagnostics；`openspec validate add-code-field-patch-plan --strict` valid；`openspec archive add-code-field-patch-plan --yes` 已同步 `cli-mcp-contract-fixtures`、`code-field-patch-plan`、`dataspec-cli` 主规格并归档；独立评审 agent `019f45e5-5ef6-7452-9d86-d538117d6574` 的 2 个 Important 和 1 个 Minor finding 均已修复并关闭 agent。
- 边界：不自动应用补丁，不保证识别所有动态 SQL；第一版聚焦 Java/SQL/JSON 等项目已有高频文件类型。

### P6-180：数据库直连采集作业断点续扫与限速保护
- 状态：已完成（2026-07-09）。
- 为什么做：大库反向导入、覆盖率和元数据浏览会遇到表多、网络慢、权限不一致或连接中断；一次性拉取失败后重来，会浪费时间也增加源库压力。
- 已有基础：已有数据库 schema dump、连接健康探测、大库扫描计划、元数据增量缓存、连接器能力探测和数据库直连只读安全诊断。
- 已完成能力：`/api/reverse-import/database/scan` 已兼容扩展 `scanJobId`、`status`、`resumeCursor`、`cancelToken`、`pageSize`、`rateLimit`、`retryPolicy`、`sourcePressureHint`、`partialResult`、`failureSummary`、`evidence` 和安全 `nextActions`；前端反向导入页支持分页扫描、继续、取消、失败摘要、只读 evidence、成功 partial tables 选择边界，并可通过短 `scanPartialId` 跳转覆盖率页生成部分覆盖率报告。
- 参考项目：`airbytehq/airbyte` 的连接器同步状态、`dagster-io/dagster` 的作业运行视图和 `singer-io/getting-started` 的 tap/state 思路；只借鉴断点与状态，不做后台数据同步平台。
- 落地产物：新增数据库 metadata 采集作业模型/API/前端进度视图；支持分页扫描、取消、恢复、限速、失败摘要和只读证据包。
- 验收标准：上千张表的元数据扫描可分批完成；中断后能从 cursor 恢复；取消或失败不会写入部分标准字段；所有连接信息继续遵守脱敏和最小权限边界。
- 验证证据：`mvn "-Dtest=FieldCoverageServiceImplTest,FieldCoverageControllerTest,DatabaseReverseImportServiceTest" test` 39/39 pass；`mvn test` 559/559 pass；`pnpm test` 164/164 pass；`pnpm build` 通过（保留既有 `@vueuse/core` pure annotation、chunk size、plugin timing warnings）；`openspec archive add-db-metadata-scan-jobs --yes` 已同步 4 个主规格并归档；`openspec validate --all` 122/122 pass；独立评审 agent `019f4667-53c9-7fd0-bc79-ee55c4d0689b` 和 `019f4674-704e-7b00-8033-1c7f3fec490e` 的 Critical/Important findings 已处理并关闭 agent。
- 边界：不扫描业务数据行，不做定时同步，不绕过源库权限；第一版只服务反向导入、覆盖率和元数据浏览。

### P6-181：标准维护 Inbox 到可执行工作流
- 状态：已完成（2026-07-09，第一版 dry-run）。
- 为什么做：覆盖率、字段质量、候选 Inbox、规则冲突和 AI 反馈已经能产生很多“该处理的事”，但用户和 AI 还需要把这些事项一键转成可执行 recipe，而不是在多个页面间手工拼步骤。
- 已有基础：已有标准候选 Inbox、AI 任务推荐队列、可复用 AI 工作流 Recipe、标准维护工作量估算、统一任务结果协议和前端命令面板。
- 已完成能力：新增只读标准维护 workflow plan API `POST /api/standard-maintenance/workflows/plan`，统一输出 `inboxAction`、`recipeBinding`、`dryRunSteps`、`executionState`、`undoHint`、`evidenceLinks` 和 `nextActions`；支持标准候选、字段质量、字段覆盖率和 AI 任务失败来源，质量计划按 `sourceIds` 收窄，partial coverage 证据保留 failed/skipped table counts。
- 参考项目：`backstage/backstage` 的开发者任务入口、`go-task/task` 的任务 recipe 和 `dagster-io/dagster` 的资产任务视图；只借鉴可执行步骤表达，不做团队排期系统。
- 落地产物：新增 `standardmaintenanceworkflow` 后端 controller/service/model、前端 API wrapper 和 `StandardMaintenanceWorkflowPlanPanel`；`StandardCandidate.vue`、`FieldQuality.vue`、`FieldCoverage.vue` 可生成维护 workflow dry-run；`standard-maintenance` workflow recipe 已进入 CLI/MCP/task-card/status-check 文档与测试；AI 推荐维护类任务可绑定 `standard-maintenance` recipe。
- 验收标准：选择候选、低质量字段或覆盖率缺口后，可生成“预检 -> 复核 -> 执行 -> 验证 -> 归档”的 dry-run 工作流，包含显式人工确认边界、验证命令、恢复提示和证据链接；不完整覆盖率来源不会把失败/未扫描字段视为已处理。
- 验证证据：`mvn test` 564/564 pass；`pnpm test` 167/167 pass；`pnpm build` 通过（保留既有 `@vueuse/core` pure annotation、chunk size、plugin timing warnings）；`node --test tools/*.test.mjs` 371 total，369 pass / 2 skipped；`openspec archive add-standard-maintenance-workflows --yes` 已同步 7 个主规格并归档；`openspec validate --all` 123/123 pass；`node tools/dataspec-status-check.mjs --format json` `status=pass`。
- 评审证据：独立只读子 agent `019f46a3-3645-7370-96d3-2d02099e7c49` 发现健康 action recipeBinding、字段质量 sourceIds、partial coverage failed/skipped evidence 和 tasks/evidence 收口问题；已全部修复并补回归测试。`close_agent` 返回 `not found`，记录为系统已清理或无法再次关闭。
- 边界：不自动批量采纳、合并、忽略或编辑标准字段，不持久化 workflow instance，不做后台任务调度；第一版只生成可复制、可验证、可恢复的 dry-run 计划。

### P6-182：前端页面对象模型与稳定测试选择器
- 状态：已完成第一版，已新增共享稳定选择器、AI action 名称、Playwright page object、route harness 和 POM 驱动的核心浏览器流程。
- 为什么做：前端页面越来越多，若 E2E 只靠文本和 CSS 选择器，页面微调会导致测试脆弱；AI browser automation 也需要稳定的页面对象和操作语义。
- 已有基础：已有前端源码级 smoke、浏览器级 E2E、端到端上手引导、前端操作录制、组件状态样例库和可访问性基线待办。
- 已完成能力：`stableTestIds` 统一声明 `data-testid` 策略和核心页面选择器；项目列表、字段库、SQL 校验、反向导入和 AI Context 页已接入稳定标识；`tests/e2e/pages` 提供 page object，`tests/e2e/support/routeHarness.ts` 提供可复用 API fixture；POM 用例通过 actionNames 完成核心流程并保留失败 URL、截图、视频和 trace。
- 参考项目：`microsoft/playwright` 的 Page Object Model、`testing-library/testing-library-docs` 的用户语义选择器和 `cypress-io/cypress` 的端到端测试组织；只借鉴测试结构，不重写前端框架。
- 落地产物：新增前端页面对象目录、稳定选择器约定和核心页面 fixture；覆盖项目选择、字段库、SQL 校验、反向导入、AI Context 等高频页面。
- 验收标准：E2E 用例能通过页面对象完成核心流程；页面文案调整不破坏选择器；AI 自动化脚本能复用 page object 输出的动作名称和失败截图。已通过 `pnpm test`、`pnpm build` 和 `pnpm exec playwright test` 验证；独立评审子 agent `019f46d4-d3bd-7c33-afc6-2b5a8e576df0` 发现的稳定选择器和 TSDoc 问题已修复并关闭。
- 边界：不做全量视觉回归，不要求所有组件立刻补选择器；第一版先覆盖主路径和高频故障页。

### P6-183：标准字段到数据库 COMMENT 回写计划
- 状态：已完成第一版，OpenSpec 已归档到 `openspec/changes/archive/2026-07-09-add-database-comment-patch-plan/`。
- 为什么做：反向导入能把现有数据库补进 DataSpec，但反过来，当字段标准被修正后，源数据库的表注释和列注释也可能长期落后；AI 需要一份可审阅的 COMMENT 回写计划，而不是直接生成不可控的修改 SQL。
- 已有基础：已有数据库直连 metadata、schema dump、二次比对、字段来源批次、schema plan 预览、DDL 生成和 SQL/DDL 验证沙箱待办。
- 已完成能力：新增只读 COMMENT patch plan API `/api/reverse-import/database/comment-plan`、CLI `comment-plan preview` 和反向导入页预览入口；响应包含 `commentPatchPlan`、`currentComment`、`targetComment`、`commentDiff`、`dryRunSql`、`dialectSupport`、`riskLevel`、`rollbackHint`、`evidence` 和 `nextActions`，并默认不执行 SQL、不写源库、不保存连接凭据。
- 参考项目：`bytebase/bytebase` 的数据库变更预览、`ariga/atlas` 的 schema diff 和 `k1LoW/tbls` 的数据库文档化；只借鉴注释差异表达，不默认写源数据库。
- 落地产物：新增 COMMENT 回写计划 API/CLI/前端预览；按 PostgreSQL/MySQL 方言生成 COMMENT ON 或 ALTER COMMENT 草稿，标记 no-op、missing、changed 和 unsupported；支持导出 SQL 与 JSON 证据。
- 验证证据：`mvn test` 569 pass；`pnpm test` 171 pass；`pnpm build` 通过，保留既有第三方 Rolldown/chunk warning；`pnpm gen:api` + `pnpm check:api` 确认 `schema.ts` 最新；`pnpm exec playwright test tests/e2e/page-object-contract.spec.ts` 1 pass；`node --test tools/*.test.mjs` 376 pass / 2 skipped；`openspec validate add-database-comment-patch-plan --strict` valid；archive 后 `openspec validate --all` 124 passed；`git diff --check` 通过。独立评审 agent `019f470b-0329-7982-b8c4-851e5625c8ef` 与复评 agent `019f4721-4b8f-7bd2-9481-6733053bf840` 均已完成并关闭，Important findings 已修复。
- 验收标准：连接数据库后能看到 DataSpec 标准注释与当前库注释差异；默认只输出可审阅计划和 dry-run SQL，不执行数据库写入；计划可进入证据包或标准变更 Patch Plan；输出内容不包含密码、token、完整 JDBC URL 或业务数据行。
- 边界：第一版只处理表/列注释，不处理表重命名、字段重命名、索引调整或数据迁移；真实执行仍交给用户或后续显式 apply 流程。

### P6-186：AI Context 质量预算与可用性评分
- 状态：已完成第一版，OpenSpec change `add-ai-context-quality-check` 已于 2026-07-08 归档到 `openspec/changes/archive/2026-07-08-add-ai-context-quality-check`。
- 为什么做：AI Context 已能裁剪和导出，但 AI 还需要知道“这份上下文是否够用”：字段是否缺枚举、样例是否太少、规则是否被截断、token 预算是否被低价值内容占满。
- 已有基础：已有 AI Context zip、按需裁剪、上下文预算、字段质量评分、标准查询 DSL、AI profile、Context 增量更新包和 Prompt 评测待办。
- 已完成能力：新增 CLI-only `context-quality check`，可本地只读读取已导出的 AI Context 目录、AI Context zip 或 `context-budget plan` JSON，输出 `contextQualityScore`、`qualityLevel`、`tokenBudgetBreakdown`、`missingCriticalResources`、`truncatedResources`、`coverageByCategory`、`taskFitHints` 和 `nextContextActions`；覆盖退化 budget plan、unsafe zip、未分类资源、fixture 三选一输入契约和敏感示例拦截。
- 参考项目：`promptfoo/promptfoo` 的 prompt 评测、`langfuse/langfuse` 的 trace/score 和 `OpenLineage/OpenLineage` 的运行元数据；只做确定性评分，不调用外部 LLM 打分。
- 产物：`tools/dataspec-cli.mjs`、CLI 单测、CLI/MCP contract fixture、OpenSpec change `add-ai-context-quality-check` 和 README CLI 说明；commit `0a1dca1`。
- 验证证据：`node --test tools/*.test.mjs` 347 pass / 2 skipped；`node tools/dataspec-cli-mcp-contract-check.mjs --format json` ok true；`openspec validate add-ai-context-quality-check --strict` valid；独立子 agent `019f3d19-63a0-7252-b194-24ddeda1d36b` 已完成只读评审并关闭，findings 已处理。
- 后续增强：补后端评分 API、前端 AI Context 页面展示、staleContextWarnings、missingCriticalFields 深度分析和真实任务结果回放；这些不属于 CLI-only 第一版。
- 边界：评分不替代真实任务结果，不保证 AI 一定生成正确；第一版只基于 DataSpec 元数据和导出内容做静态评估。

### P6-187：字段使用契约与禁用场景说明
- 状态：已完成第一版，OpenSpec change `add-field-usage-contracts` 已于 2026-07-08 归档到 `openspec/changes/archive/2026-07-08-add-field-usage-contracts`。
- 为什么做：同一个字段“是什么”不等于“什么时候该用”；AI 生成 SQL/DDL 时常会混用统计口径、展示字段、内部状态或废弃字段，需要字段级 usage contract 明确推荐场景和禁用场景。
- 已有基础：已有字段状态、敏感标记、字段格式约束、派生字段/单位规则、指标口径映射、字段知识卡、业务对象关系图和标准问答入口。
- 已完成能力：字段标准新增 `preferredUseCases`、`avoidWhen`、`joinHints`、`defaultFilters`、`aggregationHints`、`replacementGuidance` 和 `misuseExamples`；字段库创建/编辑、字段检索、标准问答、AI Context、DDL/Prompt guidance、标准快照、项目备份恢复和标准复用包均已读取或传播使用契约；命中禁用场景时会降级为需要确认，不把字段直接视为可采纳。
- 参考项目：`dbt-labs/dbt-core` 的模型文档、`OpenLineage/OpenLineage` 的上下游语义和 `open-metadata/OpenMetadata` 的资产说明；只借鉴使用说明结构，不建设重型血缘平台。
- 落地产物：扩展字段标准或新增轻量使用契约模型；字段详情、AI Context、DDL/Prompt 生成、标准问答和字段推荐可读取使用建议、禁用场景和常见误用。
- 验收标准：AI 查询“订单金额应该用哪个字段统计”时能看到单位、聚合、过滤、join hints 和禁用提示；废弃、内部状态或展示专用字段不会被推荐为写入字段；误用样例可进入规则、字段知识卡或问答提示。已通过后端、前端、OpenAPI、OpenSpec、独立评审和真实 PostgreSQL 写入/清空验证。
- 验证证据：`openspec validate add-field-usage-contracts --strict` valid；`mvn test` 553 pass；`pnpm test` 162 pass；`pnpm build` 通过，保留既有第三方 pure annotation、chunk size 和 plugin timings warning；真实 OpenAPI `node scripts/check-openapi-schema.mjs --source http://localhost:18092/api-docs` 通过；用户授权的一次性 `ai_test` 中已启动临时后端/前端，`tools/dataspec-local-smoke.mjs --skip-demo` web/api-docs pass，并通过 API 新建一次性项目 `projectId=2`、字段 `fieldId=11`，成功写入和读回 usage contract；评审修复后再次连接 `ai_test` 新建一次性项目 `projectId=5`、字段 `fieldId=14`，确认七个 usage contract 字段可通过更新接口清空并读回为空，且“统计订单金额”不会因只共享“金额”单个中文 bigram 被误降级。
- 评审记录：独立只读子 agent `019f3d66-fa51-74d2-91b0-e63149641419` 未发现 Critical，2 个 Important 已修复并关闭；修复点为 usage contract 字段允许 null 更新、禁用场景中文匹配阈值和 suggest 降级 evidence。
- 真实库遗留：完整 demo smoke 在复用旧 `ai_test` 演示项目时命中既有 `ds_rule_baseline.applied_at` 的 TIMESTAMPTZ 到 `LocalDateTime` 映射问题，已用不导入内置标准的一次性项目绕开并验证本变更真实写入；该历史数据问题不属于 P6-187。
- 边界：不做完整指标平台，不要求每个字段都补齐契约；第一版优先高风险金额、状态、时间、用户和敏感字段。

### P6-188：标准问答答案可采纳度与低置信处理
- 状态：已完成第一版。
- 为什么做：标准问答能让 AI 或用户快速问“字段叫什么”，但答案如果证据不足、标准冲突或命中候选字段，应该明确低置信，而不是给出一个看似确定的字段名。
- 已有基础：已有字段检索、标准问答入口、业务术语表、标准证据置信度、AI 输出引用证据、候选 Inbox、字段质量评分和标准查询 DSL 待办。
- 已完成能力：标准问答结果新增 `answerStatus`、`answerability`、`confidenceReason`、`missingEvidence`、`missingFacts`、`candidateOnly`、`conflictingStandards/conflicts`、`evidenceRefs`、`suggestedNextQuery`、`escalateToInbox` 和 `nextActions`；前端答案面板展示可直接采用 / 需要确认 / 不能回答、缺失证据、标准冲突和建议追问。
- 参考项目：`sourcegraph/sourcegraph` 的搜索解释、`promptfoo/promptfoo` 的评测断言和 `langfuse/langfuse` 的评分记录；只借鉴答案评分与证据展示，不接入在线问答模型。
- 落地产物：为标准问答和字段搜索增加可采纳度摘要；输出 answerStatus、confidence、evidenceRefs、missingFacts、conflicts 和 nextActions；前端展示低置信提示，CLI/MCP 可机器读取。
- 验收标准：同义词冲突、候选未采纳、字段缺格式、低质量字段或无命中时，问答不会伪装成确定答案；AI 能根据 answerStatus 决定采用、追问、转候选或停止。
- 验证证据：`node --test tests/standardQuestionDisplay.test.ts` 12 pass；`pnpm test` 159 pass；`pnpm build` 通过，保留现有第三方 pure annotation、chunk size 和 plugin timings warning；`git diff --check` 通过，仅 LF/CRLF warning；当时 `node tools/dataspec-status-check.mjs --format json` warn 仅因 active change `add-ai-context-quality-check`，该 warning 已随 P6-186 归档消除。
- 评审证据：独立子 agent `019f3d2f-b5c4-75d3-886d-c1c83c759dab` 完成只读评审并已关闭；发现的格式证据缺失 P1 已补失败测试并修复，停用字段覆盖和废弃字段替代信息误报风险已处理。
- 边界：不实现通用自然语言问答引擎，不调用外部 LLM；第一版基于现有检索、术语表、证据和质量分确定性判断。

### P6-190：稳定引用、历史别名与 Evidence claim 真实性修复
- 状态：已完成并归档，OpenSpec change 为 `fix-stable-reference-evidence-truthfulness`。
- 已完成能力：从项目级字段变更日志的 `beforeJson`/`afterJson` 白名单字段派生历史名称和别名，接入 stable reference resolve、字段检索和字段推荐；当前名称/别名保持更高优先级，历史歧义不猜测，损坏快照按记录跳过且不暴露原文。
- 已完成能力：新增 project-scoped Evidence claim resolver 和 canonical URI，覆盖 SQL check、AI job、AI batch run、AI task run；post-check 区分 `VERIFIED`、`MISSING`、`CROSS_PROJECT`、`UNVERIFIABLE`，只把真实引用加入 `evidenceLinks`，跨项目结果不泄露来源元数据；Evidence Package 仅为持久化来源输出 additive `source.evidenceRef`。
- 契约同步：Schema Registry、OpenAPI 生成类型、CLI/MCP fixture 和 PR review summary/inline/fallback 主规格已同步；自然语言句尾标点不会再污染 canonical Evidence URI。
- 验证证据：见归档 change 的 `tasks.md` `Verification Evidence`；覆盖后端全量测试、前端测试与 build、tools、OpenSpec strict/all、OpenAPI 漂移、Docker API 闭环、diff/secrets/status 门禁。
- 评审证据：首个评审 agent `019f5b7c-9dc6-71e1-8284-d513c6604a74` 超时后已关闭；替代评审 agent `019f5b86-fba8-70d2-abc6-43a46012d942` 已完成并关闭，1 个 Important 和 1 个 Minor finding 均已补失败测试或查询契约测试并修复。
- 后续增强：不新增历史别名表、不回填不可推导历史、不持久化 Evidence Package；统一 Finding/Evidence 与 AI/PR 评审闭环继续由 `P6-191` 承接。

### P6-153：AI Context 注入防护与不可信文本隔离
- 状态：已完成第一版，OpenSpec change `add-ai-context-safety-controls` 当前按项目约定保留为 active change。
- 已完成能力：AI Context package 的 `manifest.json` 新增 `contextSafetySummary`，`.dataspec/README.md`、`.dataspec/prompts.md` 和 `AGENTS.md.fragment` 明确 DataSpec 指令/契约与字段注释、样例、SQL、业务描述、glossary、metadata 等不可信业务内容的边界；字段目录新增 `contextSafety`，记录 `sourceTrustLevel`、`instructionBoundary`、redaction reasons 和 warnings。
- 验证证据：`mvn -Dtest=AiContextExportServiceTest,AiContextControllerTest,AiContextBudgetPlannerServiceTest,SensitiveDataSanitizerTest test` 48 pass；`node --test tools\dataspec-config.test.mjs tools\dataspec-mcp.test.mjs` 52 pass；`node --test tools\dataspec-cli.test.mjs tools\dataspec-cli-mcp-contract-check.test.mjs` 184 pass、2 个 symlink skip；`openspec validate add-ai-context-safety-controls --strict` 通过；`openspec validate --all` 125 passed；`git diff --check` 通过，仅 LF/CRLF warning；状态检查仅 `OPENSPEC_ACTIVE_CHANGE_PRESENT` warning，符合 active change 暂保留约定。
- 评审证据：独立评审 agent `019f4764-7724-7232-a90b-b3a29f132333` 多轮只读复评发现并跟踪敏感 format examples、snapshot field catalog、scoped matchReasons/query、glossary/enum raw text、规则 metadata、scope metadata、rule exemption、status matchReasons 和 standard metadata 等泄漏面；已全部修复并补回归测试或防御性脱敏，最终结论 Critical/Important/Minor 均无，`Ready to merge: Yes`，agent 已关闭。
- 后续增强：不替代专业 DLP，不扫描真实业务数据行；后续如要做更强 prompt-injection 分类，可在本地策略和 AI Context 质量预算主题中继续增强。

### P6-158：字段可见性等级与 AI Context 最小暴露策略
- 状态：已完成第一版，作为 `add-ai-context-safety-controls` 的字段级安全决策部分交付。
- 已完成能力：字段目录每个字段新增 `exportDecision`，敏感字段默认 `visibility=restricted`、`maskingProfile=metadata-only`，主 example 和 format valid/invalid examples 等 example-like 值输出 `[REDACTED]`；live 和 snapshot 导出路径都输出安全元数据，并把 redacted/restricted/warning 计入 package safety summary。
- 验证证据：`AiContextExportServiceTest` 覆盖 live field、snapshot field、scoped field、format examples、prompt 输入、usage examples、glossary、enum、rule metadata、rule exemption、matchReasons 和 standard metadata 的脱敏；相关后端目标测试 48 pass；OpenSpec strict 和 all 均通过。
- 后续增强：第一版不新增企业权限审批，也不回溯删除历史 Context 包；如后续需要按 AI profile 进一步裁剪字段，可承接稳定引用、查询 DSL 或安全预检主题。

### P6-164：个人安全红线配置中心
- 状态：已完成最小本地配置第一版，作为 AI Context 安全主题配置子项交付。
- 已完成能力：`.dataspec/config.json` 支持可选 `securityProfile`，CLI/MCP 配置加载会规范 `redactionStrictness`、`sensitiveFieldPolicy`、`allowedAiTools`、`neverExportPatterns`、`localOnlyPaths`、`samplePolicy` 和 `credentialPolicy`；MCP `session-state` 只输出 profile presence、policy names 和数组计数，不输出 raw pattern、local path 或 secret-like 值。
- 验证证据：`tools/dataspec-config.test.mjs` 覆盖合法 profile 和非法类型诊断；`tools/dataspec-mcp.test.mjs` 覆盖 session-state 安全摘要不泄漏 raw pattern/token/password/JDBC/local path；tools 目标测试 52 pass，CLI/MCP 契约扩展测试 184 pass、2 个 symlink skip。
- 后续增强：本轮不做前端配置页和 `.dataspec/security.json` 独立 schema；后续如需要可从配置 schema、doctor 诊断或安全红线 UI 单独开任务。

### P6-165：标准对象稳定标识与引用别名层
- 状态：已完成第一版，OpenSpec change `add-stable-standard-refs-and-ai-output-checks` 当前按项目约定保留为 active change，不自动 archive。
- 已完成能力：新增 project-scoped stableRef/canonicalRef 语义和 `StandardReferenceResolutionService`，字段搜索、字段模型、AI Context、Schema Registry、Evidence、CLI `ref resolve`、MCP `resolve_standard_refs`、CLI/MCP 契约 fixtures 均可读取稳定引用、生命周期状态、replacementRef、aliasHistory 和 secret-safe 解析结果。
- 验证证据：`mvn test` 593 pass；后端复评目标测试 63 pass；`node --test tools/*.test.mjs` 387 pass、2 个 symlink skip；`pnpm test` 176 pass；`pnpm build` 通过，保留既有 `@vueuse/core` pure annotation 和 chunk size warning；`pnpm check:api` 确认 schema 最新；`openspec validate add-stable-standard-refs-and-ai-output-checks --strict` valid；`openspec validate --all` 126 passed；`git diff --check` 通过，仅 LF/CRLF warning。
- 评审证据：评审 agent `019f4ca0-6769-72c3-bed6-077c7cdb9f88`（Locke）提出 4 个 Important 和 2 个 Minor，全部修复并关闭；复评 agent `019f4cc7-e682-7f50-9624-77f780f99da2`（Kepler）确认原 findings 均关闭，无 Critical/Important，`Ready to commit: Yes`，其 summary fallback 字段 Minor 已修复，agent 已关闭。
- 后续增强：第一版不新增稳定 ID 数据库列，不强制改写历史记录；跨项目 stableRef 映射、严格 alias history 表和消费端兼容套件可承接后续 P6-167/P6-176。

### P6-166：AI 输出后置校验与幻觉引用拦截
- 状态：已完成第一版，随 `add-stable-standard-refs-and-ai-output-checks` 交付。
- 已完成能力：新增只读 AI output post-check API、CLI `ai-output check`、MCP `check_ai_output`、前端 AI Replay 复制前 PASS/WARN/FAIL 门禁、AI Export CLI 命令提示、Evidence Package postCheckSummary、Schema Registry post-check contract 和 fixture drift 检查；校验 SQL/DDL/Markdown/JSON/plain text 中可确定的字段、枚举、规则、快照和 evidence refs，并输出 resolvedRefs、issues、replacementRefs、evidenceLinks、nextActions 和 safeToUse。
- 验证证据：`AiOutputPostCheckServiceImplTest`、`AiOutputPostCheckControllerTest`、`dataspec-cli.test.mjs`、`dataspec-mcp.test.mjs`、`dataspec-cli-mcp-contract-check.test.mjs` 和 `dataspec-web/tests/aiOutputPostCheckDisplay.test.ts` 均已接入统一验证；完整命令结果同 P6-165 验证证据。`node tools/dataspec-status-check.mjs --format json` 仅 `OPENSPEC_ACTIVE_CHANGE_PRESENT` warning，符合两个 active change 暂保留约定。
- 评审证据：同 P6-165；独立复评已确认 SQL 隐式 alias、JSON enum 字段顺序、MCP sensitive input、OpenAPI schema 和前端 replacementRef 展示均已关闭回归风险。
- 后续增强：第一版只做 DataSpec 可确定的引用校验，不判断自然语言全部事实，不自动调用外部 LLM 改写；后续可在查询 DSL、测试数据包和消费端兼容套件中扩展覆盖。

### P6-167：标准查询 DSL 与可组合筛选协议
- 状态：已完成第一版，OpenSpec change `add-standard-query-dsl` 当前按项目约定保留为 active change，不自动 archive。
- 已完成能力：新增项目内只读 Standard Query DSL v1，支持 `FIELD` target、text、category/tag/status/sensitive/sourceBatchId/stableRef/canonicalRef/hasExample/updatedSince、limit、strict 和 explain；新增 `POST /api/standard-query/search`、字段搜索 legacy 参数到 DSL 的确定性映射、AI Context field catalog/package DSL scope、Schema Registry DSL request/result/filter/summary/error schema、CLI `search-fields --dsl/--dsl-file/--stdin`、MCP `search_fields.standardQuery`、前端 API wrapper/类型/摘要展示和 CLI/MCP fixture drift 检查。
- 验证证据：`mvn -Dtest=StandardQueryServiceImplTest,StandardQueryControllerTest,AiContextExportServiceTest,FieldServiceImplTest,SchemaRegistryServiceImplTest,PerformanceBaselineTest test` 107 pass；`mvn test` 607 pass；`pnpm test` 180 pass；`pnpm build` 通过，保留既有 Rolldown pure annotation、chunk size 和 plugin timing warning；`node --test tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs` 217 total，215 pass / 2 skipped；`node --test tools/*.test.mjs` 397 total，395 pass / 2 skipped；`openspec validate add-standard-query-dsl --strict` valid；`openspec validate --all` 127 passed；`node tools/dataspec-status-check.mjs --format json` 仅 3 个 `OPENSPEC_ACTIVE_CHANGE_PRESENT` warning，符合 active change 暂保留约定；`git diff --check` 通过，仅 LF/CRLF warning；diff secrets scan 命中均为脱敏说明、测试假值、字段名或 token 变量名，未见真实凭据。
- 评审证据：独立评审 agent `019f4ec7-88fc-7232-b365-90c9a03adf3c`（Helmholtz）完成只读评审；初评指出 AI Context 未复用 DSL、stableRef/canonicalRef 未校验 projectId、validation error schema 未接入三项 P2，均已修复；复评指出 `R.error` 未携带 `STANDARD_QUERY_DSL_INVALID` 的 P2，已补后端 handler 和 CLI/MCP 回归测试。最终复评 agent `019f4f0a-6f20-7433-917e-a1d7eada074e`（Curie）指出 `canonicalRef` 过滤未按 replacement canonical 语义执行、DSL limit 契约与字段搜索实际上限不一致；已补失败测试并修复为 canonicalRef 匹配字段实际 canonical 引用、DSL limit 统一为 1..50。Curie 复评结论 Approved，无阻塞 findings，agent 已关闭。
- 后续增强：第一版不做任意 SQL 查询、全文搜索平台或跨项目查询；非 FIELD target 仍通过 ignoredFilters/strict validation 提示后续扩展；可在 `P6-176` 消费端兼容套件中进一步固化多消费端 golden payload。

### P6-76：业务对象关系图与表模板依赖
- 状态：已完成第一版，随 OpenSpec change `add-business-object-table-standards` 交付；change 按项目约定暂保留 active，不自动 archive。
- 已完成能力：新增业务对象与表结构标准闭环，支持项目级业务对象、表模板关联、必选/可选字段、关系、外键提示、审计字段、常见反模式和 AI 使用说明；新增只读 table standards API，AI Context 导出 `.dataspec/table-standards.json`，前端模板管理和 DDL 生成页可维护/查看结构标准与关系摘要。
- 验证证据：见 `openspec/changes/add-business-object-table-standards/tasks.md` 的 `Verification Evidence`；最终以本轮 OpenSpec strict、后端目标测试、tools 测试、前端测试/build、secrets scan、独立评审和本地 commit 记录为准。
- 后续增强：第一版不做完整 ER 建模器、不做拖拽图编辑、不连接业务库应用关系、不读取业务数据行；更复杂的数据域/对象图、迁移交付和变更预演可由后续主题承接。

### P6-106：表级约束、索引与主外键标准
- 状态：已完成第一版，随 OpenSpec change `add-business-object-table-standards` 交付；change 按项目约定暂保留 active，不自动 archive。
- 已完成能力：表模板可携带 `primaryKey`、`uniqueKeys`、`indexes`、`foreignKeys`、`checkHints`、`auditPolicy`、`softDeletePolicy`、`dialectNotes` 和 `aiUsageNotes`；DDL preview 安全消费结构化约束，返回 `structureSummary`、skipped hints、policy notes 和 evidence；Schema Registry、CLI、MCP 和 AI contract 均登记新增只读契约。
- 验证证据：见 `openspec/changes/add-business-object-table-standards/tasks.md` 的 `Verification Evidence`；最终以本轮 OpenSpec strict、后端目标测试、tools 测试、前端测试/build、secrets scan、独立评审和本地 commit 记录为准。
- 后续增强：第一版不执行数据库迁移、不自动改写已有表、不允许 raw SQL 约束片段直接进入 DDL；完整方言差异、反向导入约束比对和迁移 recipe 可在后续主题中继续推进。

### P6-77 / P6-107 / P6-161 / P6-175 / P6-184：字段语义、枚举、口径与命名知识卡
- 状态：已完成第一版，随 OpenSpec change `add-field-semantics-knowledge-cards` 交付；change 按项目约定暂保留 active，不自动 archive。
- 已完成能力：新增字段语义规则、枚举值 lifecycle、字段知识卡、指标口径映射和字段命名翻译辅助；后端提供维护/只读 API，前端字段库、枚举字典、DDL/指标维护和 AI Context 页面可维护或查看语义证据；AI Context 导出 `.dataspec/field-knowledge-cards.json`、`.dataspec/field-semantics.json` 和 `.dataspec/metrics.json`；Schema Registry、数据字典、字段搜索/推荐、CLI、MCP 和 contract fixture 已接入新增只读契约。
- 验证证据：见 `openspec/changes/add-field-semantics-knowledge-cards/tasks.md` 的 `Verification Evidence`；最终以本轮 OpenSpec strict、后端目标测试、tools 测试、前端测试/build、OpenAPI drift、secrets scan、独立评审和本地 commit 记录为准。
- 后续增强：第一版只沉淀 metadata guidance，不执行真实单位换算或指标计算，不连接业务库统计枚举分布，不自动改生产 SQL，不接入外部翻译、BI 或血缘平台；后续若需要更强枚举 literal lint、指标计算校验或语义检索，可按独立主题继续推进。

### P6-84：前端可访问性与键盘操作基线
- 状态：已完成第一版，按常规模式交付；不涉及 API、存储或 OpenSpec 契约变更。
- 已完成能力：应用外壳新增 skip link、`main` landmark、主导航 landmark 和路由切换主内容聚焦；命令面板支持可读快捷键入口、搜索框自动聚焦、禁用命令语义和执行命令时目标页面 / 弹窗接管焦点；项目列表、SQL 校验、字段库和反向导入补齐高频按钮 / 输入的稳定可读名称，并保持 Label in Name；新增弹窗关闭后恢复触发焦点的 `useDialogFocusReturn` composable；SQL 记录详情、项目弹窗、API Token 弹窗和连接预设弹窗已接入焦点恢复。
- 已完成能力：新增源码级可访问性基线测试 `dataspec-web/tests/accessibilityBaseline.test.ts` 并接入 `npm test`；新增 Playwright 键盘 E2E `dataspec-web/tests/e2e/accessibility-keyboard.spec.ts`，覆盖项目创建、skip link、命令面板快捷键 / 执行命令、SQL 校验、记录详情焦点恢复和字段库筛选；E2E route harness 补齐只读背景 API fixture，避免正常后台请求被误报为未覆盖。
- 验证证据：`node --test tests/accessibilityBaseline.test.ts` 通过；`npx.cmd playwright test tests/e2e/accessibility-keyboard.spec.ts` 通过；`npm.cmd test` 187 pass；`npm.cmd run build` 通过，仅保留既有 Rolldown pure annotation、chunk size 和 plugin timing warning；`git diff --check` 通过，仅有 Windows LF/CRLF 提示。
- 独立评审证据：子 agent `019f54ee-3927-7013-a998-6c41a74ff896` 已完成并关闭；评审发现 2 个 Important 和 2 个 Minor，已修复 Label in Name、命令面板执行后焦点抢占、Monaco 外层多余 tab stop 和主导航 landmark 语义问题，并复跑验证通过。
- 后续增强：第一版不承诺 WCAG 全量认证，不重做完整视觉系统；前端性能指标、端到端上手引导、问题反馈采集和字段库密集键盘编辑继续由 `P6-86`、`P6-116`、`P6-122` 和 `P6-151` 承接。

### P6-185 / P6-176：标准测试数据包与消费端兼容验收套件
- 状态：第一版已完成实现、验证、独立评审和本地 commit 收口；OpenSpec change `add-standard-test-data-compat-suite` 按项目约定暂保留 active，不自动 archive。
- 已完成能力：新增只读标准测试数据包 API `POST /api/test-data/package/generate`，按字段标准、枚举、格式约束、敏感标记和轻量对象提示生成 deterministic valid/invalid/boundary cases、mock payload、CSV 行和 SQL seed 草稿；新增 CLI `test-data generate`、MCP `generate_test_data_package`、Schema Registry `standard-test-data-package` 契约和前端 API type wiring。
- 已完成能力：新增本地消费端兼容套件 `tools/fixtures/consumer-compatibility-suite.json` 与 `consumer-compat check`，覆盖 Schema Registry、AI Context、CLI JSON、MCP descriptor/resource/tool、CLI/MCP fixture 和测试数据包 golden payload；新增 `consumer-compatibility-suite`、`consumer-compatibility-adapter-result` 和 `consumer-compatibility-breaking-rule` 契约。
- 验证证据：见 `openspec/changes/add-standard-test-data-compat-suite/tasks.md` 的 `Verification Evidence`；已完成后端目标/全量测试、tools 目标/全量测试、前端测试/build、OpenSpec strict/all、OpenAPI drift 环境风险记录、secrets scan、独立评审和本地 commit 记录。
- 后续增强：第一版不采样真实业务数据、不写业务数据库或业务仓库、不调用外部 LLM，不做第三方认证体系；SQL seed 仅为草稿，必须保留 `executable=false` 或 `requiresReview=true` 语义。更完整的第三方 adapter、协议导出和业务规则级测试数据可由后续契约消费主题承接。

## 本轮候选覆盖归档（2026-07-09）

以下条目原位于 P6 候选池，经评审确认已被现有能力覆盖，归档为不再独立排期的完成项。

### P6-94：标准来源可信度与 AI 置信度标记
- 状态：已归档，独立候选已被现有能力覆盖（2026-07-09）。
- 处理原因：已有 field-provenance-confidence 相关主规格和字段来源/可信度能力表面；独立候选改为二期增强，不再占用默认队列。
- 处理结论：不再作为默认后续开发项；如后续出现缺口，按对应主规格或二期增强重新开任务。
- 为什么做：AI 使用数标时需要知道哪些字段是人工确认的核心标准、哪些是数据库反向导入候选、哪些是样例生成或低置信度推断；否则容易把“疑似标准”当成“强制标准”使用。
- 已有基础：已有字段来源批次、变更日志、标准快照、标准候选 Inbox、字段质量评分、冲突检测、AI 输出证据和反向导入映射待办。
- 缺口：字段与规则缺少统一 provenance/confidence 结构；AI Context、字段推荐和标准问答无法稳定表达“建议使用但需确认”“已废弃但仍被引用”“仅来自样例”的差异。
- 落地产物：为字段、别名、枚举、规则和模板补充来源证据模型；输出 sourceType、sourceRef、verifiedBy、verifiedAt、confidenceLevel、evidenceCount、lastSeenAt 和 warning；前端字段详情和 AI Context 展示可信度摘要。
- 验收标准：AI 能区分 confirmed、imported、generated、deprecated、conflicting 等来源状态；低置信度字段不会被推荐为首选；标准快照包含可信度摘要且可回放。
- 边界：不做复杂组织认证流程，不引入人工审批；可信度只辅助决策，不自动删除或隐藏已有标准。

### P6-97：标准使用热区与清理优先级报告
- 状态：已归档，独立候选已被现有能力覆盖（2026-07-09）。
- 处理原因：已有 standard-usage-heatmap 相关主规格和标准使用热区能力表面；独立候选归档为已覆盖。
- 处理结论：不再作为默认后续开发项；如后续出现缺口，按对应主规格或二期增强重新开任务。
- 为什么做：标准字段、规则和候选越来越多后，用户需要知道哪些标准被 SQL、DDL、数据库表、AI 任务和业务代码频繁使用，哪些长期无人使用或冲突高，才能优先清理真正影响 AI 输出质量的部分。
- 已有基础：已有字段影响分析、字段覆盖率、业务代码引用索引、AI 使用画像、字段质量评分、冲突检测、检查记录和 AI 回放。
- 缺口：缺少跨来源 usage heatmap 和 cleanup priority；字段列表只能看静态属性，无法按“高使用低质量”“高冲突高影响”“长期未命中”排序。
- 落地产物：新增标准使用热区报告；聚合 fieldUsageCount、lastReferencedAt、sourceKinds、qualityScore、conflictCount、aiJobHits、lintHits、cleanupPriority 和 suggestedNextAction；前端提供可筛选列表和跳转。
- 验收标准：用户能一眼看到最值得优先修的字段、规则和模板；AI 可读取报告先处理高影响标准，而不是随机优化；报告生成不读取业务数据行。
- 边界：不做团队 KPI，不上传使用统计，不以使用次数自动删除低频字段；第一版只聚合 DataSpec 已有记录和用户指定扫描结果。

### P6-114：AI 任务推荐队列与下一步编排
- 状态：已归档，独立候选已被现有能力覆盖（2026-07-09）。
- 处理原因：AI task recommendation queue 已有 API/schema 与推荐队列能力表面；独立候选归档为已覆盖。
- 处理结论：不再作为默认后续开发项；如后续出现缺口，按对应主规格或二期增强重新开任务。
- 为什么做：DataSpec 已有很多页面和命令，但 AI 或用户常见问题是“不知道下一步该跑覆盖率、导出 Context、修规则还是采纳候选”；需要把诊断结果变成可执行任务队列。
- 已有基础：已有个人工作台、AI 一页式工作台待办、AI 任务卡、AI 反馈报告、字段质量、覆盖率、标准候选 Inbox、`dataspec doctor` 和 workflow recipes。
- 缺口：缺少项目级 recommended task queue，无法按当前项目状态生成优先级、依赖、输入参数、可复制命令和完成判定。
- 参考项目：`backstage/backstage` 的开发者入口、`langfuse/langfuse` 的任务观测和 MCP tools/prompts 的可执行描述；只做本地推荐，不做自动代理执行。
- 落地产物：新增任务推荐 API/前端分区；根据 missingProject、noFields、lowCoverage、pendingCandidates、staleContext、failingLint、openAiFeedback 等信号生成任务卡，包含 actionType、priority、reason、command、targetRoute 和 completionCheck。
- 验收标准：新项目、已有数据库项目和 AI 反馈较多项目会得到不同任务顺序；每张任务卡都能跳转到页面或复制 CLI/MCP 命令；完成后任务状态能刷新消失或降级。
- 边界：不自动执行写操作，不引入后台调度，不替代用户判断；第一版只推荐 DataSpec 内已有能力或明确待办中的 dry-run 动作。

### P6-115：跨来源标准证据视图
- 状态：已归档，独立候选已被现有能力覆盖（2026-07-09）。
- 处理原因：跨来源标准证据视图已有 README/API schema 与 cross-source evidence 能力表面；独立候选归档为已覆盖。
- 处理结论：不再作为默认后续开发项；如后续出现缺口，按对应主规格或二期增强重新开任务。
- 为什么做：字段标准的证据分散在数据库 metadata、SQL 检查、AI job、候选、变更日志、文档和接口契约里；AI 要解释一个字段是否可信时，需要一页聚合证据而不是翻多个页面。
- 已有基础：已有字段来源、变更日志、AI 回放、AI 反馈、标准候选、标准决策理由库待办、Explain Trace、执行证据包和项目活动时间线。
- 缺口：缺少以字段/表/规则为中心的 evidence timeline，无法稳定回答“这个标准来自哪里、被谁用过、最近哪里冲突、哪些材料支持它”。
- 参考项目：`datahub-project/datahub` 和 `open-metadata/OpenMetadata` 的资产详情页、`OpenLineage/OpenLineage` 的 lineage event；只借鉴证据聚合，不建设重型数据目录。
- 落地产物：新增只读证据视图或 API；按 subjectType/subjectId 汇总 sourceEvents、aiUsages、lintHits、candidateDecisions、changeLogs、documentRefs、contractRefs 和 confidenceSummary；AI Context 可按需引用证据摘要。
- 验收标准：打开某个标准字段能看到来源、采纳记录、最近 AI 使用、相关 SQL 问题和决策理由；证据摘要可复制给 AI，且不包含敏感连接信息或业务数据行。
- 边界：不做全量血缘平台，不自动判断证据真伪，不把临时低置信度证据写成正式标准；第一版只聚合 DataSpec 已保存的安全记录。

### P6-148：可复用 AI 工作流 Recipe 编排
- 状态：已归档，独立候选已被现有能力覆盖（2026-07-09）。
- 处理原因：workflow recipe 已通过 CLI/MCP/README/AI Context 暴露，且已有 task-card 绑定；独立候选归档为已覆盖。
- 处理结论：不再作为默认后续开发项；如后续出现缺口，按对应主规格或二期增强重新开任务。
- 为什么做：很多 AI 任务不是单个 API 调用，而是 doctor、preflight、导出 Context、执行 lint、生成 fixedSql、导出证据包等步骤的组合；每次在聊天里临时拼命令容易漏验证和边界。
- 已有基础：已有 MCP/CLI 工作流模板、AI 任务卡、AI 任务推荐队列、任务结果协议、执行证据包、doctor 和 preflight 待办。
- 缺口：缺少 machine-readable workflow recipe，无法表达 steps、inputs、requiredCapabilities、verificationCommands、artifacts、rollbackHint 和 blockedReason。
- 参考项目：GitHub Actions reusable workflow、`go-task/task` 的本地任务组织和 `casey/just` 的命令 recipe；只借鉴步骤声明，不引入远程任务调度平台。
- 落地产物：新增 `.dataspec/workflows/*.json|yaml` 约定、CLI `dataspec workflow list/run --dry-run` 和 MCP prompt/resource；内置 `safe-sql-fix`、`reverse-import-review`、`export-ai-context` 等个人高频流程。
- 验收标准：AI 能列出当前项目可执行工作流，先 dry-run 展示步骤和验证命令，再逐步执行并产出 TaskResult；失败步骤能给出可恢复位置和下一步建议。
- 边界：不做长任务队列，不自动执行高风险写入，不替代 OpenSpec 实施流程；第一版只编排已有能力和只读/显式确认步骤。

### P6-170：标准维护工作量估算与任务批量拆分
- 状态：已归档，独立候选已被现有能力覆盖（2026-07-09）。
- 处理原因：标准维护 workflow 与推荐队列已覆盖维护任务拆分的主要价值；独立候选归档为已覆盖。
- 处理结论：不再作为默认后续开发项；如后续出现缺口，按对应主规格或二期增强重新开任务。
- 为什么做：DataSpec 已能发现低质量字段、候选、导入差异、规则冲突和 AI 失败记录，但用户还需要知道“先做哪 20 分钟最值”；AI 也需要把一堆标准维护问题拆成可执行小任务，而不是一次性尝试全修。
- 已有基础：已有字段质量评分、覆盖率报告、候选 Inbox、个人健康摘要、AI 任务推荐队列、TODO 到 OpenSpec 交接、执行证据包和统一任务结果协议待办。
- 缺口：缺少 workEstimate、batchPlan、taskSlices、expectedImpact、riskLevel 和 verificationCommands；健康摘要能看到问题，但还不能按投入产出拆成可执行批次。
- 参考项目：GitHub Actions job summary、`backstage/backstage` 的开发者任务入口和 `dagster-io/dagster` 的资产任务视图；只借鉴任务摘要和优先级表达，不做团队排期系统。
- 落地产物：新增标准维护任务拆分 API/CLI；把低质量字段、未纳管字段、导入候选、规则冲突、覆盖率缺口和 AI 反馈失败项聚合成 15/30/60 分钟任务包；每个任务包包含目标对象、来源证据、预计收益、验证命令和回滚/跳过说明。
- 验收标准：打开项目后能生成“本次最值得处理的 3 个维护批次”；每个批次能跳转到候选、字段、规则或覆盖率来源；AI 可按批次逐步执行并产出 TaskResult；完成后健康摘要和任务建议会更新。
- 边界：不做团队工时估算，不自动修改标准，不把估算当承诺；第一版按本地项目指标给出启发式建议。

## 第二轮候选覆盖归档（2026-07-12）

以下 7 个候选编号的核心用户价值已由现有能力覆盖，因此按完成覆盖归档，不再重复实现。原始候选全文见 [todo-p6-candidates-2026-07-12.md](todo-p6-candidates-2026-07-12.md)。

| 编号 | 已覆盖内容 | 验证 / 产物证据 | 后续遗留 |
| --- | --- | --- | --- |
| P6-109 | AI 能力边界模拟与安全预检 | capability catalog、session bootstrap、task profile、write safety 与 OpenSpec readiness 已提供前置检查、写风险和停止条件；相关实现与测试随各自完成 commit 保留 | 只在发现新的真实越权路径时补安全用例 |
| P6-135 | AI 任务预检与缺口补齐建议 | session bootstrap、capability check、task-card BLOCKED 状态和 readiness report 已输出缺失输入与 nextActions | 不再新增第二套通用 preflight DTO |
| P6-131 | 受控样例值与合成回填 | synthetic examples 和 standard test data package 已提供脱敏、可复现的测试样例与边界用例 | 不读取真实业务数据行；若未来确需采样，重新按安全 SDD full 评审 |
| P6-143 | Agent 专用项目启动包 | `dataspec init`、AI session bootstrap、MCP guidance pack 和 `.dataspec/config.json` 已形成项目启动入口 | 后续只补发现到的缺失指引，不再建设平行启动包 |
| P6-146 | 个人标准健康摘要与下一步报告 | Standard Health 已提供健康快照、趋势、Top actions 和可复制改进计划 | 指标增强进入 Standard Health 主规格 |
| P6-156 | OpenAPI/CLI/MCP 示例契约快照 | CLI/MCP contract fixtures、成功/失败示例、schema 校验和 drift check 已接入 tools 测试 | 新命令按现有 fixture 增量维护 |
| P6-159 | AI 任务状态机与断点续跑 | AI task run 已持久化状态、失败步骤、partial artifacts、幂等键、retryable、resumeCommand，并由 API/CLI/MCP/前端消费 | 不引入外部队列或分布式工作流引擎 |

- 归档日期：2026-07-12。
- 评审证据：独立只读子 agent `019f5504-90ac-7212-8b71-7720c0999bf5` 完成覆盖核对后已关闭。
- 验证方式：本轮只调整待办状态，复用上述能力各自归档中的实现与测试证据；文档一致性检查结果随本轮 commit 记录。
