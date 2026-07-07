# ai-readable-error-contract Specification

## Purpose
定义 API、CLI 和 MCP 共享的 AI 可读错误诊断契约，在保留既有错误字段的同时提供分类、可重试性、建议动作和文档引用。
## Requirements
### Requirement: API error diagnostics

DataSpec SHALL include a machine-readable diagnostic object on failed JSON API responses while preserving the existing `code`, `message`, and `data` fields.

#### Scenario: Business error response

- **WHEN** a DataSpec API request fails with a business or validation error
- **THEN** the JSON response includes `error.code`, `error.category`, `error.retryable`, `error.suggestedAction`, and `error.docsRef`
- **AND** existing clients can still read the top-level `code` and `message`

#### Scenario: Authorization or project scope failure

- **WHEN** a request fails because an API token is missing, invalid, or lacks project access
- **THEN** the diagnostic category identifies an authorization or project access problem
- **AND** the suggested action tells the AI agent to provide a token, switch project, or run doctor as appropriate

### Requirement: CLI error diagnostics

DataSpec CLI SHALL expose DataSpec API diagnostics in a machine-readable line when a server response contains the diagnostic contract.

#### Scenario: CLI receives API diagnostic

- **WHEN** a CLI command receives a failed DataSpec API response with `error`
- **THEN** stderr includes the original human-readable error line
- **AND** stderr includes a `DataSpecError:` JSON line containing the diagnostic object

### Requirement: MCP error diagnostics

DataSpec MCP SHALL expose DataSpec API diagnostics in JSON-RPC error data when a backend response contains the diagnostic contract.

#### Scenario: MCP tool receives API diagnostic

- **WHEN** an MCP tool call receives a failed DataSpec API response with `error`
- **THEN** the JSON-RPC error keeps a normal `code` and `message`
- **AND** `error.data.dataspecError` contains the diagnostic object
