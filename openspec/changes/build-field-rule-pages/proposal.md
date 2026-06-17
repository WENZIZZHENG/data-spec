## Why

P1-4 需要让个人/小团队真正维护字段标准和规则配置。当前项目列表和顶部项目选择已接入后端，但字段库、规则配置仍是占位页，导致标准字段模型、结构化命名规则和 SQL lint 项目规则无法从前端闭环维护。

## What Changes

- 将 `FieldLibrary.vue` 从占位页改为按当前项目分页加载字段，并支持创建、编辑、删除。
- 将 `RuleConfig.vue` 从占位页改为按当前项目加载规则配置，并支持创建、编辑、删除、启停。
- 表单字段与 OpenAPI 生成类型保持一致，覆盖 P0-4 字段元数据和 P0-5 规则 `paramsJson`。
- 更新 TODO 状态和 README/验证说明（如需要）。

## Capabilities

### New Capabilities

- `field-library-page`: 维护当前项目下的标准字段库。
- `rule-config-page`: 维护当前项目下的规则配置。

### Modified Capabilities

- `project-workspace`: 当前项目选择成为字段库、规则配置的业务上下文。

## Impact

- 仅修改前端管理页面和必要文档。
- 不修改后端 CRUD、lint 规则或数据库结构。
- 验证入口为 `pnpm build`，并保留后端/工具测试的回归验证。
