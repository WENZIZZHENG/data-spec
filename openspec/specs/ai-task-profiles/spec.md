# ai-task-profiles Specification

## Purpose
定义项目级 AI 任务画像，描述常见 DataSpec 任务的上下文范围、规则集、输出格式、默认策略、诊断结果和推荐命令。
## Requirements
### Requirement: Project AI task profiles
The system SHALL expose AI task profiles that describe how an AI agent should use DataSpec for common project tasks.

#### Scenario: List project profiles
- **WHEN** a client requests AI task profiles for a project
- **THEN** the response includes built-in profiles for table generation, SQL fixing, reverse import, PR review, and minimal context usage
- **AND** each profile includes `profileId`, `taskType`, `displayName`, `description`, `contextScope`, `ruleset`, `fixedSqlPolicy`, `outputFormat`, `maxContextFields`, `recommendedCommands`, and `nextActions`.

#### Scenario: Get profile by task type
- **WHEN** a client requests a known task type
- **THEN** the response returns the matching profile and marks whether it is the default profile for the project or local config.

#### Scenario: Unknown task type
- **WHEN** a client requests an unknown task type
- **THEN** the response returns an AI-readable error or diagnostic containing supported task types and a suggested fallback.

### Requirement: Profile diagnostics
The system SHALL diagnose whether a selected AI task profile can be used safely for the current project.

#### Scenario: Profile has usable project context
- **WHEN** a project has fields and enabled rules that match the selected profile
- **THEN** diagnostics include a passing status and recommended next command.

#### Scenario: Profile references unavailable context
- **WHEN** the selected profile needs fields, rules, or scoped context that are missing
- **THEN** diagnostics include warning or failure items with stable codes and next actions.

### Requirement: SQL lint profile policy
The system SHALL allow SQL lint requests to use an AI task profile as the source of default fixed SQL policy and output expectations.

#### Scenario: Lint uses profile fixed SQL policy
- **WHEN** a lint request names a profile and does not pass an explicit `fixPolicy`
- **THEN** DataSpec uses the profile's `fixedSqlPolicy` as the effective default
- **AND** the response still includes the effective policy.

#### Scenario: Explicit lint policy wins
- **WHEN** a lint request names a profile and also passes an explicit `fixPolicy`
- **THEN** the explicit request `fixPolicy` takes precedence over the profile default.

### Requirement: Frontend profile review
The frontend SHALL provide a way to review and switch AI task profiles for the current project.

#### Scenario: Display profile details
- **WHEN** a user opens the AI profile view with a project selected
- **THEN** the page displays available profiles, diagnostics, recommended commands, context scope, fixed SQL policy, and output format.

#### Scenario: Switch current profile
- **WHEN** a user selects a profile in the frontend
- **THEN** high-frequency AI-related pages can read the selected profile as their current session default
- **AND** switching does not write project governance settings or external provider credentials.
