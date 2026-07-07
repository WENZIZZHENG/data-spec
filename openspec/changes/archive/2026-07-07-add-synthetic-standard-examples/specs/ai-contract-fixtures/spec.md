## ADDED Requirements

### Requirement: Synthetic example contract fixtures
The AI contract fixture checks SHALL cover synthetic standard example package stable fields and redaction.

#### Scenario: Synthetic example stable fields drift
- **WHEN** backend or CLI output for synthetic standard examples loses `kind`, `schemaVersion`, `projectId`, `scenario`, `specHash`, `generationParams`, `goodSql`, `badSql`, `ddlPreviewInputs`, `fieldSuggestionQuestions`, `standardQaCases`, `expectedDiagnostics`, `safety`, or `nextActions`
- **THEN** backend or Node contract tests fail with a readable assertion.

#### Scenario: Synthetic example redaction drifts
- **WHEN** synthetic example inputs or metadata contain token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values
- **THEN** tests fail if any raw sensitive value appears in generated package JSON, CLI output, fixtures, or logs.
