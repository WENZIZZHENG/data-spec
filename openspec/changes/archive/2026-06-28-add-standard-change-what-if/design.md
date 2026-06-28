## Design

### Scope

P6-41 第一版只做只读 what-if 预览，不阻断保存、不做审批、不自动回滚。覆盖对象：

- `FIELD_UPDATE`：字段属性更新前预览。
- `RULE_UPDATE`：规则名称、级别、启用状态、参数更新前预览。
- `RULE_TOGGLE`：规则启停前预览。

### Backend Contract

新增统一返回对象 `StandardChangePreview`：

- `projectId`
- `targetType`
- `targetId`
- `targetName`
- `operation`
- `riskLevel`
- `requiresConfirmation`
- `summary`
- `changes`
- `impacts`
- `validationCommands`
- `rollbackHints`
- `currentSnapshot`

`changes` 使用 `attribute/beforeValue/afterValue/riskLevel/description`。`impacts` 使用轻量 `impactType/severity/title/description/count/sourceId/metadata`，字段预览从现有 `FieldImpactReport` 转换，规则预览按规则链路生成固定影响项。

### Risk Rules

- 字段 `name/dataType/status/codeSetId/sensitive` 变化默认为 `WARNING`；存在模板或快照影响时提升到 `HIGH`。
- 字段只有非关键属性变化且无影响时为 `INFO`。
- 规则 `enabled/severity/paramsJson` 变化为 `WARNING`；禁用 `ERROR` 级启用规则或把规则改为 `ERROR` 为 `HIGH`。

### Rollback

预览只提供回退辅助，不执行回退：

- 字段：提示保存后可在字段变更日志中选择最近 update 记录回退。
- 规则：提示保存后可通过规则变更日志查看 before/after，第一版不提供一键规则回退。
- 快照：如当前项目已有快照，提示保存后按需创建新快照或保留当前快照作为可信基线。
