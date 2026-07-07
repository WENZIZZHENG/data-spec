# standard-quality-gate Specification

## Purpose
定义项目标准质量门禁策略和评估结果，用覆盖率、字段质量、错误数量、新增未管理字段和敏感标记要求给出 AI 与 CI 可读的通过或失败状态。
## Requirements
### Requirement: Configure project quality gate
DataSpec SHALL allow each project to store a lightweight standard quality gate policy.

#### Scenario: Read default gate
- **WHEN** a caller requests the quality gate config for a project without a saved config
- **THEN** DataSpec returns a default disabled policy with stable threshold fields.

#### Scenario: Save gate thresholds
- **WHEN** a caller saves a gate config with valid threshold values
- **THEN** DataSpec persists `enabled`, `minCoverage`, `minAverageFieldScore`, `maxErrorIssues`, `maxNewUnmanagedFields`, and `requiredSensitiveMarking` for that project.

#### Scenario: Reject invalid thresholds
- **WHEN** a caller saves negative counts or percentage values outside `0..100`
- **THEN** DataSpec rejects the request with a validation diagnostic and does not update the saved policy.

### Requirement: Evaluate project quality gate
DataSpec SHALL evaluate project quality gate status from existing quality, coverage, and lint signals.

#### Scenario: Gate passes
- **WHEN** the saved policy is enabled and all available signals satisfy their thresholds
- **THEN** DataSpec returns status `PASS`, check results with actual and expected values, and non-blocking next actions.

#### Scenario: Gate fails
- **WHEN** one or more enabled checks violate their thresholds
- **THEN** DataSpec returns status `FAIL`, failedChecks ordered by severity, and AI-readable nextActions pointing to the relevant repair workflow.

#### Scenario: Missing optional signal
- **WHEN** a policy includes a signal such as coverage or lint summary but no current value is available
- **THEN** DataSpec marks that check `SKIPPED` or `WARNING` instead of fabricating a pass.

### Requirement: AI-readable gate result
Quality gate evaluation results SHALL be safe and stable for AI and CI consumption.

#### Scenario: Result contains stable fields
- **WHEN** a gate evaluation is returned
- **THEN** the response includes `projectId`, `status`, `enabled`, `summary`, `checks`, `failedChecks`, `nextActions`, and `evaluatedAt`.

#### Scenario: Result avoids secrets
- **WHEN** quality gate checks include lint or coverage context
- **THEN** the result MUST NOT include token, password, Authorization header, full JDBC URL, or source database row values.

