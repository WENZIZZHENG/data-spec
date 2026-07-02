## Context

P6-51 聚焦字段标准对 AI 的“可采用性”。当前 `ds_field.status` 只有 `enabled/disabled/deprecated`，字段推荐会跳过 disabled 但仍可能用降权方式推荐 deprecated；字段质量评分只能从注释/别名文本猜替代说明；AI Context 也没有稳定的替代字段结构。

第一版采用最小数据模型扩展：保留已有 `status`，新增 `draft` 状态和两个可选字段 `replacementFieldId`、`replacementReason`。这让个人维护字段时不需要审批流，也能给 AI 足够明确的采用边界。

## Goals / Non-Goals

**Goals:**
- 定义轻量生命周期状态和可观察契约：`draft`、`enabled`、`deprecated`、`disabled`。
- 为废弃/停用/草稿字段提供结构化替代字段或替代原因。
- 字段推荐默认只推荐 enabled 字段；字段检索、AI Context 和质量评分展示同一套生命周期信息。
- 字段状态和替代信息变更继续进入现有变更日志和标准快照。

**Non-Goals:**
- 不做审批流，不做组织级发布治理，不阻止个人快速维护标准。
- 不引入发布版本流、审批人、状态流转申请或组织级治理。
- 不强制每个 deprecated/disabled 字段都必须填写替代字段；第一版允许只填替代原因，质量评分负责提示缺口。

## Decisions

1. **沿用 `status` 字段，新增 `draft` 状态。**
   - change id: `add-field-lifecycle`
   - capability: `field-lifecycle`
   - 原因：现有前后端、AI Context、质量评分已经读取 `status`，扩展枚举比重建状态机风险更低。

2. **新增结构化替代信息，但不做硬审批。**
   - `replacementFieldId` 指向同项目标准字段，不允许指向自身。
   - `replacementReason` 记录历史兼容、迁移说明或无明确替代时的说明。
   - 原因：个人/小团队场景需要快速维护，但 AI 需要结构化替代说明。

3. **默认推荐只采用 enabled。**
   - 原因：AI 建表或补标准时默认不应继续采用 draft/deprecated/disabled 字段。用户仍可通过字段检索加 status 过滤查看历史字段。

## Risks / Trade-offs

- [Risk] 增加数据库字段会影响老库升级。→ Mitigation：新增 Flyway V19，字段均可为空，保持向后兼容。
- [Risk] 默认推荐跳过 deprecated 可能减少命中。→ Mitigation：字段检索支持显式 status=deprecated/disabled/draft，返回替代提示。
- [Risk] replacementFieldId 可能跨项目或指向自身。→ Mitigation：后端保存时校验同项目和非自身。

## Open Questions

- 已确认：change id 使用 `add-field-lifecycle`，capability 使用 `field-lifecycle`。
- 已收敛：第一版覆盖字段数据模型、默认推荐保护、检索/Context/质量评分展示和前端维护入口；DDL 深度改造后续跟随字段使用契约处理。
