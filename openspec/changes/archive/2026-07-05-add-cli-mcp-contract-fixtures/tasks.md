## 1. OpenSpec Preparation

- [x] 1.1 Validate proposal, design, and delta specs for P6-75 with `openspec validate add-cli-mcp-contract-fixtures --strict`.
- [x] 1.2 Confirm the first implementation slice does not change existing CLI/MCP runtime behavior and only adds fixtures, local validation, docs, and tests.

## 2. Fixture Contract

- [x] 2.1 Add failing Node tests for the CLI/MCP contract fixture checker covering required entries, MCP descriptor drift, missing safety metadata, and secret-like example values.
- [x] 2.2 Add `tools/fixtures/cli-mcp-contracts.json` with documented contract entries for high-frequency CLI commands, MCP tools, MCP resources, and MCP prompts.
- [x] 2.3 Implement a reusable fixture checker that validates structure, required fields, examples, redaction, and MCP `tools/list` / `resources/list` / `prompts/list` alignment without calling a real backend.
- [x] 2.4 Expose a local check command under `tools/` with text/json output and non-zero diagnostics on drift.

## 3. Validation Integration

- [x] 3.1 Add or update tests so `node --test tools/*.test.mjs` covers the fixture checker and fixture file.
- [x] 3.2 Update the validation advisor so CLI/MCP contract fixture changes recommend the local fixture check and Node tests.

## 4. Documentation and TODO

- [x] 4.1 Update README and `docs/ai-contracts.md` to describe the fixture library, local check command, safety boundary, and compatible additive policy.
- [x] 4.2 Update TODO status for P6-75 and active OpenSpec state.

## 5. Review, Evidence, and Commit

- [x] 5.1 Run targeted fixture checker tests, tools tests, OpenSpec strict validation, status check, and diff checks.
- [x] 5.2 Request independent subagent review for CLI/MCP/AI protocol fixture coverage, fix or document findings, and record agent id.
- [x] 5.3 Record Verification Evidence before commit/archive.

## Verification Evidence

- `node tools\dataspec-cli-mcp-contract-check.mjs --format json`: pass, `ok=true`, 12 CLI commands, 9 MCP tools, 7 MCP resources, 3 MCP prompts, 0 diagnostics.
- `node --test tools\dataspec-cli-mcp-contract-check.test.mjs tools\dataspec-verify-advisor.test.mjs`: pass, 15 tests.
- `node --test tools/*.test.mjs`: pass, 184 tests.
- `openspec validate add-cli-mcp-contract-fixtures --strict`: pass.
- `node tools\dataspec-status-check.mjs --format json`: warn only for expected active change `OPENSPEC_ACTIVE_CHANGE_PRESENT` before archive.
- `git diff --check`: pass, only CRLF checkout warnings.
- Independent review agent `019f31b1-37e8-7372-b491-f8de6c6985da` (`Anscombe`): found blocking fixture checker/advisor coverage gaps; fixed by tightening resource/prompt descriptor alignment, stable entry field checks, and advisor recommendations; agent closed.
- Independent review agent `019f31b8-7459-7ce2-bdb4-647c98dde5e6` (`Godel`): found P1 gaps in advisor command coverage and MCP tool entry checks; fixed with failing tests first, then checker/advisor implementation; agent closed.
- Independent review agent `019f31bd-9602-7c30-9440-1fe91afee60d` (`Maxwell`): final narrow复评，未发现阻塞性问题，confirmed the two P1 findings were closed; agent closed.
