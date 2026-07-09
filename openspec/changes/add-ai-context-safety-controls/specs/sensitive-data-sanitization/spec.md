## ADDED Requirements

### Requirement: AI Context export uses shared sanitizer
DataSpec SHALL apply the shared sensitive data sanitizer to AI-consumable context outputs that may contain arbitrary business or user text.

#### Scenario: Field metadata is sanitized for AI Context
- **WHEN** field comments, aliases, default values, example values, format notes, usage contracts, or replacement guidance contain known technical secrets
- **THEN** AI Context field catalog and prompt exports SHALL contain only sanitized values
- **AND** the raw secret SHALL NOT appear in generated JSON, Markdown, YAML, prompt text, or zip package entries.

#### Scenario: Prompt inputs are sanitized
- **WHEN** create-table or fix-sql prompt generation receives business descriptions or SQL containing known technical secrets
- **THEN** the returned prompt SHALL redact those secrets
- **AND** the prompt SHALL still preserve enough non-sensitive text for local review.
