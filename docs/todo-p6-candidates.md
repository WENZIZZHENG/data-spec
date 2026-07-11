# DataSpec P6 候选池

更新时间：2026-07-11

本文件承接根 TODO.md 中尚未完成的 P6 候选详情。后续开发先读根 TODO.md、候选评审和时间评估，再按任务范围读取本文件对应条目；不要默认线性顺扫全部候选。

- 未完成候选数量：70
- 本轮已归档为现有能力覆盖：6 项，详见 [archive/todo-completed-p5-p6.md](archive/todo-completed-p5-p6.md)
- 本轮已删除独立候选：9 项，详见 [archive/todo-removed-p6-candidates.md](archive/todo-removed-p6-candidates.md)
- 已完成 P5/P6 详情：[archive/todo-completed-p5-p6.md](archive/todo-completed-p5-p6.md)
- 候选评审：[todo-p6-candidate-review.md](todo-p6-candidate-review.md)
- 剩余时间评估：[todo-p6-remaining-estimates.md](todo-p6-remaining-estimates.md)

## 候选索引

### 早期遗留候选

- P6-74：标准变更演练沙箱与样例回归
- P6-76：业务对象关系图与表模板依赖
- P6-77：派生字段、单位换算与口径规则
- P6-80：规则与模板变更 diff 包
- P6-84：前端可访问性与键盘操作基线
- P6-85：本地数据清理、重置与演示项目重建
- P6-86：前端性能体验指标与慢页面提示

### 扩展候选池 P6-94 到 P6-128

- P6-95：可插拔规则 SDK 与项目内自定义规则
- P6-96：本地语义检索索引与相似字段发现
- P6-98：面向 AI 的标准变更发布说明与迁移指令
- P6-101：环境配置漂移检测与运行指纹
- P6-102：数据源连接器能力注册与插件化边界
- P6-103：本地运行观测、慢请求与诊断包
- P6-104：标准决策理由库与字段 ADR 记录
- P6-106：表级约束、索引与主外键标准
- P6-107：代码集与枚举值生命周期
- P6-108：业务仓库迁移交付包
- P6-109：AI 能力边界模拟与安全预检
- P6-110：文档与需求材料反向提取标准候选
- P6-111：标准候选来源管道与自动入箱
- P6-112：标准候选批量决策与可撤销记录
- P6-113：候选采纳前质量门禁与影响预览
- P6-116：前端端到端上手引导与可复现演练
- P6-117：统一变更 Diff 与预览组件
- P6-118：全链路 Trace ID 与 AI 操作关联
- P6-119：版本兼容矩阵与降级策略
- P6-120：AI 场景样例库与端到端回放脚本
- P6-121：DataSpec 自身数据模型契约快照
- P6-122：前端问题反馈转任务与证据采集
- P6-123：字段标准向 API/DTO Schema 导出
- P6-124：ORM/代码模型反向提取标准候选
- P6-126：SQL 规则变异回归与误报召回基线
- P6-127：AI Context 增量更新包与差分同步

### 扩展候选池 P6-129 到 P6-176

- P6-129：标准包 Lockfile 与业务仓库可复现同步
- P6-130：SQL 格式化 Profile 与确定性输出
- P6-131：受控脱敏样例值采样与合成回填
- P6-133：规则覆盖率与死规则清理报告
- P6-134：前端 mock API 演示模式与无后端体验
- P6-135：AI 任务预检与缺口补齐建议
- P6-136：数据库直连反向导入 Plan/Apply 与可撤销批次
- P6-137：`.dataspec/config.json` Schema 与编辑器提示包
- P6-138：本地凭据复用与 Secret Provider 边界
- P6-139：标准变更迁移 Recipe 与半自动代码修复建议
- P6-140：前端/CLI/MCP 统一 TaskResult / Evidence 信封
- P6-141：AI 输出 SQL/DDL 执行前验证沙箱
- P6-142：多 schema / 多数据源反向导入合并策略
- P6-143：Agent 专用项目启动包生成
- P6-144：标准质量异常自动归因
- P6-145：前端错误到修复 Action 的闭环体验
- P6-146：个人标准健康摘要与下一步报告
- P6-149：标准包同步巡检与漂移修复建议
- P6-150：业务仓库标准合规分与 PR 摘要
- P6-151：字段库密集编辑与大表格键盘体验
- P6-154：标准消费端 SDK 与类型常量包导出
- P6-155：规则依赖图与冲突诊断
- P6-156：OpenAPI/CLI/MCP 示例契约快照自动生成
- P6-157：个人单机分发包与离线启动预案
- P6-159：AI 任务状态机与断点续跑
- P6-160：标准变更影响预演与候选修复单
- P6-161：AI 可读字段知识卡片
- P6-162：规则/标准 A/B 评测与回归数据集
- P6-163：连接器能力探测与方言 Profile
- P6-168：MCP Resource 游标分页与大字段库分片导出
- P6-171：标准规则向数据质量测试导出
- P6-173：编辑器提示与 Code Action 轻量包
- P6-174：跨协议 Schema 导出与标准适配层
- P6-175：指标口径与字段标准映射层
- P6-176：标准消费端兼容验收套件

