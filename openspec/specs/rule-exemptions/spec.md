# rule-exemptions Specification

## Purpose
TBD - created by archiving change add-rule-exemptions. Update Purpose after archive.
## Requirements
### Requirement: Manage project rule exemptions
The system SHALL let users manage project-scoped rule exemptions.

#### Scenario: Create exemption
- **WHEN** a user creates an exemption with projectId, ruleCode, reason, and at least one of tableName or columnName
- **THEN** the exemption is saved as enabled
- **AND** the system rejects exemptions without a reason or without scope.

#### Scenario: Disable exemption
- **WHEN** a user disables an exemption
- **THEN** it no longer suppresses lint issues.

### Requirement: Suppress matching lint issues
The lint engine SHALL mark issues matched by enabled, unexpired exemptions as suppressed.

#### Scenario: Matching exemption
- **WHEN** lint produces an issue whose projectId, ruleCode, tableName, and columnName match an enabled exemption
- **THEN** the issue remains in the result with `suppressed=true`
- **AND** the active error/warning/suggestion counts exclude that issue.

#### Scenario: Expired exemption
- **WHEN** an exemption is expired or disabled
- **THEN** matching lint issues remain active.

### Requirement: Export exemptions to AI Context
The AI Context export SHALL include project rule exemptions.

#### Scenario: Rules export includes exemptions
- **WHEN** a project has enabled rule exemptions
- **THEN** `rules.yaml` or `DATABASE_RULES.md` includes the rule code, scope, reason, and expiry
- **AND** the export states that exemptions are legacy exceptions, not new standards.

### Requirement: Frontend exemption management
The frontend SHALL expose a rule exemption management page.

#### Scenario: View and manage exemptions
- **WHEN** a user opens the rule exemptions page
- **THEN** the frontend lists exemptions for the current project and supports creating, disabling, and deleting them.
