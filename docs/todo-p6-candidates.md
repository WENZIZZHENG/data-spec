# DataSpec P6 精简候选池

更新时间：2026-07-14

本文件只保留仍值得进入实施队列的主题。2026-07-12 清理前的 60 项完整描述已归档到 [候选池历史快照](archive/todo-p6-candidates-2026-07-12.md)，删除或等待外部触发的候选见 [删除 / 不做归档](archive/todo-removed-p6-candidates.md)。

## 队列规则

- 活跃队列按主题推进，不再按原编号线性顺扫。
- 每个主题第一版只完成“近期范围”，其余原编号作为可选子项，不自动扩张范围。
- 只有出现本文件写明的触发条件，暂缓主题才进入 OpenSpec 或开发计划。
- `P6-190` 已完成并移入 [P5/P6 完成归档](archive/todo-completed-p5-p6.md)，不再占用活跃队列。
- `P6-189`、`P6-120` 和 `P6-86` 已完成并移入 [P5/P6 完成归档](archive/todo-completed-p5-p6.md)；确定性 token evidence 和推荐回归 fixture 继续作为 `P6-111` 的输入。
- 当前共 6 个实施主题，承接 28 个原候选编号和 1 个第三轮新增编号；其中 4 个进入近期队列，2 个等待业务触发。

## 近期队列

### 1. P6-191：统一 Finding/Evidence 与 AI/PR 评审闭环

