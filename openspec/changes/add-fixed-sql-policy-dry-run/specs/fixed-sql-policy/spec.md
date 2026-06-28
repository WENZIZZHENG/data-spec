## ADDED Requirements

### Requirement: Request-level fixed SQL policy
The system SHALL allow clients to control deterministic fixed SQL generation per lint request without changing project-level rule configuration.

#### Scenario: Default policy preserves existing behavior
- **WHEN** a client submits SQL to `/api/lint` without `fixPolicy`
- **THEN** DataSpec uses the default fixed SQL generation behavior
- **AND** the response includes the effective policy used for the result.

#### Scenario: Safe-only policy filters higher risk changes
- **WHEN** a client submits `fixPolicy.maxRiskLevel=LOW`
- **THEN** DataSpec only applies fixer changes whose risk level is `LOW`
- **AND** higher risk fixable issues are returned with skipped explanations.

#### Scenario: Disabled policy skips fixed SQL
- **WHEN** a client submits `fixPolicy.mode=DISABLED`
- **THEN** DataSpec does not return `fixedSql` or `fixedSqlDiff`
- **AND** the response includes fix explanations for skipped deterministic fixes.

### Requirement: Fixed SQL dry-run plan
The system SHALL support a dry-run mode that previews deterministic fixed SQL and explains every planned change.

#### Scenario: Dry-run returns preview and explicit dry-run flag
- **WHEN** a client submits `fixPolicy.mode=DRY_RUN`
- **THEN** DataSpec returns `fixDryRun=true`
- **AND** the response includes `fixedSql`, `fixedSqlDiff`, `fixChanges`, and next actions for manual review when a safe preview can be generated.

#### Scenario: Dry-run cannot safely rebuild
- **WHEN** deterministic fixes are requested but SQL cannot be safely rebuilt
- **THEN** DataSpec returns no `fixedSql`
- **AND** the fix plan explains why fixed SQL generation was skipped.

### Requirement: Machine-readable fix plan
The system SHALL expose a machine-readable fix plan for AI agents and frontends.

#### Scenario: Fix plan includes applied changes
- **WHEN** fixed SQL is generated
- **THEN** each applied change includes `ruleCode`, `riskLevel`, `changeType`, `tableName`, optional `columnName`, `before`, `after`, and `explain`
- **AND** the response summary includes applied, skipped, and available change counts.

#### Scenario: Fix plan includes skipped reasons
- **WHEN** a deterministic fix is not applied because of policy, suppression, unsupported rule, or unsafe rebuild
- **THEN** the response includes a skipped explanation with a stable reason code and human-readable message.
