## ADDED Requirements

### Requirement: Dialect capability matrix
DataSpec SHALL expose a deterministic PostgreSQL/MySQL dialect capability matrix for AI, CLI, API, and frontend consumers.

#### Scenario: Matrix lists verified capabilities
- **WHEN** a client requests or receives dialect diagnostics
- **THEN** each diagnostic identifies the dialect, capability, support level, code, message, and next action
- **AND** capabilities without regression coverage are not marked as fully supported

#### Scenario: Unsupported dialect is explicit
- **WHEN** SQL or database metadata indicates a dialect outside PostgreSQL/MySQL
- **THEN** DataSpec returns a diagnostic with an unsupported or unknown dialect code
- **AND** it does not silently treat the dialect as verified

### Requirement: SQL text dialect inference
DataSpec SHALL infer the likely SQL dialect from common DDL features while preserving backward-compatible SQL lint and reverse-import requests.

#### Scenario: Infer MySQL from DDL features
- **WHEN** SQL contains MySQL features such as backtick identifiers, `AUTO_INCREMENT`, inline column comments, `ENGINE`, or `DEFAULT CHARSET`
- **THEN** diagnostics identify the dialect as `mysql`
- **AND** partial or unsupported capabilities are included where relevant

#### Scenario: Default to PostgreSQL with uncertainty notice
- **WHEN** SQL has no MySQL-specific feature and no explicit dialect
- **THEN** diagnostics identify the dialect as `postgresql`
- **AND** include a notice that explicit dialect selection is more reliable for mixed or vendor-specific SQL

### Requirement: Frontend and CLI visibility
Dialect diagnostics SHALL be visible in frontend workflows and CLI output without breaking existing JSON fields.

#### Scenario: Frontend displays dialect summary
- **WHEN** SQL lint, DDL generation, or reverse import returns dialect diagnostics
- **THEN** the frontend shows the current dialect and any warning or degradation reason

#### Scenario: CLI preserves machine-readable diagnostics
- **WHEN** CLI lint or generation commands are run with JSON output
- **THEN** the JSON output includes the full `dialectDiagnostics` array
- **AND** text output includes a concise dialect summary
