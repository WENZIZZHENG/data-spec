# openapi-schema-drift-check Specification

## Purpose
定义前端 OpenAPI TypeScript schema 漂移检查命令，通过临时重新生成并比较 `schema.ts` 来发现契约未同步问题。
## Requirements
### Requirement: OpenAPI schema drift check
The frontend SHALL provide a verification command that regenerates OpenAPI TypeScript types into a temporary file and compares them with the committed `src/api/schema.ts`.

#### Scenario: Schema is current
- **WHEN** the regenerated schema text matches the committed schema text
- **THEN** the verification command exits successfully

#### Scenario: Schema drift is detected
- **WHEN** the regenerated schema text differs from the committed schema text
- **THEN** the verification command exits with a failure
- **AND** it prints a readable message instructing the developer to run `pnpm gen:api`

### Requirement: Configurable OpenAPI source
The frontend verification command SHALL support configurable OpenAPI document sources.

#### Scenario: Default source
- **WHEN** no source override is provided
- **THEN** the command uses `http://localhost:8090/api-docs`

#### Scenario: Explicit source
- **WHEN** the user passes `--source <url-or-file>` or sets `DATASPEC_API_DOCS_URL`
- **THEN** the command uses that OpenAPI source when regenerating the temporary schema

### Requirement: Stable Windows-compatible comparison
The frontend verification command SHALL avoid false failures caused only by line ending differences.

#### Scenario: Line endings differ
- **WHEN** the committed schema and regenerated schema differ only by CRLF/LF line endings
- **THEN** the verification command treats them as equal
