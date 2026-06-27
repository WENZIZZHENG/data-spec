## Why

DataSpec 已经具备字段标准、DDL 生成、SQL 校验、修正 SQL 和 AI Context 导出，但新环境里用户仍要手动组装项目、模板和规则，首次体验成本偏高。P4-10 需要把这些能力串成一个可立即上手的演示闭环。

## What Changes

- 新增一键创建或复用“DataSpec 演示项目”的后端 API。
- 演示项目自动包含内置字段/数据域、示例表模板、核心规则配置和示例 SQL。
- 前端工作台和项目列表提供“创建演示项目”入口，并自动切换到该项目。
- 工作台提供轻量快速开始入口，串联 DDL 生成、SQL 校验、AI 规则导出和反向导入。
- SQL 校验页支持通过路由参数加载演示问题 SQL；DDL 生成页支持通过路由参数预填演示表名。
- README/TODO 同步说明 P4-10 状态和使用路径。

## Capabilities

### New Capabilities
- `demo-onboarding`: 演示项目创建、首次使用入口和演示 SQL/模板引导。

### Modified Capabilities

无。

## Impact

- 后端新增项目演示 API、服务和测试，复用现有项目、字段、规则、模板仓储。
- 前端更新项目 API、类型、工作台、项目列表、SQL 校验页和 DDL 生成页。
- 不新增数据库表，不保存用户行为，不引入审批流或复杂 onboarding 状态。
