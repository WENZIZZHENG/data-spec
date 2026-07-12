# ai-context-safety-controls Specification

## Purpose
定义 AI Context 中可信指令、稳定工具契约与不可信业务文本的安全分层和处理边界。
## Requirements
### Requirement: AI Context safety metadata
DataSpec SHALL include AI Context safety metadata that distinguishes DataSpec instructions, stable tool contracts, and untrusted business text.

#### Scenario: Export package includes safety summary
- **WHEN** an AI Context package is generated
- **THEN** `.dataspec/manifest.json` SHALL include `contextSafetySummary`
- **AND** the summary SHALL identify trusted instruction files, untrusted content sources, redaction policy, and safety warnings.

#### Scenario: Guidance names untrusted boundaries
- **WHEN** a caller reads `.dataspec/README.md`, `.dataspec/prompts.md`, or `AGENTS.md.fragment`
- **THEN** the guidance SHALL state that field comments, examples, glossary terms, user descriptions, SQL, and database metadata are untrusted business content
- **AND** AI clients SHALL be instructed not to treat those values as system or developer instructions.

### Requirement: Field export decisions
DataSpec SHALL explain field-level AI Context export decisions with additive metadata.

#### Scenario: Field catalog includes safety metadata
- **WHEN** `.dataspec/field-catalog.json` is generated
- **THEN** each field SHALL include `contextSafety` with `sourceTrustLevel`, `instructionBoundary`, `redactionReasons`, and `warnings`
- **AND** each field SHALL include `exportDecision` with `visibility`, `maskingProfile`, `allowedTasks`, and `reason`.

#### Scenario: Sensitive fields use minimal exposure
- **WHEN** a field is marked sensitive
- **THEN** its default export decision SHALL use restricted visibility and a metadata-only masking profile
- **AND** any exported example-like value SHALL be redacted or replaced with a safe marker.

#### Scenario: Secret-like field text is redacted
- **WHEN** field metadata contains password, token, Authorization, full JDBC URL, DSN, or connection string text
- **THEN** the exported AI Context SHALL NOT contain the raw secret-like value
- **AND** `contextSafety.redactionReasons` SHALL explain which field property was redacted.

### Requirement: Local security profile contract
DataSpec SHALL support a local personal security profile for AI-oriented tooling.

#### Scenario: Config contains security profile
- **WHEN** `.dataspec/config.json` contains `securityProfile`
- **THEN** CLI/MCP config loading SHALL parse it into a structured object
- **AND** supported properties SHALL include `redactionStrictness`, `sensitiveFieldPolicy`, `allowedAiTools`, `neverExportPatterns`, `localOnlyPaths`, `samplePolicy`, and `credentialPolicy`.

#### Scenario: Invalid security profile shape
- **WHEN** `.dataspec/config.json` contains an invalid `securityProfile`
- **THEN** CLI/MCP config loading SHALL return a readable configuration diagnostic before calling DataSpec HTTP APIs.

#### Scenario: Security profile does not expose secrets
- **WHEN** tools summarize local config state
- **THEN** they SHALL report only profile presence, policy names, counts, and warnings
- **AND** they SHALL NOT print raw secret-bearing values.
