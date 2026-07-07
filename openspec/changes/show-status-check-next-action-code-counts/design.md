## Context

`buildStatusReport()` 已在 `summary.issueCodes[]` 中提供按首次出现顺序聚合的 `{code,count,severity}`。`buildNextActions()` 仍直接从 `issues[]` 去重生成纯 code 列表，导致下一步建议缺少问题规模和最高严重级。

## Goals / Non-Goals

**Goals:**

- 在 `nextActions[]` 的当前问题编码建议中展示每个 code 的 `count` 和最高 `severity`。
- 复用现有 issue code 聚合函数，确保 summary 和 next action 使用同一来源。
- 保持 `status`、退出码、`issues[]`、`checks[]` 和前两条行动建议不变。

**Non-Goals:**

- 不改变 issue 生成规则、code 命名或 check 归类。
- 不改变 `summary.issueCodes[]` 的 shape 或顺序。
- 不新增 CLI 参数或新的状态检查输出格式。

## Decisions

1. **复用 `buildIssueCodeSummary(issues)`。**
   - 原因：summary 与 next action 应对同一组 issues 得出一致 count/severity，避免二次实现漂移。
   - 备选方案：在 `buildNextActions()` 内继续用 `Set` 去重并单独计数。放弃原因是会复制聚合规则，未来 severity 规则变化时容易漏改。

2. **只增强第三条 next action。**
   - 原因：前两条建议承担处理策略和误报排查提示，保持不变能降低兼容风险；第三条本来就是 code 摘要，适合扩充详情。

## Risks / Trade-offs

- [Risk] 下游对第三条 next action 做精确字符串匹配。→ Mitigation：保持前缀“当前问题编码：”不变，并只兼容追加 count/severity 详情。
- [Risk] 文本变长影响可读性。→ Mitigation：仅展示聚合后的 unique code，不重复列出每条 issue。
