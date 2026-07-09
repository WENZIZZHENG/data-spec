## Context

DataSpec 已有 `index-refs` 能在业务仓库配置路径内生成字段引用索引，也有 `fixed-sql patch` 能输出带 hash、diff、确认命令和回滚提示的补丁计划。P6-179 需要把这两类能力连接起来：字段标准变化时，AI 不只看到引用位置，还能拿到可审查的候选修改计划。

这个能力运行在用户本地业务仓库内，核心输入是字段名、别名、扫描路径和变更意图。服务端 HTTP API 第一版不适合作为默认落点，因为后端通常没有业务仓库文件访问权限，强行让后端读本地路径会扩大安全边界。第一版交付 CLI 和可复用本地工具 API，后续 MCP 或 HTTP API 可复用同一计划构建函数。

## Goals / Non-Goals

**Goals:**
- 为字段重命名、类型变化和枚举变化生成只读 Patch Plan。
- 输出稳定 JSON 和人类可读 Markdown，便于 AI、用户和后续 OpenSpec 任务消费。
- 复用字段引用索引的 bounded scan、路径范围、文件分类和敏感信息脱敏。
- 对候选修改给出文件位置、建议动作、风险等级、dry-run diff 或人工步骤、验证命令和回滚提示。
- 在契约 fixture 中记录命令形状、输出字段、安全元数据和示例，防止 AI 入口漂移。

**Non-Goals:**
- 不自动写业务仓库文件，不提供 `--apply`。
- 不保证覆盖动态 SQL、反射、运行时拼接字段或框架生成代码。
- 不在第一版新增后端 HTTP API，不让服务端读取本地业务仓库路径。
- 不引入 OpenRewrite、ast-grep 或外部 codemod 依赖；第一版先做可解释的确定性计划。

## Decisions

1. 新增独立工具模块承载 Patch Plan 构建逻辑。

   方案：新增 `tools/dataspec-code-patch-plan.mjs`，导出 `buildCodeFieldPatchPlan()` 和 Markdown formatter。该模块调用 `buildCodeFieldReferenceIndex()` 获取引用，再把引用映射为 `candidateEdits[]`、`manualSteps[]`、`verificationCommands[]` 和 `nextActions[]`。

   原因：`dataspec-code-refs.mjs` 保持“索引扫描”单一职责，CLI、测试和后续 MCP/API 可复用新的计划函数，不把计划输出塞进 `dataspec-cli.mjs` 的大函数里。

   备选：直接扩展 `index-refs` 输出 Patch Plan。放弃原因是索引和计划的用户意图不同，混在一起会让现有契约膨胀并影响兼容性。

2. CLI 使用 `code-patch plan` 子命令。

   方案：命令形态为 `code-patch plan --field <name> [--to-field <new>] [--from-type <type> --to-type <type>] [--enum-change <old=new> ...] [--alias <alias|field=alias> ...] [--path <file|dir> ...] [--format json|markdown]`。至少需要提供一个变更意图：重命名、类型变化或枚举变化。

   原因：`code-patch` 为后续 `plan` 以外的动作保留命名空间，但第一版只实现 dry-run plan；选项名直观，能被 AI 从字段变更描述稳定映射。

   备选：命令名使用 `field-change plan`。放弃原因是 TODO 和产物强调业务代码 patch，`code-patch plan` 更贴近用户要审查的对象。

3. Patch Plan 是“候选修改计划”，不是可直接应用的补丁。

   方案：重命名场景为高置信引用生成 `dryRunDiff`，展示单行或小片段替换意图；类型和枚举变化默认生成 `manualSteps` 和候选文件提示，不伪造无法可靠验证的结构化改写。所有候选都包含 `confidence`、`riskLevel`、`requiresHumanReview` 和 `reason`。

   原因：字段重命名可以做确定性 token 替换预览，但类型和枚举往往涉及 Java 类型、数据库方言、序列化、测试 fixture 和迁移策略，第一版若自动生成完整 diff 容易误导 AI。

   备选：为 Java/SQL/JSON 分别做 AST/codemod。放弃原因是会扩大依赖和误改风险，不符合“最小可用闭环优先”。

4. 风险和验证命令从引用类型与变更类型推导。

   方案：SQL、DDL、migration、mapper 高置信引用默认 `HIGH`；模型和配置默认 `MEDIUM`；文本提及默认 `LOW`。类型变化和枚举变化会提高人工确认等级。验证命令第一版给出本地可执行建议，例如 `node tools/dataspec-cli.mjs code-patch plan ... --format json`、项目测试命令占位说明和重新运行 `index-refs`。

   原因：AI 需要先处理高风险文件，也需要知道计划审查后应跑什么验证；但不同业务仓库测试命令无法由 DataSpec 准确推断，只能输出建议和人工确认点。

5. 输出默认脱敏且只用相对路径。

   方案：复用现有 secret-like 文本脱敏规则，`fileRef.path`、scan path 和 Markdown 都只输出相对路径；`safety` 明确 `readOnly=true`、`writesProject=false`、`requiresDryRun=true`、`externalNetworkUsed=false`、`externalLlmUsed=false`。

   原因：Patch Plan 会被复制给 AI 或写入证据包，不能泄漏 token、password、Authorization、完整 JDBC URL、DSN 或连接串。

## Risks / Trade-offs

- [动态 SQL 或运行时代码未识别] → 输出 `diagnostics` 和 `manualSteps` 明确第一版局限，建议用户扩大 `--path` 并运行业务测试。
- [rename diff 被误认为可自动应用] → 命令不提供 `--apply`，输出 `dryRunResult.willWrite=false` 和 `requiresHumanReview=true`。
- [类型/枚举变化缺少精确 diff] → 以人工步骤和候选文件为主，不伪造确定性补丁；后续可按 Java/SQL/JSON 场景逐步增强。
- [服务端 API 缺席被误解为能力缺口] → proposal 和 specs 说明第一版本地工具 API 是稳定复用边界，HTTP API 需另行设计本地文件访问安全模型。
- [大仓扫描成本] → 沿用 `index-refs` 的 defaultPaths、显式 `--path` 和生成目录跳过策略，不扫描整个仓库。

## Migration Plan

1. 添加 OpenSpec specs 与任务清单。
2. 先写 `code-patch plan` 的 CLI/工具测试并确认红灯。
3. 新增本地 Patch Plan 模块，接入 CLI 和 Markdown formatter。
4. 更新契约 fixture 与 fixture 校验。
5. 运行 OpenSpec、tools 测试、契约检查和 diff 检查。
6. 通过独立子 agent 评审后归档 OpenSpec，并把 P6-179 状态写回 TODO。

回滚策略：删除新增 CLI 分支、工具模块、测试、fixture 和 delta specs；由于第一版不写业务文件、不改数据库 schema、不新增 HTTP API，回滚不需要数据迁移。

## Open Questions

- 后续是否需要服务端 HTTP API 取决于业务仓库文件访问模型，第一版不解决。
- 后续是否要支持自动 apply 需要单独的安全设计、确认 hash、幂等和回滚证据，本次不预留可执行写入入口。
