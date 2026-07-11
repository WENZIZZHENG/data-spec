## ADDED Requirements

### Requirement: Registry describes Standard Query DSL contracts
The standard schema registry SHALL describe Standard Query DSL request, result, filter, summary, and validation error contracts.

#### Scenario: Registry catalog includes DSL contracts
- **WHEN** the registry catalog is generated
- **THEN** it includes contract summaries for Standard Query DSL request and result objects.

#### Scenario: Contract detail describes DSL fields
- **WHEN** a caller requests Standard Query DSL contract detail
- **THEN** the JSON Schema describes target, text, filters, operators, limit, strict, explain, normalized query, applied filters, ignored filters, counts, hints, supported fields, bounds, and secret-safety constraints.
