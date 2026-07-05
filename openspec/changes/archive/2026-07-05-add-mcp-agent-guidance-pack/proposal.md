## Why

MCP clients can already list DataSpec tools, resources, and a few prompts, but AI agents still need to assemble the right order of resources, tools, safety checks, and evidence handoff from scattered README and prompt text. P6-89 needs a stable agent guidance surface so common tasks start from a reusable template rather than improvised tool calls.

## What Changes

- Add a local MCP agent guidance pack resource that lists common DataSpec task templates with required inputs, safe defaults, resource sequence, tool sequence, stop conditions, and evidence requirements.
- Add MCP resource template descriptors so clients can discover project-scoped guidance resources without guessing URIs.
- Add first-class MCP prompts for create-table, SQL review, reverse import, and field-standard Q&A workflows while keeping existing prompt names compatible.
- Extend CLI/MCP contract fixtures and local checker coverage so new prompts/resource templates are validated with the MCP descriptors.
- Update README, AI contract docs, TODO, and OpenSpec specs.
- No breaking change: existing prompts, resources, tools, and runtime behavior remain available.

## Capabilities

### New Capabilities
- `mcp-agent-guidance-pack`: covers the local MCP agent guidance pack resource and resource template descriptors.

### Modified Capabilities
- `dataspec-mcp`: adds first-class MCP agent prompts and resource template listing behavior.
- `cli-mcp-contract-fixtures`: extends the contract fixture to cover MCP resource templates and new prompt descriptors.

## Impact

- Affected code: `tools/dataspec-mcp.mjs`, `tools/dataspec-mcp.test.mjs`, `tools/fixtures/cli-mcp-contracts.json`, `tools/dataspec-cli-mcp-contract-check.mjs`, `tools/dataspec-cli-mcp-contract-check.test.mjs`, and validation advisor tests/rules if descriptor changes need fixture checks.
- Affected docs: `README.md`, `docs/ai-contracts.md`, `TODO.md`, and OpenSpec specs.
- SDD level: SDD standard because MCP prompt/resource descriptors are AI-facing external protocol. Commit/archive requires independent subagent review.
