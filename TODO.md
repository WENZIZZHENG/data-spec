# DataSpec 待办路线图

本文件记录当前仍需行动的产品与工程待办。优先级按用户可感知价值、核心链路阻断程度和后续开发解锁程度排序。

## 下一步顺序

1. 当前状态：P6-1 到 P6-73、P6-75、P6-78、P6-79、P6-81、P6-82、P6-87、P6-88、P6-89 已完成第一版；P6-78 OpenSpec change 已归档到 `openspec/changes/archive/2026-07-06-add-fixedsql-file-patch-flow`，P6-88 OpenSpec change `add-code-field-reference-index` 保持 active，后续不自动归档。
2. 近期只保留 1 个优先行动项，后续开发默认从这里选，不再从 P6-71 到 P6-188 全量顺扫：P6-90。
3. 效率优先顺序：P6-88 完成验证、独立评审和本地 commit 后，下一步优先推进 P6-90；真实数据库集成测试已作为可选 Docker profile，不默认阻塞小任务。
4. 暂缓池：P6-91 以后保留为候选池，未进入近期队列前不作为默认下一步；新增想法先合并到已有主题，避免继续追加 P6-189。
5. 每次开工先按任务类型决定快速/常规/SDD：文档与小前端走快速；单模块功能走常规；API/CLI/MCP/AI 外部协议、安全、存储或数据库写入才进入 SDD standard/full。

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
19. P6 收束后再回看哪些能力需要从个人/小团队工具升级为团队协作能力。

## 已完成能力摘要（P0-P4）

P0-P4 的详细背景、方案和验收已归档到 [docs/archive/todo-completed-p0-p4.md](docs/archive/todo-completed-p0-p4.md)。主待办只保留当前仍需行动的 P5/P6 任务。

- P0 AI 可消费主线已完成第一版：AI Context zip、CLI、MCP、个人版字段模型、结构化命名规则和 AI Prompt 生成。
- P1 核心闭环已完成第一版：SQL 校验、OpenAPI 类型契约、COMMENT 解析、前端管理页、字段推荐、结构化修复建议、DDL 生成、检查记录和 fixedSql。
- P2 标准维护与生成能力已完成第一版：内置 standards 初始化、模板 DDL、业务项目 .dataspec/ 约定、数据字典、Excel 导入导出、变更日志和个人工作台。
- P3 自动化与反向导入已完成第一版：SQL 反向导入预览、MySQL DDL 解析、CI/GitHub Action 和 PR 评论式 SQL Review。
- P4 工程化与体验增强已完成第一版：SQL 定位、fixedSql diff、.dataspec/config.json、规则配置表单、OpenAPI 防漂移、Excel dry-run、HTML/ERD、MySQL 规则覆盖、安全基线、演示项目和数据库直连反向导入前端流程。
- 后续真实待办集中在 P6：P5 已完成 dataspec doctor、数据库二次比对、导入来源追踪、SQL 定位范围增强、字段推荐质量增强、核心 fixture/golden 基线、前端高频流程细节打磨和轻量 token 管理；P6-1 已完成标准版本快照第一版，P6-2 已完成字段覆盖率第一版，P6-3 已完成 AI 回放第一版，P6-4 已完成业务仓库初始化第一版，P6-5 已完成字段质量评分第一版，P6-6 已完成轻量字段影响分析第一版，P6-7 已完成 AI Context 按需裁剪第一版，P6-8 已完成字段冲突检测第一版，P6-9 已完成规则误报豁免第一版，P6-10 已完成数据库直连配置预设第一版，P6-11 已完成 MCP/CLI 任务化工作流模板第一版，P6-12 已完成 AI 输出契约稳定性第一版，P6-13 已完成 GitHub inline review 第一版，P6-14 已完成字段分组体验第一版，P6-15 已完成字段批量维护与回退第一版，P6-16 已完成性能基线第一版，P6-17 已完成前端冒烟门禁第一版，P6-18 已完成 AI 可读错误诊断第一版，P6-19 已完成字段标准检索第一版，P6-20 已完成 OpenSpec 归档收口第一版，P6-21 已完成历史快照导出与记录回放第一版，P6-22 已完成 SQL/DDL 多方言兼容矩阵与诊断第一版，P6-23 已完成规则模板库与项目基线套件第一版，P6-24 已完成项目备份恢复迁移包第一版，P6-25 已完成数据库直连只读安全诊断第一版，P6-26 已完成 AI 批量任务交付包第一版，P6-27 已完成 AI 使用反馈与标准改进闭环第一版，P6-28 已完成标准候选 Inbox 与采纳工作台第一版，P6-29 已完成离线 AI Context 缓存第一版，P6-30 已完成数据库 schema dump 第一版，P6-31 已完成 Prompt 模板版本化与效果评测第一版，P6-32 已完成项目活动时间线第一版，P6-33 已完成前端任务式导航第一版，P6-34 已完成本地启动包第一版，P6-35 已完成 fixedSql 策略第一版，P6-36 已完成 AI 使用画像与任务模式配置第一版，P6-37 已完成标准契约 Registry 第一版，P6-38 已完成执行证据包和交付归档第一版，P6-39 已完成前端统一状态第一版，P6-40 已完成并发幂等和任务锁第一版，P6-41 已完成标准变更 What-if 预览第一版，P6-42 已完成领域 Starter Kit 第一版，P6-43 已完成 AI 能力清单第一版，P6-44 已完成前端 URL 状态与可复现操作链接第一版，P6-45 已完成敏感信息脱敏与日志输出边界第一版，P6-46 已完成按变更范围推荐验证命令第一版，P6-47 已完成 TODO 到 OpenSpec 的实施交接助手第一版，P6-48 已完成业务术语表与同义词词根库第一版，P6-49 已完成自然语言需求到标准候选草案第一版，P6-50 已完成 AI 输出引用证据与 Explain Trace 第一版，P6-51 已完成标准字段生命周期状态机第一版，P6-52 已完成业务仓库变更感知扫描与最小上下文第一版，P6-53 已完成标准健康趋势与改进计划第一版，P6-54 已完成数据库连接健康探测与方言能力画像第一版，P6-55 已完成字段值格式与校验样例库第一版，P6-56 已完成标准字段别名冲突与命名保留字检测第一版，P6-57 已完成反向导入字段映射策略与确认理由第一版，P6-58 已完成 AI 任务失败重试与断点续跑第一版，P6-59 已完成标准质量门禁与阈值策略第一版，P6-60 已完成标准字段使用示例与反例库第一版，P6-61 已完成 AI 会话启动包与当前上下文握手第一版，P6-62 已完成 AI 任务卡与单步可恢复执行协议第一版，P6-63 已完成数据库直连元数据浏览器与候选选择页第一版，P6-64 已完成大库扫描计划、分页预览与取消恢复第一版，P6-65 已完成标准字段智能合并向导第一版，P6-66 已完成前端命令面板与最近操作续跑第一版，P6-67 已完成 AI 交接证据看板第一版，P6-68 已完成多项目标准复用包与轻量继承第一版，后续再推进 P6-69 AI 写入安全策略与 dry-run 协议。
- 最新收束状态（2026-07-07）：P6-69 AI 写入安全策略与 dry-run 协议、P6-70 SQL 规则调试器与可解释匹配面板、P6-72 CLI/MCP 与服务端版本兼容握手、P6-73 前端类型化 API Client 与请求状态收口、P6-75 MCP/CLI 工具契约验收与示例调用库、P6-78 fixedSql 文件级补丁应用与人工确认、P6-79 标准问答只读入口与证据引用、P6-88 业务代码字段引用索引与重命名风险分析、P6-89 MCP Prompt/Resource 一等化与 Agent 引导包已完成第一版；近期开发按顶部优先行动队列推进，不再默认从完整 P6 候选池线性顺扫。

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
- 已完成能力：任务卡可从 `create-table/review-pr-sql/reverse-import-standards/export-min-context` workflow recipe 生成，包含 `goal/inputs/currentStep/steps/allowedActions/artifacts/resumeCommand/validationCommands/stopConditions/risks/nextActions`；缺必填输入时返回 `BLOCKED` 和 `PROVIDE_REQUIRED_INPUT`；更新步骤只改本地任务卡文件，不执行 workflow。
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
- 状态：已完成第一版，本地 commit `c993ee2`；OpenSpec change `openspec/changes/add-db-metadata-incremental-cache` 暂未归档。
- 为什么做：数据库直连反向导入、覆盖率报告和元数据浏览会反复读取同一批 schema；没有增量缓存时，大库会慢，AI 也无法判断“这次和上次相比变了什么”。
- 已有基础：已有数据库直连、连接预设、metadata 预览、覆盖率报告、字段来源批次、schema dump 待办、大库扫描计划待办和变更感知扫描待办。
- 已完成能力：新增只保存结构信息的 metadata cache；按连接预设或脱敏连接来源、schema、table 计算 fingerprint；scan/browser/dump/preview/compare/coverage 返回 cache status、lastSeenAt、expiresAt、refreshMode、metadataFingerprint 和变化摘要；前端展示缓存状态并提供 `REFRESH` 手动刷新入口。
- 验证证据：提交前已完成 OpenSpec strict、后端/前端验证、`git diff --check`、敏感词扫描和独立子 agent 只读评审；未主动 archive 或 push。
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

### P6-74：标准变更演练沙箱与样例回归
- 状态：待办。
- 为什么做：修改字段、规则或模板前，用户想知道会影响哪些 SQL、DDL、AI Prompt 和反向导入候选；需要一个轻量“先演练再保存”的入口。
- 已有基础：已有标准快照、What-if 预览待办、Prompt 评测、golden fixtures、SQL 检查记录、DDL 生成、AI 回放和标准质量门禁待办。
- 缺口：现有能力分散，缺少一次性把“改动草案 -> 样例集合 -> lint/DDL/prompt 结果 diff -> 保存建议”串起来的沙箱。
- 落地产物：新增标准草案演练入口；支持选择字段/规则/模板草案和样例集，运行 lint、DDL preview、Prompt preview、Context diff，并输出风险、收益和建议保存步骤。
- 验收标准：用户在保存规则或字段变更前能看到对 good/bad SQL、模板 DDL 和 AI Context 的影响；AI 可读取演练报告决定是否继续修改；演练不写入正式标准。
- 边界：不做多人审批发布，不替代完整 CI；第一版只覆盖项目内 fixture、历史检查记录和用户手动粘贴样例。

### P6-75：MCP/CLI 工具契约验收与示例调用库
- 状态：已完成第一版，OpenSpec change `add-cli-mcp-contract-fixtures` 已归档到 `openspec/changes/archive/2026-07-05-add-cli-mcp-contract-fixtures`。
- 为什么做：DataSpec 优先给 AI 使用时，MCP tools 和 CLI 命令本身就是 AI 的“产品界面”；仅有 OpenAPI 类型还不够，工具入参、输出 schema、错误码、示例和安全边界都需要可回归验证。
- 已有基础：已有 CLI、MCP、OpenAPI 防漂移、AI 输出契约稳定性、AI 能力清单、doctor 和 workflow recipes。
- 已完成能力：新增 `tools/fixtures/cli-mcp-contracts.json` 和 `tools/dataspec-cli-mcp-contract-check.mjs`，覆盖核心 CLI commands、MCP tools/resources/prompts 的名称、输入边界、输出 shape、成功/失败示例、安全 metadata 和 recommendedNextActions；校验会对齐本地 MCP descriptors、拒绝 secret-like 示例，并接入验证建议和 Node tests。
- 落地产物：新增 MCP/CLI contract fixtures、本地验收命令、fixture checker 单测、验证建议规则、README 与 AI 契约文档说明，以及主规格 `openspec/specs/cli-mcp-contract-fixtures/spec.md`。
- 验收标准：修改 CLI/MCP 工具参数、输出字段、resource/prompt 描述或安全 metadata 时，契约测试能失败并提示更新示例；AI 可读取示例库选择正确工具；失败样例不包含 token/password/连接串。
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

### P6-80：规则与模板变更 diff 包
- 状态：待办。
- 为什么做：规则、表模板和 Prompt 模板会持续调整；如果只看保存后的结果，AI 和用户很难判断这次变更会影响哪些 SQL 检查、DDL 生成和 Context 输出。
- 已有基础：已有规则配置、规则模板库、Prompt 模板版本化、标准变更演练沙箱、标准快照、执行证据包和 OpenSpec 归档待办。
- 缺口：字段快照相对清晰，但规则/模板/Prompt 的变更 diff、兼容性说明、影响样例和回滚信息还没有统一结构。
- 落地产物：新增规则/模板变更 diff 包；记录 before/after、changedParams、affectedRules、affectedTemplates、sampleResultDiff、compatibilityNotes、rollbackPlan 和 reviewChecklist。
- 验收标准：调整一条规则参数或模板后，能看到命中样例和生成结果差异；AI 可读取 diff 包决定是否需要补测试或更新 Context；回滚说明不依赖人工记忆。
- 边界：不做审批流，不替代 OpenSpec proposal，不要求所有历史模板补齐 diff；优先覆盖新变更。

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
- 状态：已完成第一版，OpenSpec change `add-testcontainers-db-integration-tests` 保持 active，暂未归档。
- 为什么做：数据库直连反向导入、覆盖率和二次比对已经是核心能力，但当前主要依赖 fixture 和 H2/单测；真实 PostgreSQL/MySQL 元数据、COMMENT、schema、大小写和权限行为仍可能漂移。
- 已有基础：已有 Flyway、PostgreSQL/MySQL 直连 metadata、反向导入预览、数据库覆盖率、二次比对、metadata fixture 和多方言兼容矩阵待办。
- 已完成能力：新增基于 Testcontainers 的真实数据库集成测试矩阵，覆盖 PostgreSQL/MySQL schema-only fixture、表列表、metadata dump/browser、compare、coverage、只读连接诊断和敏感信息不外泄断言；同时补充 PostgreSQL 只读账号在 JDBC 未声明 readOnly 时的权限诊断回归测试。
- 落地产物：新增 Testcontainers 测试 profile；启动 PostgreSQL/MySQL 容器，加载最小 schema fixture，验证表列表、metadata preview、compare、coverage 和连接诊断的关键字段。
- 验证证据：`mvn test` 476 pass，确认默认后端测试不运行 `*IT.java` 且不依赖 Docker；`mvn test -Pdb-integration` 已进入 Testcontainers 启动阶段，但当前环境无可用 Docker，报 `Could not find a valid Docker environment`，真实容器矩阵需在 Docker 可用环境补跑。
- 验收标准：开发者可通过专门 profile 运行真实数据库集成测试；默认 `mvn test` 不强制依赖 Docker；失败信息能定位具体方言和 metadata 字段。
- 边界：不替代数据库供应商完整兼容认证，不把 Docker 作为所有开发环境的必需前提，不扫描业务数据行。

### P6-83：README/TODO/OpenSpec 状态一致性检查
- 状态：已完成第一版，新增 `tools/dataspec-status-check.mjs` 与目标单测，README 已补验证入口。
- 为什么做：项目能力迭代很快，README、TODO、OpenSpec active/archive 和实际代码容易出现“文档说已完成但入口不可用”或“待办仍写缺口但功能已实现”的漂移。
- 已有基础：已有 README 功能清单、TODO 状态行、OpenSpec change、OpenSpec validate、AI 输出契约测试和完成项归档习惯。
- 已完成能力：状态检查工具可扫描 TODO 近期队列、完成态残留“缺口”、OpenSpec active/archive/main spec、README 工具入口和 Markdown 相对链接，并输出 `severity/code/message/file/line/suggestedFix` 等机器可读问题。
- 验收标准：文档改动后能运行 `node tools/dataspec-status-check.mjs --format json` 检查明显状态漂移；新增完成项时会提示补 README/TODO/OpenSpec 入口；检查结果不依赖外部网络。
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
- 状态：已完成第一版，已完成验证、独立评审和本地 commit 收口。
- 为什么做：DataSpec 已能生成 DDL、检查 SQL 和反向导入现有数据库，但真实落地还需要回答“从当前数据库变到目标标准要改哪些表/字段、风险是什么、怎么回退”；AI 不能只输出一段不可审计的 ALTER SQL。
- 已有基础：已有表模板、DDL 生成、数据库直连 metadata、二次比对、标准快照、fixedSql diff、备份迁移包待办和 Atlas/Terraform 风格 plan/apply 参考。
- 已完成能力：新增只读 schema plan API `/api/reverse-import/database/schema-plan`、CLI `schema-plan` 和反向导入页预览区；计划复用数据库 metadata dump 与 compare 语义，输出 `currentSchemaHash`、`targetSpecHash`、`changeSet`、`riskLevel`、`migrationSql`、`rollbackHint`、`manualChecks`、`blockedReasons` 和 `nextActions`。
- 落地产物：API/CLI/前端均只生成 dry-run 草案和风险说明；注释修正可给出 `COMMENT ON` 草案，结构属性只给出 `-- REVIEW` 人工确认文本，不拼接可执行 `ALTER TABLE`；前端展示整体风险、阻塞原因、人工检查、按表分组的 changeSet 和 SQL 草案；CLI 推荐 `--password-env` 读取数据库密码。
- 验收标准：连接数据库后可生成“当前库 -> DataSpec 标准”的变更计划；高风险 drop/rename 默认标红且不自动执行；AI 可读取 JSON 计划并决定补标准、生成迁移文件或停止等待人工确认。
- 边界：第一版不直接执行迁移，不替代 Flyway/Liquibase/Atlas 等迁移工具，不自动推断所有字段重命名；只服务个人/小团队的迁移草案和风险说明。

### P6-88：业务代码字段引用索引与重命名风险分析
- 状态：已完成第一版，OpenSpec change `add-code-field-reference-index` 保持 active，后续按需归档。
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

### P6-99：只读标准文档站与数据字典消费入口
- 状态：待办。
- 为什么做：字段标准、规则、模板和数据字典已经能在系统内维护和导出，但用户、AI agent 或业务仓库经常只需要一个只读入口来查询“当前标准是什么、哪些字段可用、有哪些示例”，不一定要进入管理页面或下载 AI Context。
- 已有基础：已有数据字典导出、HTML/ERD、AI Context、标准快照、字段检索、标准问答入口和多项目标准复用包待办。
- 缺口：缺少面向消费方的轻量文档站或静态包；当前导出更偏一次性文件，无法稳定提供版本、搜索、字段详情、规则说明、示例和复制链接。
- 参考项目：`backstage/backstage` 的开发者入口组织方式、`Redocly/redocly-cli` 的契约文档化和 `facebook/docusaurus` 的静态文档站体验；只借鉴只读信息架构，不引入复杂门户权限。
- 落地产物：新增只读标准文档导出或页面；按项目和快照生成字段目录、枚举、规则、模板、示例、来源与变更说明，支持搜索、复制字段链接和导出静态包；AI Context 可引用文档站链接或文档索引。
- 验收标准：用户能在只读入口搜索字段、查看规则和复制标准链接；业务仓库可保存一份不含 token/password 的静态文档包；AI 能根据文档索引定位字段证据。
- 边界：不做复杂 CMS，不开放写入能力，不替代后台字段维护页面；第一版只服务本地/小团队只读查询。

### P6-100：标准资产依赖图与孤儿对象检查
- 状态：待办。
- 为什么做：字段、枚举、规则、模板、快照、AI Context、SQL 检查记录和导入来源已经形成一张标准资产关系网；当某个字段废弃、规则调整或模板删除时，需要知道下游依赖和孤儿对象，AI 也需要用关系图避免误删或遗漏。
- 已有基础：已有字段影响分析、业务对象关系图、字段来源批次、标准快照、规则模板库、AI 回放、执行证据包和标准使用热区待办。
- 缺口：当前影响分析分散在单点对象上，缺少统一 asset graph；无法稳定输出 dependsOn、referencedBy、orphaned、stale、cycleRisk 和 cleanupAction。
- 参考项目：`datahub-project/datahub` 的元数据关系视角和 `OpenLineage/OpenLineage` 的输入输出事件模型；只借鉴关系表达和可追溯性，不建设重型数据目录平台。
- 落地产物：新增标准资产依赖图模型或查询接口；聚合字段、枚举、规则、模板、快照、导入批次和 AI job 的轻量关系，输出节点、边、风险标签、孤儿对象和建议动作；前端可在字段详情或项目健康页展示摘要。
- 验收标准：给定一个字段或规则，能看到主要上游来源和下游使用对象；删除或废弃前能提示孤儿模板、过期快照或高风险依赖；AI 可读取图摘要生成变更计划。
- 边界：不扫描业务数据行，不替代企业级血缘系统，不要求一次性补齐所有历史关系；第一版优先覆盖 DataSpec 已有结构化记录。

