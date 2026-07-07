## Context

`tools/dataspec-status-check.mjs` 当前返回顶层 `summary.errors` / `summary.warnings`，同时在 `checks[]` 中按检查项返回 `id`、`name`、`status` 和 `issueCount`。`status` 只在存在 error 时为 `fail`，warning-only check 仍是 `pass`，这保留了“无阻塞错误”的兼容语义，但不利于 AI 快速定位 warning 分布。

## Goals / Non-Goals

**Goals:**

- 为每个 check 增加 `errorCount` 和 `warningCount`。
- 不改变顶层 `status`、退出码、`issueCount` 和单项 `status` 的既有语义。
- 用现有 `issues[]` 归类逻辑计算 severity count，不新增第二套规则。

**Non-Goals:**

- 不把 warning-only check 的 `status` 改为 `warn`。
- 不归档或清理 existing active OpenSpec changes。
- 不新增文档状态检查项。

## Decisions

1. **采用兼容字段，而不是修改 `checks[].status`。**
   - 原因：`status=pass` 目前表达“该 check 没有 error”，直接改成 `warn` 可能影响已有脚本。
   - 备选方案：把单项状态扩展为 `pass/warn/fail`。放弃原因是更像契约语义变更，不适合当前小步优化。

2. **在 `buildChecks()` 内统一统计 severity。**
   - 原因：`codeBelongsToCheck()` 已经是 issue 到 check 的唯一归类入口，复用它可以避免统计和显示逻辑漂移。

## Risks / Trade-offs

- [Risk] AI 仍可能只看 `status`。→ Mitigation：新增 count 字段后，测试和后续文档可以引导 AI 读取 `warningCount` / `errorCount`。
- [Risk] 未来 check 数量增加时忘记统计。→ Mitigation：统计逻辑基于统一 definitions，不需要每个 check 单独维护。
