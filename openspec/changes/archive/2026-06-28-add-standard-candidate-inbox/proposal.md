## Why

覆盖率报告、反向导入、字段推荐未命中和 AI 使用反馈都会产生“可能应该进入标准库”的候选项。当前这些信号散落在不同页面，用户和 AI agent 难以持续处理，也容易反复推荐已忽略或已合并的字段。

## What Changes

- 新增项目级“标准候选 Inbox”能力，集中展示候选字段、候选来源、置信度、状态和建议动作。
- 新增后端候选模型/API，支持列表、创建候选、接受为新标准字段、合并到已有字段、忽略并记录原因、延后处理。
- 新增前端采纳工作台页面，支持按状态/来源/关键词筛选候选，查看证据，执行接受、合并、忽略和延后操作。
- 决策结果写入候选记录，并尽量复用现有字段创建、变更日志和来源追踪能力。
- 第一版只做人工确认的轻量闭环，不做审批流、自动合并、团队工单或外部行为分析。

## Capabilities

### New Capabilities

- `standard-candidate-inbox`: 覆盖标准候选聚合、候选决策状态、人工采纳/合并/忽略/延后 API 和前端工作台。

### Modified Capabilities

- 无。反向导入、覆盖率、字段推荐和 AI 反馈的现有行为保持兼容；第一版通过新增候选 Inbox 能力承接这些信号。

## Impact

- 后端：新增候选实体、Flyway 迁移、repository/service/controller；接受候选时调用字段服务或仓储写入标准字段，并记录候选决策。
- 前端：新增 API wrapper、OpenAPI/类型、候选 Inbox 页面、导航入口和展示工具测试。
- 数据库：新增轻量候选表，保存项目、候选字段、来源、证据、状态、决策原因和关联字段。
- 测试：新增后端 service/controller 单测、前端 utility/smoke 测试，并接入 `mvn test`、`pnpm test`、`pnpm build`、OpenSpec validate 和 `git diff --check`。

## Verification Evidence

- `mvn test`（`dataspec-server`）：263 tests，0 failures，0 errors，BUILD SUCCESS。
- `pnpm test`（`dataspec-web`）：71 tests，71 pass，0 fail。
- `pnpm build`（`dataspec-web`）：`vue-tsc --noEmit && vite build` 通过；仅保留 `@vueuse/core` pure annotation 和 chunk size 既有构建警告。
- `npx.cmd openspec validate add-standard-candidate-inbox`：Change valid。
- `git diff --check`：通过；仅输出工作区既有 CRLF 行尾提示。
- 本地结构化代码评审（未使用子 agent）：修复候选证据 JSON `token/password` 脱敏不足、候选 `data_type` 长度与正式字段不一致、前端项目切换后旧弹窗继续提交的边界问题。
