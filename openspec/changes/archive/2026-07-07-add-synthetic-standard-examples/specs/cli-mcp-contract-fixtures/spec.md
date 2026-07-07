## ADDED Requirements

### Requirement: CLI fixture covers synthetic examples
The CLI/MCP contract fixtures SHALL document the `synthetic-examples generate` command as a read-only AI-facing workflow.

#### Scenario: Synthetic examples fixture is validated
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** the fixture set includes `synthetic-examples-generate`
- **AND** the entry lists required options, optional options, output shape, exit codes, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Synthetic examples fixture safety metadata
- **WHEN** the fixture check reads the `synthetic-examples-generate` entry
- **THEN** its safety metadata declares `readOnly=true`, `writesProject=false`, `containsRealBusinessRows=false`, `externalLlmUsed=false`, and no raw sensitive example values.
