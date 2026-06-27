## ADDED Requirements

### Requirement: Fixed SQL dialect safety diagnostics
fixedSql output SHALL include dialect diagnostics describing whether deterministic fixes are safe for the inferred dialect.

#### Scenario: Fixed SQL is generated for PostgreSQL
- **WHEN** fixedSql is generated for PostgreSQL-style SQL
- **THEN** diagnostics indicate PostgreSQL as the target dialect
- **AND** no MySQL-only compatibility claim is made

#### Scenario: Fixed SQL is generated for MySQL-like input
- **WHEN** fixedSql is generated from MySQL-like SQL
- **THEN** diagnostics warn when the fixer normalizes output through PostgreSQL-style COMMENT or type rendering
- **AND** the warning includes a next action for manual review or later dialect-specific fixing
