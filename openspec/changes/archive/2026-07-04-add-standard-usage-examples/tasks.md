## 1. Backend Example Library

- [x] 1.1 Add failing service/controller tests for create/list/update/delete, validation, paging, and unsafe content rejection.
- [x] 1.2 Add Flyway migration, entity, mapper, repository, DTOs, service, and controller for project-scoped usage examples.
- [x] 1.3 Ensure API responses are OpenAPI-visible and project access checks follow existing project-scoped services.

## 2. AI Context Export

- [x] 2.1 Add failing AI Context tests for `.dataspec/usage-examples.json`, field-catalog additive fields, summary truncation, and no-example compatibility.
- [x] 2.2 Integrate usage example selection into AI Context package, field catalog JSON, README, and AGENTS fragment.
- [x] 2.3 Add schema/contract metadata for usage examples where existing schema registry or package schema files require additive fields.

## 3. Frontend Entry

- [x] 3.1 Regenerate OpenAPI TypeScript schema and add typed API wrappers/types for usage examples.
- [x] 3.2 Add a project-scoped usage example library page with list, filters, create/edit/delete, and shared project-required state.
- [x] 3.3 Add the route/navigation/task-entry link and extend frontend smoke tests for the page/API/schema coupling.

## 4. Verification And Release Notes

- [x] 4.1 Run backend, frontend, OpenSpec, and diff validation; fix failures.
- [x] 4.2 Run independent agent code review and resolve or document findings.
- [x] 4.3 Add Verification Evidence, archive the OpenSpec change, update TODO/README, rerun final validation, and create a local commit.