### 新近候选 P6-184 到 P6-185

- P6-184：字段标准中英文命名与翻译辅助
- P6-185：标准驱动测试数据与边界用例包

## 候选详情

### P6-74：标准变更演练沙箱与样例回归
- 状态：待办。
- 为什么做：修改字段、规则或模板前，用户想知道会影响哪些 SQL、DDL、AI Prompt 和反向导入候选；需要一个轻量“先演练再保存”的入口。
- 已有基础：已有标准快照、What-if 预览待办、Prompt 评测、golden fixtures、SQL 检查记录、DDL 生成、AI 回放和标准质量门禁待办。
- 缺口：现有能力分散，缺少一次性把“改动草案 -> 样例集合 -> lint/DDL/prompt 结果 diff -> 保存建议”串起来的沙箱。
- 落地产物：新增标准草案演练入口；支持选择字段/规则/模板草案和样例集，运行 lint、DDL preview、Prompt preview、Context diff，并输出风险、收益和建议保存步骤。
- 验收标准：用户在保存规则或字段变更前能看到对 good/bad SQL、模板 DDL 和 AI Context 的影响；AI 可读取演练报告决定是否继续修改；演练不写入正式标准。
- 边界：不做多人审批发布，不替代完整 CI；第一版只覆盖项目内 fixture、历史检查记录和用户手动粘贴样例。

### P6-76：业务对象关系图与表模板依赖
- 状态：待办。
- 为什么做：AI 建表不仅需要字段标准，还需要知道用户、订单、支付、审计等业务对象之间的关系；同时数据域和表模板需要可视化维护闭环，否则标准字段难以稳定进入 DDL 生成、需求草案和 AI Context。
- 已有基础：已有表模板、DDL 生成、领域 Starter Kit、字段分组、字段影响分析、AI Context 和多项目标准复用包待办。
- 缺口：字段标准和表模板之间缺少轻量关系模型；数据域/表模板前端入口还缺少完整 CRUD、模板字段选择、DDL 预览和自动 lint 的最小闭环；AI 无法稳定理解“订单表依赖用户表”“支付金额来自订单金额”“审计字段应应用到所有业务表”等模式。
- 落地产物：先补齐数据域与表模板设计器：数据域 CRUD、表模板 CRUD、模板字段从标准字段选择、DDL 预览和 lint 摘要；再新增业务对象/表模板关系描述，支持 entityName、tablePattern、requiredFields、optionalFields、relations、foreignKeyHints、auditFields、commonPitfalls 和 contextExport；前端可展示简易关系图。
- 验收标准：用户能维护数据域和表模板，并从模板预览可 lint 的 DDL；生成订单、支付、用户等常见 DDL 时能引用对象关系和外键建议；AI Context 能按业务对象或表模板裁剪导出；关系图不要求依赖真实数据库外键。
- 边界：不做完整 ER 建模工具，不强制所有项目维护业务对象，不自动改写已有表结构。

### P6-77：派生字段、单位换算与口径规则
- 状态：待办。
- 为什么做：很多标准字段不是孤立存在的，例如 `amount_cent` 与 `amount_yuan`、`paid_at` 与 `paid_date`、`status_code` 与状态枚举；AI 如果不知道派生关系和单位口径，容易生成混用字段。
- 已有基础：已有字段值格式与校验样例库待办、枚举/码表、字段质量评分、AI Context、DDL 生成和字段推荐。
- 缺口：字段之间缺少 derivedFrom、unitConversion、aggregationRule、timeGranularity 和 sourceOfTruth 等结构化关系；质量检查也无法提示口径混用。
- 落地产物：新增派生字段规则模型或字段扩展；支持源字段、转换表达式说明、单位、精度、时间粒度、枚举映射、推荐使用场景和反例；导出到 AI Context 并纳入质量评分。
- 验收标准：AI 能区分金额分/元、日期/时间戳、编码/展示名、原始值/派生值；DDL/Prompt 生成会提示首选字段和转换口径；测试覆盖典型金额和时间字段。
- 边界：不执行真实数据计算，不替代数据血缘平台，不强制所有字段配置派生规则。