### P6-101：环境配置漂移检测与运行指纹
- 状态：待办。
- 为什么做：DataSpec 同时有后端配置、前端生成类型、CLI/MCP、`.dataspec/config.json`、OpenAPI schema、AI Context 缓存和数据库迁移；一旦本地环境或业务仓库缓存漂移，AI 会基于错误上下文继续执行。
- 已有基础：已有 `dataspec doctor`、CLI/MCP 版本兼容握手、OpenAPI 防漂移、离线 Context、标准快照、本地启动包和 README 验证命令。
- 缺口：现有诊断更偏单点连通性，缺少环境指纹和漂移对比；无法一次性说明 serverVersion、apiSchemaHash、flywayVersion、frontendSchemaHash、cliVersion、mcpVersion、contextSnapshotHash 和 configHash 是否一致。
- 参考项目：`hashicorp/terraform` 的 plan/drift 思路和 `bufbuild/buf` 的契约 breaking 检查；只借鉴对比报告，不引入远程状态服务。
- 落地产物：新增 `dataspec doctor --fingerprint` 或等价 API；输出本地/服务端/业务仓库的运行指纹、漂移项、严重级别、建议修复命令和可继续/应停止判断；README 和 AGENTS 片段提示 AI 开始前先读取。
- 验收标准：前端类型未重新生成、CLI 版本过旧、Context 缓存过期、服务端迁移未完成或项目配置指向错误时能明确诊断；JSON 输出可被 AI 自动判断下一步。
- 边界：不自动修改用户配置，不上传环境信息，不做跨机器资产管理；第一版只读取本地和当前服务可见信息。

### P6-102：数据源连接器能力注册与插件化边界
- 状态：待办。
- 为什么做：当前数据库直连以 PostgreSQL/MySQL 为主，后续可能会接 SQLite、SQL Server、Oracle、ClickHouse 或离线 dump；如果每个方言都散落在反向导入流程里，元数据读取、只读诊断、类型映射和限制说明会越来越难维护。
- 已有基础：已有数据库元数据适配层、直连反向导入、连接预设、只读安全诊断、多方言兼容矩阵、真实数据库集成测试和 DBeaver/SchemaCrawler 参考。
- 缺口：缺少连接器 registry，无法稳定声明每种数据源支持哪些能力：comments、indexes、schemas、caseSensitivity、readonlyDiagnostic、typeMapping、pagination、offlineDump 和 unsupportedReasons。
- 参考项目：`dbeaver/dbeaver` 的多数据源体验、`schemacrawler/SchemaCrawler` 的 metadata 抽取和 `sqlalchemy/sqlalchemy` 的 dialect 分层；只借鉴能力声明和适配边界，不把本项目改成通用数据库客户端。
- 落地产物：新增 data source connector capability 模型；后端、CLI 和前端连接选择页都能读取 supportedCapabilities、defaultPort、metadataLimit、knownPitfalls 和 testProfile；新增连接器需补 fixture 和能力声明。
- 验收标准：用户选择数据源前能看到支持范围和限制；AI 调用反向导入前能判断该方言能否读取 comment/index/schema；新增方言不会破坏 PostgreSQL/MySQL 既有行为。
- 边界：不一次性支持所有数据库，不读取业务数据行，不做 SQL 编辑器或通用查询工具；第一版只把现有能力 registry 化。

### P6-103：本地运行观测、慢请求与诊断包
- 状态：待办。
- 为什么做：当后端启动慢、接口超时、前端页面卡顿或 AI 批量任务失败时，用户需要一份可复制给 AI 的本地诊断摘要，而不是在日志、浏览器控制台和终端之间手工拼线索。
- 已有基础：已有大字段库性能基线、前端性能体验指标、AI 可读错误诊断、任务重试、执行证据包、Spring Boot Actuator 可扩展空间和 README 验证入口。
- 缺口：缺少统一 run diagnostics；后端慢请求、SQL lint 耗时、数据库 metadata 扫描耗时、前端请求耗时和最近错误没有形成脱敏摘要，也没有明确的“诊断包可安全分享”边界。
- 参考项目：`open-telemetry/opentelemetry-collector` 的 signals 概念和 `langfuse/langfuse` 的 trace 组织方式；只做本地轻量摘要，不接入远程遥测。
- 落地产物：新增本地诊断包命令或页面；汇总 server health、recentErrors、slowRequests、batchRunStats、frontendBuildInfo、schemaHash、redactionStatus 和 recommendedActions；所有输出经过敏感信息扫描。
- 验收标准：用户遇到慢页面或失败任务时能导出一份 JSON/Markdown 诊断包；AI 可根据诊断包判断是服务未启动、接口慢、方言限制、契约漂移还是输入问题；包内不包含 token/password/完整 JDBC URL。
- 边界：不上传遥测，不做生产监控平台，不长期保存详细请求体；第一版只面向本地开发和个人排障。

### P6-104：标准决策理由库与字段 ADR 记录
- 状态：待办。
- 为什么做：AI 不只需要知道“标准字段叫什么”，还需要知道“为什么用这个字段、为什么不用另一个别名、哪些历史方案被否决”；否则同一类命名争议会在推荐、DDL 生成和 SQL 修复中反复出现。
- 已有基础：已有字段来源、变更日志、标准快照、业务术语表、字段冲突检测、标准问答入口、面向 AI 的变更发布说明和 OpenSpec 归档。
- 缺口：变更日志主要记录 what，来源记录主要记录 from；缺少稳定记录 why、alternatives、tradeoffs、decisionOwner、evidenceRefs 和 supersedes 的轻量决策模型。
- 参考项目：`changesets/changesets` 的变更说明组织、`agents.md` 的 agent 指令约定和 OpenSpec proposal/archive 的决策语境；只借鉴记录结构，不引入审批流程。
- 落地产物：新增标准决策 note 或字段 ADR；可关联字段、别名、规则、模板、分组或业务术语，记录决策背景、采用方案、拒绝方案、证据链接、适用范围和过期条件；导出到 AI Context 和标准问答证据。
- 验收标准：AI 能回答“为什么这里用 `user_id` 而不是 `uid`”“金额单位为什么用分”“某字段为什么废弃”；字段合并或规则调整时能引用历史决策，避免重复争论。
- 边界：不做多人审批，不要求所有历史字段补齐 ADR，不把决策理由当成不可变法规；第一版从高冲突字段和规则开始补。

### P6-105：AI 一页式标准工作台
- 状态：待办。
- 为什么做：当前 AI 相关能力已经覆盖 Context、回放、批量任务、反馈报告、候选采纳和证据包，但入口分散在多个页面；用户和 AI agent 都需要一个“当前项目下一步该做什么”的总控视图。
- 已有基础：已有个人工作台、AI Context、AI 回放、AI 批量任务、AI 反馈报告、字段质量、覆盖率、候选 Inbox、项目活动时间线和命令面板待办。
- 缺口：缺少面向 AI 使用的项目级任务面板，无法一次性看到标准健康、最新 AI 任务、待处理候选、最近失败、推荐下一步和可复制命令。
- 参考项目：`backstage/backstage` 的开发者入口组织方式、`langfuse/langfuse` 的 AI trace 概览和 `OpenLineage/OpenLineage` 的作业证据聚合；只借鉴信息架构和证据摘要，不建设重型平台。
- 落地产物：新增“AI 工作台”页面或个人工作台 AI 分区；聚合 projectReadiness、standardHealth、recentAiJobs、pendingCandidates、failedTasks、recommendedActions、copyableCommands 和 evidenceLinks。
- 验收标准：选择项目后能在一个页面判断 AI 是否可开始建表、SQL Review、反向导入或导出 Context；每个建议动作都有跳转、命令或说明；无项目/无数据时空状态清晰。
- 边界：不做聊天机器人，不替代各功能详情页，不自动执行写操作；第一版只聚合已有报告和任务入口。

### P6-106：表级约束、索引与主外键标准
- 状态：待办。
- 为什么做：字段标准解决“列叫什么、是什么类型”，但 AI 建表还需要知道主键、唯一约束、索引、外键、审计字段和软删除约定；否则 DDL 可能字段正确但表结构不可用。
- 已有基础：已有表模板、DDL 生成、业务对象关系图、SQL/DDL 多方言兼容、schema plan 预览、规则模板库和数据库反向导入。
- 缺口：缺少表级 constraint/index policy，AI Context 也无法稳定表达“哪些字段必须唯一”“哪些关系需要外键或索引”“哪些约束只做建议不强制”。
- 参考项目：`ariga/atlas` 的 schema-as-code 表达、`prisma/prisma` 的 schema introspection 和 `sqlalchemy/sqlalchemy` 的方言约束抽象；只借鉴约束建模和方言差异，不把 DataSpec 改成通用 ORM。
- 落地产物：新增轻量表级标准模型或模板扩展；支持 primaryKey、uniqueKeys、indexes、foreignKeys、checkHints、auditPolicy、softDeletePolicy、dialectNotes 和 aiUsageNotes；DDL 生成、SQL lint 和 AI Context 可消费。
- 验收标准：AI 生成订单、用户、支付等常见表时能输出合理主键、唯一键和索引建议；反向导入可识别现有索引/约束并与标准比对；规则失败时给出可解释证据。
- 边界：第一版不执行数据库迁移，不覆盖所有方言高级特性，不强制把所有历史表补齐约束标准。

### P6-107：代码集与枚举值生命周期
- 状态：待办。
- 为什么做：状态、类型、渠道、来源等枚举字段是 AI 最容易写错的地方；仅有字段名还不够，需要知道允许值、显示名、废弃值、替代值和不同系统间映射。
- 已有基础：已有数据字典、字段格式约束、字段生命周期、DDL 生成、SQL lint、标准问答和 AI Context。
- 缺口：代码集缺少生命周期、别名映射、版本、冲突检测和 AI 可读约束；SQL 修复也无法判断 `status = '1'` 与 `status = 'PAID'` 哪个是当前标准。
- 参考项目：`open-metadata/OpenMetadata` 和 `datahub-project/datahub` 的 glossary/tag 组织方式，以及 `great-expectations/great_expectations` 的枚举值校验思路；只借鉴约束和证据表达，不引入数据质量平台。
- 落地产物：增强代码集模型/API/前端；支持 enumValue、displayName、aliases、status、replacementValue、validFrom、validTo、sourceEvidence、mappingHints 和 contextExport；SQL lint 可对枚举 literal 给出建议。
- 验收标准：AI 查询或生成状态字段时能读取允许值和废弃值；SQL 检查能发现明显非法枚举 literal；枚举变更能进入标准快照和变更说明。
- 边界：不连接真实业务数据做分布统计，不自动替换生产 SQL；第一版优先覆盖人工维护代码集和反向导入可见约束。

### P6-108：业务仓库迁移交付包
- 状态：待办。
- 为什么做：DataSpec 能生成建议、fixedSql、schema plan 和证据包，但真实落地常需要把 SQL、配置、README、验证命令和回滚说明组织成一个可提交到业务仓库的交付包。
- 已有基础：已有 AI 批量任务交付包、fixedSql 文件补丁、schema 变更计划、GitHub inline review、执行证据包、备份恢复迁移包和标准变更发布说明。
- 缺口：缺少面向业务仓库的 delivery bundle 结构，AI 无法稳定输出“要改哪些文件、如何验证、如何回滚、PR 描述怎么写、哪些风险需人工确认”。
- 参考项目：`reviewdog/reviewdog` 的 PR 反馈聚合、`changesets/changesets` 的变更说明和 `hashicorp/terraform` 的 plan/apply 分离；只借鉴交付组织，不自动推送或执行高风险写入。
- 落地产物：新增迁移交付包导出；包含 patchFiles、migrationSql、verificationCommands、rollbackPlan、riskSummary、evidenceRefs、prDescription、manualChecklist 和 applyMode；CLI/MCP/前端支持 dry-run 下载。
- 验收标准：一次标准变更或 SQL 修复能生成可审阅的业务仓库交付包；用户可先看 diff 和验证命令再决定是否应用；包内不包含 token、密码或业务数据行。
- 边界：不自动创建远程 PR，不绕过人工确认，不把业务仓库写入设为默认行为。

### P6-109：AI 能力边界模拟与安全预检
- 状态：待办。
- 为什么做：AI 调用 CLI/MCP/API 前需要知道当前 token、项目、数据源、写入能力、dry-run 要求和可恢复性；如果边界不清晰，容易在错误项目执行或重复写入。
- 已有基础：已有 API Token、`dataspec doctor`、AI 能力清单、AI 写入安全策略、任务卡、幂等保护、敏感信息脱敏和连接只读诊断。
- 缺口：缺少可机器读取的 preflight/simulation 入口，不能在执行前回答“这个任务会读什么、会写什么、是否需要 dry-run、失败后怎么恢复”。
- 参考项目：`hashicorp/terraform` 的 plan 思路、MCP 规范的 tools/resources 边界和 `gitleaks/gitleaks` 的敏感信息扫描；只做本地安全预检，不引入审批流。
- 落地产物：新增 `dataspec preflight` 或等价 API/MCP 工具；输入 taskType、projectId、targetResources 和 requestedActions，输出 allowedActions、requiredDryRun、writeTargets、idempotencyRequired、redactionWarnings、resumeHint 和 stopReasons。
- 验收标准：AI 执行反向导入、批量 lint、候选采纳、fixedSql 补丁或证据包导出前能得到明确边界；高风险写入必须提示 dry-run 或人工确认；失败输出可被任务卡续跑。
- 边界：不做企业权限审批，不赋予 AI 新权限，不替代后端真实鉴权；preflight 只是执行前解释和门禁。

### P6-110：文档与需求材料反向提取标准候选
- 状态：待办。
- 为什么做：字段标准不只来自数据库和接口契约，也常存在于 Markdown 需求、Excel 字段说明、OpenSpec proposal、README、接口说明和历史设计文档里；AI 使用这些材料时需要先转成可审阅候选，而不是直接写入正式标准。
- 已有基础：已有 Excel 导入、自然语言标准候选、多源契约导入、标准候选 Inbox、业务术语表、AI Context 和标准决策理由库待办。
- 缺口：缺少从文档文本中抽取字段、枚举、业务术语、约束和决策理由的统一入口；候选来源、置信度和人工确认状态也无法追踪。
- 参考项目：`glideapps/quicktype` 的结构推断思路、`Redocly/redocly-cli` 的契约文档化和 OpenSpec proposal/archive 的需求语境；只借鉴提取与结构化，不把文档内容当成已确认标准。
- 落地产物：新增文档候选提取 API/CLI 或前端导入入口；支持 Markdown、CSV/Excel 字段表和 OpenSpec 文档，输出 candidateFields、candidateEnums、terms、constraints、decisionHints、sourceRanges、confidence 和 inboxAction。
- 验收标准：给定一份需求文档或字段说明表，能生成可审阅标准候选并进入候选 Inbox；低置信度内容明确标记人工确认；导入过程不保存无关正文或敏感片段。
- 边界：第一版不做复杂 PDF/OCR，不直接写正式标准，不承诺理解所有自然语言；只处理结构相对清晰的本地文档。

### P6-111：标准候选来源管道与自动入箱
- 状态：待办。
- 为什么做：标准候选 Inbox 第一版偏手动创建，但真正高频来源是反向导入、覆盖率报告、AI 反馈、SQL 检查和接口契约；如果这些信号不能自动变成待确认候选，AI 仍需要人工在多个页面之间搬运线索。
- 已有基础：已有标准候选 Inbox、数据库直连反向导入、字段覆盖率、AI 反馈报告、SQL 检查记录、字段来源追踪和多源契约导入待办。
- 缺口：缺少统一 candidate source pipeline，无法把不同来源标准化为 candidateName、dataType、evidence、confidence、sourceRef、dedupeKey 和 recommendedAction。
- 参考项目：`OpenLineage/OpenLineage` 的输入输出事件、`datahub-project/datahub` 的 metadata ingestion 和 `langfuse/langfuse` 的 trace evidence；只借鉴来源归因与证据组织，不做后台常驻采集。
- 落地产物：新增候选来源适配层；反向导入未纳管字段、覆盖率未命中字段、AI 高频 fallback 字段和契约字段可一键或批量进入 Inbox；重复候选按 project、name、sourceType、sourceRef 合并证据。
- 验收标准：从覆盖率报告或 AI 反馈页能生成候选并在 Inbox 看到来源证据；重复来源不会刷出多条相同待办；所有入箱动作可 dry-run 且不写正式字段。
- 边界：不自动采纳候选，不扫描业务数据行，不保存密码、token 或完整 JDBC URL；第一版只处理已在 DataSpec 内产生的安全信号。

### P6-112：标准候选批量决策与可撤销记录
- 状态：待办。
- 为什么做：候选积累后逐条采纳、合并、忽略会很慢；个人使用也需要批量处理相似字段，并在误操作后能看到决策记录和有限回退路径。
- 已有基础：已有标准候选单条采纳、合并、忽略、延后，字段变更日志、批量字段维护、规则例外和项目活动时间线待办。
- 缺口：缺少批量选择、批量采纳前预览、批量忽略理由、批量合并建议、决策日志和撤销边界说明。
- 参考项目：`github/gh` 风格的批量确认体验、`changesets/changesets` 的变更记录和 `reviewdog/reviewdog` 的诊断分组；只借鉴批量处理与审计记录，不做审批流。
- 落地产物：前端候选 Inbox 增加批量选择栏、批量决策 dialog、dry-run 预览、冲突分组和决策历史；后端提供批量决策 API，并记录每个候选的 before/after、操作者和 reason。
- 验收标准：用户可一次忽略一批低置信度候选，或批量采纳无冲突候选；存在字段名冲突、类型不一致或目标字段缺失时必须阻断并说明；误采纳能通过变更日志找到恢复线索。
- 边界：不做多人审核，不自动修改合并目标字段内容，不保证所有历史操作都能无损回滚；第一版只支持候选状态和新建字段的可解释回退。

### P6-113：候选采纳前质量门禁与影响预览
- 状态：待办。
- 为什么做：候选进入正式字段库前，应先回答“命名是否合规、是否重复、类型是否合理、是否影响现有模板/SQL/AI Context”；否则 Inbox 会把脏字段更快地推入标准库。
- 已有基础：已有字段质量评分、字段冲突检测、字段影响分析、命名规则、标准快照、SQL lint、DDL 生成和候选采纳工作台。
- 缺口：候选采纳动作缺少统一 preflight；当前只能在采纳后再通过质量报告或冲突报告发现问题。
- 参考项目：`hashicorp/terraform` 的 plan/apply 分离、`great-expectations/great_expectations` 的质量检查和 `ariga/atlas` 的 schema lint；只借鉴执行前预览，不自动应用高风险修复。
- 落地产物：新增 candidate acceptance preflight；输出 namingIssues、duplicateRisks、typeWarnings、affectedTemplates、affectedSnapshots、aiContextDelta 和 recommendedDecision；前端采纳/合并 dialog 展示阻断项和建议项。
- 验收标准：采纳 `UserID`、缺注释字段、重复别名或敏感字段时能在提交前看到明确提示；ERROR 级问题阻断采纳，WARNING/SUGGESTION 可带理由继续；结果可被 AI 读取。
- 边界：不替代正式 lint 和质量报告，不自动重命名候选，不读取业务数据值；第一版只基于项目现有标准和 metadata 证据。

