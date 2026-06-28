## Why

`fixedSql` 已能给 AI 和用户一个确定性修复候选，但当前只返回最终 SQL 和 diff，缺少“为什么改、风险多高、哪些规则被跳过、是否只是 dry-run”的机器可读解释。P6-35 需要让自动修复继续保持人工确认边界，同时让 AI 能按安全策略消费 fixedSql。

## What Changes

- `/api/lint` 请求支持可选 `fixPolicy`，包含 `mode`、`riskLevel`、`enabledRuleCodes`、`disabledRuleCodes` 和 `includeExplanations`。
- `LintResult` 新增修复计划输出，返回本次策略、变更列表、不可自动修复原因、dry-run 标记和下一步建议。
- `FixedSqlGenerator` 从“只产出 SQL 字符串”升级为“产出 SQL + 变更计划”，并为表名/字段名/必备列 fixer 标注 `ruleCode`、`riskLevel`、`before`、`after`、`explain`。
- SQL 校验页展示修复策略摘要和变更列表；dry-run 时只展示计划和 diff 预览，不鼓励直接应用。
- CLI/MCP/AI contract 通过新增可选字段保持兼容；旧请求不传 `fixPolicy` 时保持现有 fixedSql 行为。
- 不新增业务写回能力，不自动修改业务仓库文件，不把策略配置持久化为项目级治理流程。

## Capabilities

### New Capabilities

- `fixed-sql-policy`: 请求级 fixedSql 策略、dry-run 输出和修复计划解释。

### Modified Capabilities

- `sql-check-records`: SQL 检查结果与记录详情需要携带可复用的修复计划解释。
- `structured-lint-fixes`: 结构化修复元数据需要标注 fixer 风险和策略解释，供 AI、CLI、MCP 和前端展示。

## Impact

- 后端 lint 模型、controller request、`SqlLintService`、`FixedSqlGenerator`、检查记录 issue JSON、契约/fixture 测试。
- 前端 OpenAPI schema/types、`SqlLint.vue` 策略控件与结果展示。
- README、TODO、OpenSpec 规格和 AI 输出契约文档。
- 不改数据库表结构；不引入外部依赖；旧客户端字段兼容。
