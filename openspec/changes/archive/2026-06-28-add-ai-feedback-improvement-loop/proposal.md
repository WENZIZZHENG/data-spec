## Why

字段推荐、SQL 修复、AI 回放和规则例外已经能记录很多 AI 使用痕迹，但这些信号散落在单次页面里。用户需要一个项目级反馈视图，知道 AI 最常暴露哪些字段、规则和修复问题，并从反馈直接进入标准维护动作。

## What Changes

- 新增“AI 使用反馈与标准改进闭环”能力，按项目聚合已有 AI job、SQL 检查记录、fixedSql、规则豁免和反向导入来源。
- 新增后端只读报告 API，输出 summary、字段引用热区、规则问题排行、fixedSql 修复摘要、未纳管/转正信号和下一步动作。
- 新增前端项目级反馈页面，展示高频问题和跳转入口：字段库、字段质量、规则配置、规则例外、SQL 检查记录。
- 输出只读取 DataSpec 已有结构化记录，不采集业务数据行、不调用外部分析服务、不自动写入标准变更。

## Capabilities

### New Capabilities
- `ai-feedback-improvement-loop`: 覆盖 AI 使用反馈聚合、标准改进建议、项目级报告 API 和前端闭环入口。

### Modified Capabilities
- 无。现有 AI 回放、SQL 检查记录、字段推荐、规则例外和反向导入来源保持兼容；本变更只新增只读聚合视图。

## Impact

- 后端：新增反馈 report service/controller/model，复用现有 repository 或补充只读查询方法；需要解析 SQL 检查 issuesJson 和 AI job payload，但不新增写入表。
- 前端：新增 API wrapper、类型、反馈页面、导航入口和展示工具测试。
- OpenAPI/类型：同步或手工补齐 `schema.ts` 和 `types/index.ts`。
- 测试：新增后端聚合单测、前端 utility/smoke 测试，接入 `mvn test`、`pnpm test`、`pnpm build` 和 OpenSpec validate。

## Verification Evidence

- `mvn test`：通过，254 tests, 0 failures, 0 errors。
- `pnpm test`：通过，67 tests, 0 failures。
- `pnpm build`：通过，存在 `@vueuse/core` pure annotation 与 chunk size 既有 warning。
- `npx.cmd openspec validate add-ai-feedback-improvement-loop`：通过。
- `git diff --check`：通过，仅有 CRLF 换行提示。
- 本地结构化代码评审：未使用子 agent；已修复短字段名误报和聚合证据 `sourceId` 缺失两个 findings。