### P6-114：AI 任务推荐队列与下一步编排
- 状态：待办。
- 为什么做：DataSpec 已有很多页面和命令，但 AI 或用户常见问题是“不知道下一步该跑覆盖率、导出 Context、修规则还是采纳候选”；需要把诊断结果变成可执行任务队列。
- 已有基础：已有个人工作台、AI 一页式工作台待办、AI 任务卡、AI 反馈报告、字段质量、覆盖率、标准候选 Inbox、`dataspec doctor` 和 workflow recipes。
- 缺口：缺少项目级 recommended task queue，无法按当前项目状态生成优先级、依赖、输入参数、可复制命令和完成判定。
- 参考项目：`backstage/backstage` 的开发者入口、`langfuse/langfuse` 的任务观测和 MCP tools/prompts 的可执行描述；只做本地推荐，不做自动代理执行。
- 落地产物：新增任务推荐 API/前端分区；根据 missingProject、noFields、lowCoverage、pendingCandidates、staleContext、failingLint、openAiFeedback 等信号生成任务卡，包含 actionType、priority、reason、command、targetRoute 和 completionCheck。
- 验收标准：新项目、已有数据库项目和 AI 反馈较多项目会得到不同任务顺序；每张任务卡都能跳转到页面或复制 CLI/MCP 命令；完成后任务状态能刷新消失或降级。
- 边界：不自动执行写操作，不引入后台调度，不替代用户判断；第一版只推荐 DataSpec 内已有能力或明确待办中的 dry-run 动作。

### P6-115：跨来源标准证据视图
- 状态：待办。
- 为什么做：字段标准的证据分散在数据库 metadata、SQL 检查、AI job、候选、变更日志、文档和接口契约里；AI 要解释一个字段是否可信时，需要一页聚合证据而不是翻多个页面。
- 已有基础：已有字段来源、变更日志、AI 回放、AI 反馈、标准候选、标准决策理由库待办、Explain Trace、执行证据包和项目活动时间线。
- 缺口：缺少以字段/表/规则为中心的 evidence timeline，无法稳定回答“这个标准来自哪里、被谁用过、最近哪里冲突、哪些材料支持它”。
- 参考项目：`datahub-project/datahub` 和 `open-metadata/OpenMetadata` 的资产详情页、`OpenLineage/OpenLineage` 的 lineage event；只借鉴证据聚合，不建设重型数据目录。
- 落地产物：新增只读证据视图或 API；按 subjectType/subjectId 汇总 sourceEvents、aiUsages、lintHits、candidateDecisions、changeLogs、documentRefs、contractRefs 和 confidenceSummary；AI Context 可按需引用证据摘要。
- 验收标准：打开某个标准字段能看到来源、采纳记录、最近 AI 使用、相关 SQL 问题和决策理由；证据摘要可复制给 AI，且不包含敏感连接信息或业务数据行。
- 边界：不做全量血缘平台，不自动判断证据真伪，不把临时低置信度证据写成正式标准；第一版只聚合 DataSpec 已保存的安全记录。

### P6-116：前端端到端上手引导与可复现演练
- 状态：待办。
- 为什么做：功能越来越多后，新项目从“创建项目”到“导入标准、检查 SQL、导出 AI Context、采纳候选”缺少一条明确演练路径；用户自己用也需要快速确认当前环境是否可跑通。
- 已有基础：已有演示项目、前端工作台、任务式导航待办、一键启动包、E2E 冒烟、README 快速开始、项目备份恢复和 AI 工作台待办。
- 缺口：缺少前端内的 guided runbook，不能把关键页面串成可复现的 10 分钟流程，也没有把每步状态和失败原因反馈给 AI。
- 参考项目：`backstage/backstage` 的模板化 onboarding、`microsoft/playwright` 的 trace 截图和 `GoogleChrome/lighthouse` 的检查报告；只借鉴引导和报告，不做营销式首页。
- 落地产物：新增“上手演练”入口或工作台分区；按步骤检查项目、字段、规则、SQL 校验、反向导入、候选、AI Context 和 CLI/MCP 配置，输出可复制的 runbook result。
- 验收标准：空项目能按引导创建演示数据并完成一次 SQL 检查和 Context 导出；已有项目能跳过已完成步骤；失败时给出页面跳转、命令和 AI 可读错误。
- 边界：不重做所有页面 UI，不替代 README，不自动连接外部数据库；第一版优先覆盖本地演示和个人项目闭环。

### P6-117：统一变更 Diff 与预览组件
- 状态：待办。
- 为什么做：fixedSql、反向导入 compare、标准快照 diff、规则模板 diff、Prompt 评测 diff 和迁移交付包都会展示“改了什么”；如果每个页面各自实现，AI 和用户会看到不一致的风险等级、证据和应用边界。
- 已有基础：已有 fixedSql diff、数据库二次比对、标准快照、规则模板 diff 包、Prompt 模板评测待办、业务仓库迁移交付包和执行证据包。
- 缺口：缺少统一 change preview 数据结构和前端展示组件；不同来源的 before/after、riskLevel、evidenceRefs、applyMode、rollbackHint 无法复用，也难以导出给 AI。
- 参考项目：`reviewdog/reviewdog` 的诊断聚合、`changesets/changesets` 的变更说明和 `hashicorp/terraform` 的 plan 预览；只借鉴差异表达，不自动应用高风险改动。
- 落地产物：新增统一 diff DTO 和前端组件；支持文本 diff、结构化字段 diff、规则 diff、标准快照 diff 和 prompt 输出 diff；每条变更可携带 riskLevel、sourceRef、recommendedAction、applyable、rollbackHint 和 copyableEvidence。
- 验收标准：SQL 修复、反向导入差异和标准快照差异能用同一套组件展示；AI 可读取统一 JSON 判断是否可 dry-run、可应用或需人工确认；导出内容不包含密码、token 或业务数据行。
- 边界：不做像素级可视化大改，不替代各业务页面的提交逻辑；第一版只统一预览和证据表达。

### P6-118：全链路 Trace ID 与 AI 操作关联
- 状态：待办。
- 为什么做：AI 通过前端、CLI、MCP 或 API 执行任务后，相关记录可能散落在 AI job、SQL 检查记录、候选、导入批次、后端日志和诊断包里；缺少 trace ID 时，很难复盘一次操作的完整路径。
- 已有基础：已有 AI 回放、检查记录、导入批次、AI 批量任务、执行证据包、本地诊断包待办、OpenTelemetry/Trace 参考和统一 API wrapper。
- 缺口：当前记录多按各自 id 查询，缺少 correlationId/requestId 贯穿请求入口、服务处理、任务记录、证据包和前端错误状态。
- 参考项目：`open-telemetry/opentelemetry-collector` 的 trace/span 组织和 `langfuse/langfuse` 的 AI trace 视角；只做本地轻量关联，不接入远程遥测。
- 落地产物：新增 `traceId` 或 `correlationId` 约定；前端、CLI、MCP 请求自动带入并在响应、AI job、lint record、import batch、candidate decision 和 evidence bundle 中记录；诊断包可按 trace 汇总。
- 验收标准：一次 SQL 校验、反向导入或候选采纳能通过一个 trace ID 找到输入、输出、标准版本、错误、耗时和后续建议；日志和导出内容经过敏感信息脱敏。
- 边界：不做分布式追踪平台，不上传遥测，不长期保存详细请求体；第一版只覆盖 DataSpec 内部链路。

### P6-119：版本兼容矩阵与降级策略
- 状态：待办。
- 为什么做：DataSpec 的后端、前端生成类型、CLI、MCP、AI Context 缓存、标准快照和业务仓库 `.dataspec/config.json` 会独立变化；AI 如果拿旧上下文调用新接口，可能产生难以定位的失败。
- 已有基础：已有 OpenAPI 防漂移、CLI/MCP 兼容握手、离线 Context 缓存、`dataspec doctor`、标准快照、能力清单和环境指纹待办。
- 缺口：缺少机器可读 compatibility matrix，无法稳定说明 serverVersion、apiSchemaVersion、cliVersion、mcpVersion、contextVersion、snapshotVersion 和 configVersion 的兼容关系与降级动作。
- 参考项目：`bufbuild/buf` 的 breaking change 检查、`OpenAPITools/openapi-generator` 的契约生成和 `hashicorp/terraform` 的版本约束提示；只借鉴兼容性表达，不做远程升级服务。
- 落地产物：新增版本兼容 manifest；`doctor`、CLI、MCP 和前端启动时可读取 min/max compatible version、breakingChanges、deprecatedFields、fallbackCommands、mustRegenerateContext 和 stopReasons。
- 验收标准：当前端 schema.ts 过旧、CLI 版本过旧、AI Context 缓存与服务端不兼容或后端 API breaking 时，能给出明确继续/停止判断和修复命令；AI 可解析 JSON 后自动选择安全降级。
- 边界：不承诺长期 LTS，不自动升级依赖，不强制联网检查版本；第一版只管理本地仓库和当前服务可见版本。

### P6-120：AI 场景样例库与端到端回放脚本
- 状态：待办。
- 为什么做：Prompt 评测能约束单个模板，但 AI 实际使用 DataSpec 是连续任务：选项目、查标准、生成 SQL、校验、修复、导出证据；缺少场景级样例会让整体行为退化不易发现。
- 已有基础：已有演示项目、workflow recipes、AI 回放、Prompt 模板评测待办、golden fixtures、浏览器级 E2E 待办和前端上手演练待办。
- 缺口：缺少 scenario dataset，无法定义一次 AI 任务的输入上下文、期望调用顺序、关键输出断言、允许差异和失败诊断。
- 参考项目：`promptfoo/promptfoo` 的样例评测、`langfuse/langfuse` 的 datasets/trace 思路和 `microsoft/playwright` 的 trace 产物；只做本地回放，不默认调用外部 LLM。
- 落地产物：新增 `examples/ai-scenarios` 或等价目录；每个场景包含 project seed、input task、expected tools、expected artifacts、contract assertions、redaction checks 和 replay command；CLI 提供 dry-run 回放入口。
- 验收标准：建订单表、修复 bad SQL、反向导入候选、导出最小 Context 等场景可一键回放；输出不符合 JSON/Markdown/字段引用约束时验证失败；失败报告能指向具体步骤。
- 边界：不评价外部模型智能水平，不保存真实业务数据，不让回放脚本执行高风险写入；第一版以本地确定性断言为主。

### P6-121：DataSpec 自身数据模型契约快照
- 状态：待办。
- 为什么做：项目已经有 Flyway 迁移和 OpenAPI 契约，但 DataSpec 自身的表、字段、索引和 JSON payload 约束也在持续扩展；如果迁移或实体变更没有契约快照，老库升级、AI 回放和备份恢复都可能出现隐性不兼容。
- 已有基础：已有 Flyway、数据库 schema dump、OpenAPI 类型契约、备份恢复迁移包、标准快照、执行证据包和文档状态一致性待办。
- 缺口：缺少 DataSpec 内部 schema contract，不容易对比当前数据库、迁移脚本、实体模型和文档之间是否一致，也无法标记破坏性数据模型变化。
- 参考项目：`ariga/atlas` 的 schema-as-code、`liquibase/liquibase` 的数据库变更日志和 `prisma/prisma` 的 introspection 思路；只借鉴契约快照，不引入重型 ORM 或迁移平台。
- 落地产物：新增内部数据模型契约快照；记录 tables、columns、indexes、foreignKeys、jsonFields、requiredConstraints、migrationVersion、breakingChangeHints 和 restoreCompatibility；验证命令可比较 Flyway 结果与快照。
- 验收标准：修改后端表结构或重要 JSON 字段时，必须更新契约快照并通过校验；备份恢复和 AI 回放能知道目标库是否满足最低数据模型版本。
- 边界：不替代 Flyway，不自动生成所有迁移，不要求一次性补全历史数据库；第一版优先覆盖核心表和 AI/反向导入相关 payload。

### P6-122：前端问题反馈转任务与证据采集
- 状态：待办。
- 为什么做：个人使用时发现页面空状态、接口失败、校验异常或体验卡点，通常需要手工截屏、复制 URL、复制项目 ID 和日志；如果能一键生成反馈任务，AI 后续修复会更快。
- 已有基础：已有前端统一状态待办、命令面板、AI 任务推荐队列、诊断包、浏览器级 E2E、执行证据包和 TODO 到 OpenSpec 交接。
- 缺口：前端缺少轻量 feedback-to-task 入口，无法把 route、projectId、traceId、requestError、currentFilters、recentActions、screenshotHint 和 userNote 组织成可复现任务。
- 参考项目：`getsentry/sentry-javascript` 的前端错误上下文、`microsoft/playwright` 的 trace 截图和 `github/gh` 的 issue/PR 任务描述体验；只借鉴上下文采集和任务模板，不接入远程 SaaS。
- 落地产物：新增前端“反馈/生成任务”入口或错误页动作；导出脱敏 JSON/Markdown，包含复现步骤、页面状态、接口错误、traceId、相关记录 id、建议验证命令和 OpenSpec/TODO 草稿。
- 验收标准：在 SQL 校验失败、反向导入异常或字段库筛选异常时，可一键生成可复制的修复任务；生成内容不包含 token、密码、完整 JDBC URL 或业务数据行；AI 能直接基于任务继续排查。
- 边界：不自动提交 GitHub issue，不上传截图或日志，不替代用户说明；第一版只在本地浏览器和当前项目内生成证据。

### P6-123：字段标准向 API/DTO Schema 导出
- 状态：待办。
- 为什么做：DataSpec 已能从数据库、文档和契约反向提取标准，但 AI 在写接口、DTO、前端类型或校验逻辑时，还需要把字段标准正向导出成可复用的 JSON Schema、OpenAPI components 或 TypeScript 类型片段。
- 已有基础：已有标准契约 registry 待办、OpenAPI 类型生成、多源契约反向导入、字段格式约束、枚举生命周期、AI Context 和数据字典导出。
- 缺口：标准字段、枚举、格式、敏感标识和生命周期状态缺少面向 API/DTO 的导出适配；AI 只能读字段目录后自行拼 schema，容易丢失 required、format、enum、deprecated 和 example 约束。
- 参考项目：`OpenAPITools/openapi-generator` 的组件生成、`glideapps/quicktype` 的类型推导和 `Redocly/redocly-cli` 的 OpenAPI bundle/lint；只借鉴导出结构，不把 DataSpec 改成通用 API 网关。
- 落地产物：新增 schema fragment 导出 API/CLI/MCP；按项目、分组、字段集合或表模板导出 JSON Schema、OpenAPI components、TypeScript interface 片段和字段到 schema 的映射证据。
- 验收标准：AI 可直接把导出的 schema 片段用于接口 DTO、表单校验或契约文档；导出结果包含字段来源、枚举值、format、deprecated/replacement 和敏感字段提示；契约有 fixture 防漂移。
- 边界：不自动修改业务仓库，不覆盖业务项目已有 OpenAPI 文件，不保证所有后端框架类型系统完整映射；第一版只导出可审阅片段。

### P6-124：ORM/代码模型反向提取标准候选
- 状态：待办。
- 为什么做：很多项目的真实字段定义存在于 Prisma schema、TypeORM Entity、JPA 注解、MyBatis 映射或前端类型里；只靠数据库直连会漏掉尚未落库的模型、DTO 命名和业务注释。
- 已有基础：已有业务仓库初始化、defaultPaths、业务代码字段引用索引、多源契约反向导入、标准候选 Inbox、字段推荐和字段来源追踪。
- 缺口：当前代码扫描更偏引用和风险分析，缺少从 ORM/代码模型抽取 candidateFields、candidateEnums、tableName、columnName、comment、validationHints 和 sourceRange 的导入链路。
- 参考项目：`prisma/prisma` 的 schema introspection、`typeorm/typeorm` 的实体元数据和 `mybatis/mybatis-3` 的映射文件结构；只处理本地源码，不执行应用代码。
- 落地产物：新增代码模型候选提取器；支持 dry-run 扫描配置路径，输出候选字段、表关系、枚举、注释、来源文件位置、置信度和冲突原因，并可进入候选 Inbox。
- 验收标准：给定包含 Prisma/TypeORM/JPA/MyBatis 的业务仓库，能提取稳定候选并保留文件行号证据；重复扫描不会刷出重复候选；敏感源码片段不进入长期记录。
- 边界：不解析所有语言和框架，不执行业务代码，不自动采纳候选；第一版优先覆盖结构清晰的 schema/entity/mapper 文件。

### P6-125：前端组件状态样例库与视觉回归预备
- 状态：待办。
- 为什么做：页面越来越多后，空状态、加载中、错误、无项目、无权限、长文本和大表格状态容易各写各的；AI 修改前端时也缺少可查看的组件状态样例，容易破坏已打磨过的交互。
- 已有基础：已有前端源码级 smoke、浏览器级 E2E 待办、统一前端状态待办、前端任务式导航、可访问性待办和 Element Plus 组件体系。
- 缺口：缺少 Storybook 或等价本地组件状态目录；核心页面组件没有固定样例数据和截图边界，无法为后续视觉回归或设计检查提供稳定基线。
- 参考项目：`storybookjs/storybook` 的组件状态组织、`microsoft/playwright` 的截图/trace 和 `dequelabs/axe-core` 的可访问性检查；只做本地开发辅助，不引入复杂设计系统。
- 落地产物：为核心前端组件建立状态样例库，覆盖项目选择、任务入口、空状态、错误提示、diff、表格分页、详情弹窗和 Monaco 结果面板；保留可选截图验证入口。
- 验收标准：开发者和 AI 能在本地打开样例查看组件各种状态；新增核心组件时有最小样例；后续可平滑接入视觉回归和 a11y 检查。
- 边界：不重做 UI 体系，不追求像素级全站回归，不把普通开发构建强制依赖 Storybook；第一版只覆盖高频组件。

### P6-126：SQL 规则变异回归与误报召回基线
- 状态：待办。
- 为什么做：规则、fixedSql 和多方言解析会持续增强，只靠少量 good/bad fixture 容易漏掉边界；需要能系统性生成近似变体，验证规则不误杀、不漏报、不生成危险修复。
- 已有基础：已有 lint 规则测试、golden fixtures、SQL/DDL 多方言矩阵、规则调试器待办、fixedSql diff、示例反例库和标准变更演练沙箱。
- 缺口：缺少 mutation/variant corpus，把表名大小写、注释位置、schema 前缀、quoted identifier、默认值、枚举 literal 和方言差异组合成可重复回归样例。
- 参考项目：`sqlfluff/sqlfluff` 的规则 fixture 组织、`eslint/eslint` 的规则测试元数据和 `stryker-mutator/stryker-js` 的变异测试思路；只借鉴变体生成与召回统计，不要求完整 mutation testing 平台。
- 落地产物：新增 SQL rule variant generator 或 fixture 目录；为关键规则输出 falsePositive、falseNegative、unsafeFix 和 dialectCompatibility 指标，并接入可选验证命令。
- 验收标准：修改命名、COMMENT、枚举、fixedSql 或方言规则后，能看到误报/漏报基线变化；失败样例可升格为 golden fixture；默认 `mvn test` 保持轻量，可选命令跑扩展语料。
- 边界：不追求穷尽 SQL 语法，不默认调用外部 LLM 造样例，不让慢速变异测试阻塞每次小改动。

### P6-127：AI Context 增量更新包与差分同步
- 状态：待办。
- 为什么做：标准字段、规则、模板和证据越来越多后，每次都导出完整 AI Context 会浪费上下文；AI 更需要知道“自上次快照后哪些标准变了、哪些包需要重新读取、哪些旧上下文还能继续用”。
- 已有基础：已有标准快照、离线 AI Context 缓存、标准变更发布说明、版本兼容矩阵、环境指纹、字段生命周期和 Context 预算裁剪待办。
- 缺口：Context 导出缺少 delta manifest；无法按 fromSnapshot/toSnapshot 输出新增、修改、废弃、删除、规则变更、模板变更和兼容提示，也不能告诉 AI 旧包是否可增量补读。
- 参考项目：`changesets/changesets` 的变更集说明、`OpenLineage/OpenLineage` 的事件模型和 `bufbuild/buf` 的 breaking change 检查；只做标准上下文差分，不做远程同步服务。
- 落地产物：新增 Context delta API/CLI/MCP；输入基准快照或缓存 manifest，输出 changedArtifacts、breakingChanges、deprecatedFields、requiredRefresh、patchPackage 和 suggestedPrompt。
- 验收标准：AI 能基于上一份 Context 只读取增量包；破坏性标准变更会明确要求重新导出完整包或停止确认；增量包含版本、hash 和回退说明。
- 边界：不长期托管 Context 包，不自动推送到业务仓库，不保证跨大版本无限兼容；第一版只覆盖当前项目内快照差异。

