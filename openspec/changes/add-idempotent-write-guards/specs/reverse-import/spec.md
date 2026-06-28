## ADDED Requirements

### Requirement: Reverse import confirmation write guard
Reverse import confirmation SHALL use the project-scoped write guard when importing selected database candidates.

#### Scenario: Retry candidate import with same key
- **WHEN** a caller submits the same selected candidates with the same idempotency key
- **THEN** DataSpec returns the original import result without creating duplicate fields or duplicate source records.

#### Scenario: Concurrent candidate import
- **WHEN** another reverse import confirmation is already running for the same project
- **THEN** DataSpec returns a retryable conflict diagnostic instead of interleaving candidate writes.
