# AGENTS.md 项目级完整配置

本文件是 data-spec 的完整可执行项目规范，不依赖个人全局配置。运行环境如有平台级默认规则，只能作为不冲突的补充，不得新增本文件未声明的门禁。

规则优先级：用户最新指令 > 本项目 `AGENTS.md` / `SDD.md` > 运行环境平台规则。用户最新指令不能降低安全、Git、凭据和高风险最小门禁底线；若用户要求跳过这些底线，必须在实施或交付前停止并说明阻塞原因。

## 0. 语言与输出

- 始终使用中文回复；技术名词、代码名、参数名可保留 English。
- 人读字段如 `task_title`、`description`、`commit_message` 使用中文。
- 回复清晰直接，先给解决方案，再按任务需要展开必要原因和细节。
- 最终回复只保留适用项：改了什么、如何验证、是否 commit、剩余风险或下一步；不适用项可省略。

## 1. 项目目标与工作底线

- data-spec 当前优先服务个人 / 小团队快速使用，核心目标是让 AI 能稳定理解、生成、导入、校验和维护数据字段标准。
- 默认采用“最小可用闭环优先”：先完成能真实使用的一条链路，再补治理、审批、权限和批量平台化能力。
- 默认最小改动，只修改完成任务必需的代码和文档。
- 修改前先定位相关文件，不做无目的全仓库扫描。
- 用户明确要求实现、修复或修改时直接执行；只有关键信息缺失且无法合理假设时才提问。
- 用户明确只要求建议、评审、解释或方案时，先保持只读，不主动改文件。
- 常规开发命令可直接执行；commit、push、破坏性操作按第 9 节和第 10 节执行。

## 2. 下一步速查表

| 任务类型 | 流程 | 记录 / artifacts | 最低验证 | 评审 | commit |
| --- | --- | --- | --- | --- | --- |
| 只读分析、建议、评审、方案 | 只读 | 无 | 无需；必要时说明未验证 | 不适用 | 不 commit |
| README、TODO、文案、小样式、示例 | 快速模式 | 无 | 人工检查；必要时 `git diff --check` | 自查 | 默认不 commit，用户要求或需要保留历史时可 commit |
| 不改契约的前端展示、客户端 API 调用封装、单点测试补丁 | 快速模式或常规模式 | 可用 TODO / 最终回复记录 | 受影响文件的最小验证 | 自查或结构化自审 | 代码变更默认 commit |
| 普通产品功能，不改长期契约和存储语义 | 常规模式 | TODO / 轻量任务卡 / 最终回复 | 受影响模块测试或构建 | 结构化自审；agent 可选 | 默认 commit |
| 命中 SDD 硬触发但低风险、可回退、hotfix | SDD lite | 最小 SDD 记录 | 相关模块验证 + OpenSpec strict | 结构化自审；agent 可选 | 默认 commit |
| 改用户可见契约、跨模块协作、导入导出主流程 | SDD standard | OpenSpec artifacts | 相关模块验证 + OpenSpec strict | 结构化自审；高风险时 agent | 默认 commit |
| OpenAPI 契约、CLI / MCP / AI 外部协议 | SDD standard；breaking、高兼容风险或多模块时 SDD full | OpenSpec artifacts | 相关模块验证 + OpenSpec strict；full 时接近全量 | 必须独立 agent | 满足门禁后 commit |
| 安全、凭据、存储、权限、迁移、架构、部署 | SDD full | 完整 OpenSpec artifacts | 接近全量验证 + OpenSpec all | 必须独立 agent | 满足门禁后 commit |

## 3. 是否进入 SDD

两段式判断：先判断是否进入 SDD；进入 SDD 后，再按 `SDD.md` 判断 SDD lite / SDD standard / SDD full。

必须进入 SDD 的硬触发，限定为改变长期公共契约、持久化语义、安全边界或第三方依赖行为：

