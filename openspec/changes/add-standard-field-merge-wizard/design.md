## Context

DataSpec 目前已有三条相关能力：

- 字段冲突检测：只读发现重复、别名冲突、语义相近和命名风险，不写字段库。
- 标准候选 Inbox：候选可“合并到已有字段”，但只记录候选决策，不修改目标字段内容。
- 字段生命周期：正式字段支持 `deprecated` 和 `replacementFieldId/replacementReason`，并通过变更日志记录更新。

P6-65 要补的是正式字段之间的受控合并。它改变标准字段可见结果和 AI Context，因此必须先 preview，再显式 apply。

## Goals / Non-Goals

**Goals:**
- 提供 merge preview：展示保留字段、来源字段、合并后的字段摘要、字段级 diff、风险、影响对象和回滚提示。
- 提供 merge apply：只自动迁移安全可合并的 aliases/tags，并把来源字段标记为 `deprecated`，指向保留字段；example/format/source 摘要仅在 preview/result 中提示人工审阅。
- 支持从字段冲突页面或字段库进入向导，用户必须填写合并原因并确认风险。
- 响应对 AI 可读：包含 `kind/schemaVersion/nextActions/rollbackHints`，不包含密码、token 或业务数据行。

**Non-Goals:**
- 不自动选择并应用合并；只给推荐保留字段和迁移建议。
- 不删除来源字段，不删除变更日志，不做跨项目合并。
- 不静默覆盖冲突属性，例如 dataType、nullable、codeSetId、sensitive、format constraints；这些只作为 risk 展示。
- 不重写业务仓库 SQL/DTO，不修改源数据库 COMMENT。

## Decisions

1. **第一版只支持单来源字段合并到单保留字段。**
   - 这样能覆盖“两个重复字段合并为一个”的主验收，避免一次多源合并带来事务、回滚和冲突解释复杂度。
   - 后续可在同一契约上扩展 `sourceFieldIds[]`。

2. **来源字段废弃，不物理删除。**
   - apply 后来源字段 `status=deprecated`，`replacementFieldId=targetFieldId`，`replacementReason` 记录用户原因和合并摘要。
   - AI Context 可通过 status/replacement 识别历史别名，用户也能按变更日志回退字段状态。

3. **只迁移可安全合并的集合型 metadata。**
   - aliases/tags 采用去重合并；example/format/source 先以 preview 和 note 暴露，避免覆盖目标字段的权威定义。
   - 当来源字段的 name/displayName 与目标不同，来源 name/displayName 会建议加入 aliases，而不是改目标 name。

4. **变更日志是第一版回滚边界。**
   - apply 对目标字段和来源字段分别记录 update 日志，响应返回 rollbackHints，指向已有字段回退能力。
   - 不承诺一次点击撤销整个 merge transaction；先提供可追溯的双字段回退线索。

## Risks / Trade-offs

- [Risk] 合并可能误废弃仍在使用的字段。→ Mitigation：preview 暴露影响对象、来源批次和风险；apply 要求 reason，且不删除来源字段。
- [Risk] alias 迁移可能制造新冲突。→ Mitigation：preview 检测 alias owner 冲突；有阻断风险时 apply 拒绝或要求先处理冲突。
- [Risk] AI Context 短期仍可能看到 deprecated 字段。→ Mitigation：响应和文档要求标明 replacement；后续可在 AI Context 裁剪阶段默认降权 deprecated 字段。
- [Risk] 来源迁移语义复杂。→ Mitigation：第一版以 source summary 和 replacement 关系保留可追溯性，不移动或删除原始来源记录。

## Validation Strategy

- 后端测试覆盖 preview diff、推荐保留字段、alias/tag 去重、阻断风险、apply 后目标字段和来源字段状态、变更日志记录、项目隔离。
- 前端测试覆盖向导入口、preview 风险展示、确认理由、apply 调用和完成状态。
- OpenSpec strict、后端目标测试、前端目标测试、必要全量验证、独立子 agent 评审。
