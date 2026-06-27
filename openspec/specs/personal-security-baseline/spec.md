# personal-security-baseline Specification

## Purpose
TBD - created by archiving change add-personal-security-baseline. Update Purpose after archive.
## Requirements
### Requirement: 可启用的 API token 身份
DataSpec SHALL support an optional security mode where HTTP API requests require a Bearer token.

#### Scenario: 安全模式关闭
- **WHEN** `dataspec.security.enabled` is false
- **THEN** existing local API requests continue without a token

#### Scenario: 安全模式开启且 token 有效
- **WHEN** a request contains `Authorization: Bearer <token>` and the token is enabled
- **THEN** DataSpec treats the request as the token's configured operator

#### Scenario: 安全模式开启且 token 缺失或无效
- **WHEN** a request does not contain a valid enabled Bearer token
- **THEN** DataSpec rejects the request with an authentication error

### Requirement: 项目级 token 授权
DataSpec SHALL restrict project-scoped API requests to projects authorized for the current token when security mode is enabled.

#### Scenario: token 访问授权项目
- **WHEN** a token is authorized for project `1`
- **AND** the request includes `projectId=1`
- **THEN** DataSpec allows the request

#### Scenario: token 访问未授权项目
- **WHEN** a token is authorized for project `1`
- **AND** the request includes `projectId=2`
- **THEN** DataSpec rejects the request with an authorization error

#### Scenario: 全项目 token
- **WHEN** a token is configured with project scope `*`
- **THEN** DataSpec allows project-scoped requests for any project

### Requirement: 变更日志记录操作者
DataSpec SHALL record the current operator on standard change logs.

#### Scenario: token 请求写入标准变更
- **WHEN** an authenticated operator creates, updates, deletes or toggles a standard item
- **THEN** the standard change log stores that operator name

#### Scenario: 本地未启用安全写入标准变更
- **WHEN** security mode is disabled
- **THEN** the standard change log stores operator `local`

### Requirement: 前端 token 登录态
DataSpec Web SHALL let users provide an API token and send it on subsequent API requests.

#### Scenario: 保存 token
- **WHEN** the user submits a valid API token
- **THEN** the frontend stores the token locally and includes it as `Authorization: Bearer <token>` in API requests

#### Scenario: 认证失败
- **WHEN** an API response indicates authentication or authorization failure
- **THEN** the frontend clears the stored token and returns the user to the token login state

### Requirement: CLI/MCP token 透传
DataSpec CLI and MCP SHALL support sending a DataSpec API token as Bearer authorization to the DataSpec backend.

#### Scenario: 从配置文件读取 token
- **WHEN** `.dataspec/config.json` contains `apiToken`
- **THEN** CLI/MCP include `Authorization: Bearer <apiToken>` on DataSpec HTTP requests

#### Scenario: 命令行 token 覆盖配置
- **WHEN** CLI/MCP are started with `--dataspec-token <token>`
- **THEN** the command line token is sent instead of the configured token
