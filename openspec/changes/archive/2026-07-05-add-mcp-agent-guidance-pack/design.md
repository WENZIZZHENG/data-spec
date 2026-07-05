## Context

`tools/dataspec-mcp.mjs` currently keeps MCP resources in `RESOURCE_DEFS`, prompts in `PROMPTS`, and local workflow recipes in `dataspec-workflows.mjs`. P6-75 added a local CLI/MCP contract fixture and checker, so P6-89 can build on that verification path instead of adding a second validation framework.

This change is additive: it should make MCP clients better at choosing the right DataSpec workflow without changing existing prompt names or tool semantics.

## Goals / Non-Goals

**Goals:**

- Provide a machine-readable MCP agent guidance pack for common DataSpec AI workflows.
- Expose resource template descriptors so clients can discover project-scoped guidance resources.
- Add first-class task prompts for `create_table_with_dataspec`, `review_sql_with_dataspec`, `reverse_import_standards`, and `answer_field_standard_question`.
- Keep prompt/resource template changes covered by contract fixtures and Node tests.

**Non-Goals:**

- Do not call external LLMs.
- Do not execute workflows from prompts or guidance resources.
- Do not add approval, scheduling, or agent memory systems.
- Do not remove or rename the existing `dataspec_create_table`, `dataspec_review_sql`, or `dataspec_design_fields` prompts.

## Decisions

1. Add a local `agent-guidance-pack` resource rather than a backend API.
   - Reason: the guidance is deterministic descriptor metadata and does not need project data beyond `projectId`.
   - Alternative: backend endpoint. Rejected for first version because it would widen API surface and validation cost.

2. Add `resources/templates/list` to the MCP handler.
   - Reason: MCP clients can discover URI templates even when the server starts without a default project.
   - Alternative: only list concrete resources when projectId exists. That leaves projectless clients guessing.

3. Reuse a shared guidance template registry for prompts and resources.
   - Reason: prompt text, resource payload, and contract fixtures should describe the same safe task sequence.
   - Alternative: duplicate long prompt strings. Rejected because drift would be easy.

4. Extend the existing contract fixture checker.
   - Reason: P6-75 already verifies descriptor drift and secret-like examples, so the new MCP surfaces should join that path.
   - Alternative: add a separate MCP prompt checker. Rejected to avoid another validation command.

## Risks / Trade-offs

- [Risk] Guidance text becomes too rigid for real tasks.
  - Mitigation: encode recommended sequence and stop conditions, not a mandatory execution engine.
- [Risk] Additional prompts duplicate existing prompt names.
  - Mitigation: keep old names as compatibility aliases and add clearer new names.
- [Risk] Resource templates may drift from concrete resources.
  - Mitigation: fixture checker validates required template descriptors and tests cover `resources/templates/list`.

## Migration Plan

- Additive release only; no migration or rollback steps are required.
- If a client does not support `resources/templates/list`, it can continue using existing `resources/list`, `prompts/list`, and `tools/list`.

## Open Questions

- None for the first version. The TODO scope is narrow enough to implement with deterministic local MCP descriptors.
