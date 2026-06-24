## Context

现有安全基线包含 `ds_api_token`、`ApiTokenService.authenticate`、Bearer token 拦截器、前端 token 登录态和 CLI/MCP token 透传。当前缺口是 token 的日常创建和禁用仍需要手写 SQL，且没有最后使用时间供用户判断 token 是否仍在使用。

本变更是 `full` 级别 SDD：涉及安全、公共 API、存储字段和前端管理入口。

## Goals / Non-Goals

**Goals:**

- 提供轻量 token 管理 API 和页面：列表、创建、禁用。
- 创建 token 时后端生成随机明文 token，并且只在创建响应返回一次。
- 数据库只保存 SHA-256 hash；列表和管理响应不暴露 hash 或明文。
- 记录 `last_used_at`，便于用户识别 CLI/MCP token 是否仍在活跃使用。
- 安全模式开启时，管理动作要求当前 principal 拥有全项目权限；安全模式关闭时本地模式可用。

**Non-Goals:**

- 不做复杂 RBAC、用户体系、审批流或组织成员管理。
- 不做 token 自动轮换、过期策略或邮件通知。
- 不提供读取历史明文 token 的能力。
- 不解决“首次开启安全模式时没有任何 token”的 bootstrap 流程；可通过本地模式创建后再开启安全模式，或保留一次性 SQL bootstrap。

## Decisions

1. 新增 `V8__extend_api_token_management.sql`，为 `ds_api_token` 增加 `last_used_at` 和 `disabled_at`。
   - 原因：最后使用时间和禁用时间是管理页面直接需要的元数据。
   - 替代方案：只用 `updated_at` 推断禁用/使用状态。语义混乱，无法区分配置修改和使用。

2. 后端生成原始 token，格式为 `ds_` + 32 字节随机 hex。
   - 原因：避免用户自填弱 token，同时保留 CLI/MCP 易识别前缀。
   - 替代方案：用户输入 token。实现简单，但安全性和重复风险更差。

3. 管理服务和认证服务共用 hash 逻辑，但创建响应只返回一次 `plainToken`。
   - 原因：保持现有认证兼容，不改变 CLI/MCP 传 token 的方式。

4. 管理接口要求 `ProjectAccessGuard.requireAllProjects`。
   - 原因：当前没有角色体系，项目级 token 不应管理可授权到其他项目的新 token。
   - 替代方案：允许任意 token 管理自身项目范围。会引入授权收窄、提权和 UI 解释复杂度。

5. `authenticate` 成功后异步语义上更新 `last_used_at`，但认证结果不依赖更新时间写入成功。
   - 原因：last-used 是诊断字段，不应因为写入失败影响业务 API 认证。

## Risks / Trade-offs

- [首次 bootstrap 仍需路径] → README 明确本地模式创建后开启安全模式，或保留 SQL bootstrap 作为兜底。
- [明文 token 只显示一次，用户没复制会丢失] → 前端创建成功后突出一次性明文和复制按钮。
- [全项目 token 权限较大] → 不新增更复杂 RBAC，P5-9 仅适配个人/小团队；页面文案提示妥善保管。
- [last_used_at 更新增加一次写库] → 只更新当前 token 行，失败不阻断认证；后续性能问题再优化。

## Migration Plan

1. 执行 V8 迁移，给已有 token 补 `last_used_at`、`disabled_at` 空字段。
2. 已有 token 继续可用，首次使用后写入 `last_used_at`。
3. 回滚时可忽略新字段；旧认证逻辑不依赖这些字段。
