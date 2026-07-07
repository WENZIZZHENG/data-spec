## Context

`buildStatusReport()` 当前在顶层 `summary` 中提供 `errors`、`warnings` 和 `totalIssues`，并在 `issues[]` 中保留每条具体问题。`checks[]` 已能按检查项展示 error/warning count，但仍无法直接回答“当前有哪些 issue code、各出现多少次”。

## Goals / Non-Goals

**Goals:**

- 在 `summary.issueCodes[]` 中输出 issue code 聚合。
- 聚合复用现有 `issues[]`，不新增第二套诊断规则。
- 当同一 code 同时出现 error/warning 时，`severity` 取最高严重级，避免低估风险。

**Non-Goals:**

- 不删除或压缩 `issues[]`。
- 不改变 issue code、message、suggestedFix 或 check 归类。
- 不改变 text 输出；text 已可通过问题明细和检查项摘要定位。

## Decisions

1. **把聚合放在 `summary.issueCodes[]`。**
   - 原因：这是报告级摘要，不属于单个 check；放在 summary 便于 AI 先判断问题类型分布。
   - 备选方案：新增顶层 `issueCodes`。放弃原因是 summary 已承载 count 类数据，放在其中更紧凑。

2. **按首次出现顺序输出。**
   - 原因：保持与 `issues[]` 的阅读顺序一致，不引入额外排序规则。

## Risks / Trade-offs

- [Risk] 下游误把 `issueCodes[]` 当作唯一问题来源。→ Mitigation：文档说明它是摘要，具体文件、行号和建议仍以 `issues[]` 为准。
- [Risk] 同 code 混合 severity 时信息损失。→ Mitigation：`severity` 采用最高严重级，`count` 仍保留总数；当前项目 issue code 按设计通常对应固定 severity。
