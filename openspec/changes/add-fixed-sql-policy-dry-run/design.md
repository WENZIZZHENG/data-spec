## Context

当前 SQL lint 已输出 `fixedSql`、`fixedSqlDiff` 和 `LintIssue.suggestion/replacement/before/after/confidence`，并把 issues JSON 保存到 SQL 检查记录。`FixedSqlGenerator` 直接从 `LintResult.issues` 重建 PostgreSQL 风格 SQL，但没有暴露哪些 issue 真的参与了重建、哪些被风险或规则策略跳过，也没有请求级 dry-run 语义。P6-35 的读者包括前端用户、CLI/MCP/AI agent 和后续文件级 patch 能力。

## Goals / Non-Goals

**Goals:**

- 保持旧请求默认 fixedSql 行为不变。
- 支持请求级 `fixPolicy`，让 AI 或前端可以选择 `GENERATE`、`DRY_RUN` 或 `DISABLED`，并按风险上限和规则 allow/deny list 过滤 fixer。
- 返回机器可读修复计划：变更类型、规则、风险、状态、before/after、解释和跳过原因。
- 让检查记录通过已保存的 issues JSON 保留每个 issue 的 fixer 风险和策略状态。
- 前端 SQL 校验页展示策略控件、风险摘要和变更列表。

**Non-Goals:**

- 不自动写回业务仓库文件；文件级 patch 留给 P6-78。
- 不做项目级持久修复策略、审批流或复杂权限。
- 不新增数据库表字段；本轮依赖 `fixedSql` 与 issues JSON 保留必要信息。
- 不实现语义高风险类型改写；金额类型、字段类型迁移仍只给建议。

## Decisions

1. **请求级 `FixPolicy`，默认兼容旧行为。**

   `LintController.LintRequest` 新增可选 `fixPolicy`。后端缺省为 `mode=GENERATE`、`maxRiskLevel=MEDIUM`、解释开启、无规则过滤。旧客户端不传策略时仍返回 `fixedSql`。

2. **风险分级先覆盖已有确定性 fixer。**

   表名/字段名重命名标为 `LOW`，必备列补充标为 `MEDIUM`；类型变更类规则暂不自动改写，返回 `SKIPPED` 解释。策略 `maxRiskLevel=LOW` 可形成 safe-only 输出。

3. **`FixedSqlGenerator` 返回 plan，而不是只返回字符串。**

   新增 `FixedSqlPlan`/`FixChange`/`FixPlanSummary` 模型。生成器先为每个可修复 issue 计算候选变更和风险，再按策略筛选，最后用允许的变更重建 SQL。无法安全重建时返回失败解释，不给危险 SQL。

4. **把 issue 级策略状态写回 `LintIssue`。**

   在 `LintIssue` 上新增 `fixRiskLevel`、`fixChangeType`、`fixStatus`、`fixExplain`。这样 SQL 检查记录不需要新增列，历史详情也能读到本次策略状态；`LintResult.fixChanges` 仍服务当前响应和前端聚合展示。

5. **dry-run 是“预览但不鼓励应用”。**

   `mode=DRY_RUN` 仍计算 `fixedSql` 和 diff，方便前端展示具体影响，但返回 `fixDryRun=true`、next action 提示人工确认；`mode=DISABLED` 不计算 `fixedSql`，只返回可修复项和跳过原因。

## Risks / Trade-offs

- [Risk] 不新增 `fix_plan_json` 会让记录列表无法直接展示完整 plan 摘要。→ Mitigation：记录详情已有 issues JSON，可展示 issue 级风险和状态；完整结构化 plan 只保证当前 lint 响应。
- [Risk] 重建式 fixedSql 不是文本补丁，可能改变格式。→ Mitigation：继续保留 dialect diagnostics 和 diff，并明确 dry-run/人工确认。
- [Risk] 策略字段增加后 OpenAPI/前端类型漂移。→ Mitigation：更新 schema.ts、AI contract 文档和前端 smoke 测试。
- [Risk] 高风险 fixer 未来增加后默认策略可能过宽。→ Mitigation：默认 `maxRiskLevel=MEDIUM`，新增 HIGH fixer 必须显式测试并在 UI 标记。
