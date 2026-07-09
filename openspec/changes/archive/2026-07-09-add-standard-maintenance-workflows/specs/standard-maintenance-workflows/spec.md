## ADDED Requirements

### Requirement: 标准维护 workflow dry-run 计划
DataSpec SHALL provide a project-scoped, read-only standard maintenance workflow plan that turns maintenance inbox signals into explicit dry-run steps without executing write operations.

#### Scenario: 生成候选维护计划
- **WHEN** a caller requests a workflow plan with `projectId` and source type `STANDARD_CANDIDATE`
- **THEN** DataSpec returns a plan containing `inboxAction`, `recipeBinding`, `dryRunSteps`, `executionState`, `undoHint`, `evidenceLinks`, and `nextActions`
- **AND** the plan references existing candidate decision APIs or routes as explicit user-confirmed actions, not as automatically executed operations.

#### Scenario: 生成质量或覆盖率维护计划
- **WHEN** a caller requests a workflow plan from field quality or field coverage signals
- **THEN** DataSpec groups low quality fields, issue codes, unmanaged fields, possible duplicates, and partial coverage boundaries into safe maintenance steps
- **AND** the plan identifies precheck, review, execute, verify, and archive phases.

#### Scenario: 空信号返回可恢复计划
- **WHEN** the requested source has no actionable items or only insufficient context
- **THEN** DataSpec returns a dry-run plan with `executionState.status` set to `BLOCKED` or `DRY_RUN`
- **AND** `nextActions` tells the caller which report, candidate list, or diagnostic page to refresh next.

### Requirement: 维护 workflow 安全边界
Standard maintenance workflow plans MUST remain safe for AI-assisted use and MUST NOT expose secrets or perform hidden writes.

#### Scenario: Plan API does not write
- **WHEN** a caller generates a workflow plan
- **THEN** DataSpec MUST NOT create, update, accept, merge, ignore, postpone, delete, or archive any standard field, candidate, task run, source database object, or business repository file.

#### Scenario: Plan output is secret-safe
- **WHEN** a workflow plan includes evidence, routes, commands, step descriptions, or undo guidance
- **THEN** the output MUST NOT include raw SQL, AI raw payloads, candidate raw evidence, API tokens, database passwords, bearer tokens, full JDBC URLs, DSNs, Authorization headers, connection strings, or sampled business data rows.

### Requirement: 维护 workflow 恢复与验证提示
Standard maintenance workflow plans SHALL tell users and AI agents how to resume, verify, and record a maintenance action.

#### Scenario: Plan includes verification and recovery guidance
- **WHEN** a workflow plan contains executable maintenance steps
- **THEN** DataSpec includes verification commands or checks, evidence links, a recoverable current step, and an `undoHint` explaining how to back out or stop safely.

#### Scenario: Failed or partial source is visible
- **WHEN** the source report is partial, failed, cancelled, or blocked
- **THEN** DataSpec includes that state in `executionState` and `evidenceLinks`
- **AND** it MUST NOT treat skipped, failed, or unreviewed items as completed.