- 改数据库 schema、迁移、索引、字段模型、导入写入规则或历史数据兼容。
- 改 API route、OpenAPI schema、请求 / 响应字段、错误码、分页结构或跨端契约。
- 改持久化写入、事务、幂等、并发、可恢复性、数据一致性。
- 改敏感信息、数据库连接凭据、认证、权限、安全边界。
- 改 CLI / MCP / AI 外部协议的入参、出参、可被第三方依赖的行为。
- 改架构、构建、部署、任务调度或核心解析 / 校验链路的可观察结果。
- 用户明确要求按 SDD、OpenSpec、规范项目或设计文档流程执行。

默认不进入 SDD：

- 普通问答、只读分析、轻量 prompt 文案优化。
- README、TODO、说明文案、示例、小样式、小型页面体验修正。
- 仅客户端 / 前端调用封装，且不改变 API route、schema、错误码、分页结构或跨端契约。
- 兼容性新增、内部 bug 修复、测试补丁、配置微调，且不改变长期公共契约。

快速修 / 跳过 SDD 规则：

- “快速修”不等于“跳过 SDD”。
- 命中 SDD 硬触发时，默认降为 SDD lite / hotfix，而不是完全跳过。
- 用户明确要求“跳过 SDD”时，只能跳过完整文档流程，不能跳过最小安全门禁：hotfix 记录、相关验证、以及第 7 节要求的评审仍要保留。
- 安全、凭据、存储、权限、OpenAPI 契约、CLI / MCP / AI 外部协议类变更如果用户坚持完全跳过最小门禁，必须停止在实施或交付前，并说明阻塞原因。

data-spec 边界例子：

| 例子 | 判定 | 原因 |
| --- | --- | --- |
| SQL 检查记录前端展示 | 常规模式 | 不改后端契约和存储语义 |
| 仅新增前端 API wrapper 调用既有接口 | 快速 / 常规模式 | 不改 route、schema、错误码 |
| 前端类型引用因后端响应字段变化而更新 | SDD standard | 契约已经变化，需要记录和验证 |
| 数据库连接配置 UI 只读脱敏摘要 / 存在性状态 | 常规模式 | 不展示 raw secret，不存储、不传输真实凭据 |
| 数据库连接凭据展示、存储、传输或反向导入写入数标 | SDD full | 涉及凭据、写入和数据一致性 |
| 修改 MCP tool 入参 / 出参 schema | SDD standard；breaking 或多模块时 SDD full | 外部 AI 协议变化，始终强制独立评审 |
| README 能力描述修正 | 快速模式 | 纯文档 |

## 4. 模式定义

### 快速模式

适用：文档、小文案、小样式、小前端展示、仅客户端调用封装、单点测试补丁，且不命中 SDD 硬触发。

要求：

- 不强制 OpenSpec。
- 不强制独立代码评审子 agent。
- 运行最小有效验证；纯文档可人工检查加 `git diff --check`。
- 纯文档小改默认不 commit，用户要求或需要保留历史时可 commit。

### 常规模式

适用：普通产品功能，影响范围清晰、可回退，不改变长期公共契约、持久化语义或安全边界。

要求：

- 默认不强制 OpenSpec。
- 用 TODO、轻量任务卡或最终回复记录范围、验收点和验证结果。
- 必须结构化自审：需求覆盖、边界、错误处理、类型、测试、文档和无关改动。
- 独立 agent 评审可用时可使用，但不作为门禁。
- 运行受影响模块的测试或构建。

### SDD 模式

适用：命中第 3 节硬触发，或用户明确要求 SDD / OpenSpec。

要求：

- 进入 `SDD.md`，由 `SDD.md` 决定 SDD lite / SDD standard / SDD full。
- 完成与风险匹配的 artifacts、测试、文档和验证证据。
- commit 或 OpenSpec archive 前满足第 7 节评审门禁。

## 5. 术语定义

