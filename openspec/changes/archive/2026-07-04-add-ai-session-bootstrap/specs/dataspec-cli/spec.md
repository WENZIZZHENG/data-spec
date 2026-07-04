## ADDED Requirements

### Requirement: CLI session bootstrap command
The DataSpec CLI SHALL expose a `bootstrap` command for AI agents starting a new repository session.

#### Scenario: Bootstrap from server
- **WHEN** a user runs `dataspec bootstrap --project <id> --format json`
- **THEN** the CLI calls `/api/bootstrap/session`
- **AND** it prints the returned bootstrap package as stable JSON without exposing token values.

#### Scenario: Bootstrap server unavailable
- **WHEN** the DataSpec server cannot be reached while running `bootstrap --format json`
- **THEN** the CLI prints a local fallback bootstrap package with `status` set to `BLOCKED`
- **AND** the package contains structured `nextActions` suggesting `dataspec doctor --format json`, service startup, token verification, or project selection as applicable.

#### Scenario: Bootstrap exit code
- **WHEN** the bootstrap status is `READY`
- **THEN** the CLI exits with code `0`
- **AND** when the status is `DEGRADED` or `BLOCKED`, it exits with code `1`.

#### Scenario: Bootstrap text output
- **WHEN** a user runs `dataspec bootstrap --format text`
- **THEN** the CLI prints a concise readable summary of status, project, spec version, recommended commands, risks, and next actions.
