## ADDED Requirements

### Requirement: Field usage contract storage
DataSpec SHALL store optional field-level usage contracts that explain recommended and forbidden use cases for a standard field.

#### Scenario: Create or update field usage contract
- **WHEN** a caller creates or updates a field with preferred use cases, avoid conditions, join hints, default filters, aggregation hints, replacement guidance, or misuse examples
- **THEN** DataSpec persists those values with the field in the same project
- **AND** omitted usage contract values remain empty without changing existing field defaults

#### Scenario: Reject unsafe usage contract content
- **WHEN** a caller submits usage contract text containing an obvious password, token, Authorization header, complete JDBC URL, DSN, or private key
- **THEN** DataSpec rejects the request with a non-sensitive validation message
- **AND** the unsafe value is not persisted

### Requirement: Field usage contract display
DataSpec SHALL expose field usage contracts in field detail and editing surfaces without making every field noisy.

#### Scenario: Display populated contract
- **WHEN** a user opens a field that has usage contract values
- **THEN** the frontend shows recommended use, avoid conditions, join hints, default filters, aggregation hints, replacement guidance, and misuse examples in a compact section

#### Scenario: Empty contract stays quiet
- **WHEN** a field has no usage contract values
- **THEN** the frontend does not show empty warning blocks or imply the field is incomplete solely because the contract is blank

### Requirement: Usage contract low-confidence handling
DataSpec SHALL use field usage contracts as evidence for AI-facing answer and recommendation quality without auto-applying changes.

#### Scenario: Question asks a usage boundary
- **WHEN** a user or AI asks which field should be used for a metric, join, filter, write operation, or replacement scenario
- **THEN** DataSpec includes matching usage contract evidence when available
- **AND** if usage contract evidence is missing or contradicts the requested scenario, DataSpec marks the answer or recommendation as requiring confirmation

#### Scenario: Avoid condition matches the request
- **WHEN** a requested scenario matches a field avoid condition or misuse example
- **THEN** DataSpec does not present that field as directly adoptable
- **AND** the next actions explain why the field needs confirmation or a replacement field

### Requirement: Usage contract examples remain separate
DataSpec SHALL keep field usage contracts separate from the standard usage examples library.

#### Scenario: Field contract exists without examples
- **WHEN** a field has usage contract text but no standard usage examples
- **THEN** DataSpec can still export and display the field contract
- **AND** it does not create, modify, or delete standard usage examples automatically
