## ADDED Requirements

### Requirement: Contract preview can seed candidate inbox review
DataSpec SHALL let contract candidate preview output seed the existing standard candidate inbox review flow without automatically persisting candidates.

#### Scenario: Preview exposes inbox payload
- **WHEN** a contract candidate preview returns candidate fields
- **THEN** each candidate includes an `inboxPayload` compatible with the existing candidate creation semantics
- **AND** the payload includes source type, candidate field metadata, confidence, and non-sensitive evidence describing the contract source.

#### Scenario: Preview does not persist candidates
- **WHEN** a caller previews contract candidates
- **THEN** the standard candidate inbox remains unchanged
- **AND** previewed candidates do not appear in candidate list, AI Context export, or candidate decision results unless a separate reviewed create flow persists them.

#### Scenario: Contract source evidence is distinguishable
- **WHEN** a reviewed flow persists a candidate derived from a contract preview
- **THEN** candidate evidence can distinguish API, JSON Schema, or Protobuf contract sources from database reverse-import sources.
