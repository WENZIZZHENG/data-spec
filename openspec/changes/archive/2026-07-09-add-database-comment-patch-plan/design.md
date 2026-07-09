## Context

DataSpec 目前已经有数据库直连 metadata、schema dump、schema change plan、反向导入预览和前端数据库直连流程。现有 schema change plan 能覆盖更宽泛的结构变更，但 P6-183 需要一个更窄的 COMMENT 回写计划：只比较表/列注释，只输出审阅用 dry-run SQL 和证据，不执行源库写入。

本变更跨后端 API、CLI、前端和工具契约，属于 SDD standard。由于新增 CLI/API 外部契约，提交或归档前按项目门禁执行独立子 agent 只读评审。

## Goals / Non-Goals

**Goals:**
- 生成项目级、只读的 `commentPatchPlan`，覆盖 PostgreSQL/MySQL 表注释和列注释差异。
- 输出 `currentComment`、`targetComment`、`commentDiff`、`dryRunSql`、`dialectSupport`、`riskLevel`、`rollbackHint`、`evidence` 和 `nextActions`。
- 复用现有数据库 metadata 读取、缓存/fingerprint、项目访问控制和脱敏边界。
- 提供 API、CLI JSON/text 和反向导入页预览入口。
- 默认不写源数据库、不写 DataSpec 字段库、不保存连接凭据。

**Non-Goals:**
- 不执行 COMMENT SQL，不新增 apply/confirm 写库流程。
- 不处理表重命名、字段重命名、索引、类型、nullable、default 或数据迁移。
- 不扫描源库业务数据行，不采样真实数据。
- 不保存数据库密码、完整 JDBC URL、DSN 或连接串。

## Decisions

1. **新增专用 comment plan API，而不是扩展 schema change plan。**
   选择新增 `/api/reverse-import/database/comment-plan`，因为 COMMENT 回写是低风险但高频的审阅流，输出字段和安全边界比通用 schema 迁移更窄。schema change plan 继续保留更宽泛的结构预览，comment plan 可复用其 metadata 和 SQL escape helper。

2. **计划只读，执行权留给用户。**
   后端只返回 dry-run SQL 和 JSON 证据；CLI 和前端均不得执行 SQL。计划包含 `planHash` 和 `rollbackHint`，便于用户把 SQL 交给人工 migration 流程或后续显式 apply 变更。

3. **方言能力显式建模。**
   PostgreSQL 表/列注释使用 `COMMENT ON TABLE/COLUMN` 草稿。MySQL 表注释可输出 `ALTER TABLE ... COMMENT = ...`；列注释只有在 metadata 足以安全还原列定义时输出草稿，否则标记 `UNSUPPORTED` 并给出人工处理建议，避免生成破坏列属性的 SQL。

4. **差异状态面向 AI 和前端稳定消费。**
   表/列项统一输出 `status=NO_OP|MISSING|CHANGED|UNSUPPORTED`、`riskLevel=LOW|MEDIUM|HIGH`、`commentDiff` 和 `evidenceRefs`。CLI、前端和证据包使用同一响应，不各自推导语义。

5. **脱敏在输入摘要和输出文本两侧执行。**
   请求可包含连接参数，但响应、错误、SQL 注释文本、evidence 和 CLI stderr 必须经过敏感词清洗；测试覆盖 password、token、Authorization、完整 JDBC URL、DSN 和 connection string。

## Risks / Trade-offs

- **MySQL 列注释 SQL 需要完整列定义** → metadata 不完整时返回 `UNSUPPORTED`，不生成可能改变列类型或约束的 SQL。
- **标准字段与数据库字段匹配不唯一** → 第一版沿用现有字段名/别名匹配和反向导入 metadata 逻辑，遇到歧义时标记人工确认。
- **计划可能被误当成可直接执行迁移** → API/CLI/前端均使用 dry-run 文案、`safety.readOnly=true`、`nextActions` 和 `rollbackHint` 明确需要人工审阅。
- **跨模块契约漂移** → 增加后端、CLI、前端和工具 fixture 测试，并在 `Verification Evidence` 中记录命令结果和子 agent 评审结论。

## Migration Plan

1. 创建 OpenSpec delta 并通过 strict 校验。
2. 先补后端、CLI/tools、前端失败测试，记录 RED。
3. 实现只读 API、CLI 命令和前端预览。
4. 运行后端、前端、tools、OpenSpec 和通用检查。
5. 启动独立子 agent 只读评审；修复或记录 findings。
6. 完成后归档 OpenSpec change，运行 `openspec validate --all`，再按 Git 规则提交。

回滚策略：本变更无数据库迁移和写库副作用；回滚代码即可移除 API/CLI/UI 入口，已有数据不受影响。