- 长期公共契约：被前端、CLI、MCP、AI tool、外部脚本或用户文档依赖的稳定输入 / 输出 / 行为。例如 OpenAPI response 字段、MCP tool 参数。
- 可观察契约：用户或调用方能看到并依赖的行为、错误、字段、命令输出或持久化结果。例如 SQL 校验结果字段、CLI JSON 输出。
- 最小有效验证：足以证明本次风险被覆盖的最小命令集合。例如只改前端展示优先 `pnpm test` 或 `pnpm build`，只改 tools 优先 `node --test tools/*.test.mjs`。
- 受影响模块：本次 touched files 所在模块及其直接调用方。例如改 `tools/` 只验证 tools；改 API 契约则验证后端、前端类型和相关 tools。
- 接近全量验证：覆盖所有受影响运行面，通常包括后端、前端、tools、OpenSpec 和 `git diff --check`。
- 多模块：跨 3 个及以上模块，或一次变更超过 8 个文件。
- 高兼容风险：删除 / 重命名字段、改变默认行为、改变错误码、改变命令输出结构、改变外部协议必填项或可能破坏历史数据的变更。
- 公共代码表面：会被跨文件、跨模块、前端 / 后端 / CLI / MCP / AI tool、测试或外部调用方依赖的类、接口、函数、组件、类型、schema、配置、数据结构、数据库对象和 API。

## 6. 代码注释与实现规范

- 新代码必须遵循项目现有目录结构、命名风格、错误处理方式、日志方式、测试组织方式和相似实现。
- 注释解释“为什么这样做”“业务约束是什么”“边界和兼容逻辑是什么”，不要重复描述代码已经清楚表达的“做了什么”。
- 复杂业务规则、非显而易见的状态流转、边界条件、兼容逻辑、异常兜底、降级 / 重试策略、公共函数、导出 API、配置项、数据结构约束和跨模块契约，必须补充必要注释或文档说明。
- 新建或修改公共代码表面时，必须补充对应语言的标准文档注释，说明职责、业务语义、输入 / 输出边界、数据来源、存储或调用约束；这条规则适用于 Java、TypeScript、JavaScript、SQL、YAML / JSON schema、OpenAPI、MCP tool schema 和脚本工具。
- 新建或修改面向 API、数据库、AI、CLI / MCP、配置或前端展示的数据结构时，字段 / 属性 / 枚举值必须有字段级说明或 schema description；字段名看似直观也要说明业务含义、单位、取值范围、是否可空、默认值、兼容约束或脱敏要求中的关键项。
- Java 新增的 `Controller`、`Service`、`Repository`、`Entity`、`DTO`、`record`、`enum`、配置类、请求 / 响应对象和示例类必须使用 Javadoc 或等价注释说明类职责；公开方法、查询方法和非显而易见字段必须说明用途和边界。例如 `StandardUsageExample` 的字段、`StandardUsageExampleRepository` 的数据访问职责、`StandardUsageExampleController` 的接口职责均不得缺省注释。
- TypeScript / JavaScript / Vue 新增的导出类型、接口、组件 props / emits、composable、API wrapper、Pinia store 状态、工具函数和协议对象必须使用 TSDoc、字段注释或 schema description 说明公共语义。
- SQL 迁移、表、列、索引、约束和枚举字典新增或调整时，优先补数据库 `COMMENT` 或项目约定的字段说明；OpenAPI / MCP / AI tool schema 变更必须同步补 `description`，确保 AI 能稳定理解字段标准。
- 与安全、权限、数据一致性、幂等性、事务、并发、存储写入、用户可见结果相关的关键逻辑，必须说明关键约束或设计取舍。
- 简单赋值、直观变量、常规框架样板代码不强制注释；若需要注释才能读懂，应优先重命名、拆分函数或收窄作用域。
- 不要求给每一行代码写注释；但公共代码表面缺少职责说明或字段语义说明时，视为实现未完成，评审和自查必须拦截。
- 不为假设中的未来需求增加框架式抽象；只有当前需求真实需要，或同类模式已经稳定出现，才新增抽象。

