# dataspec-init Specification

## Purpose
定义 `dataspec init` 对业务仓库的初始化能力，生成 `.dataspec` 配置、README 和可选 AGENTS 片段，同时避免默认覆盖用户文件。
## Requirements
### Requirement: Initialize DataSpec project files
DataSpec CLI SHALL provide an `init` command that initializes a business repository for DataSpec usage.

#### Scenario: Create initial config and README
- **WHEN** a user runs `init --project <id> --server <url>` in a repository without `.dataspec/config.json`
- **THEN** DataSpec creates `.dataspec/config.json` containing `projectId`, normalized `server`, and `defaultPaths`.
- **AND** DataSpec creates `.dataspec/README.md` with common CLI/MCP usage and token guidance.

#### Scenario: Default paths are configurable
- **WHEN** a user runs `init --project <id> --default-path sql --default-path db/migrations`
- **THEN** `.dataspec/config.json` stores those default paths in the same order.

#### Scenario: Init rejects missing project
- **WHEN** a user runs `init` without `--project` and no existing config provides projectId
- **THEN** DataSpec exits with an error explaining that projectId is required.

### Requirement: Protect existing user files
DataSpec CLI SHALL avoid overwriting existing business repository files unless the user explicitly opts in.

#### Scenario: Existing files are skipped by default
- **WHEN** `.dataspec/config.json` or `.dataspec/README.md` already exists
- **THEN** `init` leaves the existing file unchanged.
- **AND** the output reports the file as skipped.

#### Scenario: Force updates DataSpec managed files
- **WHEN** a user runs `init --force`
- **THEN** DataSpec overwrites `.dataspec/config.json` and `.dataspec/README.md` with the newly generated content.

### Requirement: Add optional AGENTS instruction fragment
DataSpec CLI SHALL optionally add DataSpec instructions to the repository `AGENTS.md` without duplicating the fragment.

#### Scenario: Append AGENTS fragment with marker
- **WHEN** a user runs `init --with-agents`
- **THEN** DataSpec writes a DataSpec section between `<!-- dataspec-agents:start -->` and `<!-- dataspec-agents:end -->` in `AGENTS.md`.

#### Scenario: Existing AGENTS marker is replaced only with force
- **WHEN** `AGENTS.md` already contains a DataSpec managed marker block
- **THEN** `init --with-agents` without `--force` leaves the marker block unchanged and reports it as skipped.
- **AND** `init --with-agents --force` replaces only the marker block.

### Requirement: Run doctor after initialization
DataSpec CLI SHALL run a light `doctor` check after initializing project files.

#### Scenario: Init includes doctor result in JSON output
- **WHEN** a user runs `init --project <id> --format json`
- **THEN** DataSpec outputs JSON containing written files, skipped files, configPath, and doctor checks.

#### Scenario: Init text output summarizes next actions
- **WHEN** a user runs `init --project <id>`
- **THEN** DataSpec prints the generated or skipped files and the doctor summary.
- **AND** the output includes next-step commands that AI agents can run.

### Requirement: Init generates editor-ready config schema assets
DataSpec CLI SHALL initialize a local JSON Schema and associate the generated config with the supported schema version.

#### Scenario: New repository receives schema association
- **WHEN** a user runs `init --project <id> --server <url>` in a repository without DataSpec managed files
- **THEN** DataSpec writes `.dataspec/config.schema.json`, `.dataspec/config.json`, and `.dataspec/README.md`
- **AND** config contains `$schema: "./config.schema.json"`, `configVersion: 1`, projectId, normalized server, and defaultPaths
- **AND** config does not contain an API token or another reusable credential.

#### Scenario: Existing managed files remain protected
- **WHEN** one or more of the schema, config, or README files already exist
- **THEN** init without `--force` leaves each existing file unchanged and reports it as skipped
- **AND** missing managed files are still created independently.

#### Scenario: Force refreshes schema assets
- **WHEN** a user runs `init --force`
- **THEN** DataSpec replaces the schema, config, and README with the current managed versions
- **AND** the generated config and schema declare the same supported config version.

#### Scenario: Server URL contains reusable userinfo
- **WHEN** init receives a server URL containing a username or password
- **THEN** it rejects the URL before writing schema, config, README, or AGENTS files
- **AND** the diagnostic does not echo the username or password.

#### Scenario: Existing config declares a future version
- **WHEN** an existing configVersion is greater than the current CLI supported version
- **THEN** init rejects the operation before writing any managed file even when `--force` is present
- **AND** it does not downgrade, replace, or partially pair the future config with a current schema.
