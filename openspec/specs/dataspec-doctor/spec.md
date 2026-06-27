# dataspec-doctor Specification

## Purpose
TBD - created by archiving change add-dataspec-doctor. Update Purpose after archive.
## Requirements
### Requirement: CLI 环境自检
DataSpec CLI SHALL provide a `doctor` command that checks whether the local DataSpec CLI environment is ready for AI, CI, or personal use.

#### Scenario: 自检全部通过
- **WHEN** a user runs `doctor --project <id> --server <url>`
- **THEN** the CLI checks DataSpec server reachability, project accessibility, auth identity, default paths, and OpenAPI availability
- **AND** it exits with code `0`
- **AND** it prints a readable summary of passed checks

#### Scenario: 自检存在失败项
- **WHEN** one or more doctor checks fail
- **THEN** the CLI exits with code `1`
- **AND** it prints the failed check name and a readable remediation message
- **AND** it does not throw an uncaught exception stack trace

#### Scenario: 命令参数非法
- **WHEN** a user passes an unknown option or invalid option value to `doctor`
- **THEN** the CLI exits with code `2`
- **AND** it prints a readable argument error to stderr

### Requirement: 机器可读输出
DataSpec CLI SHALL allow `doctor` results to be emitted as stable JSON for AI agents.

#### Scenario: JSON 输出
- **WHEN** a user runs `doctor --format json`
- **THEN** the CLI prints a JSON object containing `ok`, `server`, `projectId`, and `checks`
- **AND** each check contains `name`, `status`, and `message`

### Requirement: 配置与 token 诊断
DataSpec CLI SHALL make `doctor` use the same configuration and token precedence as other CLI commands.

#### Scenario: 读取仓库级配置
- **WHEN** `.dataspec/config.json` contains `projectId`, `server`, `apiToken`, and `defaultPaths`
- **THEN** `doctor` uses those values when explicit options are omitted
- **AND** it reports the config file path in the config check

#### Scenario: 显式参数覆盖配置
- **WHEN** config contains `projectId` and `server`, and the user passes `--project` or `--server`
- **THEN** `doctor` uses the explicit CLI values for checks

#### Scenario: token 身份可读
- **WHEN** a DataSpec API token is available from `--dataspec-token`, `DATASPEC_TOKEN`, or config
- **THEN** `doctor` sends it as a Bearer token to DataSpec API requests
- **AND** it reports the current auth identity when `/api/auth/me` succeeds

### Requirement: OpenAPI 状态检查
DataSpec CLI SHALL include OpenAPI status in `doctor` output.

#### Scenario: 默认轻量检查
- **WHEN** a user runs `doctor`
- **THEN** the CLI checks that `/api-docs` is reachable and the local `dataspec-web/src/api/schema.ts` file exists

#### Scenario: 完整契约漂移检查
- **WHEN** a user runs `doctor --check-openapi`
- **THEN** the CLI runs the existing OpenAPI schema drift check against the configured server API docs
- **AND** it reports a failed `openapi` check if the generated schema differs from the committed schema
