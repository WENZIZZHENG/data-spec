## ADDED Requirements

### Requirement: Contract import preview contract fixtures
The AI contract fixture checks SHALL cover contract candidate preview package stable fields and redaction.

#### Scenario: Contract import stable fields drift
- **WHEN** backend or CLI output for contract candidate preview loses `kind`, `schemaVersion`, `projectId`, `sourceKind`, `sourcePath`, `contractHash`, `summary`, `candidateFields`, `diagnostics`, `safety`, or `nextActions`
- **THEN** backend or Node contract tests fail with a readable assertion.

#### Scenario: Contract candidate stable fields drift
- **WHEN** a candidate item loses `candidateKey`, `candidateName`, `dataType`, `sourcePath`, `confidence`, `recommendedAction`, `conflictReasons`, or `inboxPayload`
- **THEN** backend or Node contract tests fail with a readable assertion.

#### Scenario: Contract import redaction drifts
- **WHEN** contract import inputs or metadata contain token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values
- **THEN** tests fail if any raw sensitive value appears in generated package JSON, CLI output, fixtures, or logs.