### P6-128：前端包体性能预算与路由懒加载
- 状态：待办。
- 为什么做：功能页面持续增加后，Monaco、Element Plus、图表和大页面逻辑可能让首屏变慢；个人工具也需要保持打开即用，AI browser automation 也依赖稳定加载。
- 已有基础：已有前端性能体验指标、浏览器级 E2E 待办、源码级 smoke、Vite 构建、Monaco Editor、AI 回放和多页面路由。
- 缺口：缺少 bundle budget、chunk 分析、重型页面懒加载策略和构建后性能摘要；当前 `pnpm build` 只验证能否构建，不约束包体变化和首屏依赖。
- 参考项目：`vitejs/vite` 的构建分析能力、`GoogleChrome/lighthouse` 的性能预算和 `TanStack/query` 的请求缓存思路；只做本地预算与懒加载，不接入外部性能平台。
- 落地产物：新增前端包体报告和预算阈值；对 Monaco/图表/大页面启用路由级懒加载或动态导入；README 说明如何查看构建体积和常见优化路径。
- 验收标准：新增重型页面时能看到 chunk 变化；首屏不强制加载 SQL 编辑器等非当前页面资源；预算超标时给出可读提示和候选优化项。
- 边界：不追求极限性能分数，不为了包体牺牲核心交互，不把所有页面一次性重写为复杂异步组件。

### P6-129：标准包 Lockfile 与业务仓库可复现同步
- 状态：待办。
- 为什么做：AI 在业务仓库里使用 DataSpec 时，不能只知道当前服务地址和 projectId，还需要知道自己读取的标准包、Context、规则和快照是否被固定；否则同一任务隔几天重跑可能引用不同版本标准。
- 已有基础：已有标准快照、离线 AI Context 缓存、业务仓库 `.dataspec/config.json`、Context 增量更新包、版本兼容矩阵和 `dataspec doctor`。
- 缺口：缺少类似 lockfile 的消费端固定文件，无法记录 specHash、contextHash、rulesHash、schemaVersion、生成命令、兼容范围和最后校验结果。
- 参考项目：`pnpm/pnpm` 的 lockfile 可复现安装、`rust-lang/cargo` 的 `Cargo.lock` 和 `hashicorp/terraform` 的 provider lock 思路；只借鉴固定依赖与校验结构，不把标准发布成包管理器生态。
- 落地产物：新增 `.dataspec/lock.json` 约定和 CLI/API 校验；`dataspec init/export-context/doctor` 可生成、更新和检查 lock；AI Context manifest 引用 lock 元数据。
- 验收标准：业务仓库能固定某一版标准包并检测过期；AI 执行前能判断本地 lock 与服务端快照是否一致；变更 lock 会产生清晰 diff 和建议命令。
- 边界：不自动提交业务仓库，不强制所有用户启用 lock，不替代标准快照；第一版只覆盖本地仓库和当前 DataSpec 服务。

### P6-130：SQL 格式化 Profile 与确定性输出
- 状态：待办。
- 为什么做：SQL lint 和 fixedSql 能指出问题与候选修复，但 AI 生成的 SQL 仍可能在缩进、关键字大小写、逗号位置和 COMMENT 排列上不稳定，影响 review、diff 和后续规则判断。
- 已有基础：已有 SQL lint、多方言诊断、fixedSql diff、规则配置、SQL 规则调试器、示例反例库和 golden fixtures。
- 缺口：缺少项目级 SQL style profile 和 deterministic formatter，无法把“格式偏好”作为 AI 可读取、可验证、可回放的标准契约。
- 参考项目：`sqlfluff/sqlfluff` 的 lint/format 分层、`darold/pgFormatter` 的 PostgreSQL 格式化经验和 `sql-formatter-org/sql-formatter` 的多方言格式化；只借鉴格式 profile，不做复杂语义重写。
- 落地产物：新增 SQL 格式化配置、CLI/API preview 和前端格式化预览；支持 keywordCase、indentSize、commaStyle、commentPlacement、dialect 和 unsafeChange 标记；接入 fixture/golden。
- 验收标准：同一 SQL 在同一 profile 下输出稳定；格式化不改变表/字段/默认值语义；前端和 CLI 能展示 format diff，并明确哪些差异只是格式。
- 边界：不替代 lint 规则，不承诺支持所有 SQL 方言语法，不默认修改业务仓库文件；第一版优先覆盖 DataSpec 已支持的建表 SQL。

### P6-131：受控脱敏样例值采样与合成回填
- 状态：待办。
- 为什么做：字段标准对 AI 最有帮助的不只是名称和类型，还包括安全的示例值、格式边界和反例；但直接读取真实业务数据会带来隐私和泄漏风险。
- 已有基础：已有敏感字段标记、字段示例值、字段格式约束、数据库直连只读诊断、脱敏日志边界、字段质量评分和示例反例库。
- 缺口：缺少一个只读、可预览、默认脱敏的样例采样/合成入口，无法从真实 schema 观察格式，也无法安全地补齐 AI Context 中的 examples。
- 参考项目：`microsoft/presidio` 的 PII 检测与脱敏、`faker-js/faker` 的合成数据生成和 `gitleaks/gitleaks` 的敏感信息扫描思路；只借鉴脱敏与合成，不保存原始业务数据。
- 落地产物：新增样例采样 dry-run 和合成回填流程；按字段输出 maskedExamples、syntheticExamples、invalidExamples、riskWarnings、samplingSql 和 userConfirmation；字段库可选择性写入脱敏样例。
- 验收标准：手机号、身份证、邮箱、金额、时间等字段能生成安全示例；任何原始值默认不落库、不进日志、不进 AI Context；用户确认后只保存脱敏或合成值。
- 边界：不默认扫描数据行，不保存完整查询结果，不处理高风险敏感字段的真实采样；第一版仅限小样本、只读连接和显式确认。

### P6-132：标准消费清单与过期项目盘点
- 状态：待办。
- 为什么做：当多个业务仓库、脚手架或 AI 任务开始消费同一个 DataSpec 标准后，需要知道哪些仓库仍在用旧 Context、旧 lock 或旧规则，否则标准变更发布后很难追踪影响。
- 已有基础：已有多项目标准复用包、业务仓库初始化、标准包 lockfile 待办、标准变更发布说明、AI Context 缓存和环境配置漂移检测。
- 缺口：缺少 consumer inventory，无法按 repo/path/branch/projectId/contextHash/lastCheckedAt 盘点消费端，也无法提醒哪些业务仓库应刷新标准包。
- 参考项目：`backstage/backstage` 的软件目录、`renovatebot/renovate` 的依赖过期检测和 `OpenLineage/OpenLineage` 的消费关系表达；只做本地清单，不自动扫描远程组织。
- 落地产物：新增标准消费清单 API/CLI；从 `.dataspec/config.json`、`.dataspec/lock.json` 和本地扫描结果生成 consumer records；前端展示过期、缺 lock、无法访问和兼容风险。
- 验收标准：用户能看到哪些本地业务仓库消费了当前项目标准；标准快照更新后能列出需刷新 Context 的仓库；输出可被 AI 用于生成迁移任务。
- 边界：不上传本地仓库路径，不自动改业务仓库，不接入组织级资产管理；第一版只处理用户显式配置或本地允许扫描的路径。

### P6-133：规则覆盖率与死规则清理报告
- 状态：待办。
- 为什么做：规则、豁免、模板和 fixedSql 持续增加后，需要知道哪些规则长期无命中、哪些规则只产生误报、哪些规则缺少 fixture；否则规则体系会变重，AI 也会读到低价值约束。
- 已有基础：已有规则配置、规则调试器、规则误报豁免、SQL 规则变异回归、golden fixtures、检查记录和字段质量门禁。
- 缺口：缺少 rule coverage/report，无法聚合 lastTriggeredAt、fixtureCoverage、falsePositiveHints、suppressionRate、fixerCoverage 和 recommendedAction。
- 参考项目：`eslint/eslint` 的规则测试组织、`istanbuljs/nyc` 的覆盖率报告和 `sqlfluff/sqlfluff` 的规则 fixture 体系；只借鉴覆盖率指标，不把 lint 规则变成测试覆盖率平台。
- 落地产物：新增规则覆盖率报告 API/CLI 和前端摘要；基于历史检查记录、fixture、豁免和 fixedSql 计划统计规则活跃度、质量风险和清理建议。
- 验收标准：能识别长期未触发规则、缺 fixture 规则、高豁免率规则和无 fixer 规则；报告给出保留、补测试、降级或停用建议；不会影响正常 lint 执行。
- 边界：不自动删除规则，不用覆盖率作为唯一质量判断，不上传 SQL 内容；第一版只统计 DataSpec 已保存的安全元数据和本地 fixture。

### P6-134：前端 mock API 演示模式与无后端体验
- 状态：待办。
- 为什么做：本地部署包已经能启动完整服务，但前端开发、截图走查、组件状态验证和 AI browser automation 有时只需要稳定假数据；无后端 mock 模式能降低调试成本，也能让 UI 回归更可复现。
- 已有基础：已有本地演示启动包、前端源码级 smoke、组件状态样例库、端到端上手引导、Vite 开发环境和 OpenAPI schema.ts。
- 缺口：前端目前依赖真实后端和数据库状态，缺少 mock API 层、稳定 demo fixtures、错误状态脚本和无后端页面验收入口。
- 参考项目：`mswjs/msw` 的 API mock、`storybookjs/storybook` 的组件状态样例和 `microsoft/playwright` 的可复现测试数据；只做本地开发与演示，不把 mock 当真实数据源。
- 落地产物：新增 `pnpm dev:mock` 或等价 mock 开关；基于 schema.ts 和 fixtures 拦截项目、字段、SQL lint、反向导入、AI Context 等高频 API；支持切换成功、空数据、错误和慢请求场景。
- 验收标准：不启动后端也能打开核心前端页面并完成演示流程；mock 数据与 OpenAPI 类型保持一致；浏览器级测试可选择 mock 模式稳定复现 UI 状态。
- 边界：不替代真实集成测试，不在生产构建启用 mock，不保存用户输入；第一版覆盖高频页面和失败状态即可。

### P6-135：AI 任务预检与缺口补齐建议
- 状态：待办。
- 为什么做：DataSpec 越来越适合 AI 使用后，AI 在执行建表、修 SQL、反向导入、导出 Context 前，需要先知道当前项目是否具备必要字段、规则、快照、profile、lock 和连接配置；只靠通用 `doctor` 还不够任务化。
- 已有基础：已有 `dataspec doctor`、AI 使用画像待办、标准快照、字段质量报告、AI Context、标准包 lockfile 待办和版本兼容矩阵。
- 缺口：缺少按 `taskType` 输出的 preflight DTO，无法把 blockers、warnings、recommendedCommands、requiredInputs、contextBudget 和 stopReasons 一次性给 AI。
- 参考项目：`backstage/backstage` 的模板执行前校验、`Schemathesis/schemathesis` 的契约前置检查和 `github/gh` 的命令式诊断体验；只借鉴任务预检，不做复杂流程编排。
- 落地产物：新增 `/api/ai-preflight`、CLI `dataspec preflight --task` 和 MCP 工具；按任务返回 readiness、missingItems、safeToProceed、recommendedCommands、relatedDocs 和 machineReadableHints；前端 AI 工作台展示同一结果。
- 验收标准：AI 在执行 SQL 修复或反向导入前能先拿到明确的可继续/需补齐判断；缺少 projectId、profile、字段标准、启用规则或 Context 过期时给出可执行命令；输出 JSON 有契约测试。
- 边界：不自动修复项目配置，不替代人工确认，不把 preflight 做成审批流；第一版只覆盖核心 AI 任务。

### P6-136：数据库直连反向导入 Plan/Apply 与可撤销批次
- 状态：待办。
- 为什么做：数据库直连反向导入已经能预览和确认导入，但大库或高冲突场景需要更清楚的“计划 -> 应用 -> 可追溯撤销”体验，避免 AI 或用户一次性采纳过多候选。
- 已有基础：已有数据库直连 metadata、反向导入预览、compare、字段来源批次、导入映射策略待办、统一 diff 待办和标准候选 Inbox。
- 缺口：缺少持久化 import plan、逐项 decision、apply summary、undo hints 和冲突理由；导入后的回退主要依赖人工查批次。
- 参考项目：`hashicorp/terraform` 的 plan/apply、`bytebase/bytebase` 的数据库变更预览和 `openrewrite/rewrite` 的 dry-run recipe；只应用到 DataSpec 标准库，不写源数据库。
- 落地产物：新增 import plan 模型/API 和前端计划详情；支持保存候选选择、冲突处理、字段映射理由、预计新增/更新/跳过数量、apply result 和可撤销批次提示；CLI/MCP 可读取 plan。
- 验收标准：同一次反向导入可先保存计划再应用；应用后能按批次看到每个字段的来源和 decision；误导入时能生成可审查的撤销建议。
- 边界：不自动删除已有标准字段，不修改业务数据库，不承诺一键无损回滚所有人工编辑；第一版只覆盖导入批次内新增/更新的标准记录。

### P6-137：`.dataspec/config.json` Schema 与编辑器提示包
- 状态：待办。
- 为什么做：`.dataspec/config.json` 会承载 projectId、serverUrl、defaultPaths、aiProfile、taskType、contextScope 和 lock 等配置；字段变多后，AI 和用户都需要编辑器提示与稳定校验，避免拼错配置导致任务失败。
- 已有基础：已有业务仓库初始化、`.dataspec/config.json` 读取、doctor、AI profile 待办、标准包 lockfile 待办和版本兼容矩阵。
- 缺口：缺少 JSON Schema、示例 fixtures、配置迁移说明和编辑器可发现入口；当前类型错误主要运行时才暴露。
- 参考项目：`SchemaStore/schemastore` 的配置 Schema 组织、`Redocly/redocly-cli` 的配置校验和 `vitejs/vite` 的 typed config 体验；第一版只在仓库内发布 Schema。
- 落地产物：新增 `schemas/dataspec-config.schema.json`、示例配置和 CLI `dataspec config validate`；`init` 写入 `$schema`；doctor 使用同一 Schema 输出字段级错误；README/AGENTS 片段说明配置含义。
- 验收标准：编辑 `.dataspec/config.json` 时能获得字段提示；拼错枚举、类型或未知字段时 doctor 返回可读错误；Schema 与 CLI 解析有契约测试。
- 边界：不马上提交到官方 SchemaStore，不支持任意插件扩展字段，不改变现有配置向后兼容行为。

### P6-138：本地凭据复用与 Secret Provider 边界
- 状态：待办。
- 为什么做：数据库直连、API Token 和业务仓库配置都需要凭据，但个人/小团队使用时既不能把密码写入仓库，也不应该每次操作都重复输入；需要明确的本地凭据引用方式和脱敏诊断。
- 已有基础：已有 API Token 管理、数据库连接预设、只读安全诊断、敏感信息脱敏、`.dataspec/config.json` 和前端反向导入记忆。
- 缺口：目前主要依赖“不保存密码”和手动输入，缺少 `env:`、本地 secret 文件、系统 keyring 或命令读取的统一引用协议，也缺少 doctor 对 secret 引用可用性的检查。
- 参考项目：`getsops/sops` 的密文配置、`dotenvx/dotenvx` 的环境变量管理和 `gitleaks/gitleaks` 的 secret 检测；只做本地安全边界，不接入企业密钥平台。
- 落地产物：新增 secret reference 约定，如 `env:DATASPEC_DB_PASSWORD`、`file:` 或 `command:` 的最小集合；前端和 CLI 对敏感字段统一脱敏；doctor 检查引用是否存在但不输出明文；文档说明安全边界。
- 验收标准：反向导入可以复用非明文凭据完成连接；日志、记录、AI Context、错误提示和导出包不泄漏 secret；缺失凭据时给出明确修复命令。
- 边界：不保存真实密码到数据库，不实现团队级 vault，不在浏览器本地存储长期保存数据库密码；第一版优先服务本机 CLI 和显式前端输入。

### P6-139：标准变更迁移 Recipe 与半自动代码修复建议
- 状态：待办。
- 为什么做：字段重命名、类型调整、枚举变更或标准废弃后，AI 不只需要知道“标准变了”，还需要可执行的迁移 recipe，才能在业务仓库里安全搜索、生成补丁或提示人工修改。
- 已有基础：已有字段影响分析、标准变更日志、字段生命周期、标准变更发布说明、业务代码字段引用索引、API/DTO Schema 导出和 ORM/代码模型候选提取待办。
- 缺口：缺少 machine-readable migration recipe，无法表达 rename、replaceType、enumMapping、deprecatedReplacement、riskLevel、affectedPatterns 和 verificationCommands。
- 参考项目：`openrewrite/rewrite` 的代码迁移 recipe、`renovatebot/renovate` 的批量升级说明和 `sourcegraph/sourcegraph` 的代码检索能力；只生成建议和 dry-run，不默认改业务仓库。
- 落地产物：新增标准变更 recipe 结构、CLI dry-run 扫描和 AI Context 迁移片段；字段变更记录可导出 recipe；前端展示影响、候选替换和验证命令。
- 验收标准：字段 `user_id` 改名或废弃时，AI 能拿到推荐替换字段、匹配模式、示例补丁和验证命令；高风险替换需要人工确认；recipe 有 fixture 防漂移。
- 边界：不做完整 IDE 重构引擎，不自动提交业务代码，不承诺覆盖所有语言；第一版覆盖 SQL、Java/TypeScript 常见字符串和 schema 片段。

### P6-140：前端/CLI/MCP 统一任务结果协议
- 状态：待办。
- 为什么做：同一件事通过前端、CLI、MCP 或 API 执行后，现在结果展示字段、下一步建议、证据链接和失败状态容易不一致；AI 需要稳定读取“完成/部分完成/阻塞”和后续动作。
- 已有基础：已有 AI 任务卡、AI 回放、执行证据包、全链路 Trace、前端统一状态、MCP/CLI 工具契约验收和 AI 可读错误码待办。
- 缺口：缺少统一 `TaskResult` 协议，无法复用 status、summary、counts、artifacts、evidenceRefs、nextActions、retryable、blockedReason、traceId 和 suggestedCommands。
- 参考项目：`github/gh` 的命令输出、GitHub Actions job summary、`modelcontextprotocol/servers` 的工具结果结构和 `getsentry/sentry-javascript` 的错误上下文；只统一结果表达，不引入任务调度平台。
- 落地产物：新增 TaskResult JSON Schema/DTO、CLI/MCP 输出适配和前端结果卡片组件；SQL 校验、Context 导出、反向导入、doctor/preflight 逐步接入；文档列出字段语义。
- 验收标准：AI 调用任一核心任务都能用同一方式判断是否成功、下一步做什么、证据在哪；前端失败卡片与 CLI JSON 的关键字段一致；兼容旧响应并有契约测试。
- 边界：不要求所有历史接口一次性迁移，不做长任务队列，不改变现有 API 的核心业务语义；第一版先包裹高频任务结果。

### P6-141：AI 输出 SQL/DDL 执行前验证沙箱
- 状态：待办。
- 为什么做：AI 生成的 SQL、DDL 或 fixedSql 即使通过规则检查，也可能在目标数据库方言、保留字、索引约束或权限边界上失败；执行前需要一个只读、可解释的验证沙箱。
- 已有基础：已有 SQL lint、fixedSql、DDL 生成、多方言诊断、数据库直连只读诊断、数据库连接预设、规则模板库和执行证据包待办。
- 缺口：缺少面向 AI 的 `validate-only` 入口，无法在不写源库的前提下返回 parse/explain/plan 结果、方言降级原因、风险等级和建议修复。
- 参考项目：`bytebase/bytebase` 的 SQL Review、`ariga/atlas` 的 schema lint 和 `sqlfluff/sqlfluff` 的规则诊断；只借鉴执行前验证，不默认执行真实变更。
- 落地产物：新增 SQL/DDL 验证 API、CLI 和 MCP 工具；按数据源能力选择 parser-only、EXPLAIN、事务回滚或离线方言校验；输出 diagnostics、riskLevel、databaseCapability、suggestedFixes 和 evidenceRefs。
- 验收标准：AI 在应用 fixedSql、建表 DDL 或迁移 SQL 前可获得明确的可执行/需修复结论；验证过程不修改业务数据库；失败信息可被前端、CLI 和 MCP 统一展示。
- 边界：不执行 DML 写入，不替代数据库发布系统，不保存业务数据行；不支持的方言必须明确降级为 parser-only。

