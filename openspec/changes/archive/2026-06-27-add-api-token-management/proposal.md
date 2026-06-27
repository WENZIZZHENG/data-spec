## Why

安全基线已经支持 API Token 认证、项目边界和 CLI/MCP token 透传，但 token 仍主要靠手写 SQL 配置。P5-9 要补一个个人/小团队可用的轻量管理入口，让用户能在前端创建、查看和禁用 CLI/MCP token。

## What Changes

- 新增 API Token 管理后端能力：列表、创建、禁用和最后使用时间记录。
- 创建 token 时由后端生成随机明文 token，数据库只保存 SHA-256 hash；明文 token 仅在创建响应中返回一次。
- 管理接口只返回 token 元数据，不返回 hash 或明文 token。
- 安全模式开启时，仅允许全项目 token 管理其他 token；本地安全模式关闭时仍可本地管理。
- 新增前端 API Token 管理页面，支持创建、复制一次性明文 token、查看授权范围/最后使用时间和禁用 token。

## Capabilities

### New Capabilities

- `api-token-management`: API Token 元数据管理、一次性明文创建响应、禁用和最后使用时间。

### Modified Capabilities

无。

## Impact

- 后端：新增 token 管理 DTO/API/service/repository 方法，扩展 `ds_api_token` 管理字段迁移。
- 前端：新增 token 管理 API 封装、类型、页面和路由入口。
- 安全：不保存明文 token，不向列表/详情响应泄漏 `tokenHash`；管理接口要求全项目权限。
- 验证入口：后端单测/全量测试、前端测试/构建、OpenSpec validate、diff 检查。
