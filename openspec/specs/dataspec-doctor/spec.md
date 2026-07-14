# dataspec-doctor Specification

## Purpose
定义 `dataspec doctor` 环境自检命令，检查服务连通、项目访问、认证身份、默认路径、OpenAPI 状态和 JSON 诊断输出。
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

### Requirement: AI Context cache diagnostics
DataSpec CLI SHALL include repository AI Context cache diagnostics in `doctor` output.

#### Scenario: Cache exists and is fresh
- **WHEN** a user runs `doctor --format json` in a repository with a non-expired `.dataspec/context/cache-metadata.json`
- **THEN** the JSON output includes a context-cache check with status `pass`
- **AND** includes cache metadata such as exportedAt, expiresAt, projectId, source, specVersion, and specHash when available.

#### Scenario: Cache missing
- **WHEN** no `.dataspec/context/cache-metadata.json` exists
- **THEN** `doctor` includes a context-cache check with status `warn`
- **AND** suggests running `export-context --cache`.

#### Scenario: Service unavailable with stale cache
- **WHEN** the DataSpec service check fails and the cache is expired
- **THEN** `doctor` reports the context-cache check as `warn`
- **AND** explains that offline use can continue only with stale context.

#### Scenario: Remote standard differs from cache
- **WHEN** DataSpec service is reachable and current remote standard metadata differs from cached metadata
- **THEN** `doctor` reports the context-cache check as `fail` or `warn`
- **AND** suggests refreshing the cache before AI schema work.

### Requirement: AI profile doctor diagnostics
DataSpec CLI doctor SHALL diagnose repository AI task profile readiness.

#### Scenario: Profile config is valid
- **WHEN** a user runs `doctor --format json` with a valid configured profile
- **THEN** the JSON output includes an `ai-profile` check with status `pass`
- **AND** the check includes the selected profile id, task type, and recommended next command when available.

#### Scenario: Profile config is unknown
- **WHEN** `.dataspec/config.json` references an unknown profile or task type
- **THEN** doctor reports the `ai-profile` check as `fail` or `warn`
- **AND** it suggests supported profile ids or task types.

#### Scenario: Service unavailable
- **WHEN** DataSpec service is unavailable
- **THEN** doctor still reports local profile configuration shape
- **AND** it marks remote profile validation as unavailable rather than throwing an uncaught exception.

### Requirement: Doctor reports local config schema compatibility
DataSpec doctor SHALL expose a secret-safe summary of the local config schema and version state without introducing a second runtime fingerprint protocol.

#### Scenario: Config uses the supported schema
- **WHEN** config declares the supported version and local schema reference and the schema file exists
- **THEN** doctor reports supportedVersion, declaredVersion, effectiveVersion, schemaRef, schemaPath, schemaFilePresent, and associationStatus
- **AND** the config check passes without exposing apiToken, security patterns, or local-only path values
- **AND** a non-canonical schemaRef is represented by a fixed safe placeholder rather than its raw value.

#### Scenario: Legacy config has no association
- **WHEN** config omits `$schema`, `configVersion`, or the local schema file
- **THEN** doctor keeps the config usable and returns a warning with the effective legacy version and a migration hint.

#### Scenario: Config declares an unsupported future version
- **WHEN** configVersion is greater than the current doctor supportedVersion
- **THEN** the config check fails and tells the caller to upgrade the CLI or use a supported config
- **AND** doctor does not silently claim the future version is compatible.

#### Scenario: No local config exists
- **WHEN** doctor cannot find `.dataspec/config.json`
- **THEN** it reports the supported schema version and expected local schema filename alongside the existing missing-config warning.
