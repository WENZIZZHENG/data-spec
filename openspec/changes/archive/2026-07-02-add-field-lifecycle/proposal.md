## Why

字段已有状态，但 AI 使用时更需要明确哪些字段是草稿、可用、废弃、停用，以及废弃字段应替换成什么；否则 AI 可能继续推荐历史字段。

## What Changes

- 定义个人/小团队优先的轻量字段生命周期：`draft`、`enabled`、`deprecated`、`disabled`。
- 为字段增加结构化替代信息 `replacementFieldId` 和 `replacementReason`，用于解释废弃/停用字段的替代建议或历史兼容原因。
- 字段推荐默认只推荐 `enabled` 字段；字段检索、AI Context 和字段质量评分读取同一套生命周期信息，向 AI 展示替代提示。
- 同步前端字段库表单、README/TODO、OpenSpec 主规格和契约类型。

## Capabilities

### New Capabilities
- `field-lifecycle`: 标准字段生命周期状态和替代说明的轻量契约。

### Modified Capabilities
- `field-suggestion`、`field-standard-search`、`ai-context-package` 和字段质量评分读取字段生命周期信息。

## Impact

- 已有基础：字段模型已有 status、变更日志、标准快照、质量评分中的废弃说明检查和字段推荐。
- 缺口：字段状态语义还不够统一，缺少结构化 replacementFieldId/replacementReason 和导出给 AI 的稳定说明。
- 落地产物：数据库迁移、后端模型/API、推荐与检索策略、AI Context 输出、质量评分、前端字段库表单、文档和测试。
- 验收标准：废弃/停用/草稿字段不会被默认推荐给 AI；显式检索这些字段时能说明状态、替代字段或替代原因；AI Context 可读取生命周期字段；字段状态和替代信息变更进入变更日志和快照。
- 边界：不做审批流，不做组织级发布治理，不阻止个人快速维护标准。