### P6-80：规则与模板变更 diff 包
- 状态：待办。
- 为什么做：规则、表模板和 Prompt 模板会持续调整；如果只看保存后的结果，AI 和用户很难判断这次变更会影响哪些 SQL 检查、DDL 生成和 Context 输出。
- 已有基础：已有规则配置、规则模板库、Prompt 模板版本化、标准变更演练沙箱、标准快照、执行证据包和 OpenSpec 归档待办。
- 缺口：字段快照相对清晰，但规则/模板/Prompt 的变更 diff、兼容性说明、影响样例和回滚信息还没有统一结构。
- 落地产物：新增规则/模板变更 diff 包；记录 before/after、changedParams、affectedRules、affectedTemplates、sampleResultDiff、compatibilityNotes、rollbackPlan 和 reviewChecklist。
- 验收标准：调整一条规则参数或模板后，能看到命中样例和生成结果差异；AI 可读取 diff 包决定是否需要补测试或更新 Context；回滚说明不依赖人工记忆。
- 边界：不做审批流，不替代 OpenSpec proposal，不要求所有历史模板补齐 diff；优先覆盖新变更。

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

### P6-98：面向 AI 的标准变更发布说明与迁移指令
- 状态：待办。
- 为什么做：标准字段、规则、模板和 Prompt 变化后，AI agent 需要知道“这次标准变了什么、旧写法如何迁移、哪些任务要重跑”，而不只是看到一份 diff 或快照 hash。
- 已有基础：已有标准快照、变更日志、规则与模板 diff 包、执行证据包、AI 会话启动包、TODO 到 OpenSpec 交接、MCP/CLI 工作流模板和 OpenSpec 归档。
- 缺口：缺少面向 AI 的 release note/migration guide；变更完成后没有统一输出 breakingChanges、deprecatedFields、replacementHints、requiredChecks 和 recommendedWorkflow。
- 落地产物：新增标准变更发布说明生成器；按快照 diff 或变更日志生成 machine-readable changelog，包含 changeType、affectedArtifacts、migrationSteps、beforeAfterExamples、verificationCommands、rollbackHint 和 agentInstructions；CLI/MCP/AI Context 可读取。
- 验收标准：标准升级后 AI 能解释本次变化并按迁移指令修 SQL、DDL 或字段引用；破坏性变更会明确标记并要求人工确认；发布说明可随备份包或执行证据包归档。
- 边界：不做复杂发布审批，不自动修改业务仓库；第一版只生成说明和建议命令，实际改动仍走 dry-run 或人工确认。

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

### P6-133：规则覆盖率与死规则清理报告
- 状态：待办。
- 为什么做：规则、豁免、模板和 fixedSql 持续增加后，需要知道哪些规则长期无命中、哪些规则只产生误报、哪些规则缺少 fixture；否则规则体系会变重，AI 也会读到低价值约束。
- 已有基础：已有规则配置、规则调试器、规则误报豁免、SQL 规则变异回归、golden fixtures、检查记录和字段质量门禁。
- 缺口：缺少 rule coverage/report，无法聚合 lastTriggeredAt、fixtureCoverage、dialectCoverage、falsePositiveHints、suppressionRate、fixerCoverage、mutationExamples 和 recommendedAction。
- 参考项目：`eslint/eslint` 的规则测试组织、`istanbuljs/nyc` 的覆盖率报告和 `sqlfluff/sqlfluff` 的规则 fixture 体系；只借鉴覆盖率指标，不把 lint 规则变成测试覆盖率平台。
- 落地产物：新增规则覆盖率报告 API/CLI 和前端摘要；基于历史检查记录、fixture、豁免、fixedSql 计划和方言样例统计规则活跃度、质量风险和清理建议；输出机器可读 JSON，方便接入本地验证入口。
- 验收标准：能识别长期未触发规则、缺 fixture 规则、缺方言样例规则、高豁免率规则和无 fixer 规则；报告给出保留、补测试、降级或停用建议；移除关键 fixture 时相关验证能失败或明确提示覆盖缺口；不会影响正常 lint 执行。
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

