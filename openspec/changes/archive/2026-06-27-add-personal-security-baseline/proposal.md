## Why

DataSpec 已经具备字段标准、SQL 校验、生成、导入导出、CLI/MCP 和 AI Context 能力。个人自用阶段可以本地裸跑，但小团队或让 AI agent 持续调用时，需要最小身份、项目边界和 API token，避免不同项目的标准数据被误读或误改。

## What Changes

- 新增可启用的个人/小团队安全开关，默认保持本地开发兼容。
- 新增 API token 模型，保存 token hash、操作者名称和授权项目范围。
- 新增认证接口，用 token 换取当前身份信息，前端保存并注入 Bearer token。
- 后端请求拦截器在安全开启后校验 Bearer token，并对带 `projectId` 的请求执行项目授权检查。
- 标准变更日志增加 `operator` 字段，记录当前 token 对应操作者；未启用安全时记录 `local`。
- README/TODO 记录启用方式、边界和 P4-9 状态。
- CLI/MCP 支持通过 `.dataspec/config.json` 的 `apiToken`、`DATASPEC_TOKEN` 环境变量或 `--dataspec-token` 参数向 DataSpec 后端发送 Bearer token。

## Capabilities

### New Capabilities

- `personal-security-baseline`: DataSpec 在开启安全模式后使用 API token 识别操作者，并按授权项目限制 API 访问。

### Modified Capabilities

- `standard-change-log`: 变更日志记录操作者，便于回溯字段、枚举和规则的修改来源。

## Impact

- 后端新增安全配置、API token 表、认证服务、请求拦截器和单元测试。
- 数据库新增 `ds_api_token` 表，并为 `ds_standard_change_log` 增加 `operator` 字段。
- 前端请求层新增 Bearer token 注入和 401/403 处理，增加轻量登录页/令牌管理。
- CLI/MCP 不新增复杂登录流程；通过 HTTP header 使用 token，并支持 `.dataspec/config.json` 的 `apiToken` 字段。
- 不做审批流、发布流、团队/角色 RBAC、多租户组织模型或密码账号体系。
