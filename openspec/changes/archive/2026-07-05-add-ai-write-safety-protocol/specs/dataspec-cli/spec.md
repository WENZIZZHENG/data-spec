## ADDED Requirements

### Requirement: CLI exposes capability safety metadata
The DataSpec CLI SHALL preserve and validate AI write safety metadata in capability commands.

#### Scenario: Capability list preserves safety JSON
- **WHEN** a user runs `dataspec capability list --format json`
- **THEN** the CLI prints the server catalog including each capability `safety` object
- **AND** it does not remove existing `writeRisk`, `preflightChecks`, or `nextActions` fields.

#### Scenario: Capability show text includes safety summary
- **WHEN** a user runs `dataspec capability show <id> --format text`
- **THEN** the CLI prints a concise safety summary including read-only/write status, dry-run requirement, idempotency requirement, sensitive input categories, and next actions.

#### Scenario: Capability check validates safety metadata
- **WHEN** a user runs `dataspec capability check --format json`
- **THEN** the CLI validates that every core capability includes a valid `safety` object with the required fields
- **AND** write-capable capabilities without safety metadata fail the check with a diagnostic instead of being treated as safe.

### Requirement: CLI returns structured safety errors
The DataSpec CLI SHALL surface backend safety diagnostics without exposing secrets.

#### Scenario: CLI receives idempotency safety error
- **WHEN** a CLI write command receives a backend diagnostic for a missing idempotency key
- **THEN** the CLI exits with code `2`
- **AND** stderr includes `DataSpecError` JSON with the backend safety `code`, `category`, `missing`, `safety`, and `nextActions`.

#### Scenario: CLI safety error redaction
- **WHEN** a safety error or argument error contains token, password, Authorization, API key, complete JDBC URL, DSN, or connection string text
- **THEN** the CLI redacts those values from stdout and stderr.

#### Scenario: Existing low-risk commands remain compatible
- **WHEN** an existing read-only or low-risk command does not declare `safety.requiresIdempotencyKey=true`
- **THEN** the CLI does not require a new idempotency key parameter solely because the command has compatible safety metadata.
