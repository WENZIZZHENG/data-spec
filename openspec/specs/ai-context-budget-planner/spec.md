# ai-context-budget-planner Specification

## Purpose
定义 DataSpec 如何在导出 AI Context 前生成 token 预算计划，帮助用户按任务目标选择必要上下文、识别质量风险并获得可复现的导出参数建议。
## Requirements
### Requirement: Generate AI Context budget plan
DataSpec SHALL generate a read-only AI Context budget plan for a project and task before exporting an AI Context package.

#### Scenario: Plan within a token budget
- **WHEN** a caller requests an AI Context budget plan with `projectId`, optional `taskType` or `profileId`, optional `query` or target hints, and `tokenBudget`
- **THEN** DataSpec returns stable JSON containing `kind`, `schemaVersion`, `projectId`, `request`, `estimation`, `selectedArtifacts[]`, `droppedArtifacts[]`, `qualityRisk`, `fallbackSteps[]`, `recommendedExportParams`, `diagnostics[]`, and `recommendedNextActions[]`.
- **AND** each selected or dropped artifact includes `artifact`, `estimatedTokens`, `reason`, `riskImpact`, and the effective scope or limit when applicable.

#### Scenario: Recommend full, standard, and minimal plans
- **WHEN** the token budget can fit all important AI Context artifacts
- **THEN** the plan marks quality risk as `LOW` and recommends full export parameters.
- **AND** when the budget is lower but enough for scoped field catalog and database rules
- **THEN** the plan marks quality risk as `MEDIUM` and recommends scoped export parameters.
- **AND** when the budget is too low for critical field/rule context
- **THEN** the plan marks quality risk as `HIGH` and recommends increasing budget, narrowing query, or stopping for human confirmation.

#### Scenario: Use deterministic estimation
- **WHEN** DataSpec estimates context size
- **THEN** it uses a deterministic local estimate and reports `estimationMethod`.
- **AND** it MUST NOT call an external LLM, external tokenizer service, or upload project standards to any external service.

### Requirement: Keep budget planning safe and non-sensitive
DataSpec SHALL keep AI Context budget planning read-only and non-sensitive.

#### Scenario: Read-only budget plan
- **WHEN** DataSpec builds a budget plan
- **THEN** it reads project metadata only and does not create, modify, delete, export, cache, stage, commit, or push AI Context files.

#### Scenario: Non-sensitive response
- **WHEN** a budget plan is returned
- **THEN** the response contains counts, estimates, artifact names, risks, recommended parameters, and next actions.
- **AND** it MUST NOT include raw token, password, Authorization header, API key, complete JDBC URL, DSN, connection string, source database rows, or full field catalog content.

### Requirement: Budget plan diagnostics
DataSpec SHALL explain missing or weak context that affects AI task readiness.

#### Scenario: Missing query for narrow budget
- **WHEN** a caller provides a low token budget without query, target table, target file, or profile context
- **THEN** the plan includes a diagnostic explaining that narrow retrieval may be low quality.
- **AND** recommended next actions include providing a query or increasing tokenBudget.

#### Scenario: Empty scoped matches
- **WHEN** the effective scope and query match no standard fields or examples
- **THEN** the plan marks quality risk at least `MEDIUM`.
- **AND** fallback steps suggest broadening the query, using full context, or checking field search before proceeding.