### P6-140：前端/CLI/MCP 统一 TaskResult / Evidence 信封
- 状态：待办。
- 为什么做：同一件事通过前端、CLI、MCP 或 API 执行后，现在结果展示字段、下一步建议、证据链接和失败状态容易不一致；AI 需要稳定读取“完成/部分完成/阻塞”和后续动作。
- 已有基础：已有 AI 任务卡、AI 回放、执行证据包、全链路 Trace、前端统一状态、MCP/CLI 工具契约验收和 AI 可读错误码待办。
- 缺口：缺少统一 `TaskResult` / Evidence envelope，无法复用 status、summary、counts、artifacts、evidenceRefs、nextActions、retryable、blockedReason、traceId、suggestedCommands 和 validationEvidence。
- 参考项目：`github/gh` 的命令输出、GitHub Actions job summary、`modelcontextprotocol/servers` 的工具结果结构和 `getsentry/sentry-javascript` 的错误上下文；只统一结果表达，不引入任务调度平台。
- 落地产物：新增 TaskResult JSON Schema/DTO、Evidence envelope、CLI/MCP 输出适配和前端结果卡片组件；SQL 校验、Context 导出、反向导入、doctor/preflight 逐步接入；第一版为 `lint-files`、`evidence export`、`task show` 或等价高频入口建立 golden payload；文档列出字段语义。
- 验收标准：AI 调用任一核心任务都能用同一方式判断是否成功、下一步做什么、证据在哪；前端失败卡片与 CLI JSON 的关键字段一致；证据引用和验证建议能进入同一信封；兼容旧响应并有契约测试。
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
- 缺口：缺少变更前 dry-run 和 Patch Plan，无法按标准对象输出 impactedApis、impactedSql、impactedFiles、databaseCommentPlan、aiContextImpact、suggestedPatches、verificationCommands、riskLevel 和 rollbackHint。
- 参考项目：`openrewrite/rewrite` 的迁移 recipe、`hashicorp/terraform` 的 plan/apply 思路和 `reviewdog/reviewdog` 的 diff 诊断评论；只做本地预演和修复单，不自动改真实仓库。
- 落地产物：新增标准变更 dry-run API/CLI；保存标准前可生成影响摘要、候选修复单和可审查 Patch Plan；前端在字段/规则保存前展示影响预演；证据包记录本次变更的影响、验证命令和取舍。
- 验收标准：变更一个字段、枚举、规则或模板时能看到 DataSpec 内部引用、数据库 COMMENT 回写草案、业务代码引用风险、建议验证命令和证据包入口；用户确认后再保存；AI 可把修复单转成后续任务。
- 边界：不强制所有保存都阻断，不直接连接生产仓库写补丁；第一版覆盖字段、枚举和默认规则。

### P6-161：AI 可读字段知识卡片
- 状态：待办。
- 为什么做：AI 使用字段标准时，单纯字段列表还不够；它需要每个字段的使用场景、禁止写法、示例、相关字段和常见误用，才能在建表、修 SQL 和问答时少走弯路。
- 已有基础：已有字段标准、数据字典、标准问答入口、业务术语表、示例反例库、自然语言标准候选和字段推荐质量增强。
- 缺口：缺少可直接嵌入 AI Context 的 FieldKnowledgeCard，无法稳定表达 aliases、antiPatterns、usageExamples、relatedFields、enumHints、riskNotes 和 lastVerifiedAt。
- 参考项目：`backstage/backstage` 的 catalog 实体页面、`facebook/docusaurus` 的结构化文档组织和 `great-expectations/great_expectations` 的规则说明；只生成轻量知识卡，不做复杂知识图谱。
- 落地产物：为字段标准生成 AI 可读知识卡；支持前端查看、CLI 导出和 MCP resource 读取；聚合字段元数据、usage examples、反例、冲突、来源、问答证据、规则命中和用户反馈。
- 验收标准：AI 针对单个字段能拿到完整、短小、可引用的标准说明；卡片能说明何时使用、何时不要用、如何命名、关联哪些字段以及有哪些常见误用；高频、敏感、金额、状态和时间字段优先覆盖；变更后有版本或更新时间。
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