## 7. 验证与评审门禁

候选验证命令按 touched files 和风险选择，full 才默认接近全量：

- 后端：`mvn test`。
- 前端：在 `dataspec-web` 运行 `pnpm test`、`pnpm build`。
- CLI / MCP / tools：`node --test tools/*.test.mjs`。
- OpenSpec：`openspec validate <change-id> --strict`；full 收口时运行 `openspec validate --all`。
- 通用检查：`git diff --check`；存在暂存内容时同时运行 `git diff --cached --check`。

评审门禁：

| 场景 | 交付 / commit / archive 前要求 |
| --- | --- |
| 快速模式 | 自查即可 |
| 常规模式 | 结构化自审；agent 可选 |
| SDD lite | 结构化自审；agent 可选 |
| SDD standard | 结构化自审；高风险、跨 3 个模块或超过 8 个文件时用 agent |
| SDD full | 必须独立 agent 评审 |
| 安全、凭据、存储、权限、OpenAPI 契约、CLI / MCP / AI 外部协议 | 必须独立 agent 评审 |
| `AGENTS.md` / `SDD.md` 项目规范变更 | 修改后必须独立 agent 只读复评 |

强制 agent 门禁不可降级；agent 不可用、失败、超时或返回不完整时，禁止交付、commit 或 archive，并记录阻塞原因。非强制场景才允许结构化自审降级。

项目规范自修改规则：

- 修改 `AGENTS.md` 或 `SDD.md` 后，最终交付、commit 或 archive 前必须启动独立子 agent 做只读复评。
- 该复评门禁由执行规范修改的主 agent 触发一次；被派出的只读复评子 agent 不再递归触发同类复评。
- 复评重点至少覆盖：规则自洽性、流程效率、安全 / Git 边界、是否引入个人路径或全局配置依赖、是否新增冲突或模糊条款。
- 子 agent findings 必须修复，或在最终回复中明确记录暂不处理的技术理由。
- 若子 agent 工具不可用、线程满或失败，先用当前会话可调用的等价关闭工具关闭已完成或不再需要的子 agent 后重试；仍不可用时停止在交付 / commit / archive 前并说明阻塞原因。

子 agent 生命周期规则：

- 每次创建子 agent 后必须记录 agent id 和用途；记录位置为最终回复、`Verification Evidence` 或当前任务的等价验证记录。
- 子 agent 完成、超时、失败或不再需要时，必须调用当前会话可用的子 agent 关闭工具（例如 `close_agent`）释放线程位。
- 如果关闭工具调用失败，记录 agent id、用途和失败原因；通过重新检查 agent 状态确认已完成或不再需要的 agent 均已关闭，或失败 agent 已是 `already closed` / `not found` 等无需再清理状态前，不得继续创建新的非必要子 agent。
- 如果当前环境没有可调用的子 agent 关闭工具，必须在最终回复或 `Verification Evidence` 中记录 agent id、用途和无法关闭原因；强制评审场景仍需先说明阻塞，再按用户最新指令决定是否继续。
- 创建新子 agent 前，如果遇到线程上限，先关闭已完成 agent，再重试创建。
- 不保留无用途的已完成子 agent；避免因为未关闭导致后续评审或并行任务失败。

## 8. 文档更新节奏

- 只更新本次变更直接影响且已存在的权威文档。
- 用户可见能力、启动方式、命令、接口或核心边界变化时更新 `README.md`。
- 待办、优先级或状态变化时更新 `TODO.md`。
- 长期取舍、方案原因或后续影响需要沉淀时更新 `DECISIONS.md` 或项目已有等价文档；不存在时不强行创建。
- OpenSpec artifacts 是 SDD 变更的主记录；快速和常规模式不为了流程完整性强行创建 OpenSpec。

