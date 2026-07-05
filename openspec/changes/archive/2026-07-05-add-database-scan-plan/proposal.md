## Why

真实数据库可能有大量 schema、表和字段；一次性拉取所有 metadata 会慢、容易超时，也不利于 AI 在上下文有限时逐步处理。P6-63 已提供选中表的只读 metadata browser，但它仍要求一次性提交全部选表。本次要补一个大库友好的 scan plan，让用户和 AI 能按页浏览、生成部分预览、取消并用 cursor 恢复。

## What Changes

- 新增数据库 metadata scan plan 契约：基于直连信息和分页参数返回 scanId、estimatedTableCount、cursor、progress、partialSummary、resumeCommand 和 cancel 状态。
- 扩展后端只读 metadata 查询能力：支持表列表分页、按 cursor 读取下一批、按当前页表名生成部分 browser/preview，不保存数据库密码，不执行任意 SQL。
- 扩展反向导入前端：提供批次扫描入口、当前页表选择、继续下一批、取消扫描、复制/查看 resumeCommand 的轻量体验。
- 更新 README/TODO/AI 契约文档，明确第一版边界和验证证据。

## Capabilities

### New Capabilities
- `db-metadata-scan-plan`: 数据库直连 metadata 分页扫描计划、取消与恢复 cursor。

### Modified Capabilities
- `db-metadata-dump`: dump/browser 可复用 scan plan 的选中表分页结果生成部分预览。
- `db-reverse-import-frontend`: 反向导入页可按批次浏览大库表 metadata。

## Impact

- API/AI 契约：新增 scan plan 请求/响应对象和 endpoint；新增字段必须有 description/Javadoc，并保证不泄漏 password/token/JDBC URL。
- 后端：复用 `DatabaseMetadataAdapter.listTables/exportDump`，第一版不引入后台任务或分布式调度；scanId/cursor 为无凭据、可重建的短期上下文标识。
- 前端：在既有反向导入页上增加分页扫描控件，保持原“加载表/浏览元数据/确认导入”流程可用。
- 验证：需要后端 service/controller 测试、前端 util/smoke 测试、OpenSpec strict、全量或受影响模块验证、独立子 agent 评审。