### P6-168：MCP Resource 游标分页与大字段库分片导出
- 状态：待办。
- 为什么做：字段库、规则、模板、证据和历史记录变大后，一次性通过 MCP resource 或 Context 包返回全部内容会浪费上下文，也可能超过客户端限制；AI 需要按游标分片读取并知道下一片是否必要。
- 已有基础：已有 AI Context 裁剪、字段分页、性能基线、大库扫描计划、元数据增量缓存、MCP/CLI 工作流模板和 MCP/CLI 兼容握手待办。
- 缺口：MCP resources 缺少统一 cursor、pageSize、chunkHash、hasMore、resumeToken 和 compactSummary；AI 无法可靠分批读取 field-catalog、rules、evidence、records 等大资源。
- 参考项目：Model Context Protocol resources 设计、`TanStack/query` 的分页缓存和 `OpenLineage/OpenLineage` 的事件分片结构；只借鉴分页与恢复，不引入远程缓存服务。
- 落地产物：为大 MCP resource 和 CLI 导出定义分页契约；支持 first/next/summary 三类读取模式；Context 包 manifest 记录分片 hash；doctor 可提示客户端是否支持分页能力。
- 验收标准：上千字段项目可通过 MCP 分页读取完整标准，不超出单次上下文预算；中断后可用 resumeToken 继续；分片内容与完整导出 hash 可校验一致。
- 边界：不替代完整 zip 导出，不为所有小资源强制分页；第一版优先 field-catalog、rule catalog 和 evidence summary。

### P6-171：标准规则向数据质量测试导出
- 状态：待办。
- 为什么做：字段标准、枚举、必填规则和命名规则已经能指导建表，但落到业务仓库后仍需要可执行的数据质量测试；AI 需要把 DataSpec 标准转成 dbt、Great Expectations 或 SQL 断言，而不是只生成说明文档。
- 已有基础：已有规则配置、字段覆盖率、DDL 生成、执行证据包、标准消费端 SDK 待办和业务仓库迁移交付包待办。
- 缺口：缺少 dataQualityTestSpec、testTarget、expectationSuite、dbtSchemaYaml、sqlAssertion 和 verificationResult；标准规则无法直接导出为可落地的测试工件。
- 参考项目：`dbt-labs/dbt-core` 的 `schema.yml` tests、`great-expectations/great_expectations` 的 expectation suite 和 `TobikoData/sqlmesh` 的模型质量校验；只借鉴测试工件表达，不引入完整数据平台。
- 落地产物：新增标准到数据质量测试的 API/CLI 导出；支持按项目、表、字段、规则生成 dbt tests、Great Expectations suite 和只读 SQL assertion 模板；交付包可附带验证命令和风险说明。
- 验收标准：选择一个项目后能导出结构稳定的测试包；枚举值、非空、字段格式、命名和敏感字段规则能转成可执行或可人工确认的断言；导出内容不包含业务数据行或数据库密码。
- 边界：第一版不连接生产库跑全量数据质量扫描，不承诺覆盖所有 dbt/GE 高级能力；先做确定性规则到测试工件的转换。

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
- 落地产物：新增标准消费端兼容套件目录和 CLI `compat check`；在现有 CLI/MCP contract fixture 基础上扩展字段、规则、枚举、Context、MCP resource、CLI JSON、API schema registry 和 schema export 的 golden payload；输出 breaking/compatible/deprecated 结果。
- 验收标准：修改核心 DTO、API 字段或导出格式时能一键检查主要消费端契约；失败结果包含破坏字段、影响入口和迁移建议；自有消费端、关键 AI 入口和示例 adapter 有 golden 覆盖；CI/本地验证入口可复用。
- 边界：不要求所有第三方工具接入，不阻止个人本地实验性变更；第一版覆盖 DataSpec 自有消费端和示例 adapter。

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
- 落地产物：新增测试数据包生成 API/CLI；按字段标准、枚举、格式、敏感标记和业务对象生成 JSON/CSV/SQL seed/mock 草稿，同时输出 valid、invalid 和 boundary cases。
- 验收标准：手机号、金额、时间、枚举、JSON 等字段能生成可解释样例；导出包可被前端 mock、后端测试、数据质量测试或 AI 生成单测复用；生成结果不包含原始业务数据。
- 边界：不自动写入业务数据库，不保证满足所有业务规则；第一版聚焦字段级和轻量对象级样例。


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