### P6-142：多 schema / 多数据源反向导入合并策略
- 状态：待办。
- 为什么做：个人项目也可能同时连接本地库、测试库、多个 schema 或微服务数据库；反向导入若只按单连接处理，容易把同名字段、同义字段和环境差异混在一起。
- 已有基础：已有数据库直连反向导入、compare、来源批次、字段映射策略、标准候选 Inbox、数据源连接器注册和多源契约导入待办。
- 缺口：缺少跨连接的 source namespace、schema priority、conflict group、merge decision 和 environment tag，无法稳定回答“这个字段来自哪个库、是否应合并为同一标准”。
- 参考项目：`datahub-project/datahub` 的数据源来源建模、`open-metadata/OpenMetadata` 的资产元数据和 `schemacrawler/SchemaCrawler` 的 schema 抽取；只做本地标准候选合并，不做完整数据目录平台。
- 落地产物：新增多数据源导入计划模型；反向导入预览可按连接/schema 分组，支持冲突合并建议、来源权重、环境标签和批次级 apply summary；AI Context 标明字段候选的来源集合。
- 验收标准：从两个 schema 导入同名字段时能显示来源差异和推荐决策；跨库同义字段可进入同一候选合并组；apply 后来源批次可追溯每个字段的连接和 schema。
- 边界：不自动跨库合并正式标准，不保存密码或完整连接串，不扫描业务数据行；第一版只服务人工确认和 AI 建议。

### P6-143：Agent 专用项目启动包生成
- 状态：待办。
- 为什么做：DataSpec 优先给 AI 使用，但不同 Agent（Codex、Claude、Cursor、MCP client）读取项目说明、命令、配置和安全边界的方式不同；每次手写上下文容易遗漏项目 ID、profile、验证命令或禁写边界。
- 已有基础：已有 AI 会话启动包、MCP/CLI 工作流模板、`.dataspec/config.json`、AI profile、doctor、README 快速开始和 `AGENTS.md` 约定。
- 缺口：缺少按目标 Agent 生成的最小启动包，无法稳定产出 `AGENTS.md` 片段、MCP 配置、CLI 命令、任务 profile、只读边界和验证入口。
- 参考项目：`agents.md` 的 Agent 指令约定、`modelcontextprotocol/servers` 的 MCP 配置样例和 `backstage/backstage` 的开发者入口；只生成本地说明和配置片段，不托管外部账号。
- 落地产物：新增 `dataspec agent-kit export --target codex|claude|cursor|mcp` 或等价 API/前端下载；输出项目摘要、推荐 profile、MCP resource、CLI 命令、验证命令和敏感信息禁用清单。
- 验收标准：新 Agent 拿到启动包后能完成读取标准、校验 SQL、导出 Context 和执行 doctor 的基础流程；包内不包含 token、密码、完整 JDBC URL 或业务数据行。
- 边界：不创建外部工作区，不管理 Agent 权限，不把 profile 当鉴权；第一版只覆盖本地文件和命令说明。

### P6-144：标准质量异常自动归因
- 状态：待办。
- 为什么做：字段质量分、覆盖率、冲突和候选采纳都会变化；如果只看到分数下降，AI 和用户仍要手动翻导入批次、规则变更和标准修改记录才能找到原因。
- 已有基础：已有字段质量评分、覆盖率报告、项目活动时间线、标准候选 Inbox、导入来源批次、标准快照、全链路 Trace 和健康趋势待办。
- 缺口：缺少 root-cause hints，无法把质量异常关联到最近导入批次、规则调整、字段合并、候选采纳、连接差异或契约导入。
- 参考项目：`great-expectations/great_expectations` 的数据质量结果、`getsentry/sentry-javascript` 的错误上下文和 `OpenLineage/OpenLineage` 的运行事件；只借鉴归因线索，不做自动判责。
- 落地产物：新增质量异常归因服务和前端摘要；按项目输出 changedMetrics、suspectedCauses、relatedEvents、evidenceRefs、recommendedChecks 和 suggestedCommands；CLI/MCP 可读取同一 JSON。
- 验收标准：覆盖率或质量分下降时能列出最可能的 3 个原因和证据链接；AI 可基于建议命令继续排查；归因结果不会包含敏感值或源库明文。
- 边界：不自动回滚标准，不把归因当最终事实，不引入复杂观测平台；第一版只基于 DataSpec 已保存的事件和脱敏摘要。

### P6-145：前端错误到修复 Action 的闭环体验
- 状态：待办。
- 为什么做：前端页面已经有较多诊断、空状态和错误信息，但用户或 AI browser automation 真正需要的是下一步可执行动作，例如创建项目、选择 profile、运行 doctor、重新加载表或导出证据包。
- 已有基础：已有前端统一数据状态、AI 可读错误码、任务式导航、端到端上手引导、doctor、preflight 待办和统一任务结果协议待办。
- 缺口：错误提示缺少 action schema，页面无法统一展示 `primaryAction`、`secondaryActions`、`copyCommand`、`retry`、`openDocs` 和 `createTask` 等动作。
- 参考项目：`TanStack/query` 的请求状态组织、`github/gh` 的命令式修复建议和 `microsoft/playwright` 的可测试用户动作；只做本地页面交互，不引入复杂流程引擎。
- 落地产物：新增前端 `ActionableState` 组件和轻量 action schema；项目未选、后端未启动、API 契约漂移、反向导入连接失败、AI profile 缺失等高频状态接入修复按钮和复制命令。
- 验收标准：关键页面遇到错误时至少提供一个明确可执行动作；按钮、命令和跳转可被 smoke 或浏览器测试覆盖；不会把敏感连接信息写入 URL 或剪贴板。
- 边界：不替代后端真实错误码，不自动执行高风险写入，不做企业审批流；第一版聚焦前端高频阻塞状态。

### P6-146：个人标准健康摘要与下一步报告
- 状态：待办。
- 为什么做：DataSpec 功能增多后，个人每天打开项目时需要快速知道“今天最值得处理什么”：新增候选、低质量字段、失败 SQL、导入差异、标准过期或 AI 任务阻塞。
- 已有基础：已有项目工作台、活动时间线、健康趋势、候选 Inbox、检查记录、覆盖率报告、AI 任务推荐队列、执行证据包和统一任务结果协议待办。
- 缺口：缺少按项目聚合的 digest DTO，无法稳定生成 todaySummary、topRisks、quickWins、blockedTasks、recommendedCommands 和 evidenceLinks。
- 参考项目：GitHub Actions job summary、`langfuse/langfuse` 的任务观测和 `backstage/backstage` 的开发者入口；只做个人项目摘要，不做团队通知系统。
- 落地产物：新增项目健康摘要 API、CLI `dataspec summary` 和前端工作台摘要卡；支持时间范围、只读导出 Markdown/JSON、复制给 AI 的下一步清单和证据链接。
- 验收标准：打开项目即可看到候选、规则、质量、导入、AI 任务的关键摘要；AI 可读取 JSON 后自动选择下一步任务；摘要不暴露 token、密码、完整 JDBC URL 或业务数据行。
- 边界：不发邮件/IM 通知，不做多人分派，不要求后台定时任务；第一版按用户主动打开或 CLI 调用即时生成。

### P6-147：Schema Registry 契约浏览器与兼容说明页
- 状态：待办。
- 为什么做：Schema Registry 第一版偏向 AI/CLI/MCP 消费，但用户和 AI browser automation 也需要在前端快速确认某个契约的版本、稳定字段、废弃字段和兼容策略。
- 已有基础：已有标准契约 registry 待办、OpenAPI 类型契约、AI Context manifest、CLI/MCP 契约出口和 README 兼容说明。
- 缺口：缺少前端可视化入口，无法按 contractId 查看 JSON Schema、stableFields、deprecatedFields、breakingChangePolicy 和示例 payload，也不能把契约 URI 复制给 AI。
- 参考项目：`Redocly/redocly-cli` 的契约文档化、`SchemaStore/schemastore` 的 Schema 目录和 `bufbuild/buf` 的 breaking change 检查；只借鉴浏览与兼容说明，不做外部 schema registry 服务。
- 落地产物：新增“系统设置 / 契约 Registry”页面或等价入口；支持契约列表、详情、兼容策略、废弃字段提示、示例 JSON 和复制 CLI/MCP resource URI；前端类型由 OpenAPI schema.ts 生成或 re-export。
- 验收标准：用户能在页面看到 Field、Rule、LintResult、AI Context 等契约的版本和稳定字段；AI 可复制一段契约上下文继续执行任务；契约字段变化有最小前端 smoke 或类型验证覆盖。
- 边界：不重做完整文档站，不允许前端编辑契约，不把内部 DTO 全量暴露为稳定契约；第一版只展示 AI 消费面。

### P6-148：可复用 AI 工作流 Recipe 编排
- 状态：待办。
- 为什么做：很多 AI 任务不是单个 API 调用，而是 doctor、preflight、导出 Context、执行 lint、生成 fixedSql、导出证据包等步骤的组合；每次在聊天里临时拼命令容易漏验证和边界。
- 已有基础：已有 MCP/CLI 工作流模板、AI 任务卡、AI 任务推荐队列、任务结果协议、执行证据包、doctor 和 preflight 待办。
- 缺口：缺少 machine-readable workflow recipe，无法表达 steps、inputs、requiredCapabilities、verificationCommands、artifacts、rollbackHint 和 blockedReason。
- 参考项目：GitHub Actions reusable workflow、`go-task/task` 的本地任务组织和 `casey/just` 的命令 recipe；只借鉴步骤声明，不引入远程任务调度平台。
- 落地产物：新增 `.dataspec/workflows/*.json|yaml` 约定、CLI `dataspec workflow list/run --dry-run` 和 MCP prompt/resource；内置 `safe-sql-fix`、`reverse-import-review`、`export-ai-context` 等个人高频流程。
- 验收标准：AI 能列出当前项目可执行工作流，先 dry-run 展示步骤和验证命令，再逐步执行并产出 TaskResult；失败步骤能给出可恢复位置和下一步建议。
- 边界：不做长任务队列，不自动执行高风险写入，不替代 OpenSpec 实施流程；第一版只编排已有能力和只读/显式确认步骤。

### P6-149：标准包同步巡检与漂移修复建议
- 状态：待办。
- 为什么做：业务仓库里缓存的 `.dataspec/`、Context 包、lockfile、标准快照和 CLI 配置会随 DataSpec 项目变化而过期；AI 如果读取旧包，会生成不符合当前标准的 SQL 或代码。
- 已有基础：已有业务仓库初始化、离线 AI Context 缓存、标准包 Lockfile、Context 增量更新包、版本兼容矩阵、doctor 和标准消费清单待办。
- 缺口：缺少同步巡检入口，无法比较业务仓库当前标准包与 DataSpec 服务端 registry/snapshot 的差异，也缺少安全的更新建议。
- 参考项目：`renovatebot/renovate` 的依赖漂移检测、`pnpm/pnpm` 与 `rust-lang/cargo` 的 lockfile 可复现策略；只借鉴漂移报告，不自动改业务仓库。
- 落地产物：新增 CLI `dataspec sync check` 或等价 doctor 子检查；输出 currentVersion、latestVersion、missingContracts、staleFiles、recommendedCommands 和 safeUpdatePlan；README 说明何时需要刷新 Context。
- 验收标准：业务仓库标准包过期时能明确提示需要更新哪些文件和原因；AI 可根据 JSON 输出生成更新 PR 建议；报告不包含 token、密码或业务数据行。
- 边界：不自动提交业务仓库，不强制联网，不把所有历史 Context 包回填；第一版只检查本地 `.dataspec/` 与当前服务端/导出物差异。

### P6-150：业务仓库标准合规分与 PR 摘要
- 状态：待办。
- 为什么做：DataSpec 的标准价值最终要体现在业务仓库变更里；单个 lint 结果太局部，AI 和用户需要一个 PR/变更级摘要判断本次修改对字段标准、命名、枚举、SQL 和 Context 版本的影响。
- 已有基础：已有 GitHub inline review、字段影响分析、标准质量门禁、业务代码字段引用索引、规则覆盖率、执行证据包和 TaskResult 待办。
- 缺口：缺少变更级 complianceScore、passedChecks、warnings、requiredActions、evidenceRefs 和 suggestedFixes，无法把多项检查合成一个可读 PR summary。
- 参考项目：`reviewdog/reviewdog` 的 diff 诊断聚合、GitHub Actions job summary 和 `great-expectations/great_expectations` 的验证结果结构；只借鉴摘要与分数，不把它变成团队审批流。
- 落地产物：新增 `dataspec review summary --format markdown|json`；聚合 SQL lint、字段引用、标准包版本、契约兼容、敏感信息和迁移 recipe；前端或证据包可展示同一摘要。
- 验收标准：一次业务仓库变更能生成机器可读和人可读的标准合规摘要；AI 可据此优先修复高风险项；低分原因必须有证据链接或可执行命令。
- 边界：不阻断用户本地提交，不替代 CI 审批，不读取业务数据行；第一版聚合已有静态检查和 DataSpec 元数据。

### P6-151：字段库密集编辑与大表格键盘体验
- 状态：待办。
- 为什么做：字段标准数量增加后，用户日常维护会从“偶尔新增一个字段”变成“批量筛选、对比、编辑、标记、撤销”；当前表格体验如果不够密集高效，会拖慢个人使用和 AI browser automation。
- 已有基础：已有字段库、字段分组、批量维护、来源追踪、质量评分、冲突检测、字段检索、前端性能和可访问性待办。
- 缺口：缺少可保存筛选视图、键盘多选、批量编辑草稿、列配置、虚拟滚动、行内校验和撤销提示，AI 自动化也缺少稳定 data-testid 与可预测焦点流。
- 参考项目：`TanStack/table` 的表格状态模型、`ag-grid/ag-grid` 的密集数据编辑体验和 Element Plus 表格组件；只借鉴交互模式，不引入过重企业表格平台。
- 落地产物：升级字段库表格体验；支持密集模式、列显示配置、保存筛选、键盘导航、多选批量操作、批量编辑预览和错误行定位；补前端 smoke/单测覆盖关键交互。
- 验收标准：用户能在几百到几千字段下流畅筛选和批量维护；键盘操作、焦点状态和错误提示可用；批量保存前能预览影响并保留撤销路径。
- 边界：不重做整个前端设计系统，不一次性迁移所有表格，不牺牲简单项目的轻量体验；第一版聚焦字段库和候选 Inbox 高频表格。

### P6-152：P6 待办里程碑收束与实施队列
- 状态：已完成快速收束第一版；顶部“下一步顺序”已压缩为近期优先队列，已完成 active OpenSpec change 按需归档，P6-73、P6-89、P6-71、P6-81 和 P6-82 状态已同步。
- 为什么做：P6 待办已经覆盖大量增强方向，如果只按编号线性追加，AI 和用户都容易在“下一个最该做什么”上迷路；需要把待办转成更可执行的 Now/Next/Later 和 OpenSpec 输入队列。
- 已有基础：已有 TODO 路线图、OpenSpec change 流程、归档记录、README 当前功能摘要、执行证据包和 TODO 到 OpenSpec 交接助手待办。
- 已完成能力：主 TODO 顶部不再把 P6-71 到 P6-188 当作默认线性实施顺序；近期任务、暂缓池、active change 按需保留/归档边界和 SDD 触发边界已显式写入。
- 参考项目：`backstage/backstage` 的开发者入口、`changesets/changesets` 的变更组织和 OpenSpec tasks 的可验证清单；只借鉴规划方式，不引入外部项目管理系统。
- 后续增强：完整拆分 P6-71 到 P6-188 到子路线、归档已完成 P5/P6 长段内容、自动状态检查脚本由 P6-83 承接。
- 验收标准：AI 打开项目后能先看到近期队列和效率优先顺序；已完成 active OpenSpec change 按当前任务边界保留或归档；新增建议能先归并到已有主题而不是继续追加新编号。
- 边界：不删除历史任务，不改变已完成事实，不要求一次性重写所有 P6 内容；第一版先收束当前 P6 队列和新增任务入口。

### P6-153：AI Context 注入防护与不可信文本隔离
- 状态：待办。
- 为什么做：DataSpec 会把字段注释、表注释、业务文档和数据库 metadata 提供给 AI，这些内容可能包含“忽略上文”“泄漏 token”等提示注入文本；如果不标记可信边界，AI 容易把业务文本误当作系统指令。
- 已有基础：已有 AI Context、敏感信息脱敏、受控脱敏样例、AI 能力边界模拟、执行证据包、标准契约 Registry 和 Agent 启动包待办。
- 缺口：缺少统一的不可信文本包装、sourceTrustLevel、instructionBoundary、redactionReason 和 contextSafetyWarnings，CLI/MCP/前端也没有把“业务内容不是指令”稳定写入上下文。
- 参考项目：`gitleaks/gitleaks` 的敏感信息扫描、`microsoft/presidio` 的 PII 识别和 MCP 规范中的 resources/prompts 分层；只借鉴安全边界，不引入外部 LLM 安全服务。
- 落地产物：为 AI Context、证据包、MCP resource 和 README/AGENTS 片段新增不可信文本边界说明；导出时为 comment、sample、document、metadata 标记 trustLevel 和 sanitizer 结果；高风险文本给出 warning 和可复核位置。
- 验收标准：AI 读取 Context 时能明确区分系统指令、工具契约和业务原文；检测到可疑提示注入或 secret-like 文本时会脱敏或标记；相关契约和 fixture 有测试覆盖。
- 边界：不替代人工安全审查，不扫描真实业务数据行，不做企业 DLP；第一版聚焦本地 AI 上下文的结构化隔离。

### P6-154：标准消费端 SDK 与类型常量包导出
- 状态：待办。
- 为什么做：AI 在业务仓库落地标准时，不只需要 JSON Context，还需要能被 Java、TypeScript 或 SQL 脚本直接引用的字段常量、枚举值、校验器和版本信息，减少手写字符串造成的漂移。
- 已有基础：已有 Schema Registry、OpenAPI 类型契约、标准向 API/DTO Schema 导出、业务仓库初始化、标准包 Lockfile、标准消费清单和配置 Schema 待办。
- 缺口：缺少面向消费端的 `dataspec-sdk` 导出格式，无法生成 FieldId、EnumCode、RuleCode、snapshotHash、deprecated 标记和兼容提示，也没有把生成物纳入 lockfile 校验。
- 参考项目：`OpenAPITools/openapi-generator` 的代码生成、`glideapps/quicktype` 的类型推导和 `bufbuild/buf` 的契约兼容检查；只生成轻量本地包，不发布公共包管理仓库。
- 落地产物：新增 CLI `dataspec sdk export --lang ts|java --output ...`；生成只读常量、枚举、schema version 和 README；AI Context 中记录 SDK 版本与生成命令；doctor 可检查 SDK 是否过期。
- 验收标准：业务仓库能引用生成的标准常量而不是散落字符串；标准变更后能检测 SDK 过期并提示重新生成；输出不包含 token、密码、连接串或业务数据行。
- 边界：第一版不做运行时 ORM 集成，不自动修改业务代码，不上传 npm/maven 仓库；只支持稳定字段契约和高频语言。

### P6-155：规则依赖图与冲突诊断
- 状态：待办。
- 为什么做：规则越来越多后，AI 和用户需要知道“哪些规则互相依赖、哪些规则可能给出冲突修复建议、为什么同一字段被多个规则命中”；否则会出现 fixedSql 或候选建议互相打架。
- 已有基础：已有规则配置、规则模板库、规则调试器、规则覆盖率、死规则清理、变异回归、fixedSql 策略和标准质量门禁待办。
- 缺口：缺少 ruleDependencies、conflictsWith、fixPriority、mutuallyExclusive 和 diagnosticExamples 等元数据，无法生成规则依赖图或冲突报告。
- 参考项目：`eslint/eslint` 的规则元数据、`sqlfluff/sqlfluff` 的规则分层和 `great-expectations/great_expectations` 的验证结果结构；只借鉴规则解释，不引入复杂规则引擎。
- 落地产物：扩展规则元数据契约；新增 CLI/API 输出规则依赖图和冲突诊断；前端规则配置页可查看“影响哪些检查/修复”；fixedSql 生成时按优先级解释取舍。
- 验收标准：新增或调整规则时能声明依赖和冲突；同一 SQL 出现多规则建议时能解释优先级；冲突诊断有 fixture 覆盖并纳入统一验证入口。
- 边界：不重写现有 lint 引擎，不强制所有历史规则一次性补齐完整图谱；第一版覆盖默认启用和 fixedSql 相关规则。

