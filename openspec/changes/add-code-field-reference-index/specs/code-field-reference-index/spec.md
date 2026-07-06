## ADDED Requirements

### Requirement: Generate business code field reference index
DataSpec SHALL generate a read-only business code field reference index for one or more standard field names inside configured business-repository paths.

#### Scenario: Index references inside configured paths
- **WHEN** a user runs field reference indexing with at least one field name and explicit paths or `.dataspec/config.json` `defaultPaths`
- **THEN** DataSpec returns stable JSON containing `kind`, `schemaVersion`, `fields[]`, `summary`, `references[]`, `renameRisk`, `suggestedAction`, `diagnostics[]`, and `nextActions[]`.
- **AND** each reference includes `fieldName`, `matchedText`, `referenceKind`, `file`, `line`, `column`, `confidence`, `possibleReference`, and a non-sensitive `snippet`.

#### Scenario: Stop when no scan paths are configured
- **WHEN** a user runs field reference indexing without explicit paths and no `defaultPaths` are configured
- **THEN** DataSpec returns a recoverable diagnostic with code `DATASPEC_DEFAULT_PATHS_MISSING`.
- **AND** DataSpec MUST NOT scan the entire repository.

#### Scenario: Skip large generated directories
- **WHEN** a configured path contains `.git`, `node_modules`, `dist`, `build`, `target`, or other skipped generated directories
- **THEN** DataSpec omits those directories from reference scanning.
- **AND** the output summary includes enough diagnostics or counts for the user to understand the bounded scan scope.

### Requirement: Classify field references and confidence
DataSpec SHALL classify business code references by source kind and confidence so rename risk can be reviewed without pretending every match is exact.

#### Scenario: High-confidence SQL or DDL identifier match
- **WHEN** a field name or alias appears as a SQL/DDL identifier, quoted identifier, backtick identifier, or dotted table-column path in a SQL or migration file
- **THEN** the reference uses a SQL or DDL `referenceKind`.
- **AND** the reference confidence is `HIGH`.

#### Scenario: Medium-confidence model or config match
- **WHEN** a field name or alias appears in common ORM, model, schema, mapper, JSON, YAML, or config files
- **THEN** the reference uses a model, mapper, schema, or config `referenceKind`.
- **AND** the reference confidence is at least `MEDIUM` unless the match is only a free-text mention.

#### Scenario: Low-confidence free-text match
- **WHEN** a field name or alias appears only as prose, comments, or an ambiguous token
- **THEN** the reference is marked `possibleReference=true`.
- **AND** the confidence is `LOW`.

### Requirement: Report rename risk and suggested action
DataSpec SHALL summarize rename and deprecation risk from business code references.

#### Scenario: High rename risk
- **WHEN** at least one high-confidence SQL, DDL, migration, mapper, or model reference exists
- **THEN** `renameRisk` is `HIGH`.
- **AND** `suggestedAction` tells the user to review or update the referenced business files before renaming or deprecating the field.

#### Scenario: No references found
- **WHEN** no configured files reference the requested field names or aliases
- **THEN** `renameRisk` is `LOW`.
- **AND** the output says the scan found no known business-code references within the configured scope.

### Requirement: Keep reference indexing read-only and non-sensitive
DataSpec SHALL keep business code reference indexing read-only and MUST NOT expose secrets in output.

#### Scenario: Read-only scan
- **WHEN** DataSpec scans business repository files for field references
- **THEN** it reads files only and does not create, modify, delete, stage, commit, or push business repository files.

#### Scenario: Redact sensitive output
- **WHEN** a matched line or diagnostic contains token, password, Authorization, API key, JDBC URL, DSN, or connection-string-like text
- **THEN** DataSpec redacts the sensitive value from `snippet`, stdout, stderr, diagnostics, and example output.
