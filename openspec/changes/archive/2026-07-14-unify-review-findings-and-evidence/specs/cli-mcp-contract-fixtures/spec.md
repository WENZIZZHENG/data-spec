## ADDED Requirements

### Requirement: CLI and MCP fixtures cover shared findings
CLI/MCP contract fixtures SHALL document structured finding input, output, evidence gating, and review delivery metadata.

#### Scenario: Fixture validates AI output findings
- **WHEN** fixture validation runs
- **THEN** the ai-output check CLI and `check_ai_output` MCP entries describe optional findings, shared fields, bounds, PASS/WARN/FAIL behavior, and secret-safe examples
- **AND** the checker recursively compares the live MCP findings item schema, including fields, required, additionalProperties, schemaVersion minimum, array limits, and every text maxLength
- **AND** required fields, subject/location/waiver fields, per-text bounds, live descriptor drift, or missing evidence-gating metadata fail with a readable diagnostic.

#### Scenario: Fixture validates post-check receipt handoff
- **WHEN** fixture validation runs
- **THEN** post-check output documents verificationReceipt and Evidence Package input documents postCheckReceipt
- **AND** fixture metadata states that external findings require a matching receipt in addition to summary and evidence revalidation.

#### Scenario: Fixture validates review delivery envelope
- **WHEN** fixture validation runs
- **THEN** the review-pr entry documents commitSha, comment URLs, findings, SQL check IDs, postCheck, evidence package entries, existing fields, and exit codes
- **AND** examples contain no raw GitHub token, Authorization value, JDBC URL, DSN, or reusable secret.

### Requirement: CLI can post-check a findings file
The CLI SHALL accept an optional bounded JSON findings file for AI output post-check.

#### Scenario: CLI checks structured findings
- **WHEN** an AI agent runs `ai-output check` with `--findings <json>`
- **THEN** the CLI parses a JSON array, sends it to the existing post-check API, and prints the additive normalized findings result
- **AND** invalid JSON, non-array input, non-positive schemaVersion, unknown top-level/nested fields, or unsafe bounds fail before the server call with exit code 2.
