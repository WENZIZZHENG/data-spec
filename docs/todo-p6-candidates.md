# DataSpec P6 精简候选池

更新时间：2026-07-13

本文件只保留仍值得进入实施队列的主题。2026-07-12 清理前的 60 项完整描述已归档到 [候选池历史快照](archive/todo-p6-candidates-2026-07-12.md)，删除或等待外部触发的候选见 [删除 / 不做归档](archive/todo-removed-p6-candidates.md)。

## 队列规则

- 活跃队列按主题推进，不再按原编号线性顺扫。
- 每个主题第一版只完成“近期范围”，其余原编号作为可选子项，不自动扩张范围。
- 只有出现本文件写明的触发条件，暂缓主题才进入 OpenSpec 或开发计划。
- `P6-190` 已完成并移入 [P5/P6 完成归档](archive/todo-completed-p5-p6.md)，不再占用活跃队列。
- 当前共 9 个实施主题，承接 36 个原候选编号和 2 个第三轮新增编号；其中 7 个进入近期队列，2 个等待业务触发。

## 近期队列

### 1. P6-189：确定性数标命名解析与缩写治理

- 类型：核心解析、字段检索、命名治理；进入 OpenSpec / SDD full。
- 为什么值得做：当前中文主要依赖整段子串匹配，英文只做基础 camel/snake 拆分，拼音与缩写依赖人工条目或少量硬编码，无法稳定解释多词查询、acronym、数字、单位和歧义缩写。
- 已有基础：业务术语表已有 term、synonyms、rootTerms、abbreviations、disabledTerms 和 canonicalFieldId；字段推荐、检索、标准问答和 Explain Trace 已能承接统一解析结果。
- 近期范围：新增共享 `QueryNormalizer` 或等价内部能力，按“分隔符/camel/acronym/数字/单位拆分 -> 项目词典最长匹配 -> 缩写展开 -> 禁用词与歧义检查 -> token evidence”处理输入；复用现有 glossary，不新增第二套词典。
- 验收：`HTTPStatus2Code`、`会员手机号`、`ord_amt` 等样例得到稳定 token、canonical 提示和来源；缩写冲突可解释；推荐、检索、标准问答共享同一结果且不破坏既有 API。
- OpenSpec 交接：建议 change-id 为 `add-deterministic-name-tokenization`；依赖 `P6-190` 提供可信历史别名与 evidence，完成后作为 `P6-120` 和 `P6-111` 的输入；重点更新 `business-glossary-synonym-roots`、`field-suggestion`、`field-standard-search`、`standard-query-dsl`、`explain-trace`，只新增兼容的 token evidence 和解释语义，不改变现有 API 必填字段、不自动写术语表；验证至少覆盖后端、前端、CLI/MCP/tools、命名 golden fixtures、`openspec validate <change-id> --strict`、`openspec validate --all` 和 `git diff --check`。
- 参考：借鉴 [jieba](https://github.com/fxsjy/jieba) 的自定义词典和确定性最长匹配；不直接引入 Python 运行时、外部 LLM、向量数据库或自动全拼采纳。
- 边界：不把普通自然语言分词结果直接当标准名，不自动写入术语表，不默认生成有歧义的拼音缩写。
- 估算：3-5 个工作日。

### 2. P6-120：推荐质量与 AI 场景回归基线

- 原编号：`P6-120`、`P6-126`、`P6-133`、`P6-162`。
- 为什么值得做：现有 synthetic examples、golden fixtures、CLI/MCP 契约和 local smoke 已覆盖单点能力，但缺少命名推荐质量指标和少量完整 AI 使用链路回放。
- 近期范围：复用现有测试入口增加中文、英文、缩写、多词、历史别名 fixture，统计 Top-1/Top-3、误召回和排序退化；在 local smoke 中补 bootstrap、最小 Context 和维护 plan 两条确定性链路。
- 验收：命名或评分变化能定位到具体退化样例；高频 AI 链路可由一条现有验证命令回放；不新增第二套回放框架。
- 边界：不调用外部 LLM，不做 A/B 平台、向量评测平台或通用覆盖率产品。
- 估算：1-2 个工作日。

### 3. P6-86：前端性能体验与字段库密集操作

- 原编号：`P6-86`、`P6-122`、`P6-145`、`P6-151`。
- 为什么值得做：字段库无筛选时仍会加载全量数据后浏览器分页，搜索结果又有固定上限；大字段库会同时出现性能退化和结果截断。
- 近期范围：只完成字段库服务端分页、可分页搜索、请求防抖、慢状态提示和超过 50 条结果的浏览器测试；测量结果证明需要时再做局部渲染优化。
- 验收：目标数据量下分页、筛选、滚动和键盘操作无明显卡顿，搜索不因固定 50 条上限丢失可达结果，关键路径有浏览器测试。
- 边界：不建设通用遥测平台，不一次优化所有页面，不为了包体或虚拟化重写现有组件体系。
- 估算：2-3 个工作日。

### 4. P6-191：统一 Finding/Evidence 与 AI/PR 评审闭环

- 类型：AI 评审、CLI/MCP/GitHub 契约、证据治理；进入 OpenSpec / SDD full。
- 为什么值得做：SQL lint、质量门禁、AI output post-check、PR inline 评论和 Evidence Package 已存在，但 finding 结构、证据真实性和交付入口彼此割裂。
- 已有基础：`review-pr`、AI output post-check、SQL check records、task runs、Evidence Package 和 GitHub line mapping 可复用。
- 近期范围：定义共享 Finding + Evidence 语义，至少覆盖 code、severity、subject、location、trigger、expected、observed、evidenceRefs、confidence、suggestedFix、autoFixSafe 和 waiver；扩展现有 `review-pr` 输出 commit SHA、评论 URL、SQL check IDs、post-check 状态和 evidence package 入口。
- 验收：一次 PR 评审可生成去重的 inline/fallback findings 和可验证证据包；外部 AI 返回的结构化 finding 必须经过 post-check 和 evidence resolver；无高置信问题时允许空 findings。
- OpenSpec 交接：建议 change-id 为 `unify-review-findings-and-evidence`；依赖 `P6-190` 的 evidence resolver，复用既有 `review-pr` 而非新建入口；重点更新 `ai-output-postcheck`、`ai-evidence-package`、`github-inline-review`、`sql-inline-review-location`、`cli-mcp-contract-fixtures`，Finding 字段采用 additive/versioned 兼容策略，远程评论写入继续遵守现有 dry-run、权限和脱敏边界；验证至少覆盖后端、前端、CLI/MCP/tools、GitHub fixture、evidence package fixture、`openspec validate <change-id> --strict`、`openspec validate --all` 和 `git diff --check`。
- 参考：借鉴 [SQLFluff](https://github.com/sqlfluff/sqlfluff) 的稳定规则/位置/fix 协议和 [PR-Agent](https://github.com/The-PR-Agent/pr-agent) 的受约束结构化输出；不采用 PR 总分，不允许无确定性证据的 AI finding 成为门禁。
- 边界：第一版不由 DataSpec 调用外部 LLM，不新建另一套 PR reviewer，不自动应用修复或自动创建远程 PR。
- 估算：3-5 个工作日。

### 5. P6-137：`.dataspec/config.json` Schema 与编辑器提示

- 原编号：`P6-101`、`P6-103`、`P6-118`、`P6-119`、`P6-137`、`P6-173`。
- 为什么值得做：配置文件是 CLI、MCP 和业务仓库集成入口；JSON Schema 是确定性缺口，而独立运行指纹会重复现有 doctor、版本握手、apiSchemaHash、specHash 和 bootstrap。
- 近期范围：提供 JSON Schema、字段说明、版本字段和最小编辑器关联示例；在不新增第二套诊断协议的前提下，让 doctor 输出配置 schema/version 摘要。
- 验收：错误配置能被本地校验发现，AI 和编辑器能读取字段说明，doctor 能解释配置版本或现有 hash 漂移，CLI 测试保持宽松兼容。
- 边界：不开发 VS Code 插件，不接远程观测平台，不做全链路 tracing 或独立 fingerprint 服务。
- 估算：0.5-1 个工作日。

### 6. P6-111：标准候选来源管道

- 原编号：`P6-110`、`P6-111`、`P6-112`、`P6-113`、`P6-124`、`P6-136`、`P6-142`。
- 为什么值得做：候选 Inbox 和维护 workflow 已存在，下一步价值在于把一类证据充分的安全信号稳定送入 Inbox，而不是同时接入所有来源。
- 近期范围：第一版只接一个来源；在 `P6-189` 完成后优先把未知业务词、歧义缩写和禁用命名转为 dry-run 候选，使用 project、name、sourceType、sourceRef 去重。
- 验收：同一事实不会重复入箱，候选可追溯 token evidence 并进入现有决策流程，写入前有 dry-run 和人工确认。
- 边界：覆盖率、反向导入、AI 反馈、文档/ORM 解析、多数据源冲突和批量 apply 仅在首个来源稳定后按真实需求增量接入。
- 估算：2-3 个工作日。

### 7. P6-85：演示项目 dry-run 清理与完整重建

- 原编号：`P6-85`。
- 为什么值得做：个人本地反复演示会留下检查和任务记录，当前项目删除和演示项目复用不足以表达完整清理语义。
- 近期范围：只做演示项目完整重建：dry-run 列出影响，显式确认后以事务清理演示项目资产并重新种入；保留 Token、连接预设和其他项目边界。
- 验收：用户能预览影响范围，确认后得到可重复的干净演示项目，不影响其他项目和安全配置；失败时事务回滚并给出恢复提示。
- 边界：不建设任意项目自由清理平台，不默认删除 Docker volume，不绕过备份恢复和凭据边界。
- 估算：3-5 个工作日。

## 暂缓主题

### 8. P6-74：标准变更 diff、发布说明与迁移预演

- 原编号：`P6-74`、`P6-80`、`P6-98`、`P6-104`、`P6-108`、`P6-117`、`P6-139`、`P6-160`。
- 保留原因：现有 What-if、Schema Plan 和代码 Patch Plan 已覆盖基础预览；仍可能需要跨标准包的统一 diff 和 AI 可读发布说明。
- 启动条件：出现真实标准包升级，且现有预览无法说明兼容影响或迁移步骤。
- 第一版边界：只做结构化 diff、release note、验证命令和简短 decision note，不建设自动迁移引擎或独立 ADR 平台。
- 估算：2-3 个工作日。

### 9. P6-123：标准消费 Schema、Contract-as-Code 与轻量锁定

- 原编号：`P6-123`、`P6-127`、`P6-129`、`P6-149`、`P6-154`、`P6-174`。
- 保留原因：标准最终可能需要被 API、DTO 或类型常量直接消费，但当前没有明确消费仓库和协议范围。
- 启动条件：至少一个真实业务仓库明确需要 JSON Schema、OpenAPI fragment 或语言常量包，或多仓消费已经出现标准包版本漂移故障。
- 第一版边界：只支持一个已确认目标，例如 JSON Schema 或 ODCS-compatible DataSpec profile，并记录 snapshot/context/package hash；借鉴 [DataContract CLI](https://github.com/datacontract/datacontract-cli) 的 lint/test/export 和 [ODCS](https://github.com/bitol-io/open-data-contract-standard) 的 schema 结构，不同时铺开 Protobuf、Avro、多语言 SDK 或远程包仓库。
- 估算：3-5 个工作日。

## 近期建议顺序

1. `P6-189`：统一命名解析、缩写治理和 token evidence。
2. `P6-120`：立即固化命名推荐与 AI 链路回归基线。
3. `P6-86`：解决已有数据证明的字段库性能和结果截断。
4. `P6-191`：复用已完成的 evidence resolver，统一 Finding/Evidence 和 PR 评审。
5. `P6-137`：以低成本补稳定配置入口并吸收运行摘要。
6. `P6-111`：复用命名解析结果接入第一个候选来源。
7. `P6-85`：最后收口演示项目重建和本地维护边界。

近期 7 个主题串行约 14.5-24 个工作日；两个暂缓主题仅在触发条件成立后增加约 5-8 个工作日。