- 类型：AI 评审、CLI/MCP/GitHub 契约、证据治理；进入 OpenSpec / SDD full。
- 为什么值得做：SQL lint、质量门禁、AI output post-check、PR inline 评论和 Evidence Package 已存在，但 finding 结构、证据真实性和交付入口彼此割裂。
- 已有基础：`review-pr`、AI output post-check、SQL check records、task runs、Evidence Package 和 GitHub line mapping 可复用。
- 近期范围：定义共享 Finding + Evidence 语义，至少覆盖 code、severity、subject、location、trigger、expected、observed、evidenceRefs、confidence、suggestedFix、autoFixSafe 和 waiver；扩展现有 `review-pr` 输出 commit SHA、评论 URL、SQL check IDs、post-check 状态和 evidence package 入口。
- 验收：一次 PR 评审可生成去重的 inline/fallback findings 和可验证证据包；外部 AI 返回的结构化 finding 必须经过 post-check 和 evidence resolver；无高置信问题时允许空 findings。
- OpenSpec 交接：建议 change-id 为 `unify-review-findings-and-evidence`；依赖 `P6-190` 的 evidence resolver，复用既有 `review-pr` 而非新建入口；重点更新 `ai-output-postcheck`、`ai-evidence-package`、`github-inline-review`、`sql-inline-review-location`、`cli-mcp-contract-fixtures`，Finding 字段采用 additive/versioned 兼容策略，远程评论写入继续遵守现有 dry-run、权限和脱敏边界；验证至少覆盖后端、前端、CLI/MCP/tools、GitHub fixture、evidence package fixture、`openspec validate <change-id> --strict`、`openspec validate --all` 和 `git diff --check`。
- 参考：借鉴 [SQLFluff](https://github.com/sqlfluff/sqlfluff) 的稳定规则/位置/fix 协议和 [PR-Agent](https://github.com/The-PR-Agent/pr-agent) 的受约束结构化输出；不采用 PR 总分，不允许无确定性证据的 AI finding 成为门禁。
- 边界：第一版不由 DataSpec 调用外部 LLM，不新建另一套 PR reviewer，不自动应用修复或自动创建远程 PR。
- 估算：3-5 个工作日。

### 2. P6-137：`.dataspec/config.json` Schema 与编辑器提示

- 原编号：`P6-101`、`P6-103`、`P6-118`、`P6-119`、`P6-137`、`P6-173`。
- 为什么值得做：配置文件是 CLI、MCP 和业务仓库集成入口；JSON Schema 是确定性缺口，而独立运行指纹会重复现有 doctor、版本握手、apiSchemaHash、specHash 和 bootstrap。
- 近期范围：提供 JSON Schema、字段说明、版本字段和最小编辑器关联示例；在不新增第二套诊断协议的前提下，让 doctor 输出配置 schema/version 摘要。
- 验收：错误配置能被本地校验发现，AI 和编辑器能读取字段说明，doctor 能解释配置版本或现有 hash 漂移，CLI 测试保持宽松兼容。
- 边界：不开发 VS Code 插件，不接远程观测平台，不做全链路 tracing 或独立 fingerprint 服务。
- 估算：0.5-1 个工作日。

### 3. P6-111：标准候选来源管道

- 原编号：`P6-110`、`P6-111`、`P6-112`、`P6-113`、`P6-124`、`P6-136`、`P6-142`。
- 为什么值得做：候选 Inbox 和维护 workflow 已存在，下一步价值在于把一类证据充分的安全信号稳定送入 Inbox，而不是同时接入所有来源。
- 近期范围：第一版只接一个来源；复用已完成 `P6-189` 的 token evidence，优先把未知业务词、歧义缩写和禁用命名转为 dry-run 候选，使用 project、name、sourceType、sourceRef 去重。
- 验收：同一事实不会重复入箱，候选可追溯 token evidence 并进入现有决策流程，写入前有 dry-run 和人工确认。
- 边界：覆盖率、反向导入、AI 反馈、文档/ORM 解析、多数据源冲突和批量 apply 仅在首个来源稳定后按真实需求增量接入。
- 估算：2-3 个工作日。

### 4. P6-85：演示项目 dry-run 清理与完整重建

- 原编号：`P6-85`。
- 为什么值得做：个人本地反复演示会留下检查和任务记录，当前项目删除和演示项目复用不足以表达完整清理语义。
- 近期范围：只做演示项目完整重建：dry-run 列出影响，显式确认后以事务清理演示项目资产并重新种入；保留 Token、连接预设和其他项目边界。
- 验收：用户能预览影响范围，确认后得到可重复的干净演示项目，不影响其他项目和安全配置；失败时事务回滚并给出恢复提示。
- 边界：不建设任意项目自由清理平台，不默认删除 Docker volume，不绕过备份恢复和凭据边界。
- 估算：3-5 个工作日。

## 暂缓主题

### 5. P6-74：标准变更 diff、发布说明与迁移预演

- 原编号：`P6-74`、`P6-80`、`P6-98`、`P6-104`、`P6-108`、`P6-117`、`P6-139`、`P6-160`。
- 保留原因：现有 What-if、Schema Plan 和代码 Patch Plan 已覆盖基础预览；仍可能需要跨标准包的统一 diff 和 AI 可读发布说明。
- 启动条件：出现真实标准包升级，且现有预览无法说明兼容影响或迁移步骤。
- 第一版边界：只做结构化 diff、release note、验证命令和简短 decision note，不建设自动迁移引擎或独立 ADR 平台。
- 估算：2-3 个工作日。

### 6. P6-123：标准消费 Schema、Contract-as-Code 与轻量锁定

- 原编号：`P6-123`、`P6-127`、`P6-129`、`P6-149`、`P6-154`、`P6-174`。
- 保留原因：标准最终可能需要被 API、DTO 或类型常量直接消费，但当前没有明确消费仓库和协议范围。
- 启动条件：至少一个真实业务仓库明确需要 JSON Schema、OpenAPI fragment 或语言常量包，或多仓消费已经出现标准包版本漂移故障。
- 第一版边界：只支持一个已确认目标，例如 JSON Schema 或 ODCS-compatible DataSpec profile，并记录 snapshot/context/package hash；借鉴 [DataContract CLI](https://github.com/datacontract/datacontract-cli) 的 lint/test/export 和 [ODCS](https://github.com/bitol-io/open-data-contract-standard) 的 schema 结构，不同时铺开 Protobuf、Avro、多语言 SDK 或远程包仓库。
- 估算：3-5 个工作日。

## 近期建议顺序

1. `P6-191`：复用已完成的 evidence resolver，统一 Finding/Evidence 和 PR 评审。
2. `P6-137`：以低成本补稳定配置入口并吸收运行摘要。
3. `P6-111`：复用命名解析结果接入第一个候选来源。
4. `P6-85`：最后收口演示项目重建和本地维护边界。

近期 4 个主题串行约 8.5-14 个工作日；两个暂缓主题仅在触发条件成立后增加约 5-8 个工作日。
