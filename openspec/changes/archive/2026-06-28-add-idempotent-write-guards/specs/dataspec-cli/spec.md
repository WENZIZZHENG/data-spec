## ADDED Requirements

### Requirement: CLI idempotency key forwarding
The DataSpec CLI SHALL let write commands forward an idempotency key to the backend API.

#### Scenario: CLI option sets idempotency key
- **WHEN** a user runs a backend write command with `--idempotency-key`
- **THEN** the CLI sends the value as the `Idempotency-Key` HTTP header.

#### Scenario: CLI environment key fallback
- **WHEN** `DATASPEC_IDEMPOTENCY_KEY` is set and the command does not pass `--idempotency-key`
- **THEN** the CLI sends the environment value as the `Idempotency-Key` HTTP header.
