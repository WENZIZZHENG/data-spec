## Context

DataSpec 的左侧导航按模块组织，适合熟手；但个人/小团队首次进入项目时更常见的问题是“我现在要做什么”：导入现有数据库、检查 SQL、补标准字段、生成覆盖率或导出给 AI。Dashboard 已经承担项目总览和活动时间线，适合作为任务入口的第一版落点。

## Goals / Non-Goals

**Goals:**

- 在 Dashboard 提供稳定任务入口矩阵。
- 记录最近点击的任务入口，按当前项目展示最近任务。
- 在 App 顶部显示轻量面包屑，减少页面迷失感。
- 通过源码级 smoke 测试防止入口、文案和路由断线。

**Non-Goals:**

- 不引入复杂工作流引擎或跨页面状态系统。
- 不替换左侧模块导航。
- 不重做所有页面空态和错误态；统一状态组件留给后续 P6-43。
- 不新增后端 API。

## Decisions

1. **任务入口定义放在 Dashboard 本地。**

   第一版只服务 Dashboard，不抽公共 registry，避免为了少量入口提前抽象。后续如果 CLI/MCP/前端共用任务定义，再提升为共享 catalog。

2. **最近任务使用 localStorage。**

   最近任务属于个人浏览器偏好，不需要后端持久化，也不应污染项目标准数据。存储只包含 task key、title、route、projectId 和 usedAt。

3. **面包屑基于 route meta。**

   现有路由已经维护标题，App 可直接生成“工作台 / 当前页”结构，不需要新增路由配置模型。

## Risks / Trade-offs

- [Risk] Dashboard 任务入口过多导致页面拥挤。→ Mitigation：使用紧凑 grid，限制第一版入口数量。
- [Risk] localStorage 数据损坏影响渲染。→ Mitigation：解析失败时清空最近任务并继续渲染。
- [Risk] 任务入口与真实路由漂移。→ Mitigation：纳入 `frontendSmoke.test.ts`。

## Migration Plan

无数据库或 API 迁移。发布后用户会在 Dashboard 看到任务入口和最近任务；已有左侧导航继续可用。