## 9. Git 提交与 push

commit 规则：

- 只读、建议、方案和评审任务不 commit。
- 除快速模式纯文档小改外，完成代码或 SDD 修改并通过必要验证，且满足对应 OpenSpec 与评审门禁后，默认创建本地 commit。
- 仓库状态安全才 commit。安全 = 仅本次任务相关文件有变更，或可精确 stage 本次变更；同文件混有无法拆分的用户改动则不 commit。
- commit 只 stage 本次任务产生的变更；使用显式 pathspec 或 hunk stage。
- commit 前固定核对 `git status --short`、`git diff --check`、`git diff --cached --check`、`git diff --cached --stat` 和必要的 `git diff --cached`。
- commit 前至少扫描 staged diff 和 staged 文件名中的常见敏感项：`password`、`passwd`、`token`、`secret`、`authorization`、`api_key`、`apikey`、`jdbc:`、`dsn`。
- commit message 使用轻量 Conventional Commits：标题格式为 `<type>: <中文说明>`，可按需使用 `<type>(<scope>): <中文说明>`；标题用中文概括用户可理解的变更结果。
- 常用 `type` 取值：`feat` 新增或扩展用户可见能力，`fix` 修复缺陷，`docs` 文档，`test` 测试，`refactor` 不改变行为的重构，`chore` 日常维护，`build` 构建或依赖，`ci` 持续集成，`perf` 性能优化，`style` 代码格式或样式，`revert` 回滚。
- 纯规范、流程、任务清单等项目管理类文档优先使用 `docs`；只影响开发工具、脚本或仓库维护且不属于用户文档时使用 `chore`。
- breaking change、安全边界、凭据、存储语义、API / CLI / MCP / AI 外部协议变化，除使用合适 `type` 外，commit 正文必须说明影响范围、兼容策略和关键验证证据。

push 规则：

- 不主动 push。
- 用户要求 push 时，先确认当前 branch、remote 和将要推送的 commit。
- 禁止 force push、tag push、多个 remote 同时 push，除非用户逐项明确要求。
- push 前必须确认工作区、暂存区、commit 内容和 secrets 检查安全；至少扫描将要推送的 commit range 或当前 `HEAD` 相关 diff。

## 10. 安全与权限边界

- 默认只在仓库工作区内执行本地开发 / 测试命令。
- 涉及系统文件、注册表、用户级配置、批量删除、破坏性 Git、Docker prune / volume 删除、云资源、生产或远端服务写入、数据库写入 / 迁移、凭据文件读写时，先停下并确认。
- 包管理器执行未知 install / postinstall 脚本、修改全局环境变量、访问真实服务或写入用户目录，也视为高风险外部副作用。
- 不得提交真实 `.env`、密码、token、私钥、生产 DSN 或含凭据的数据库连接串。
- UI、日志、测试快照、README 和 OpenSpec 示例默认脱敏 password、token、secret、Authorization、JDBC URL、DSN。
- 涉及真实、可逆或可复制凭据的展示、存储、传输、导入导出均按安全 / SDD full 处理；脱敏摘要、存在性状态、不含 raw secret 的只读展示可按常规模式处理。
- 涉及敏感信息的 commit 前，至少用 `rg` 检查常见敏感字段；可用 secrets scanner 时优先使用。
- 非必要不联网；涉及最新版本、价格、法规、新闻、依赖文档等易变化信息时再 web verify。
- 按任务需要使用当前会话已提供且可调用的 skills / plugins；用户点名的能力优先加载。
- 前端界面、交互、可用性、可访问性、响应式、组件体验、信息层级、图表和视觉质量相关任务，默认优先使用 `ui-ux-pro-max`。`frontend-design` 仅在需要高辨识度品牌页、营销页、作品集、视觉概念探索，或用户明确点名时作为补充使用。
