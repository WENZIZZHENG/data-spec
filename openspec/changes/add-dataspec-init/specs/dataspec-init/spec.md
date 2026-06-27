## ADDED Requirements

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
