# frontend-smoke-regression-gate Specification

## Purpose
定义轻量前端冒烟回归门禁，在无需浏览器自动化或后端服务的情况下检查关键路由、项目状态耦合、API wrapper 和可见操作入口。
## Requirements
### Requirement: Frontend critical-flow smoke gate

DataSpec SHALL provide a lightweight frontend smoke regression gate that can run locally without a browser automation platform and verify that critical user flows still expose their required routes, project state coupling, API wrappers, and visible action entry points.

#### Scenario: Run frontend smoke gate

- **WHEN** a developer runs the documented frontend test command
- **THEN** the smoke gate is executed as part of the existing frontend test suite
- **AND** it verifies the SQL lint, reverse import, field library, DDL generation, AI Context/export, coverage, and replay page entry points
- **AND** it fails if the corresponding route or core page/API coupling is removed without updating the gate

#### Scenario: Preserve project-coupled flows

- **WHEN** project selection or project-scoped pages are changed
- **THEN** the smoke gate verifies that the app still loads projects through the project store
- **AND** project-scoped pages still reference current project state before calling core APIs

#### Scenario: Avoid heavy E2E dependency

- **WHEN** the smoke gate is run in a local development checkout
- **THEN** it does not require Playwright, browser binaries, screenshots, or a running backend service
- **AND** `pnpm build` remains the type/build gate for Vue compilation.

### Requirement: Unified State Smoke Coverage
The frontend smoke regression gate SHALL cover the unified request state and project-required entry points for migrated pages.

#### Scenario: State helpers are removed
- **WHEN** a developer removes the unified request state utility or state display components while migrated pages still depend on them
- **THEN** the frontend smoke test fails with a readable assertion.

#### Scenario: Migrated page loses retry entry
- **WHEN** a migrated page no longer exposes a visible retry or recovery action for failed requests
- **THEN** the frontend smoke test fails.
