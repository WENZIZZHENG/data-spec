## Why

个人/小团队不需要企业审批流，但需要快速知道一个项目最近发生了什么：谁或哪个 AI/CLI 修改了标准、创建了快照、导入了字段、跑了 SQL 检查或生成了 DDL。当前活动散落在多个页面，排查“标准什么时候变了”成本偏高。

## What Changes

- 新增项目活动时间线只读 API，按项目聚合字段变更、标准快照、反向导入批次、SQL 检查、AI/DDL 记录和 token 使用摘要。
- 活动项统一输出 actionType、title、description、occurredAt、actor、source、severity、detailRoute 和 metadata。
- 前端工作台增加最近活动时间线，支持按动作类型筛选并跳转到相关详情页。
- 保持个人/小团队边界：不引入审批、长期审计存储或复杂权限模型。

## Capabilities

### New Capabilities
- `project-activity-timeline`: 按项目聚合关键活动并提供只读 API 与前端时间线。

### Modified Capabilities
- `dashboard`: 工作台展示项目最近关键活动，并在项目切换时刷新。

## Impact

- 后端新增 activity model/service/controller，复用已有 repository/service 查询，不新增持久化表。
- 前端工作台新增时间线区域和 activity API 封装。
- OpenAPI 类型生成产物可能随新增 API 更新。
- 后端/前端测试覆盖活动聚合、筛选和页面耦合。
