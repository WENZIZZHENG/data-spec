## ADDED Requirements

### Requirement: CLI fixture covers contract import preview
The CLI/MCP contract fixtures SHALL document the `contract-import preview` command as a read-only AI-facing workflow.

#### Scenario: Contract import fixture is validated
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** the fixture set includes `contract-import-preview`
- **AND** the entry lists required options, optional options, output shape, exit codes, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Contract import fixture safety metadata
- **WHEN** the fixture check reads the `contract-import-preview` entry
- **THEN** its safety metadata declares `readOnly=true`, `writesProject=false`, `containsRealBusinessRows=false`, `externalNetworkUsed=false`, `externalLlmUsed=false`, and no raw sensitive example values.
