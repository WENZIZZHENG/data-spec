## Context

`tools/dataspec-status-check.mjs` 先汇总 `issues[]` 得到顶层 `status`，再通过 `buildNextActions(status, issues)` 给 AI 下一步建议。当前实现只区分 `pass` 与非 `pass`，导致 `warn` 状态也输出“优先修复 severity=error”，而真实仓库常见状态是只有 active OpenSpec warning、没有 error。

## Goals / Non-Goals

**Goals:**

- 让 `nextActions[]` 的首条建议反映当前最高 severity。
- 在 warning-only 状态下避免提及不存在的 error。
- 保持现有 JSON 字段、退出码和 issue 编码不变。

**Non-Goals:**

- 不新增新的 severity、issue code 或 check。
- 不改变 active OpenSpec change 是否为 warning 的判定。
- 不归档现有 active changes。

## Decisions

1. **在 `buildNextActions()` 内基于 `issues[]` 计算最高严重级。**
   - 原因：`issues[]` 是现有报告事实来源，复用它可以避免额外状态与 summary 漂移。
   - 备选方案：调用方传入 `summary.errors/warnings`。放弃原因是目前函数签名已有 `issues`，由函数内部计算更局部。

2. **只调整首条行动建议，保留后两条通用建议。**
   - 原因：后两条“脚本误报先补 fixture”和“当前问题编码”仍适用于 warning-only 场景；最小改动能降低兼容风险。

## Risks / Trade-offs

- [Risk] 下游如果用完整字符串匹配旧文案，可能观察到文本变化。→ Mitigation：`nextActions[]` 本身是给 AI 的建议文本，不改变稳定字段或退出码；测试锁定 warning-only 的新语义。
- [Risk] 未来新增 severity 时没有对应文案。→ Mitigation：当前只存在 error/warning，函数仍以 `status === 'pass'` 作为 clean 分支，其余未知情况会回到非 pass 建议路径。
