## ADDED Requirements

### Requirement: Cache AI Context From CLI
The CLI SHALL allow `export-context` to write a repository-local AI Context cache in addition to downloading a zip file.

#### Scenario: Export context cache
- **WHEN** a user runs `export-context --project <id> --cache`
- **THEN** the CLI downloads the AI Context package from the configured DataSpec server
- **AND** writes the package contents and cache metadata to `.dataspec/context/` under the configured repository root
- **AND** exits with code `0`.

#### Scenario: Cache mode preserves existing zip output
- **WHEN** a user runs `export-context --project <id> --output <zip> --cache`
- **THEN** the CLI writes the zip bytes to the requested output path
- **AND** also refreshes `.dataspec/context/`.

#### Scenario: Cache mode accepts scoped context options
- **WHEN** a user passes scope, query, status, limit, snapshotId, or snapshotVersion with `--cache`
- **THEN** the CLI forwards those options to the AI Context package endpoint
- **AND** records the same options in cache metadata.

#### Scenario: Cache mode rejects unsafe archive paths
- **WHEN** the downloaded package contains an absolute path or a path escaping the cache directory
- **THEN** the CLI rejects the package
- **AND** it exits with code `2` without writing outside `.dataspec/context/`.
