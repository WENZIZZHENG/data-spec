## 1. OpenSpec Preparation

- [x] 1.1 Validate proposal, design, and delta specs with `openspec validate add-mcp-agent-guidance-pack --strict`.
- [x] 1.2 Confirm the first implementation slice is additive and keeps existing MCP prompt/resource/tool names compatible.

## 2. TDD Coverage

- [x] 2.1 Add failing MCP tests for `resources/templates/list`, `agent-guidance-pack` resource read, and first-class agent prompts.
- [x] 2.2 Add failing contract fixture checker tests for MCP resource template drift and required first-class prompts.
- [x] 2.3 Add or update validation advisor tests so MCP descriptor changes recommend the fixture checker and Node tests.

## 3. MCP Implementation

- [x] 3.1 Add a shared local agent guidance template registry with required inputs, safe defaults, resource sequence, tool sequence, stop conditions, evidence requirements, and next actions.
- [x] 3.2 Expose `dataspec://project/<id>/agent-guidance-pack` as a local JSON resource with `structuredContent`.
- [x] 3.3 Implement `resources/templates/list` for project-scoped resource templates.
- [x] 3.4 Add first-class prompts `create_table_with_dataspec`, `review_sql_with_dataspec`, `reverse_import_standards`, and `answer_field_standard_question` while preserving existing prompt names.

## 4. Contract Fixtures and Documentation

- [x] 4.1 Extend `tools/fixtures/cli-mcp-contracts.json` with MCP resource template and first-class prompt entries.
- [x] 4.2 Extend `tools/dataspec-cli-mcp-contract-check.mjs` to validate MCP resource templates against local descriptors.
- [x] 4.3 Update README, `docs/ai-contracts.md`, and TODO status for P6-89.

## 5. Validation, Review, and Archive

- [x] 5.1 Run targeted MCP/fixture tests, fixture checker, tools tests, OpenSpec strict validation, status check, and diff checks.
- [x] 5.2 Request independent subagent review for MCP/AI protocol coverage, fix or document findings, and record agent id.
- [x] 5.3 Record Verification Evidence before archive/commit.

## Verification Evidence

- `node --test tools\dataspec-mcp.test.mjs tools\dataspec-cli-mcp-contract-check.test.mjs tools\dataspec-verify-advisor.test.mjs` -> 60 tests passed.
- `node tools\dataspec-cli-mcp-contract-check.mjs --format json` -> `ok: true`, `mcpResourceTemplates: 7`, `mcpPrompts: 7`, `diagnostics: 0`.
- `node --test tools/*.test.mjs` -> 190 tests passed.
- `openspec validate add-mcp-agent-guidance-pack --strict` -> change is valid.
- `node tools\dataspec-status-check.mjs --format json` -> only `OPENSPEC_ACTIVE_CHANGE_PRESENT` warning before archive; no errors.
- `git diff --check` -> no whitespace errors; PowerShell reported only existing LF/CRLF conversion warnings.
- Independent review subagent `019f31d3-129c-71e1-baf7-05136259b37f` (Sagan) completed read-only MCP/AI protocol review. Findings: prompt descriptor fixture coverage was too shallow for argument required/safety/full guidance drift, and fixture lacked full `dataspecGuidance`. Resolution: added first-class prompt `safety` descriptors, full fixture `dataspecGuidance`, checker comparisons for argument required/description, prompt safety, and full guidance fields, plus negative tests for required/safety/safeDefaults/toolSequence/stopConditions/evidenceRequirements drift.
- Follow-up review subagent `019f31db-ac3d-7380-b460-bd28eae699b4` (Socrates) was created only for non-gating re-review and failed with `deactivated_workspace`; it was closed and not used as review evidence.
