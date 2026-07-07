# dashboard Specification

## Purpose
定义个人版项目 Dashboard 的摘要、活动时间线和任务入口，让用户能从默认首页查看标准资产、近期检查、趋势和核心工作流。
## Requirements
### Requirement: Project Dashboard Summary

DataSpec SHALL provide a project-level dashboard summary for personal use.

#### Scenario: Query dashboard summary

- **WHEN** a user queries dashboard summary by project
- **THEN** DataSpec returns standard field count, enum dictionary count, rule count, forbidden term count, recent SQL check count, field hit rate, recent checks, and issue trend.

### Requirement: Dashboard Page

DataSpec SHALL expose a Dashboard page as the default entry, include a project activity timeline for the current project, and provide task-oriented entry points into core workflows.

#### Scenario: Open app root

- **WHEN** a user opens the app root route
- **THEN** the app navigates to Dashboard
- **AND** Dashboard refreshes when the current project changes.

#### Scenario: Show project activity timeline

- **WHEN** Dashboard has a current project
- **THEN** Dashboard loads and displays recent project activities
- **AND** the user can filter activities by action type.

#### Scenario: Jump to activity detail

- **WHEN** an activity contains a detailRoute
- **THEN** Dashboard provides an action that navigates to that route.

#### Scenario: Start core workflow from Dashboard

- **WHEN** Dashboard has a current project
- **THEN** Dashboard shows task entry points for import, SQL lint, coverage, field maintenance, AI Context export, and token management.

#### Scenario: Show empty project entry actions

- **WHEN** Dashboard has no current project
- **THEN** Dashboard offers creating a demo project and opening the project list.
