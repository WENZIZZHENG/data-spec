## ADDED Requirements

### Requirement: AI Context cache diagnostics
DataSpec CLI SHALL include repository AI Context cache diagnostics in `doctor` output.

#### Scenario: Cache exists and is fresh
- **WHEN** a user runs `doctor --format json` in a repository with a non-expired `.dataspec/context/cache-metadata.json`
- **THEN** the JSON output includes a context-cache check with status `pass`
- **AND** includes cache metadata such as exportedAt, expiresAt, projectId, source, specVersion, and specHash when available.

#### Scenario: Cache missing
- **WHEN** no `.dataspec/context/cache-metadata.json` exists
- **THEN** `doctor` includes a context-cache check with status `warn`
- **AND** suggests running `export-context --cache`.

#### Scenario: Service unavailable with stale cache
- **WHEN** the DataSpec service check fails and the cache is expired
- **THEN** `doctor` reports the context-cache check as `warn`
- **AND** explains that offline use can continue only with stale context.

#### Scenario: Remote standard differs from cache
- **WHEN** DataSpec service is reachable and current remote standard metadata differs from cached metadata
- **THEN** `doctor` reports the context-cache check as `fail` or `warn`
- **AND** suggests refreshing the cache before AI schema work.
