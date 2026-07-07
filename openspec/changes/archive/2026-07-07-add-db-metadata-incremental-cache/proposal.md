## Why

数据库直连反向导入、metadata 浏览和字段覆盖率报告会反复读取同一批 schema；大库场景下这会拖慢操作，也让 AI 无法判断本次结构是否与上次一致。P6-71 需要引入只保存结构信息的 metadata cache 和稳定 fingerprint，让重复扫描可以复用缓存、字段变化可以形成摘要、AI 可以据此决定是否重跑反向导入或 Context 生成。

本变更命中存储和数据一致性边界，按 SDD full 处理：先定义缓存契约、失效策略、差异摘要和安全边界，再实现最小闭环。

## What Changes

- 新增数据库 metadata 增量缓存能力：按项目、连接预设或脱敏连接指纹、schema、table 维度保存结构快照、fingerprint、缓存时间、源数据库版本和刷新方式。
- 新增稳定 fingerprint 与 change summary：重复扫描同一目标时返回 cache freshness；字段新增、删除、类型/注释/默认值/可空性变化时返回差异摘要。
- 扩展数据库直连 scan/browser/preview/compare/coverage 流程：优先复用未过期缓存；显式强制刷新时重新读取源库并更新缓存；返回 AI 可读 fingerprint。
- 强化安全边界：缓存只保存表、字段、索引、注释、类型等结构 metadata，不保存密码、token、JDBC URL、连接串或业务数据行。
- 前端展示缓存状态、最近扫描时间、刷新方式、fingerprint 和字段变更摘要，帮助用户判断是否刷新。
- 不做实时同步、不监听 binlog、不默认后台扫描全库。

## Capabilities

### New Capabilities
- `db-metadata-incremental-cache`: 覆盖数据库 metadata cache、fingerprint、lastSeenAt、changeSummary、刷新/失效策略和安全存储边界。

### Modified Capabilities
- `db-metadata-scan-plan`: metadata scan 响应需要包含缓存命中、过期、fingerprint、刷新方式和 AI 可读下一步。
- `db-metadata-dump`: schema dump 需要携带结构 fingerprint、源数据库版本和缓存来源信息，供离线分析复用。
- `db-reverse-import-frontend`: 反向导入页需要展示缓存状态、刷新入口和结构变更摘要。
- `field-coverage-report`: 数据库直连覆盖率报告需要支持基于缓存的结构输入，并暴露本次报告关联的 metadata fingerprint。

## Impact

- 后端：新增 metadata cache 持久化对象、迁移、repository/service；调整 `DatabaseReverseImportServiceImpl`、`DatabaseMetadataAdapter` 相关数据流和模型字段。
- API/契约：扩展数据库 scan/browser/dump/preview/compare/coverage 响应，新增缓存状态、fingerprint、changeSummary、lastSeenAt、expiresAt、refreshMode 等字段；请求增加可选刷新策略，不改变现有必填参数。
- 前端：更新 `ReverseImport.vue`、字段覆盖率页、API types/schema 和相关工具测试，展示缓存状态并允许手动刷新。
- 验证：补充后端缓存单测、前端工具/冒烟测试、OpenSpec strict 校验和敏感信息扫描；commit 前必须使用独立子 agent 评审。
