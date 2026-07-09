## ADDED Requirements

### Requirement: CLI fixture covers code patch plan
The CLI/MCP contract fixtures SHALL document the `code-patch plan` command as a read-only AI-facing workflow for business code patch planning.

#### Scenario: Code patch plan fixture is validated
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** the fixture set includes `code-patch-plan`.
- **AND** the entry lists required options, optional options, output shape, exit codes, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Code patch plan fixture safety metadata
- **WHEN** the fixture check reads the `code-patch-plan` entry
- **THEN** its safety metadata declares `readOnly=true`, `writesProject=false`, `requiresDryRun=true`, `requiresIdempotencyKey=false`, `externalNetworkUsed=false`, `externalLlmUsed=false`, and no raw sensitive example values.
