## Why

字段冲突检测已经能发现重复、别名冲突和语义相近字段，但用户仍要手动判断哪个字段保留、哪些 aliases/tags/examples/source 应迁移，以及被合并字段如何废弃和回退。候选 Inbox 的“合并”目前只记录候选决策，不会真正修改正式标准字段。P6-65 需要把正式字段之间的合并变成可预览、可确认、可追溯的工作流，降低 AI 或用户误合并标准字段的风险。

## What Changes

- 新增标准字段 merge preview 契约：输入保留字段和来源字段，输出目标字段 before/after、来源字段废弃计划、alias/tag/example/source 迁移建议、风险、影响对象、回滚提示和下一步动作。
- 新增标准字段 merge apply 契约：在用户确认后应用预览计划，更新保留字段的可安全合并元数据，将来源字段标记为 `deprecated` 并写入 `replacementFieldId/replacementReason`，记录标准变更日志。
- 扩展前端字段冲突/字段库入口：从疑似重复组进入合并向导，查看 preview diff、风险和确认理由，再执行 apply。
- 更新 README/TODO/AI 契约文档，明确第一版边界：不自动合并、不删除历史字段、不跨项目合并、不静默覆盖冲突属性。

## Capabilities

### New Capabilities
- `standard-field-merge-wizard`: 标准字段合并预览、确认应用、回滚提示和 AI 可读合并证据。

### Modified Capabilities
- `field-conflict-detection`: 冲突报告可作为合并向导入口，帮助用户选择保留字段和来源字段。
- `field-library`: 字段库可展示来源字段的 replacement 关系，并通过变更日志支持回退。
- `ai-context-field-catalog`: AI 读取字段目录时应能识别 deprecated/replacement 关系，避免继续推荐被合并字段。

## Impact

- SDD 等级：SDD full。该变更涉及正式标准字段写入、生命周期状态、替代关系、AI 外部契约和数据一致性。
- API/AI 契约：新增 merge preview/apply 请求响应对象和 endpoint；所有公共字段必须有 Javadoc/TSDoc 或 schema 说明。
- 后端：沿用 `FieldServiceImpl`、`StandardChangeLogService`、字段生命周期状态和现有 repository 模式；合并操作必须项目隔离、事务化、可回滚提示明确。
- 前端：新增或扩展现有页面控件，保持字段冲突报告、字段库和候选 Inbox 原有流程兼容。
- 验证：需要后端 service/controller 测试、前端 util/smoke 测试、OpenSpec strict、受影响模块全量验证、独立子 agent 评审。
