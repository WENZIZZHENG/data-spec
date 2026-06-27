## Context

当前项目没有 Spring Security 依赖，也没有账号体系。已有 API 基本以 `projectId` 作为项目范围，变更日志集中通过 `StandardChangeLogService` 写入。P4-9 的目标是个人/小团队安全基线，不是企业治理系统，因此第一版应复用现有 Spring MVC、MyBatis-Plus 和统一响应格式，避免引入复杂账号、角色和审批模型。

## Goals / Non-Goals

**Goals:**

- 安全模式可通过配置开启，默认不破坏个人本地开发。
- Bearer token 可识别操作者。
- Token 可限制到一个或多个项目，`*` 表示所有项目。
- 带 `projectId` 的请求在安全开启后必须通过项目授权。
- 字段、枚举、规则变更日志记录操作者。
- 前端提供最小 token 登录/退出状态，并通过统一 request 拦截器注入 token。
- CLI/MCP 可从 `.dataspec/config.json`、环境变量或命令行参数读取 token 并注入 Authorization header。
- 为 token 验证、项目授权和操作者日志补测试。

**Non-Goals:**

- 不做用户名密码登录、注册、找回密码。
- 不做角色权限矩阵、审批流、发布流或组织多租户模型。
- 不在第一版保存数据库直连密码或第三方密钥。
- 不把所有按资源 ID 的详情接口一次性改成深度授权查询；第一版优先覆盖携带 `projectId` 的标准 API 和变更入口。

## Decisions

1. **使用自研轻量拦截器而非引入 Spring Security**
   - 理由：当前项目没有安全依赖，第一版只需要 token 身份和项目边界；Spring Security 会带来过滤器链、测试和前端重定向成本。
   - 替代方案：引入 Spring Security；更完整，但对个人版来说过重。

2. **数据库只保存 token hash**
   - 理由：避免明文 token 泄露；后端用 SHA-256 hash 查询。
   - 替代方案：保存明文 token；实现简单但不符合安全基线。

3. **授权项目用文本字段保存**
   - 理由：第一版 token 数量很少，`project_ids` 可保存 `*` 或逗号分隔 ID，减少联表和管理页面复杂度。
   - 替代方案：新增 token-project 关联表；更规范，但当前收益不高。

4. **CLI/MCP 使用 `apiToken` 和 `--dataspec-token`**
   - 理由：`review-pr --token` 已用于 GitHub token，DataSpec token 需要单独命名，避免误传到第三方 API。
   - 替代方案：复用 `--token`；命令语义冲突，容易把 DataSpec token 发给 GitHub 或反过来。

5. **默认 `dataspec.security.enabled=false`**
   - 理由：避免本地首次启动和现有测试/脚本突然全部需要 token。
   - 替代方案：默认开启；安全更强，但会破坏现有开发体验。

6. **变更日志通过 `SecurityContext` 读取操作者**
   - 理由：服务层已有统一日志入口，只需在该入口填充 `operator`，所有字段、枚举、规则变更自动覆盖。
   - 替代方案：每个 service 显式传 operator；侵入面大且容易遗漏。

## Risks / Trade-offs

- **请求体内 projectId 的授权覆盖不足**：拦截器无需读取 JSON body，第一版优先检查 query/form `projectId`，写操作由服务层实体 projectId 和后续专项逐步补齐。前端和 CLI/MCP 常用读/导出路径先获得项目边界。
- **token 管理还不完整**：第一版支持数据库预置和校验，不做完整管理页面；后续可在安全页补 token 创建/吊销。
- **默认安全关闭**：生产或小团队部署必须显式开启；README 需要清楚写出配置方式。
- **hash 查询不可反查原 token**：符合安全预期，token 只在创建/配置时展示一次。

## Verification Strategy

- 后端单元测试覆盖 token hash 验证、项目授权通过/拒绝、禁用 token 拒绝。
- `StandardChangeLogService` 测试覆盖安全上下文存在和不存在时的 operator 写入。
- Node 测试覆盖 CLI/MCP 从配置和参数读取 DataSpec token 并注入 Authorization header。
- 前端构建覆盖 request token 注入、登录状态类型和页面类型。
- `mvn test`、`pnpm build`、`openspec validate add-personal-security-baseline`、`git diff --check` 作为最终门禁。
