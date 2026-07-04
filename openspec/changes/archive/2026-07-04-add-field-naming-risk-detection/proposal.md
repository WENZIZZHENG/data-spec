## Why

字段冲突检测已经能发现标准库内部重复，但 AI 建表和修 SQL 时还会踩到 SQL 方言保留字、危险词、大小写碰撞和 alias/canonical 歧义。P6-56 需要把这些命名风险提前暴露给前端和 AI Context，避免生成不可执行或含义不清的字段名。

## What Changes

- 扩展字段冲突报告，新增命名风险类型：保留字/危险词、大小写碰撞、alias 指向多个标准字段、alias 与 canonical name 歧义。
- 内置 PostgreSQL/MySQL/通用 SQL 的第一版高价值保留字和危险词清单，并输出涉及方言、证据和替代命名建议。
- 前端字段冲突页展示新增冲突类型、风险证据和建议动作，保持现有筛选/跳转编辑体验。
- AI Context 导出字段命名风险摘要，让 agent 在生成 DDL/SQL 前能读取哪些字段名或别名需要避让。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `field-conflict-detection`: 扩展冲突报告，覆盖方言保留字、危险命名、大小写碰撞和 alias/canonical 歧义。
- `ai-context-package`: 导出字段命名风险摘要，提醒 AI 避免直接采用高风险字段名或歧义 alias。

## Impact

- 后端：扩展 field conflict model/service/test，必要时复用现有字段服务；AI Context 增加只读风险摘要。
- 前端：更新类型、字段冲突页显示文案和烟测。
- 文档/TODO/OpenSpec：记录 P6-56 第一版能力和边界。
- 不新增 Flyway 迁移，不自动重命名字段，不阻断字段保存。
