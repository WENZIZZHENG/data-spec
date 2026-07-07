## Context

`formatStatusReportText(report)` 目前输出状态、TODO 数、近期队列、active changes、总问题数、问题明细和下一步。随着 `checks[]` 增加 `errorCount` / `warningCount`，JSON 消费者可以快速看出哪个检查项只有 warning，但 text 输出仍缺少这层摘要。

## Goals / Non-Goals

**Goals:**

- 在 text 输出中展示每个 check 的状态和 severity count。
- 使用已有 `report.checks[]`，不重新归类 issue。
- 保持问题明细和下一步建议的现有位置与内容基本稳定。

**Non-Goals:**

- 不改变 JSON 输出字段或语义。
- 不隐藏问题明细，也不新增过滤参数。
- 不改变 status-check 的退出码。

## Decisions

1. **在总问题数后、问题明细前输出“检查项”段。**
   - 原因：先给读者一个 check 级概览，再展开 issue 明细，符合排查顺序。
   - 备选方案：把 count 混进问题明细。放弃原因是 issue 明细按单条问题展开，无法自然表达无问题或 warning-only 的 check。

2. **输出 id 而非只输出中文 name。**
   - 原因：`id` 是稳定机器可读标识，便于 agent 和脚本从 text 中定位检查项；中文 name 仍保留给人读。

## Risks / Trade-offs

- [Risk] text 输出多出一段，依赖精确全文匹配的脚本可能观察到变化。→ Mitigation：text 输出不是 JSON 契约；新增段落不改变 JSON shape、退出码或已有问题明细。
- [Risk] 行内容过长。→ Mitigation：每个 check 一行，字段固定且紧凑，当前 check 数量有限。