### P6-156：OpenAPI/CLI/MCP 示例契约快照自动生成
- 状态：待办。
- 为什么做：DataSpec 已有 OpenAPI、CLI 和 MCP 多个 AI 出口，但示例请求/响应如果靠手写，极易与真实契约漂移；AI 最需要的是可直接复制、可回归测试的示例快照。
- 已有基础：已有 OpenAPI 类型契约、Schema Registry、MCP/CLI 工具契约验收、AI 场景样例库、核心 fixture/golden 基线和 README 状态一致性检查待办。
- 缺口：缺少从测试 fixture 或本地服务自动生成 examples 的工具，无法让 docs、MCP prompts、CLI help 和契约测试共享同一份示例源。
- 参考项目：`Redocly/redocly-cli` 的 OpenAPI 文档化、`Schemathesis/schemathesis` 的契约测试和 `modelcontextprotocol/servers` 的工具示例组织；只生成本地快照，不引入远程文档平台。
- 落地产物：新增 `tools/generate-examples` 或等价脚本；从后端/CLI/MCP fixture 生成 `docs/examples/*.json|md`；README、MCP prompts 和 CLI help 引用生成产物；变更时通过测试提示更新。
- 验收标准：核心 API、CLI 命令和 MCP 工具有稳定示例；契约字段变化会导致示例快照差异可见；AI 可直接读取 examples 执行任务。
- 边界：不追求覆盖所有边缘接口，不把示例当成真实用户数据，不在构建时强制启动完整外部数据库。

### P6-157：个人单机分发包与离线启动预案
- 状态：待办。
- 为什么做：DataSpec 优先自己用，如果每次换机器或断网都要手动装 Java、Node、PostgreSQL、pnpm 和配置环境，AI 使用入口会被运行环境拖住；需要一个个人单机可迁移方案。
- 已有基础：已有本地部署与演示数据一键启动包、本地数据清理、备份恢复迁移包、前端 mock 模式、doctor、环境配置漂移检测和本地运行诊断待办。
- 缺口：缺少打包产物矩阵、离线依赖缓存、默认端口探测、版本升级说明和故障恢复脚本，README 也没有明确“新电脑 10 分钟启动”的离线路径。
- 参考项目：`dbeaver/dbeaver` 的本地数据库工具体验、`vitejs/vite` 的开发构建和 `backstage/backstage` 的开发者入口；只做个人单机包，不做云部署平台。
- 落地产物：补充 `scripts/package-local` 或等价流程；打包 server、web 静态资源、示例数据、配置模板和启动脚本；doctor 能识别离线包版本和缺失依赖；README 增加离线恢复路径。
- 验收标准：新机器在无公网或弱网环境下能按文档启动演示项目；升级时能保留本地数据或明确提示备份；启动失败时有可复制给 AI 的诊断包。
- 边界：不承诺跨平台安装器第一版完整覆盖，不自动修改系统级服务，不内置真实业务数据。

### P6-158：字段可见性等级与 AI Context 最小暴露策略
- 状态：待办。
- 为什么做：即使不包含业务数据行，字段名、注释、枚举值和样例也可能暴露业务敏感信息；AI Context 需要按任务只暴露必要字段，尤其是个人把 Context 交给不同 AI 工具时。
- 已有基础：已有 AI Context 裁剪、上下文预算、敏感信息脱敏、受控脱敏样例、字段生命周期、标准消费清单、API Token 和 AI 使用画像待办。
- 缺口：缺少字段级 visibility、sensitivity、allowedTasks、maskingProfile、reason 和 exportDecision，无法解释某字段为什么出现在某个 Context 包里，也不能按任务模式自动隐藏敏感字段。
- 参考项目：`microsoft/presidio` 的敏感信息识别、`faker-js/faker` 的安全样例生成和 `gitleaks/gitleaks` 的 secret 防泄漏；只借鉴识别与标记，不做复杂权限系统。
- 落地产物：扩展字段标准元数据和 AI profile；导出 Context 时按 taskProfile、visibility 和 maskingProfile 做最小暴露；CLI/MCP 输出 exportSummary，列出包含/排除字段数量、原因和脱敏策略。
- 验收标准：同一项目在 SQL 修复、字段推荐、文档问答等任务下导出的字段范围不同且可解释；敏感字段默认被遮蔽或仅提供安全别名；导出结果有测试覆盖。
- 边界：不做企业权限审批，不对历史 Context 包回溯删除，不扫描真实业务数据行；第一版只服务个人/小团队的 AI 使用安全。

### P6-159：AI 任务状态机与断点续跑
- 状态：待办。
- 为什么做：AI 执行 SQL 修复、反向导入、覆盖率分析或批量任务时，最怕中途失败后只能重跑；需要把任务状态、输入、阶段产物和恢复建议结构化，方便 AI 接着做而不是重新猜。
- 已有基础：已有 AI 批量任务、AI 回放、项目活动时间线、执行证据包、统一任务结果协议和并发幂等保护待办。
- 缺口：缺少统一 taskState、checkpoint、idempotencyKey、retryPolicy、resumeCommand 和 partialArtifacts，前端/CLI/MCP 无法稳定展示“可继续、需重试、需人工处理”的状态。
- 参考项目：`temporalio/temporal` 的 workflow 状态与重试模型、`dagster-io/dagster` 的资产任务运行记录和 `OpenLineage/OpenLineage` 的运行事件结构；只借鉴任务状态表达，不引入分布式调度平台。
- 落地产物：定义 AI 任务状态机契约；为高频 AI 任务记录 checkpoint 和可恢复动作；CLI/MCP 输出 resume 指令；前端展示失败阶段、可重试按钮和证据包关联。
- 验收标准：一个任务中断后，AI 能读取记录并继续到下一阶段；重复提交不会产生冲突副作用；失败原因、恢复入口和已生成产物可复查。
- 边界：不做长时企业工作流编排，不引入外部队列；第一版聚焦本地单机和当前已有 AI 任务。

### P6-160：标准变更影响预演与候选修复单
- 状态：待办。
- 为什么做：字段名、枚举、规则或可见性变更会影响 SQL、DDL、AI Context、业务仓库和文档；保存前应该让 AI 和用户看到影响范围，并生成可审查的修复清单。
- 已有基础：已有字段影响分析、标准变更日志、标准变更迁移 recipe、fixedSql 文件补丁、业务仓库迁移交付包和执行证据包待办。
- 缺口：缺少变更前 dry-run，无法按标准对象输出 impactedApis、impactedSql、impactedFiles、suggestedPatches、riskLevel 和 rollbackHint。
- 参考项目：`openrewrite/rewrite` 的迁移 recipe、`hashicorp/terraform` 的 plan/apply 思路和 `reviewdog/reviewdog` 的 diff 诊断评论；只做本地预演和修复单，不自动改真实仓库。
- 落地产物：新增标准变更 dry-run API/CLI；保存标准前可生成影响摘要和候选修复单；前端在字段/规则保存前展示影响预演；证据包记录本次变更的影响和取舍。
- 验收标准：变更一个字段或规则时能看到受影响对象、建议修复和风险等级；用户确认后再保存；AI 可把修复单转成后续任务。
- 边界：不强制所有保存都阻断，不直接连接生产仓库写补丁；第一版覆盖字段、枚举和默认规则。

### P6-161：AI 可读字段知识卡片
- 状态：待办。
- 为什么做：AI 使用字段标准时，单纯字段列表还不够；它需要每个字段的使用场景、禁止写法、示例、相关字段和常见误用，才能在建表、修 SQL 和问答时少走弯路。
- 已有基础：已有字段标准、数据字典、标准问答入口、业务术语表、示例反例库、自然语言标准候选和字段推荐质量增强。
- 缺口：缺少可直接嵌入 AI Context 的 FieldKnowledgeCard，无法稳定表达 aliases、antiPatterns、usageExamples、relatedFields、enumHints、riskNotes 和 lastVerifiedAt。
- 参考项目：`backstage/backstage` 的 catalog 实体页面、`facebook/docusaurus` 的结构化文档组织和 `great-expectations/great_expectations` 的规则说明；只生成轻量知识卡，不做复杂知识图谱。
- 落地产物：为字段标准生成 AI 可读知识卡；支持前端查看、CLI 导出和 MCP resource 读取；从示例反例、规则命中和用户反馈中补充卡片内容。
- 验收标准：AI 针对单个字段能拿到完整、短小、可引用的标准说明；卡片能说明何时使用、何时不要用、如何命名和关联哪些字段；变更后有版本或更新时间。
- 边界：不把知识卡当审批文档，不要求每个字段一次性补齐长文案；第一版优先覆盖高频字段和敏感字段。

### P6-162：规则/标准 A/B 评测与回归数据集
- 状态：待办。
- 为什么做：优化规则、推荐权重或 AI prompt 后，需要知道新版本是否真的更好；否则容易靠感觉改标准，导致误报、漏报或 AI 输出变差。
- 已有基础：已有 Prompt 模板评测、规则变异回归、核心 fixture/golden、字段推荐质量增强、AI 使用反馈和质量门禁待办。
- 缺口：缺少把标准版本 A/B、规则集 A/B、推荐结果 A/B 放到同一评测入口的机制，也缺少 precision/recall、falsePositive、acceptedSuggestionRate 和 regressionNotes。
- 参考项目：`promptfoo/promptfoo` 的批量评测、`stryker-mutator/stryker-js` 的回归召回和 `great-expectations/great_expectations` 的验证结果组织；只做本地可复现评测，不接外部实验平台。
- 落地产物：新增评测数据集格式和 CLI/API；支持对比两个规则或标准版本在 SQL fixture、推荐样例和 AI 输出样例上的结果差异；前端显示回归摘要。
- 验收标准：修改规则或标准前后能跑同一套样例并看到提升/退化；关键指标可进入证据包；失败样例能沉淀回 fixture。
- 边界：不做在线流量 A/B，不上传业务 SQL；第一版用本地脱敏样例和项目 fixture。

### P6-163：连接器能力探测与方言 Profile
- 状态：待办。
- 为什么做：不同数据库和驱动在 schema、注释、默认值、索引、枚举、保留字和分页语法上差异很大；AI 如果不知道当前连接器能力，就会生成不适配的 SQL 或导入计划。
- 已有基础：已有多方言 SQL/DDL 兼容矩阵、数据库连接诊断、数据源连接器注册、数据库直连反向导入、schema dump 和数据库元数据浏览待办。
- 缺口：缺少 per-connection capability profile，无法稳定记录 supportsComments、supportsEnums、identifierQuote、reservedWordsVersion、maxIdentifierLength、driverVersion 和 knownLimitations。
- 参考项目：`dbeaver/dbeaver` 的连接器能力抽象、`sqlalchemy/sqlalchemy` 的 dialect 分层和 `schemacrawler/SchemaCrawler` 的 metadata 抽取；只做只读探测，不执行破坏性 SQL。
- 落地产物：连接数据库后生成能力探测报告；AI Context、反向导入 plan 和 SQL 校验结果携带方言 profile；前端连接诊断页展示不支持项和替代建议。
- 验收标准：同一个任务在 MySQL/PostgreSQL 等连接下能获得不同方言提示；不支持的能力会提前警告；探测过程只读且不保存密码。
- 边界：不承诺覆盖所有数据库第一版，不自动安装驱动；优先 PostgreSQL/MySQL 和当前已支持路径。

### P6-164：个人安全红线配置中心
- 状态：待办。
- 为什么做：DataSpec 优先个人/小团队使用，但 AI Context、证据包、样例、连接信息和业务仓库路径仍可能踩到用户自己的安全红线；需要一个简单可见的本地策略入口。
- 已有基础：已有 API Token、安全基线、敏感信息脱敏、字段可见性等级、AI Context 注入防护、凭据复用、受控脱敏样例和本地运行诊断待办。
- 缺口：缺少统一 securityProfile，无法声明 neverExportPatterns、allowedAiTools、localOnlyPaths、samplePolicy、credentialPolicy 和 redactionStrictness，也无法让 doctor/CLI/MCP 统一检查。
- 参考项目：`gitleaks/gitleaks` 的 secret 检测、`getsops/sops` 的本地密文配置、`dotenvx/dotenvx` 的环境变量管理和 `microsoft/presidio` 的 PII 识别；只借鉴本地安全策略，不做企业权限平台。
- 落地产物：新增个人安全红线配置页和 `.dataspec/security.json` schema；导出、证据包、样例生成、CLI/MCP 调用前统一读取策略；doctor 输出策略缺失或冲突提示。
- 验收标准：用户能明确配置哪些内容永不导出、哪些 AI 工具可用、样例如何脱敏；违反红线时前端/CLI/MCP 给出可执行阻断或警告；策略本身不泄漏 secret。
- 边界：不做组织级审批，不扫描真实业务数据全量内容，不替代专业 DLP；第一版聚焦本机使用安全。

### P6-165：标准对象稳定标识与引用别名层
- 状态：待办。
- 为什么做：AI、CLI、MCP 和业务仓库长期引用字段标准时，如果只引用可变的字段名或显示名，字段重命名、合并或废弃后很容易引用漂移；需要一个稳定标识和别名解析层，让 AI 知道“这是同一个标准对象的历史名称”。
- 已有基础：已有标准快照、Schema Registry、字段生命周期、字段影响分析、变更日志、标准包 lockfile、标准消费端 SDK 和 AI 输出引用证据待办。
- 缺口：缺少 fieldStableId、aliasHistory、canonicalRef、deprecatedRefs 和 referenceResolutionResult；当前 Context、证据包、SDK 或业务仓库扫描结果难以统一判断旧引用是否仍有效。
- 参考项目：`datahub-project/datahub` 的实体 urn、`open-metadata/OpenMetadata` 的资产标识和 `bufbuild/buf` 的 breaking change 检查；只借鉴稳定引用和兼容检查，不做组织级元数据平台。
- 落地产物：为字段、枚举、规则、模板等标准对象定义稳定引用格式；导出 Context、SDK、证据包和问答结果时同时携带 stableRef 与当前 displayName；新增别名解析 API/CLI，支持把历史字段名解析到当前标准对象。
- 验收标准：字段重命名后，历史 SQL 检查记录、AI 证据包和业务仓库引用仍能解析到同一标准对象；废弃或合并的引用会给出替代建议；解析结果有契约测试覆盖。
- 边界：不强制重写历史记录，不把 stableRef 暴露成用户必须手填的字段；第一版优先覆盖字段和枚举。

### P6-166：AI 输出后置校验与幻觉引用拦截
- 状态：待办。
- 为什么做：即使 AI 能读取标准，它仍可能在 SQL、DDL、文档或修复说明里引用不存在的字段、过期枚举或错误规则；需要在 AI 产物生成后做一次确定性校验，避免“看起来引用了标准，实际引用错了”。
- 已有基础：已有 SQL lint、fixedSql、DDL 生成、AI 回放、AI 输出契约稳定性、引用证据与 Explain Trace、规则/标准 A/B 评测和执行证据包。
- 缺口：缺少 postGenerationCheck、unknownStandardRefs、staleRefs、unsupportedClaims 和 confidenceThreshold；AI 输出中的字段名、枚举值、规则说明和证据引用无法统一二次验证。
- 参考项目：`promptfoo/promptfoo` 的输出断言、`great-expectations/great_expectations` 的验证结果和 `Schemathesis/schemathesis` 的契约回归；只做本地确定性检查，不依赖外部 LLM 评判。
- 落地产物：新增 AI 产物校验 API/CLI/MCP；支持校验 SQL/DDL/Markdown/JSON 中的标准引用，输出 missingRefs、staleRefs、unsafeClaims、suggestedFixes 和 evidenceLinks；前端在复制或下载 AI 产物前显示校验摘要。
- 验收标准：AI 输出引用不存在字段时能被拦截或明确警告；过期字段能提示替代字段；校验结果可进入执行证据包和回放记录。
- 边界：不判断自然语言内容的全部事实正确性，不自动调用外部模型改写；第一版聚焦 DataSpec 可确定验证的字段、枚举、规则和快照引用。

### P6-167：标准查询 DSL 与可组合筛选协议
- 状态：待办。
- 为什么做：AI 经常需要按“订单域 + 金额字段 + 可用于建表 + 非敏感 + 最近验证”这类组合条件取标准；如果每次只能拼多个松散参数，Context 裁剪、字段检索、问答和 CLI 查询都会越来越难稳定复用。
- 已有基础：已有字段检索、字段分组、AI Context 按需裁剪、字段质量评分、字段可见性、业务术语表、本地语义检索和前端类型化 API Client 待办。
- 缺口：缺少统一 query expression，无法稳定表达 domain/tag/status/visibility/quality/source/updatedSince/hasExample/relatedTo 等条件，也缺少 explainable query plan 说明哪些条件命中或被降级。
- 参考项目：`sourcegraph/sourcegraph` 的搜索语法、`TanStack/table` 的筛选状态模型和 MCP resource 参数化协议；只借鉴查询表达与可解释筛选，不引入复杂全文搜索平台。
- 落地产物：定义轻量 StandardQuery DSL 与 JSON Schema；字段检索、AI Context、CLI/MCP 和前端筛选逐步接入；输出 querySummary、appliedFilters、ignoredFilters、resultCount 和 nextQueryHints。
- 验收标准：AI 可用同一条查询在 API、CLI 和 MCP 中获取一致字段集合；无效条件会给出结构化错误；查询语义有 fixture 防漂移。
- 边界：不做任意 SQL 查询，不允许 DSL 绕过项目边界或安全红线；第一版只覆盖标准对象元数据筛选。

### P6-168：MCP Resource 游标分页与大字段库分片导出
- 状态：待办。
- 为什么做：字段库、规则、模板、证据和历史记录变大后，一次性通过 MCP resource 或 Context 包返回全部内容会浪费上下文，也可能超过客户端限制；AI 需要按游标分片读取并知道下一片是否必要。
- 已有基础：已有 AI Context 裁剪、字段分页、性能基线、大库扫描计划、元数据增量缓存、MCP/CLI 工作流模板和 MCP/CLI 兼容握手待办。
- 缺口：MCP resources 缺少统一 cursor、pageSize、chunkHash、hasMore、resumeToken 和 compactSummary；AI 无法可靠分批读取 field-catalog、rules、evidence、records 等大资源。
- 参考项目：Model Context Protocol resources 设计、`TanStack/query` 的分页缓存和 `OpenLineage/OpenLineage` 的事件分片结构；只借鉴分页与恢复，不引入远程缓存服务。
- 落地产物：为大 MCP resource 和 CLI 导出定义分页契约；支持 first/next/summary 三类读取模式；Context 包 manifest 记录分片 hash；doctor 可提示客户端是否支持分页能力。
- 验收标准：上千字段项目可通过 MCP 分页读取完整标准，不超出单次上下文预算；中断后可用 resumeToken 继续；分片内容与完整导出 hash 可校验一致。
- 边界：不替代完整 zip 导出，不为所有小资源强制分页；第一版优先 field-catalog、rule catalog 和 evidence summary。

### P6-169：前端操作录制与可复现脚本导出
- 状态：待办。
- 为什么做：前端页面和 AI browser automation 越来越多，问题复现常常依赖“我刚才点了哪些筛选、切了哪个项目、复制了什么 SQL”；需要把用户操作和页面状态转成可脱敏的复现脚本，方便 AI 或本地 E2E 重放。
- 已有基础：已有前端 URL 状态、统一数据状态、命令面板、浏览器级 E2E、前端反馈转任务、执行证据包和 Playwright 失败截图待办。
- 缺口：缺少 userActionTrace、routeState、selectedProject、apiCalls、redactedInputs 和 replayScript；错误反馈只能描述现象，不能稳定生成可运行的 Playwright/手工复现步骤。
- 参考项目：`microsoft/playwright` 的 trace 与 codegen、`getsentry/sentry-javascript` 的 breadcrumb 和 `github/gh` 的 issue 模板；只借鉴本地复现信息采集，不上传远程平台。
- 落地产物：新增前端可选“录制复现”模式；记录路由、关键按钮、表单状态、接口摘要和错误 traceId；导出 Markdown、JSON 和 Playwright 草稿脚本，自动脱敏 token、密码和连接串。
- 验收标准：SQL 校验、反向导入、字段库筛选等核心流程出错时，可导出一份 AI 能复现的脚本；导出内容不含敏感值；脚本至少能作为本地 Playwright 骨架运行或转成任务描述。
- 边界：不默认持续录制用户行为，不录制真实业务数据行，不替代完整 E2E 测试套件；第一版由用户显式开启。

