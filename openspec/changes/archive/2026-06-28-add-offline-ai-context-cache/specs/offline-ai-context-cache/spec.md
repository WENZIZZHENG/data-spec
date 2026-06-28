## ADDED Requirements

### Requirement: Repository AI context cache layout
DataSpec SHALL support a repository-local AI Context cache under `.dataspec/context/` for offline AI use.

#### Scenario: Cache contains AI-readable context files
- **WHEN** a user exports AI Context with cache mode
- **THEN** DataSpec writes the exported context files under `.dataspec/context/`
- **AND** the cache includes field catalog, rules, prompts, manifest or equivalent metadata, and agent instructions when present in the source package.

#### Scenario: Cache metadata is machine readable
- **WHEN** cache mode writes `.dataspec/context/cache-metadata.json`
- **THEN** the metadata includes projectId, server, exportedAt, expiresAt, export options, contentHash, and standard metadata when available
- **AND** it MUST NOT include API tokens, database passwords, bearer tokens, full JDBC URLs, or source database row data.

### Requirement: Offline cache safety
The repository AI Context cache SHALL be read-only with respect to DataSpec server state.

#### Scenario: Service unavailable
- **WHEN** DataSpec service is unavailable and a cache exists
- **THEN** AI agents and CLI diagnostics can read the cached context and stale status
- **AND** DataSpec MUST NOT treat cached files as writes to the server.

#### Scenario: Cache rewrite stays within context directory
- **WHEN** cache mode refreshes the local cache
- **THEN** it clears and rewrites only `.dataspec/context/`
- **AND** it MUST NOT remove `.dataspec/config.json`, tokens, business SQL files, or files outside the context directory.

### Requirement: Cache freshness
DataSpec SHALL expose whether a repository AI Context cache is fresh, stale, missing, unreadable, or different from the remote standard metadata.

#### Scenario: Cache older than TTL
- **WHEN** the current time is later than cache metadata expiresAt
- **THEN** DataSpec reports the cache as stale
- **AND** includes a remediation command to refresh the cache.

#### Scenario: Remote standard differs
- **WHEN** DataSpec service is reachable and remote standard metadata differs from cache metadata
- **THEN** DataSpec reports the cache as remote-different
- **AND** includes the cached and remote standard hash or version when available.
