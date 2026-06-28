## Why

DataSpec 页面已经覆盖字段库、反向导入、SQL 校验、覆盖率和 AI Context，但新用户或 AI 辅助操作仍需要从模块菜单里猜路径。P6-33 先把工作台改成按任务进入的入口，并补一个轻量面包屑和最近任务记忆，让个人/小团队更快进入标准闭环。

## What Changes

- Dashboard 新增任务入口矩阵，覆盖导入现有库、校验 SQL、生成覆盖率、补标准字段、导出 AI Context 和管理 Token。
- Dashboard 记录当前浏览器最近点击的任务入口，按项目展示最近任务并可一键回到该任务。
- App 顶部增加轻量面包屑，显示工作台和当前路由标题。
- 空项目状态保留演示项目和项目列表入口，并与任务入口保持同一任务语言。
- 不做大规模视觉重构、不引入工作流引擎、不迁移所有页面状态管理。

## Capabilities

### New Capabilities
- `frontend-task-entrypoints`: 前端提供任务式入口、最近任务和轻量面包屑。

### Modified Capabilities
- `dashboard`: 工作台从统计面板扩展为任务入口和最近任务中心。

## Impact

- 前端 `App.vue`、`Dashboard.vue`、源码级 smoke 测试和 README/TODO。
- 不新增后端 API、数据库迁移或外部依赖。
