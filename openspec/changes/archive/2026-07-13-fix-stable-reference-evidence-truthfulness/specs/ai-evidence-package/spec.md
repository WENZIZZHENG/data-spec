## ADDED Requirements

### Requirement: Persisted evidence sources expose resolvable references
AI evidence packages SHALL expose an additive canonical evidence ref for persisted sources and SHALL NOT claim that payload-only sources are independently resolvable.

#### Scenario: Package uses a persisted source
- **WHEN** an evidence package is generated from a SQL check, AI job, AI batch run, or AI task run record
- **THEN** its source includes `evidenceRef` in the format `dataspec://evidence/<source-type>/<source-id>`
- **AND** resolving that ref in the source project verifies the same persisted source.

#### Scenario: Package uses a payload-only source
- **WHEN** an evidence package is generated from a coverage report payload or another non-persisted source
- **THEN** its source `evidenceRef` is empty
- **AND** DataSpec does not fabricate a packageId-based or payload-based verifiable reference.

#### Scenario: Existing evidence clients read the package
- **WHEN** a client that only understands the existing source fields reads a package containing `evidenceRef`
- **THEN** all existing fields and their semantics remain compatible
- **AND** the additive field contains no secret, raw business row, or connection detail.
