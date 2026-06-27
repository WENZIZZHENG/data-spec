## ADDED Requirements

### Requirement: Snapshot-scoped AI Context export
The AI Context package SHALL support exporting context from a specified historical standard snapshot.

#### Scenario: Preview field catalog for snapshot
- **WHEN** a caller previews `field-catalog.json` with `snapshotId`
- **THEN** the response uses the snapshot payload
- **AND** the top-level standard metadata marks the source as `snapshot`

#### Scenario: Preview rules for snapshot
- **WHEN** a caller previews `rules.yaml` with `snapshotId`
- **THEN** the response uses the snapshot rule payload
- **AND** the YAML standard block includes snapshot ID, version, hash, and source

#### Scenario: Download package for snapshot
- **WHEN** a caller downloads the AI Context zip with `snapshotId`
- **THEN** `.dataspec/manifest.json`, `.dataspec/field-catalog.json`, and `.dataspec/rules.yaml` all reference the same snapshot metadata
