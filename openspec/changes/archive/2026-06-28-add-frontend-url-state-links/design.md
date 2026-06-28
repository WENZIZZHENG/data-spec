## Context

DataSpec Web 目前有全局项目选择、Vue Router、Pinia project store、字段库 `keyword/fieldId` query 雏形、反向导入 localStorage 记忆和统一请求状态。高频页面的筛选、详情弹窗和记录定位仍主要在组件内存中，刷新或把链接交给 AI/browser automation 后无法稳定复现。

P6-44 是前端可观察行为变更，但不新增后端数据模型或接口。设计重点是把“可安全公开的定位信息”写进 URL，并把“敏感或大体积输入”明确排除。

## Goals / Non-Goals

**Goals:**
- 为关键页面提供一致 URL 状态协议，支持通过 query 恢复当前项目、筛选条件和详情记录。
- 提供可复用的 URL 状态工具，避免每个页面手写松散解析、重复保留 query 或误写敏感字段。
- 为字段库、SQL 检查记录、AI 回放、字段覆盖率和反向导入批次提供复制当前链接入口。
- 对无效 id、不可访问详情、未知状态值等情况给出可恢复提示，并移除或忽略无效 query。
- 用前端单测和 smoke test 覆盖 query 解析、敏感字段剔除和页面接线。

**Non-Goals:**
- 不新增后端 API，不改变记录详情、覆盖率或反向导入响应结构。
- 不把 SQL 原文、数据库连接信息、password、token、Authorization、完整 JDBC URL 或大型 JSON 写进 URL。
- 不重做整体路由体系，不引入新的前端状态库。
- 不保证所有表单字段都可复现；第一版只覆盖可安全定位的筛选和详情状态。

## Decisions

1. **使用 query 参数而不是 hash 或 localStorage 作为可复制协议。**
   - 原因：Vue Router 已使用 history 模式，query 易于浏览器新标签页、AI/browser automation 和手工编辑；localStorage 适合个人记忆，但不能作为分享协议。
   - 备选：hash fragment。暂不采用，因为现有页面没有 hash 语义，query 更适合测试和后续路由守卫。

2. **新增轻量 `urlState` utility，页面声明式读写白名单参数。**
   - 工具提供 `readPositiveIntQuery`、`readStringQuery`、`readEnumQuery`、`mergeRouteQuery`、`dropUnsafeUrlParams` 和 `copyCurrentUrl` 等函数。
   - 页面只传入允许的 query key，不透传任意表单对象，避免把敏感字段或大 SQL 文本写入 URL。

3. **`projectId` 由全局项目选择和页面 URL 协同。**
   - 打开带 `projectId` 的链接时，App 在项目列表加载后尝试切换当前项目；找不到或无权限时保留页面可用并提示用户重新选择。
   - 顶部项目选择切换时，保留当前路径并更新 `projectId` query；页面自己的筛选 watcher 再按项目刷新数据。

4. **详情状态使用 id query，详情内容仍走现有 API。**
   - SQL 检查详情使用 `recordId`；AI 回放详情使用 `aiJobId`；反向导入来源/批次详情使用 `sourceBatchId`；字段库使用 `fieldId`；覆盖率只持久化 `table/status` 等筛选。
   - 无效 id 或详情加载失败时显示可恢复提示，并移除对应 id query，避免刷新后重复失败。

5. **复制链接按钮放在已有工具区或详情操作区。**
   - 按钮命名统一为“复制链接”，图标优先使用 Element Plus/lucide 可用图标；复制成功用 `ElMessage.success`，失败给出手动复制提示。
   - 不为第一版新增大型分享面板或链接历史。

## Risks / Trade-offs

- [Risk] watcher 互相触发导致重复请求或路由循环。→ Mitigation：写 query 前比较 normalized query，仅在值变化时 `router.replace`；详情打开和关闭分别收敛到单一函数。
- [Risk] 无效 query 打开后页面空白。→ Mitigation：所有解析函数返回安全默认值，页面显示 `ElMessage.warning` 或 StateBlock 兜底，并清理无效参数。
- [Risk] 敏感字段被误加入 URL。→ Mitigation：工具层维护禁止 key 列表，页面只写白名单参数；测试覆盖 password/token/Authorization/JDBC/SQL 原文剔除。
- [Risk] 覆盖率报告本身不是持久化资源，刷新后无法恢复报告结果。→ Mitigation：第一版只恢复筛选条件；若没有当前报告，页面保留筛选 query，等待用户重新生成报告。

## Migration Plan

- 前端变更向后兼容；没有 query 的旧链接继续按默认页面状态打开。
- 已有 localStorage 反向导入记忆保留，URL 只覆盖轻量定位参数，不迁移历史本地记忆。
- 回滚时删除前端工具和页面接线即可；URL 中遗留 query 不会影响后端数据。
