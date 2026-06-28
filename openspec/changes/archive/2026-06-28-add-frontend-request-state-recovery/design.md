## Context

DataSpec Web 目前大量页面各自维护 `loading`、`hasProject`、空表格文案和错误提示。请求失败时主要依赖 Axios interceptor 弹出 `ElMessage`，页面本身通常没有可恢复状态；未选择项目时也常用分散的 `el-empty`。P6-39 第一版要抽出一个轻量模式，让页面能稳定表达“缺项目、加载中、空结果、失败可重试、最近更新时间”，并给 AI smoke tests 可检查的稳定入口。

约束：

- 不替换 Pinia、Axios、Element Plus 或现有 API wrapper。
- 不做大视觉改版，组件样式保持工作台/业务页的紧凑工具感。
- 不一次性迁移所有页面，第一版只迁移高频且低风险的项目依赖页。
- 统一状态必须兼容现有 interceptor，避免重复弹出太多全局错误。

## Goals / Non-Goals

**Goals:**

- 提供 `useRequestState` 或等价组合函数，统一 loading/error/lastUpdated/retry。
- 提供 `ProjectRequired`、`StateBlock` 或等价轻量组件，统一项目缺失、空数据、失败重试展示。
- 迁移 Dashboard、AI 批量任务、覆盖率报告和 SQL 校验记录区域的关键状态入口。
- 通过单测和 smoke gate 覆盖项目缺失、空数据、错误重试和状态组件存在性。

**Non-Goals:**

- 不建立全局 server-state cache，不引入 TanStack Query。
- 不改变后端错误协议，不改 Axios 认证清理逻辑。
- 不把所有页面一次性改完；其余页面后续按同一模式渐进迁移。
- 不做视觉主题重构、路由重写或权限系统。

## Decisions

1. **先做本地组合函数而不是引入请求库。**
   - 做法：新增 `useRequestState` 管理 `loading`、`errorMessage`、`errorDetail`、`lastUpdatedAt` 和 `run/reset`。
   - 理由：项目已有 Axios wrapper 和页面级请求函数，引入新库会放大迁移面。
   - 备选：直接上 TanStack Query。暂不采用，P6-39 第一版只需要状态收口，不需要缓存和复杂失效策略。

2. **组件只负责呈现和动作，不发请求。**
   - 做法：`StateBlock` 接收 type/title/description/action；`ProjectRequired` 只检查项目状态并提供跳转/创建项目入口。
   - 理由：保持页面仍掌握业务请求，避免组件里混入 API 依赖。

3. **优先迁移高频页面的外层状态。**
   - 做法：Dashboard 和 AI Batch 迁移页面级加载/错误/项目缺失；FieldCoverage 和 SqlLint 先迁移覆盖率结果/记录区等明确状态。
   - 理由：这些页面已被 smoke gate 覆盖，且用户最容易遇到“没项目/没数据/请求失败不知道怎么办”。

4. **错误对象只展示可操作摘要。**
   - 做法：从 `error.dataspecError` 中读取 `suggestedAction`、`docsRef`、`retryable`，页面展示建议和重试按钮。
   - 理由：避免把 raw backend/axios 文本直接铺到页面，同时让 AI 自动化能判断下一步。

## Risks / Trade-offs

- [Risk] 页面局部迁移导致新旧状态并存。→ Mitigation：新增 smoke test 锁住迁移页面，后续页面按同一组件扩展。
- [Risk] interceptor 已经弹错，页面错误块再展示一次显得重复。→ Mitigation：页面错误块强调恢复动作，保留全局短提示。
- [Risk] 组合函数过度泛化。→ Mitigation：只暴露当前页面真实需要的字段，不支持缓存、分页、并发取消等未来能力。

## Migration Plan

1. 新增状态组合函数和组件。
2. 补 utility/component 测试，先验证错误解析、重试状态和项目空态文案。
3. 迁移选定页面，保持原 API 调用和业务逻辑不变。
4. 扩展 smoke gate，确认统一状态入口存在。
5. 运行 `pnpm test`、`pnpm build`、`openspec validate` 和 `git diff --check`。
