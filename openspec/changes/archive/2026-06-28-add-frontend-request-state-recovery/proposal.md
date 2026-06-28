## Why

DataSpec Web 的业务页已经覆盖 SQL 校验、反向导入、覆盖率、AI 批量任务和标准维护，但 loading、空数据、未选择项目、接口失败和可重试动作仍由各页面分散处理。P6-39 第一版要给 AI 和用户一个一致、可判断、可恢复的前端状态模式，减少“页面没数据但不知道下一步”的摩擦。

## What Changes

- 新增前端统一请求状态组合函数，用于收敛 loading/error/retry/lastUpdated 等页面级状态。
- 新增轻量项目必选/状态展示组件，统一未选择项目、空状态、错误状态和重试动作。
- 先迁移少量高频页面，优先覆盖 Dashboard、AI 批量任务、覆盖率报告和 SQL 校验记录区域等已有项目强依赖流程。
- 前端 smoke/utility tests 覆盖项目缺失、空数据、失败重试和错误建议文案。
- 不改后端 API，不替换 Element Plus，不做大视觉改版，不一次性重写所有页面。

## Capabilities

### New Capabilities

- `frontend-request-state-recovery`: 前端业务页统一 loading/empty/error/retry/project guard 状态与可恢复动作。

### Modified Capabilities

- `frontend-smoke-regression-gate`: 前端 smoke gate 需要覆盖统一状态组件、项目必选提示和失败重试入口。
- `frontend-task-entrypoints`: 高频任务入口页需要在无项目或请求失败时提供一致的下一步动作，而不是散落的原始空态。

## Impact

- 前端：新增 `src/composables`、`src/components` 或等价目录；迁移部分 Vue 页面使用统一状态模式。
- 测试：新增前端工具测试，扩展 `frontendSmoke.test.ts`；继续使用 `pnpm test` 和 `pnpm build` 作为门禁。
- 文档/规范：更新 OpenSpec specs、README 或 TODO 中的已完成状态。
- 风险：状态组件过度抽象会拖慢后续页面开发；本轮只做轻量、可渐进迁移的模式。

## Verification Evidence

- `pnpm test`（`dataspec-web/`）：84 tests passed，0 failed。
- `pnpm build`（`dataspec-web/`）：`vue-tsc --noEmit && vite build` 通过；仅保留既有 `@vueuse/core` pure annotation、chunk size 与 plugin timing 警告。
- `npx.cmd openspec validate --all`：76 items passed，0 failed。
- `git diff --check`：通过；仅输出 Windows 换行转换提示。
- 本地结构化代码评审：发现 Dashboard / AI Batch 的统一状态请求失败后可能产生未处理 Promise；已补本地 catch、显式 `void` 自动刷新调用，并用 frontend smoke 锁定恢复入口。
