## Context

DataSpec 已有 `dataspec-todo-openspec-handoff.mjs` 负责把 TODO 转成 OpenSpec 草稿，也有 `dataspec-verify-advisor.mjs` 负责根据变更路径推荐验证命令。P6-177 补的是二者之间的开工前质量门禁：判断一个 change 是否足够清楚，能不能交给 AI 或人工继续实施。

## Goals / Non-Goals

**Goals:**
- 对一个 repo-local active OpenSpec change 输出确定性的准备度报告。
- 报告包含分数、等级、缺失事实、影响规格、验证计划、评审边界、风险标记、需要人工确认的问题和下一步动作。
- 支持 `--format json|text`，JSON 字段稳定，便于 AI 读取。
- 检查只读，不执行 `openspec validate`、不修改 artifacts、不自动实现、不归档。

**Non-Goals:**
- 不替代人工判断，不把低分 change 作为强制阻断。
- 不做可配置规则引擎，不引入 OPA/Spectral/Redocly 依赖。
- 不扫描 archived change，不跨仓库读取 linked workspace。

## Decisions

1. **新增独立工具，不塞进主 CLI。**
   - 脚本名：`tools/dataspec-openspec-readiness.mjs`。
   - 原因：第一版面向开发/AI 自检，独立脚本更轻，避免扩大 `dataspec-cli.mjs` 的外部命令契约。

2. **评分采用扣分模型并保留明细。**
   - 起始 100 分，按缺 artifacts、缺核心内容、占位、缺验证命令、缺 spec delta、缺边界、缺人工确认等规则扣分，最低为 0。
   - 输出每条 check 的 `id/status/weight/message/file/line`，让分数可解释。

3. **等级只做提示。**
   - `READY`：80 分及以上且没有 error 级缺口。
   - `NEEDS_REVIEW`：50-79 分或只有 warning 级缺口。
   - `BLOCKED`：低于 50 分或缺少关键 artifact。
   - 工具自身退出码：能生成报告就返回 0；找不到 change 或参数错误返回 2。低分不作为失败退出码，避免把提示工具变成硬门禁。

4. **验证计划优先从 artifacts 提取，再给默认建议。**
   - 从 `tasks.md` 和 `proposal.md` 中提取反引号包裹的验证命令。
   - 对 active change 默认补 `openspec validate <change-id> --strict` 和 `git diff --check` 建议。
   - 不执行命令，只输出建议。

## Risks / Trade-offs

- [Risk] 静态规则可能误判“短但足够清楚”的 change。→ Mitigation：诊断为建议性质，输出 `humanQuestions` 供用户确认。
- [Risk] 规则过多会变成治理负担。→ Mitigation：第一版只检查 OpenSpec 实施前最常见的缺口，不做可配置策略。
- [Risk] 生成的 `validationPlan` 可能不完整。→ Mitigation：明确它是建议入口；真正验证仍按 AGENTS/SDD 和 `dataspec-verify-advisor` 选择。
