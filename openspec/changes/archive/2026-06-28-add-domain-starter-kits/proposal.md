## Why

新项目从空字段库开始时，AI 很难快速写出用户、订单、支付、库存、审计等常见表的可用 SQL/DDL。DataSpec 已有内置基础标准和演示项目，但还缺少可选择、可重复应用、能向 AI 解释来源的领域 Starter Kit。

## What Changes

- 新增领域 Starter Kit 能力，提供若干内置小型领域包，包含字段、枚举和表模板。
- 提供后端 API 列出 starter kits，并将一个 kit 幂等应用到指定项目。
- 项目创建 UI 支持选择 starter kit；已存在项目也能手动应用 starter kit。
- AI Context 字段目录在字段来源标签存在时暴露 starter kit 来源，帮助 AI 判断字段来自哪个领域模板。
- Starter Kit 重复应用默认只补缺失项，不覆盖用户已修改字段、枚举或模板。

## Capabilities

### New Capabilities
- `domain-starter-kits`: 领域 starter kit 的列表、幂等应用、应用结果和前端入口。

### Modified Capabilities
- `project-standards`: 项目创建可以选择应用领域 starter kit，同时保留内置基础标准开关。
- `ai-context-package`: 字段目录导出 starter kit 来源元数据，保持现有字段结构兼容。

## Impact

- 后端：新增 starter kit 模型、服务和控制器；复用现有字段、枚举、模板、项目访问校验和事务。
- 数据库：新增轻量 starter kit 安装记录表，用于记录应用过的 kit、版本和结果摘要。
- 前端：项目列表/创建对话框新增 starter kit 选择与已选项目应用入口；新增 API 薄封装和类型。
- 文档与验证：更新 README、TODO 和 OpenSpec；补后端服务测试、前端 smoke，并运行现有验证入口。
