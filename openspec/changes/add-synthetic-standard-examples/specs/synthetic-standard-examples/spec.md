## ADDED Requirements

### Requirement: Synthetic Standard Example Package
DataSpec SHALL generate a deterministic, project-scoped synthetic standard example package for supported business scenarios.

#### Scenario: Generate supported scenario package
- **WHEN** a caller requests synthetic examples with `projectId`, `scenario` equal to `user`, `order`, `payment`, or `audit`, and optional generation parameters
- **THEN** the response includes stable fields `kind`, `schemaVersion`, `projectId`, `scenario`, `specHash`, `generationParams`, `sourceSummary`, `goodSql`, `badSql`, `ddlPreviewInputs`, `fieldSuggestionQuestions`, `standardQaCases`, `expectedDiagnostics`, `safety`, and `nextActions`.
- **AND** the case arrays contain deterministic identifiers, titles, scenario names, source references, and AI-readable descriptions.

#### Scenario: Spec hash is deterministic
- **WHEN** the same project standard summary, scenario, schema version, and generation parameters are used twice
- **THEN** the generated package uses the same `specHash`.
- **AND** when selected standard fields, templates, scenario, schema version, or generation parameters change
- **THEN** the `specHash` changes.

#### Scenario: Unsupported scenario is rejected
- **WHEN** a caller requests an unsupported synthetic scenario
- **THEN** DataSpec rejects the request with a non-sensitive validation diagnostic that lists supported scenario values.

### Requirement: Synthetic Case Coverage
The synthetic example package SHALL cover SQL, DDL preview, field suggestion, standard Q&A, and expected diagnostics for each generated scenario.

#### Scenario: Good and bad SQL cases are paired with diagnostics
- **WHEN** DataSpec generates a package for a supported scenario
- **THEN** `goodSql` includes examples that use preferred standard fields and comments for that scenario.
- **AND** `badSql` includes anti-examples with `expectedDiagnostics` references that identify the intended rule or standard mismatch.

#### Scenario: DDL preview and field suggestion inputs are reusable
- **WHEN** DataSpec generates a package for a supported scenario
- **THEN** `ddlPreviewInputs` contains table names, business object descriptions, and expected field names suitable for DDL preview tests.
- **AND** `fieldSuggestionQuestions` contains natural-language questions with expected standard field names or fallback names.

#### Scenario: Standard Q&A cases cite standard evidence
- **WHEN** DataSpec generates `standardQaCases`
- **THEN** each Q&A case includes a question, expected answer outline, referenced field names, and confidence or review guidance for AI evaluation.

### Requirement: Synthetic Generation Safety
Synthetic example generation SHALL be read-only and SHALL NOT expose real secrets or business rows.

#### Scenario: Generation has no write side effects
- **WHEN** a caller generates synthetic examples through API or CLI
- **THEN** DataSpec does not create, update, delete, import, export, or persist project records.
- **AND** the `safety` object declares `readOnly=true`, `writesProject=false`, `containsRealBusinessRows=false`, and `externalLlmUsed=false`.

#### Scenario: Sensitive content is not emitted
- **WHEN** standard metadata or generation parameters include token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string patterns
- **THEN** the generated package redacts those values from examples, diagnostics, `sourceSummary`, and `nextActions`.

#### Scenario: Insufficient metadata uses explicit fallback
- **WHEN** a project lacks enough matching fields, templates, code-set references, or relation hints for the selected scenario
- **THEN** DataSpec may use built-in synthetic scenario defaults
- **AND** the package records fallback usage in `sourceSummary` or diagnostics without pretending the fallback came from persisted project standards.
