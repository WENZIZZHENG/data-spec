## Why

数据库直连反向导入已经能生成字段候选并完成导入，但导入后的字段无法回答“来自哪次连接、哪个 schema.table.column、当时 metadata 是什么”。新增来源与批次追踪可以让个人/小团队在后续清理、复盘和字段覆盖率分析时有可靠上下文。

## What Changes

- 新增反向导入批次记录，记录项目、来源类型、数据库类型、database/schema、选择表、导入时间、操作者和摘要统计。
- 新增字段来源记录，将通过直连反向导入创建的标准字段关联到 batchId、schema、table、column、原始类型、默认值、可空、注释和原始 metadata 快照。
- 反向导入确认导入时写入批次和字段来源；普通手工字段不受影响。
- 字段列表或字段详情暴露来源摘要，前端可查看字段是否来自数据库直连反向导入。
- 不保存数据库密码，不保存完整连接串明文，不做定时同步或跨项目来源合并。

## Capabilities

### New Capabilities

- `reverse-import-source-tracking`: 数据库直连反向导入写入批次和字段来源追踪信息，并允许用户在字段侧查看来源摘要。

### Modified Capabilities

无。

## Impact

- 影响后端 `reverseimport`、`field` 相关 controller/service/model，以及 Flyway 迁移。
- 影响前端字段库或反向导入页的导入结果/来源展示。
- 新增数据库表用于批次和字段来源追踪，不改变现有字段主表结构。
- 需要补充 service 单测和前端 build 验证。