### P6-170：标准维护工作量估算与任务批量拆分
- 状态：待办。
- 为什么做：DataSpec 已能发现低质量字段、候选、导入差异、规则冲突和 AI 失败记录，但用户还需要知道“先做哪 20 分钟最值”；AI 也需要把一堆标准维护问题拆成可执行小任务，而不是一次性尝试全修。
- 已有基础：已有字段质量评分、覆盖率报告、候选 Inbox、个人健康摘要、AI 任务推荐队列、TODO 到 OpenSpec 交接、执行证据包和统一任务结果协议待办。
- 缺口：缺少 workEstimate、batchPlan、taskSlices、expectedImpact、riskLevel 和 verificationCommands；健康摘要能看到问题，但还不能按投入产出拆成可执行批次。
- 参考项目：GitHub Actions job summary、`backstage/backstage` 的开发者任务入口和 `dagster-io/dagster` 的资产任务视图；只借鉴任务摘要和优先级表达，不做团队排期系统。
- 落地产物：新增标准维护任务拆分 API/CLI；把低质量字段、导入候选、规则冲突和失败 SQL 聚合成 15/30/60 分钟任务包；每个任务包包含目标对象、预计收益、验证命令和回滚/跳过说明。
- 验收标准：打开项目后能生成“本次最值得处理的 3 个维护批次”；AI 可按批次逐步执行并产出 TaskResult；完成后健康摘要和任务建议会更新。
- 边界：不做团队工时估算，不自动修改标准，不把估算当承诺；第一版按本地项目指标给出启发式建议。

### P6-171：标准规则向数据质量测试导出
- 状态：待办。
- 为什么做：字段标准、枚举、必填规则和命名规则已经能指导建表，但落到业务仓库后仍需要可执行的数据质量测试；AI 需要把 DataSpec 标准转成 dbt、Great Expectations 或 SQL 断言，而不是只生成说明文档。
- 已有基础：已有规则配置、字段覆盖率、DDL 生成、执行证据包、标准消费端 SDK 待办和业务仓库迁移交付包待办。
- 缺口：缺少 dataQualityTestSpec、testTarget、expectationSuite、dbtSchemaYaml、sqlAssertion 和 verificationResult；标准规则无法直接导出为可落地的测试工件。
- 参考项目：`dbt-labs/dbt-core` 的 `schema.yml` tests、`great-expectations/great_expectations` 的 expectation suite 和 `TobikoData/sqlmesh` 的模型质量校验；只借鉴测试工件表达，不引入完整数据平台。
- 落地产物：新增标准到数据质量测试的 API/CLI 导出；支持按项目、表、字段、规则生成 dbt tests、Great Expectations suite 和只读 SQL assertion 模板；交付包可附带验证命令和风险说明。
- 验收标准：选择一个项目后能导出结构稳定的测试包；枚举值、非空、字段格式、命名和敏感字段规则能转成可执行或可人工确认的断言；导出内容不包含业务数据行或数据库密码。
- 边界：第一版不连接生产库跑全量数据质量扫描，不承诺覆盖所有 dbt/GE 高级能力；先做确定性规则到测试工件的转换。

### P6-172：标准变更事件流与本地 Webhook
- 状态：待办。
- 为什么做：AI、CLI、MCP 和业务仓库希望在标准变更后自动更新 Context、刷新缓存或提示重新校验；当前只能轮询活动时间线或手动导出，缺少稳定事件入口。
- 已有基础：已有项目活动时间线、变更日志、标准快照、执行证据包、Trace ID、幂等写保护和任务状态机待办。
- 缺口：缺少 dataspecEvent、eventType、subjectRef、projectId、snapshotHash、redactedPayload、deliveryStatus 和 replayCursor；AI 无法订阅“字段标准已变更”“规则已更新”“Starter Kit 已应用”等事件。
- 参考项目：`cloudevents/spec` 的事件信封、`OpenLineage/OpenLineage` 的运行事件和 GitHub webhook 的本地回调模式；只借鉴事件格式与回放，不做远程消息队列。
- 落地产物：定义 `dataspec.event.v1` 事件模型；新增只读事件列表 API、CLI `events tail/replay` 和 `.dataspec/events.json` 本地 webhook 配置；事件 payload 默认脱敏并关联标准快照。
- 验收标准：字段、规则、反向导入、Starter Kit、Context 导出等关键动作产生可查询事件；AI 可按 cursor 增量读取；本地 webhook 失败有重试摘要但不阻断主流程。
- 边界：不引入 Kafka/RabbitMQ 等外部依赖，不做团队通知系统；第一版面向本机自动化和 AI agent 消费。

### P6-173：编辑器提示与 Code Action 轻量包
- 状态：待办。
- 为什么做：很多字段标准问题在开发者写 SQL、DDL、实体类或迁移脚本时就能发现；只靠前端页面或 CLI 事后检查，AI 和人类都容易等到 PR 阶段才修。
- 已有基础：已有 CLI lint、pre-commit/IDE 保存前检查待办、前端类型化 API Client、字段检索 API、标准消费端 SDK 和 `.dataspec/config.json` Schema 待办。
- 缺口：缺少 editorDiagnostics、completionItems、codeActions、hoverDocs 和 localCache；编辑器无法直接提示标准字段、推荐替换、字段说明和修复命令。
- 参考项目：`microsoft/vscode-extension-samples` 的扩展示例、`sourcegraph/sourcegraph` 的代码索引体验和 `redhat-developer/vscode-java` 的诊断/Code Action 形态；只借鉴编辑器交互，不做完整 IDE 平台。
- 落地产物：新增 VS Code 轻量模板或插件骨架，复用 CLI/API 输出 diagnostics、completion 和 code action；支持从 `.dataspec/config.json` 读取项目与 token；提供本地缓存和脱敏日志。
- 验收标准：在 SQL/DDL 文件中能看到字段命名、枚举、敏感字段和推荐标准字段提示；一键复制或执行修复命令不泄漏 token；插件骨架有最小冒烟测试或示例工作区。
- 边界：第一版不发布 Marketplace，不强制绑定 VS Code；其他 IDE 先通过 CLI JSON/Problem Matcher 复用。

### P6-174：跨协议 Schema 导出与标准适配层
- 状态：待办。
- 为什么做：字段标准最终会被 API、事件、离线数据和消息系统消费；如果只能导出 DataSpec 自身 JSON，AI 在生成 OpenAPI、Protobuf、Avro 或 JSON Schema 时仍要猜类型与约束映射。
- 已有基础：已有 Schema Registry、字段标准向 API/DTO Schema 导出待办、标准消费端 SDK、类型常量包、OpenAPI 契约和字段可见性策略待办。
- 缺口：缺少 schemaTarget、protocolMapping、typeMapping、constraintMapping、compatibilityNotes 和 unsupportedFeatures；不同协议之间的类型、枚举、必填和注释映射不可验证。
- 参考项目：`protocolbuffers/protobuf`、`apache/avro`、`confluentinc/schema-registry`、`json-schema-org/json-schema-spec` 和 `bufbuild/buf` 的兼容检查；只借鉴协议映射与兼容提示，不做全量代码生成平台。
- 落地产物：新增标准对象到 JSON Schema、OpenAPI schema fragment、Protobuf message 草稿和 Avro schema 的导出 API/CLI；输出 mapping report、兼容风险和不可表达约束列表。
- 验收标准：同一字段标准可导出多个协议的结构化草稿；枚举、必填、敏感标记、格式和注释能被保留或明确标记为降级；导出结果有 fixture 快照测试。
- 边界：第一版不自动改业务仓库代码，不保证复杂协议特性完全等价；以“AI 可读、人工可审”的草稿和映射报告为主。

### P6-175：指标口径与字段标准映射层
- 状态：待办。
- 为什么做：AI 生成报表 SQL 或数据产品说明时，经常把字段名、指标名和业务口径混在一起；需要把“订单金额”“支付成功率”“活跃用户数”这类指标口径映射到标准字段、过滤条件和聚合规则。
- 已有基础：已有派生字段、单位换算与口径规则待办、业务对象关系图、字段知识卡、业务术语表、标准问答入口和数据模型契约待办。
- 缺口：缺少 metricDefinition、measureFields、dimensionFields、filterRule、aggregationRule、timeGrain 和 ownerNotes；AI 无法区分字段标准和指标口径，也无法解释查询结果的业务边界。
- 参考项目：`dbt-labs/metricflow` 的语义指标模型、`cube-js/cube` 的 metrics layer 和 `datahub-project/datahub` 的 glossary/metric 元数据；只借鉴指标口径表达，不做 BI 平台。
- 落地产物：新增轻量指标口径模型/API/前端维护入口；支持把指标关联到标准字段、枚举过滤、时间粒度、聚合方式和示例 SQL；AI Context 和标准问答可按需导出指标摘要。
- 验收标准：AI 查询“订单金额口径”或生成报表 SQL 时能引用明确 metricDefinition；口径变更可追溯到快照和决策理由；典型金额、数量、转化率指标有 fixture 覆盖。
- 边界：不接管真实指标计算平台，不自动校验数据结果正确性；第一版只沉淀口径元数据和可解释查询建议。

### P6-176：标准消费端兼容验收套件
- 状态：待办。
- 为什么做：DataSpec 会被前端、CLI、MCP、SDK、业务仓库和 AI 工具共同消费；仅后端测试通过不代表消费端解析稳定，需要一套可复用的兼容样例，防止字段改名或契约变更悄悄破坏外部使用。
- 已有基础：已有 OpenAPI 防漂移、CLI/MCP 工具契约验收、示例契约快照、标准消费清单、标准消费端 SDK 和前端冒烟门禁。
- 缺口：缺少 consumerCompatibilitySuite、goldenPayloads、minimumSupportedVersion、breakingChangeRules 和 adapterResults；每个消费端只能靠各自测试发现问题。
- 参考项目：`Schemathesis/schemathesis` 的契约回归、`OpenAPITools/openapi-generator` 的生成兼容策略、`bufbuild/buf` 的 breaking change 检查和 `Redocly/redocly-cli` 的 OpenAPI lint；只借鉴兼容验收，不做公开认证体系。
- 落地产物：新增标准消费端兼容套件目录和 CLI `compat check`；内置字段、规则、枚举、Context、MCP resource、CLI JSON、schema export 的 golden payload；输出 breaking/compatible/deprecated 结果。
- 验收标准：修改核心 DTO、API 字段或导出格式时能一键检查主要消费端契约；失败结果包含破坏字段、影响入口和迁移建议；CI/本地验证入口可复用。
- 边界：不要求所有第三方工具接入，不阻止个人本地实验性变更；第一版覆盖 DataSpec 自有消费端和示例 adapter。

### P6-177：OpenSpec Change 准备度评分与缺口检查
- 状态：待办。
- 为什么做：TODO 到 OpenSpec 草稿生成后，真正开工前还需要判断 proposal、design、spec、tasks 是否已经足够明确；如果缺影响范围、验证命令或边界，AI 很容易边做边猜。
- 已有基础：已有 TODO 到 OpenSpec 交接助手、OpenSpec validate、验证命令推荐工具、P6 里程碑收束待办和多次归档经验。
- 缺口：缺少 readinessScore、missingFacts、affectedSpecs、validationPlan、reviewBoundary、riskFlags 和 humanQuestions；当前只能看格式是否通过，不能看需求是否可实施。
- 参考项目：`stoplightio/spectral` 的规则化 lint、`open-policy-agent/conftest` 的策略校验和 `Redocly/redocly-cli` 的契约检查；只借鉴可配置规则与诊断输出，不引入复杂治理流程。
- 落地产物：新增 OpenSpec change 准备度检查 CLI/脚本；扫描 proposal/design/spec/tasks，输出评分、缺口、建议验证命令、可能影响的能力和需要用户确认的问题。
- 验收标准：对一个待实施 change 能输出可读报告和 JSON；缺少验收标准、边界、影响规格或验证命令时给出明确诊断；通过准备度检查不自动实现、不自动归档。
- 边界：不替代人工判断，不把所有低分 change 阻塞掉；第一版作为本地开工前提示和 AI 自检入口。

### P6-178：MCP 会话状态与当前项目记忆
- 状态：待办。
- 为什么做：AI 通过 MCP 使用 DataSpec 时，经常需要重复确认 currentProjectId、标准快照、最近导出范围、上一轮任务结果和下一步建议；缺少会话状态会增加上下文浪费和误操作概率。
- 已有基础：已有 MCP/CLI 工作流模板、AI 会话启动包、AI 能力清单、AI 任务状态机、统一任务结果协议和敏感信息脱敏边界。
- 缺口：缺少 sessionState、currentProject、currentSnapshot、lastTaskResult、toolCursor、safeDefaults 和 redactedMemory；MCP resources/tools 之间无法稳定共享“当前项目上下文”。
- 参考项目：`modelcontextprotocol/servers` 的 resource/tool 组织、`langchain-ai/langgraph` 的状态化 agent 流程和 `temporalio/temporal` 的可恢复任务状态；只借鉴状态模型，不接入远程编排服务。
- 落地产物：新增本地会话状态文件或服务端轻量 session API；MCP 暴露当前项目、标准快照、最近任务结果和可恢复动作；所有状态默认脱敏且可清理。
- 验收标准：AI 第一次选择项目后，后续 MCP 调用能读取同一项目上下文；切换项目有明确记录和确认边界；状态文件不包含 token、密码、JDBC URL 或业务数据行。
- 边界：不做云端长期记忆，不跨用户同步，不把会话状态当权限依据。

### P6-179：标准字段到业务代码 Patch Plan
- 状态：待办。
- 为什么做：字段标准变更后，仅知道影响哪些文件还不够，AI 需要一个可审查的 Patch Plan 来判断哪些实体、DTO、SQL、迁移脚本和测试可能要改。
- 已有基础：已有业务代码字段引用索引、标准变更迁移 Recipe、编辑器提示、业务仓库合规分、字段影响分析和 fixedSql 文件补丁待办。
- 缺口：缺少 patchPlan、candidateEdit、fileRef、riskLevel、dryRunDiff、manualStep 和 rollbackHint；当前还不能把标准变化转成可审查的代码修改计划。
- 参考项目：`openrewrite/rewrite` 的迁移 recipe、`codemod-com/codemod` 的代码修改计划和 `ast-grep/ast-grep` 的结构化匹配；只借鉴 patch planning，不默认改写业务仓库。
- 落地产物：新增字段变更到业务代码 Patch Plan 的 CLI/API；基于引用索引和规则生成候选修改、风险等级、验证命令和人工确认点；可导出 Markdown/JSON。
- 验收标准：字段重命名、类型变化或枚举变化时能列出候选文件和建议修改；默认 dry-run，不写业务文件；AI 可据此逐项确认并生成后续 OpenSpec 或代码任务。
- 边界：不自动应用补丁，不保证识别所有动态 SQL；第一版聚焦 Java/SQL/JSON 等项目已有高频文件类型。

### P6-180：数据库直连采集作业断点续扫与限速保护
- 状态：待办。
- 为什么做：大库反向导入、覆盖率和元数据浏览会遇到表多、网络慢、权限不一致或连接中断；一次性拉取失败后重来，会浪费时间也增加源库压力。
- 已有基础：已有数据库 schema dump、连接健康探测、大库扫描计划、元数据增量缓存、连接器能力探测和数据库直连只读安全诊断。
- 缺口：缺少 scanJobId、resumeCursor、pageSize、rateLimit、partialResult、cancelToken、retryPolicy 和 sourcePressureHint；当前更多是同步式操作和局部分页。
- 参考项目：`airbytehq/airbyte` 的连接器同步状态、`dagster-io/dagster` 的作业运行视图和 `singer-io/getting-started` 的 tap/state 思路；只借鉴断点与状态，不做后台数据同步平台。
- 落地产物：新增数据库 metadata 采集作业模型/API/前端进度视图；支持分页扫描、取消、恢复、限速、失败摘要和只读证据包。
- 验收标准：上千张表的元数据扫描可分批完成；中断后能从 cursor 恢复；取消或失败不会写入部分标准字段；所有连接信息继续遵守脱敏和最小权限边界。
- 边界：不扫描业务数据行，不做定时同步，不绕过源库权限；第一版只服务反向导入、覆盖率和元数据浏览。

### P6-181：标准维护 Inbox 到可执行工作流
- 状态：待办。
- 为什么做：覆盖率、字段质量、候选 Inbox、规则冲突和 AI 反馈已经能产生很多“该处理的事”，但用户和 AI 还需要把这些事项一键转成可执行 recipe，而不是在多个页面间手工拼步骤。
- 已有基础：已有标准候选 Inbox、AI 任务推荐队列、可复用 AI 工作流 Recipe、标准维护工作量估算、统一任务结果协议和前端命令面板。
- 缺口：缺少 inboxAction、recipeBinding、dryRunSteps、executionState、undoHint 和 evidenceLinks；待处理项能看见，但还不能稳定转成“预检 -> 执行 -> 验证 -> 归档”的闭环。
- 参考项目：`backstage/backstage` 的开发者任务入口、`go-task/task` 的任务 recipe 和 `dagster-io/dagster` 的资产任务视图；只借鉴可执行步骤表达，不做团队排期系统。
- 落地产物：新增 Inbox action 到 workflow recipe 的绑定层；前端可从候选、质量问题或覆盖率缺口发起 dry-run；CLI/MCP 可读取同一任务步骤和证据链接。
- 验收标准：选择一个未纳管字段批次后，可生成可执行工作流，包含预检、采纳/忽略/补资料、验证命令和结果记录；失败步骤能显示可恢复位置。
- 边界：不自动批量采纳标准，不跳过人工确认；第一版只绑定高频维护动作。

### P6-182：前端页面对象模型与稳定测试选择器
- 状态：待办。
- 为什么做：前端页面越来越多，若 E2E 只靠文本和 CSS 选择器，页面微调会导致测试脆弱；AI browser automation 也需要稳定的页面对象和操作语义。
- 已有基础：已有前端源码级 smoke、浏览器级 E2E、端到端上手引导、前端操作录制、组件状态样例库和可访问性基线待办。
- 缺口：缺少 pageObject、dataTestIdPolicy、stableSelectors、testFixtures、routeHarness 和 aiActionNames；测试和 AI 自动化难以复用同一套页面动作。
- 参考项目：`microsoft/playwright` 的 Page Object Model、`testing-library/testing-library-docs` 的用户语义选择器和 `cypress-io/cypress` 的端到端测试组织；只借鉴测试结构，不重写前端框架。
- 落地产物：新增前端页面对象目录、稳定选择器约定和核心页面 fixture；覆盖项目选择、字段库、SQL 校验、反向导入、AI Context 等高频页面。
- 验收标准：E2E 用例能通过页面对象完成核心流程；页面文案调整不破坏选择器；AI 自动化脚本能复用 page object 输出的动作名称和失败截图。
- 边界：不做全量视觉回归，不要求所有组件立刻补选择器；第一版先覆盖主路径和高频故障页。

