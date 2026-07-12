# dataspec-local-config Specification

## Purpose
定义 CLI/MCP 读取仓库级 `.dataspec/config.json` 的配置优先级、默认扫描路径、token 来源和可读错误边界。
## Requirements
### Requirement: 读取仓库级 DataSpec 配置
DataSpec CLI/MCP SHALL 从当前工作目录向上查找 `.dataspec/config.json`，并将其中的 `projectId`、`server` 和 `defaultPaths` 作为默认配置。

#### Scenario: 从当前目录读取配置
- **WHEN** 当前目录包含 `.dataspec/config.json`
- **THEN** CLI/MCP 使用该文件中的默认项目 ID 和服务地址

#### Scenario: 从父目录读取配置
- **WHEN** 命令在业务仓库子目录执行且父目录存在 `.dataspec/config.json`
- **THEN** CLI/MCP 使用最近父目录中的配置文件

#### Scenario: 配置文件 JSON 非法
- **WHEN** `.dataspec/config.json` 不是合法 JSON
- **THEN** CLI/MCP 返回可读错误，并不继续调用 DataSpec HTTP API

### Requirement: 命令行参数优先
DataSpec CLI/MCP SHALL 让显式命令行参数优先于 `.dataspec/config.json` 中的默认值。

#### Scenario: 覆盖 projectId
- **WHEN** 配置文件包含 `projectId` 且命令行也传入 `--project`
- **THEN** CLI/MCP 使用命令行传入的项目 ID

#### Scenario: 覆盖 server
- **WHEN** 配置文件包含 `server` 且命令行也传入 `--server`
- **THEN** CLI/MCP 使用命令行传入的服务地址

### Requirement: lint-files 默认路径
DataSpec CLI SHALL 在 `lint-files` 未提供位置参数时使用配置文件中的 `defaultPaths`。

#### Scenario: 使用 defaultPaths 扫描 SQL
- **WHEN** 用户运行 `lint-files --project <id>` 且配置文件包含 `defaultPaths`
- **THEN** CLI 扫描 `defaultPaths` 指定的 SQL 文件或目录

#### Scenario: 缺少路径和 defaultPaths
- **WHEN** 用户运行 `lint-files` 且没有位置参数，配置文件也没有 `defaultPaths`
- **THEN** CLI 返回可读错误，提示需要提供 SQL 文件或目录路径

### Requirement: Local AI profile defaults
DataSpec CLI and MCP SHALL read optional AI task profile defaults from `.dataspec/config.json`.

#### Scenario: Config contains ai profile
- **WHEN** `.dataspec/config.json` contains `aiProfile` or `taskType`
- **THEN** CLI/MCP use those values as the default profile selection when explicit command options are omitted.

#### Scenario: Explicit profile overrides config
- **WHEN** a command passes an explicit profile or task type option
- **THEN** the explicit option takes precedence over `.dataspec/config.json`.

#### Scenario: Invalid profile config shape
- **WHEN** `.dataspec/config.json` contains non-string `aiProfile` or `taskType`
- **THEN** CLI/MCP return a readable configuration diagnostic before calling DataSpec HTTP APIs.

### Requirement: Local security profile defaults
DataSpec CLI and MCP SHALL read optional local AI security profile defaults from `.dataspec/config.json`.

#### Scenario: Config contains security profile
- **WHEN** `.dataspec/config.json` contains a valid `securityProfile` object
- **THEN** CLI/MCP SHALL expose the normalized profile to local command handlers
- **AND** existing `projectId`, `server`, `apiToken`, `aiProfile`, `taskType`, and `defaultPaths` behavior SHALL remain compatible.

#### Scenario: Security profile contains invalid field types
- **WHEN** `securityProfile.allowedAiTools`, `securityProfile.neverExportPatterns`, or `securityProfile.localOnlyPaths` is not a string array
- **THEN** CLI/MCP SHALL return a readable configuration error
- **AND** no DataSpec HTTP API call SHALL be made.

#### Scenario: Security profile scalar policy validation
- **WHEN** `securityProfile.redactionStrictness`, `securityProfile.sensitiveFieldPolicy`, `securityProfile.samplePolicy`, or `securityProfile.credentialPolicy` is present but not a string
- **THEN** CLI/MCP SHALL return a readable configuration error
- **AND** the error message SHALL NOT include token, password, Authorization, JDBC URL, DSN, or raw secret values.
