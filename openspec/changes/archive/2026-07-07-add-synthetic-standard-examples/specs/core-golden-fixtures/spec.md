## ADDED Requirements

### Requirement: Synthetic example golden fixtures
DataSpec SHALL keep reusable synthetic standard example fixtures that are executed by the backend test suite.

#### Scenario: Synthetic scenarios are generated in backend tests
- **WHEN** `mvn test` runs backend tests for synthetic examples
- **THEN** user, order, payment, and audit scenario packages are generated from deterministic metadata
- **AND** tests verify stable fields, `specHash`, case counts, safety metadata, and expected diagnostic references.

#### Scenario: Bad SQL expected diagnostics are checked
- **WHEN** a generated bad SQL case declares an expected diagnostic
- **THEN** backend tests verify the diagnostic id, severity, and related case id are present in the generated package.
