## 1. OpenSpec Preparation

- [x] 1.1 Confirm proposal, design, and delta specs describe the additive API/CLI/MCP/AI contract.
- [x] 1.2 Validate the change with `openspec validate add-version-compatibility-handshake --strict`.

## 2. Backend Handshake

- [x] 2.1 Add tests for `/api/capabilities/version` covering default, compatible, incompatible, and unknown client-version responses.
- [x] 2.2 Implement the read-only compatibility payload with documented response fields and non-secret capability metadata.
- [x] 2.3 Add the `version-compatibility` entry to the AI capability catalog and recommended first actions.

## 3. CLI Compatibility

- [x] 3.1 Add CLI tests for `compat check --format json` success, incompatible exit code, and unreachable-server diagnostics.
- [x] 3.2 Implement `compat check` with local CLI version, stable JSON/text output, redacted errors, and exit codes `0`, `1`, and `2`.
- [x] 3.3 Extend `doctor --format json` to include a compatibility check without hiding existing checks when the endpoint is unavailable.

## 4. MCP Resource

- [x] 4.1 Add MCP tests for listing, reading, and failure diagnostics for `dataspec://version-compatibility`.
- [x] 4.2 Implement the read-only MCP resource with JSON text and structured compatibility payload.

## 5. Documentation, TODO, and Evidence

- [x] 5.1 Update README and TODO status text for the new compatibility workflow and current active change state.
- [x] 5.2 Run backend, CLI/MCP, OpenSpec, status-check, diff, and staged-diff validations appropriate to this contract change.
- [x] 5.3 Request independent subagent review, address findings, and record verification evidence before commit.

## Verification Evidence

- Backend targeted tests: `mvn "-Dtest=AiCapabilityCatalogControllerTest,AiCapabilityCatalogServiceImplTest" test` passed with 18 tests.
- CLI tests: `node --test tools\dataspec-cli.test.mjs` passed.
- MCP tests: `node --test tools\dataspec-mcp.test.mjs` passed with 39 tests.
- Backend full tests: `mvn test` in `dataspec-server` passed with 463 tests.
- Tools tests: `node --test tools/*.test.mjs` passed with 176 tests.
- OpenSpec strict validation: `openspec validate add-version-compatibility-handshake --strict` passed.
- Diff whitespace check: `git diff --check` passed with only existing line-ending normalization warnings.
- TODO/status check: `node tools/dataspec-status-check.mjs --format json` passed with the expected `OPENSPEC_ACTIVE_CHANGE_PRESENT` warning while this change remains active.
- Independent review: subagent `019f3192-e389-7ed2-bd96-92af1ccc5d01` (`Bacon`) completed read-only API/CLI/MCP/AI contract review and was closed. Findings:
  - P1 MCP backend/HTTP failures for `dataspec://version-compatibility` could leak upstream error codes instead of stable `VERSION_COMPATIBILITY_UNAVAILABLE`; fixed by wrapping all resource failures and adding an HTTP 500 regression test.
  - P2 backend coverage did not prove the real `/api/capabilities/version` route/default clientVersion behavior; fixed with MockMvc route coverage and missing-version service coverage.
