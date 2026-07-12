## ADDED Requirements

### Requirement: CLI fixture covers test data generation
The CLI/MCP contract fixtures SHALL document the `test-data generate` command as a read-only AI-facing workflow.

#### Scenario: Test data fixture is validated
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** the fixture set includes `test-data-generate`
- **AND** the entry lists required options, optional options, output shape, exit codes, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Test data fixture safety metadata
- **WHEN** the fixture check reads the `test-data-generate` entry
- **THEN** its safety metadata declares `readOnly=true`, `writesProject=false`, `writesBusinessRepo=false`, `containsRealBusinessRows=false`, `externalNetworkUsed=false`, `externalLlmUsed=false`, and no raw sensitive example values.

### Requirement: CLI fixture covers consumer compatibility check
The CLI/MCP contract fixtures SHALL document the `consumer-compat check` command as a local read-only compatibility workflow.

#### Scenario: Compatibility fixture is validated
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** the fixture set includes `consumer-compat-check`
- **AND** the entry lists required options, optional options, output shape, exit codes, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Compatibility fixture safety metadata
- **WHEN** the fixture check reads the `consumer-compat-check` entry
- **THEN** its safety metadata declares `readOnly=true`, `writesProject=false`, `requiresServer=false`, `externalNetworkUsed=false`, `externalLlmUsed=false`, and no raw sensitive example values.
