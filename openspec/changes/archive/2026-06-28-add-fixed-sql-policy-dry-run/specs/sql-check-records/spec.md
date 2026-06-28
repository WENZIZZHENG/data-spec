## ADDED Requirements

### Requirement: SQL check record fix metadata
SQL check records SHALL preserve issue-level fixed SQL policy metadata for later review.

#### Scenario: Persist issue fix metadata
- **WHEN** a lint result is saved as a SQL check record
- **THEN** the serialized issues include fixer risk, change type, status, and explanation fields when present
- **AND** existing record fields remain readable by older clients.

#### Scenario: Record detail shows fix metadata
- **WHEN** a client requests SQL check record detail
- **THEN** the returned issues include the saved fixed SQL policy metadata
- **AND** the record still includes original SQL, fixed SQL, issue counts, and replay metadata.

### Requirement: SQL lint page fix plan review
The SQL lint page SHALL expose the fixed SQL policy and planned changes for the current lint result.

#### Scenario: Show fix plan summary
- **WHEN** a lint result includes a fix plan
- **THEN** the page displays effective policy, dry-run status, applied count, skipped count, and next actions.

#### Scenario: Show change risk beside fixed SQL
- **WHEN** fixed SQL or fixed SQL diff is displayed
- **THEN** the page shows planned changes with corresponding rule code, risk level, before value, after value, and explanation.
