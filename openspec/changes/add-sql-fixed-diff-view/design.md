## Context

现有 `FixedSqlGenerator` 会基于解析表结构和结构化修复建议重建完整 SQL。它适合复制最终结果，但不适合快速审查变更范围。前端当前只展示完整 `fixedSql` 文本，CLI/PR Review 如果直接输出整段 SQL 也不够聚焦。

## Goals / Non-Goals

**Goals:**
- 在 lint 响应中增加 `fixedSqlDiff`。
- diff 使用简单 unified 文本格式，方便 CLI、PR Review 和前端共享。
- 前端用颜色区分新增、删除和上下文行。
- 记录详情可基于已保存的 `originalSql/fixedSql` 重新生成视图。

**Non-Goals:**
- 不做自动覆盖源文件。
- 不做 Monaco side-by-side editor。
- 不引入大型 diff 依赖或复杂 patch 应用。
- 不改变 `fixedSql` 的生成策略。

## Decisions

### 1. 后端输出统一 diff 文本

`SqlLintService` 在生成 `fixedSql` 后调用 `SqlDiffGenerator.generate(originalSql, fixedSql)`，把结果放入 `LintResult.fixedSqlDiff`。这样 CLI/MCP/PR Review 等 JSON 消费方无需各自实现 diff。

### 2. 第一版使用整文件行级 diff

diff 生成器按行做 LCS，输出：

- `--- original.sql`
- `+++ fixed.sql`
- `@@`
- 上下文行以空格开头
- 删除行以 `-` 开头
- 新增行以 `+` 开头

如果 `fixedSql` 为空或与原 SQL 相同，返回 `null`。第一版不做精细 hunk 拆分；SQL 文件通常较短，整文件上下文更利于个人复核。

### 3. 前端轻量渲染

前端把 `fixedSqlDiff` 拆行渲染为代码块，按前缀着色。历史记录详情不要求后端存 diff，直接用当前记录的 `originalSql/fixedSql` 在前端计算同类行级 diff。

## Risks / Trade-offs

- 整文件 diff 在大 SQL 上较长 → 第一版先保持完整上下文，后续可按 hunk 折叠。
- 后端重建 SQL 的格式化差异可能导致 diff 较多 → 这正是当前 `fixedSql` 语义的一部分，页面保留完整修正 SQL供复制。
- 前端和后端各有 diff 逻辑可能轻微不一致 → 当前记录详情只用于展示，核心 API 以 `fixedSqlDiff` 为准。
