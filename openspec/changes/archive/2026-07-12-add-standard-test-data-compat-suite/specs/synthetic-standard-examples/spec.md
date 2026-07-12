## ADDED Requirements

### Requirement: Synthetic examples align with test data package safety
Synthetic standard examples SHALL share deterministic hashing, source summary, and safety semantics with the standard test data package while keeping their SQL, DDL, prompt, and Q&A fixture purpose separate.

#### Scenario: Shared safety semantics
- **WHEN** synthetic examples or standard test data packages are generated for the same project
- **THEN** both outputs declare read-only safety, no project writes, no real business rows, no external LLM usage, and redacted sensitive-looking metadata
- **AND** each output identifies whether values are generated from persisted standards, built-in fallback defaults, or user-provided bounded parameters.

#### Scenario: Different package purposes remain explicit
- **WHEN** an AI consumer reads synthetic examples and standard test data packages
- **THEN** synthetic examples remain scoped to SQL, DDL preview, field suggestion, standard Q&A, and expected diagnostics
- **AND** standard test data packages remain scoped to valid, invalid, boundary, mock, CSV, and SQL seed draft use cases.