### P6-183：标准字段到数据库 COMMENT 回写计划
- 状态：待办。
- 为什么做：反向导入能把现有数据库补进 DataSpec，但反过来，当字段标准被修正后，源数据库的表注释和列注释也可能长期落后；AI 需要一份可审阅的 COMMENT 回写计划，而不是直接生成不可控的修改 SQL。
- 已有基础：已有数据库直连 metadata、schema dump、二次比对、字段来源批次、schema plan 预览、DDL 生成和 SQL/DDL 验证沙箱待办。
- 缺口：缺少 commentPatchPlan、currentComment、targetComment、commentDiff、dryRunSql、dialectSupport、riskLevel 和 rollbackHint；目前只能导入注释，不能稳定输出“标准 -> 数据库注释”的只读预览。
- 参考项目：`bytebase/bytebase` 的数据库变更预览、`ariga/atlas` 的 schema diff 和 `k1LoW/tbls` 的数据库文档化；只借鉴注释差异表达，不默认写源数据库。
- 落地产物：新增 COMMENT 回写计划 API/CLI/前端预览；按 PostgreSQL/MySQL 方言生成 COMMENT ON 或 ALTER COMMENT 草稿，标记 no-op、missing、changed 和 unsupported；支持导出 SQL 与 JSON 证据。
- 验收标准：连接数据库后能看到 DataSpec 标准注释与当前库注释差异；默认只 dry-run，不执行数据库写入；输出内容不包含密码、token、完整 JDBC URL 或业务数据行。
- 边界：第一版只处理表/列注释，不处理表重命名、字段重命名、索引调整或数据迁移；真实执行仍交给用户或后续显式 apply 流程。

### P6-184：字段标准中英文命名与翻译辅助
- 状态：待办。
- 为什么做：项目里常见“用户/会员/account/user”“手机号/mobile/phone”等中英文混用；如果没有结构化命名映射，AI 生成字段、搜索标准或解释差异时容易把翻译偏好当成猜测。
- 已有基础：已有字段 displayName/name、aliases、业务术语表、字段检索、字段推荐、数据字典、AI Context 和 Prompt 生成。
- 缺口：缺少 localizedNames、preferredEnglishName、forbiddenTranslations、translationAliases、translationConfidence 和 exampleMappings；当前别名能粗略匹配，但不能表达“推荐翻译”和“不要这样翻译”。
- 参考项目：`open-metadata/OpenMetadata` 的 glossary/term 管理、`datahub-project/datahub` 的元数据标签和 `i18next/i18next` 的 key/value 资源组织；只借鉴命名映射，不做完整 UI 国际化。
- 落地产物：扩展字段或术语模型，支持中文名、英文推荐名、禁用翻译、别名和示例映射；字段推荐、标准搜索、AI Context 和数据字典导出可返回 translationReason。
- 验收标准：AI 查询“会员手机号”或生成用户表字段时，能明确知道推荐字段名、可接受别名和禁用翻译；冲突翻译能在术语表或字段库中被提示。
- 边界：不接入外部机器翻译服务，不自动覆盖已有字段名；第一版只维护项目级命名映射和 AI 可读说明。

### P6-185：标准驱动测试数据与边界用例包
- 状态：待办。
- 为什么做：AI 在业务仓库写单测、mock、seed 或示例 SQL 时，需要安全、符合标准的样例值，也需要典型无效值验证规则；不能从真实业务数据里复制样例。
- 已有基础：已有字段格式约束、示例/反例库、受控脱敏样例采样、前端 mock 演示模式、数据质量测试导出和 fixture/golden 基线。
- 缺口：缺少 testDataCase、validExamples、invalidExamples、boundaryExamples、seedProfile、mockPayload 和 coverageReport；现有样例分散在字段描述、fixture 和手写文档里。
- 参考项目：`faker-js/faker` 的合成数据生成、`mswjs/msw` 的前端 mock、`storybookjs/storybook` 的状态样例和 `great-expectations/great_expectations` 的期望/反例组织；只生成安全样例，不采集真实数据行。
- 落地产物：新增测试数据包生成 API/CLI；按字段标准、枚举、格式、敏感标记和业务对象生成 JSON/CSV/SQL seed/mock 草稿，同时输出有效、无效和边界样例。
- 验收标准：手机号、金额、时间、枚举、JSON 等字段能生成可解释样例；导出包可被前端 mock、后端测试或 AI 生成单测复用；生成结果不包含原始业务数据。
- 边界：不自动写入业务数据库，不保证满足所有业务规则；第一版聚焦字段级和轻量对象级样例。

### P6-186：AI Context 质量预算与可用性评分
- 状态：待办。
- 为什么做：AI Context 已能裁剪和导出，但 AI 还需要知道“这份上下文是否够用”：字段是否缺枚举、样例是否太少、规则是否被截断、token 预算是否被低价值内容占满。
- 已有基础：已有 AI Context zip、按需裁剪、上下文预算、字段质量评分、标准查询 DSL、AI profile、Context 增量更新包和 Prompt 评测待办。
- 缺口：缺少 contextQualityScore、tokenBudgetBreakdown、missingCriticalFields、truncatedResources、coverageByCategory、staleContextWarnings 和 nextContextActions；现在只能看导出是否成功，不能判断是否适合某个 AI 任务。
- 参考项目：`promptfoo/promptfoo` 的 prompt 评测、`langfuse/langfuse` 的 trace/score 和 `OpenLineage/OpenLineage` 的运行元数据；只做确定性评分，不调用外部 LLM 打分。
- 落地产物：新增 Context 质量评分 API/CLI；导出前后输出质量摘要、token 分布、缺口、截断说明和推荐补充动作；前端 AI Context 页面展示评分与可复制 JSON。
- 验收标准：最小 Context、标准 Context 和完整 Context 能分别给出可解释评分；缺少枚举、格式、业务术语或关键规则时有明确 nextAction；AI 可据此决定继续、补导出或停止。
- 边界：评分不替代真实任务结果，不保证 AI 一定生成正确；第一版只基于 DataSpec 元数据和导出内容做静态评估。

### P6-187：字段使用契约与禁用场景说明
- 状态：待办。
- 为什么做：同一个字段“是什么”不等于“什么时候该用”；AI 生成 SQL/DDL 时常会混用统计口径、展示字段、内部状态或废弃字段，需要字段级 usage contract 明确推荐场景和禁用场景。
- 已有基础：已有字段状态、敏感标记、字段格式约束、派生字段/单位规则、指标口径映射、字段知识卡、业务对象关系图和标准问答入口。
- 缺口：缺少 preferredUseCases、avoidWhen、joinHints、defaultFilters、aggregationHints、replacementGuidance 和 misuseExamples；当前 AI 只能从描述和标签推断使用边界。
- 参考项目：`dbt-labs/dbt-core` 的模型文档、`OpenLineage/OpenLineage` 的上下游语义和 `open-metadata/OpenMetadata` 的资产说明；只借鉴使用说明结构，不建设重型血缘平台。
- 落地产物：扩展字段标准或新增轻量使用契约模型；字段详情、AI Context、DDL/Prompt 生成、标准问答和字段推荐可读取使用建议、禁用场景和常见误用。
- 验收标准：AI 查询“订单金额应该用哪个字段统计”时能看到单位、聚合、过滤和禁用提示；废弃或展示专用字段不会被推荐为写入字段；误用样例可进入规则或问答提示。
- 边界：不做完整指标平台，不要求每个字段都补齐契约；第一版优先高风险金额、状态、时间、用户和敏感字段。

### P6-188：标准问答答案可采纳度与低置信处理
- 状态：待办。
- 为什么做：标准问答能让 AI 或用户快速问“字段叫什么”，但答案如果证据不足、标准冲突或命中候选字段，应该明确低置信，而不是给出一个看似确定的字段名。
- 已有基础：已有字段检索、标准问答入口、业务术语表、标准证据置信度、AI 输出引用证据、候选 Inbox、字段质量评分和标准查询 DSL 待办。
- 缺口：缺少 answerability、confidenceReason、missingEvidence、candidateOnly、conflictingStandards、suggestedNextQuery 和 escalateToInbox；问答结果还不能稳定表达“可直接采用/需要确认/不能回答”。
- 参考项目：`sourcegraph/sourcegraph` 的搜索解释、`promptfoo/promptfoo` 的评测断言和 `langfuse/langfuse` 的评分记录；只借鉴答案评分与证据展示，不接入在线问答模型。
- 落地产物：为标准问答和字段搜索增加可采纳度摘要；输出 answerStatus、confidence、evidenceRefs、missingFacts、conflicts 和 nextActions；前端展示低置信提示，CLI/MCP 可机器读取。
- 验收标准：同义词冲突、候选未采纳、字段缺格式、低质量字段或无命中时，问答不会伪装成确定答案；AI 能根据 answerStatus 决定采用、追问、转候选或停止。
- 边界：不实现通用自然语言问答引擎，不调用外部 LLM；第一版基于现有检索、术语表、证据和质量分确定性判断。

## 参考项目索引

- [`sqlfluff/sqlfluff`](https://github.com/sqlfluff/sqlfluff)：模块化、可配置、多方言 SQL linter。
- [`darold/pgFormatter`](https://github.com/darold/pgFormatter)：PostgreSQL SQL 格式化与风格配置参考。
- [`sql-formatter-org/sql-formatter`](https://github.com/sql-formatter-org/sql-formatter)：多方言 SQL 格式化器参考。
- [`eslint/eslint`](https://github.com/eslint/eslint)：可插拔规则、fixture 测试和规则元数据设计参考。
- [`istanbuljs/nyc`](https://github.com/istanbuljs/nyc)：覆盖率报告和阈值展示参考。
- [`ariga/atlas`](https://github.com/ariga/atlas)：schema-as-code、schema lint 和迁移规划。
- [`ariga/atlas-action`](https://github.com/ariga/atlas-action)：数据库 schema 变更的 GitHub Actions lint 入口。
- [`liquibase/liquibase`](https://github.com/liquibase/liquibase)：数据库变更日志、schema contract 和迁移可追溯性参考。
- [`bytebase/bytebase`](https://github.com/bytebase/bytebase)：数据库 DevOps 工作台、SQL Review、数据库 CI/CD。
- [`bytebase/example-gitops-github-flow`](https://github.com/bytebase/example-gitops-github-flow)：Bytebase + GitHub Flow 数据库发布示例。
- [`k1LoW/tbls`](https://github.com/k1Low/tbls)：CI-friendly 数据库文档生成工具。
- [`dbt-labs/dbt-core`](https://github.com/dbt-labs/dbt-core)：项目化数据模型、文档和可复现构建的参考。
- [`great-expectations/great_expectations`](https://github.com/great-expectations/great_expectations)：数据质量规则、验证结果和文档化体验参考。
- [`datahub-project/datahub`](https://github.com/datahub-project/datahub)：数据目录、字段影响分析和元数据关系参考。
- [`open-metadata/OpenMetadata`](https://github.com/open-metadata/OpenMetadata)：元数据采集、数据质量和资产视图参考。
- [`i18next/i18next`](https://github.com/i18next/i18next)：多语言资源、key/value 命名和翻译映射组织参考。
- [`schemacrawler/SchemaCrawler`](https://github.com/schemacrawler/SchemaCrawler)：数据库 metadata 抽取、schema 快照和文档化参考。
- [`prisma/prisma`](https://github.com/prisma/prisma)：schema introspection、开发期数据库工具和本地工作流参考。
- [`pnpm/pnpm`](https://github.com/pnpm/pnpm)：lockfile、依赖解析和可复现安装参考。
- [`rust-lang/cargo`](https://github.com/rust-lang/cargo)：`Cargo.lock` 与依赖固定策略参考。
- [`promptfoo/promptfoo`](https://github.com/promptfoo/promptfoo)：prompt 输出评测、回归样例和批量评估参考。
- [`langfuse/langfuse`](https://github.com/langfuse/langfuse)：AI trace、prompt 版本和生成任务观测参考。
- [`temporalio/temporal`](https://github.com/temporalio/temporal)：工作流状态、重试、断点恢复和任务可观测性参考。
- [`dagster-io/dagster`](https://github.com/dagster-io/dagster)：数据资产任务、运行记录和本地任务编排体验参考。
- [`airbytehq/airbyte`](https://github.com/airbytehq/airbyte)：数据源连接器同步状态、增量采集和失败恢复参考。
- [`singer-io/getting-started`](https://github.com/singer-io/getting-started)：tap/state 风格的轻量增量采集协议参考。
- [`OpenLineage/OpenLineage`](https://github.com/OpenLineage/OpenLineage)：作业运行、输入输出和血缘事件模型参考，可借鉴执行证据包结构。
- [`OpenAPITools/openapi-generator`](https://github.com/OpenAPITools/openapi-generator)：契约优先、代码生成和版本兼容策略参考。
- [`bufbuild/buf`](https://github.com/bufbuild/buf)：Protobuf schema lint、breaking change 检查和契约管理参考。
- [`glideapps/quicktype`](https://github.com/glideapps/quicktype)：从 JSON/Schema 推导类型与结构的契约反向提取参考。
- [`protocolbuffers/protobuf`](https://github.com/protocolbuffers/protobuf)：Protobuf 协议、消息 schema 和兼容性约束参考。
- [`apache/avro`](https://github.com/apache/avro)：Avro schema、数据契约和跨语言序列化参考。
- [`confluentinc/schema-registry`](https://github.com/confluentinc/schema-registry)：多协议 schema 注册、兼容检查和版本化参考。
- [`json-schema-org/json-schema-spec`](https://github.com/json-schema-org/json-schema-spec)：JSON Schema 规范与约束表达参考。
- [`cloudevents/spec`](https://github.com/cloudevents/spec)：事件信封、事件类型和跨系统事件元数据参考。
- [`dbt-labs/metricflow`](https://github.com/dbt-labs/metricflow)：语义层指标定义、聚合口径和维度建模参考。
- [`cube-js/cube`](https://github.com/cube-js/cube)：metrics layer、语义查询和指标 API 组织参考。
- [`backstage/backstage`](https://github.com/backstage/backstage)：项目模板、开发者入口和脚手架体验参考。
- [`renovatebot/renovate`](https://github.com/renovatebot/renovate)：依赖过期检测、批量更新和迁移提示参考。
- [`dbeaver/dbeaver`](https://github.com/dbeaver/dbeaver)：数据库连接配置、metadata 浏览和多方言体验参考。
- [`reviewdog/reviewdog`](https://github.com/reviewdog/reviewdog)：基于 diff 的代码审查评论、诊断聚合和 PR 反馈参考。
- [`pre-commit/pre-commit`](https://github.com/pre-commit/pre-commit)：本地变更钩子、按文件质量门禁和轻量开发工作流参考。
- [`go-task/task`](https://github.com/go-task/task)：本地任务 recipe、跨平台命令编排和可复用工作流参考。
- [`casey/just`](https://github.com/casey/just)：轻量命令 recipe、参数化本地工作流和开发者命令入口参考。
- [`langchain-ai/langgraph`](https://github.com/langchain-ai/langgraph)：状态化 agent 工作流和可恢复步骤模型参考。
- [`hashicorp/terraform`](https://github.com/hashicorp/terraform)：plan/apply、状态记录和 dry-run 风格的写入前演练参考。
- [`openrewrite/rewrite`](https://github.com/openrewrite/rewrite)：代码迁移 recipe、批量修复 dry-run 和可审查补丁参考。
- [`codemod-com/codemod`](https://github.com/codemod-com/codemod)：代码修改计划、codemod 发布和可审查迁移体验参考。
- [`ast-grep/ast-grep`](https://github.com/ast-grep/ast-grep)：结构化代码匹配、按语言规则扫描和轻量代码改写参考。
- [`changesets/changesets`](https://github.com/changesets/changesets)：变更集、版本发布说明和迁移提示组织参考。
- [`sourcegraph/sourcegraph`](https://github.com/sourcegraph/sourcegraph)：代码引用检索、搜索索引和仓库级影响分析参考。
- [`Redocly/redocly-cli`](https://github.com/Redocly/redocly-cli)：OpenAPI lint、bundle 和契约治理参考。
- [`stoplightio/spectral`](https://github.com/stoplightio/spectral)：规则化契约 lint、诊断输出和自定义规则集参考。
- [`open-policy-agent/conftest`](https://github.com/open-policy-agent/conftest)：基于策略的配置与文档校验参考。
- [`SchemaStore/schemastore`](https://github.com/SchemaStore/schemastore)：JSON Schema 目录、编辑器提示和配置校验参考。
- [`Schemathesis/schemathesis`](https://github.com/schemathesis/schemathesis)：基于 OpenAPI 的契约测试和接口行为回归参考。
- [`sqlmesh/sqlmesh`](https://github.com/TobikoData/sqlmesh)：数据模型依赖、plan/apply 和变更影响分析参考。
- [`microsoft/playwright`](https://github.com/microsoft/playwright)：浏览器级 E2E、trace、截图和稳定选择器参考。
- [`testing-library/testing-library-docs`](https://github.com/testing-library/testing-library-docs)：面向用户语义的前端测试选择器参考。
- [`cypress-io/cypress`](https://github.com/cypress-io/cypress)：端到端测试组织、fixture 和调试体验参考。
- [`microsoft/vscode-extension-samples`](https://github.com/microsoft/vscode-extension-samples)：VS Code 扩展、诊断和 Code Action 示例参考。
- [`redhat-developer/vscode-java`](https://github.com/redhat-developer/vscode-java)：编辑器诊断、补全和修复入口组织参考。
- [`getsentry/sentry-javascript`](https://github.com/getsentry/sentry-javascript)：前端错误上下文、breadcrumb 和本地反馈证据采集参考。
- [`testcontainers/testcontainers-java`](https://github.com/testcontainers/testcontainers-java)：Java 集成测试中启动真实 PostgreSQL/MySQL 容器的参考。
- [`TanStack/query`](https://github.com/TanStack/query)：前端 server state、请求缓存、重试和错误状态收口参考。
- [`TanStack/table`](https://github.com/TanStack/table)：复杂表格状态、列配置、筛选排序和虚拟化体验参考。
- [`ag-grid/ag-grid`](https://github.com/ag-grid/ag-grid)：密集数据表格、行内编辑和大数据量交互模式参考。
- [`dequelabs/axe-core`](https://github.com/dequelabs/axe-core)：前端可访问性自动检查规则参考。
- [`GoogleChrome/lighthouse`](https://github.com/GoogleChrome/lighthouse)：页面性能、可访问性和最佳实践审计参考。
- [`storybookjs/storybook`](https://github.com/storybookjs/storybook)：前端组件状态样例库、交互文档和视觉回归预备参考。
- [`mswjs/msw`](https://github.com/mswjs/msw)：前端 API mock、测试夹具和无后端开发模式参考。
- [`vitejs/vite`](https://github.com/vitejs/vite)：前端构建、代码分割和包体分析参考。
- [`gitleaks/gitleaks`](https://github.com/gitleaks/gitleaks)：敏感信息检测、日志脱敏和 secret 防泄漏参考。
- [`getsops/sops`](https://github.com/getsops/sops)：本地密文配置、密钥分离和配置安全边界参考。
- [`dotenvx/dotenvx`](https://github.com/dotenvx/dotenvx)：环境变量管理、加密 env 文件和本地凭据加载参考。
- [`microsoft/presidio`](https://github.com/microsoft/presidio)：PII 检测、匿名化和脱敏流水线参考。
- [`faker-js/faker`](https://github.com/faker-js/faker)：合成测试数据和安全示例值生成参考。
- [`pgvector/pgvector`](https://github.com/pgvector/pgvector)：本地或自托管向量检索索引的设计参考。
- [`facebook/docusaurus`](https://github.com/facebook/docusaurus)：静态文档站、版本化文档和搜索入口参考。
- [`sqlalchemy/sqlalchemy`](https://github.com/sqlalchemy/sqlalchemy)：数据库方言分层、连接能力抽象和类型映射参考。
- [`open-telemetry/opentelemetry-collector`](https://github.com/open-telemetry/opentelemetry-collector)：本地 traces/metrics/logs 信号组织和诊断摘要参考。
- [`typeorm/typeorm`](https://github.com/typeorm/typeorm)：ORM 实体元数据、装饰器映射和代码模型反向提取参考。
- [`mybatis/mybatis-3`](https://github.com/mybatis/mybatis-3)：XML/注解映射、SQL 片段和 Java 项目字段来源提取参考。
- [`stryker-mutator/stryker-js`](https://github.com/stryker-mutator/stryker-js)：变异测试、回归召回和质量指标组织参考。
- [Model Context Protocol 规范](https://modelcontextprotocol.io/specification/2025-06-18)：AI 应用接入 resources、prompts、tools 的协议基础。
- [`modelcontextprotocol/servers`](https://github.com/modelcontextprotocol/servers)：MCP server 参考实现集合。
- [`agents.md`](https://agents.md/)：面向 coding agent 的项目指令文件约定。
