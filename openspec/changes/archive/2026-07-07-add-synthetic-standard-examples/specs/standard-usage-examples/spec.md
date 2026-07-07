## ADDED Requirements

### Requirement: Synthetic Usage Example Drafts
DataSpec SHALL treat generated synthetic standard examples as reviewable usage-example drafts rather than automatically persisted project examples.

#### Scenario: Synthetic cases can seed manual review
- **WHEN** a synthetic example package includes `standardQaCases`, `goodSql`, or `badSql`
- **THEN** each generated case contains enough source metadata for a user or AI workflow to convert it into a standard usage example draft after review.

#### Scenario: Synthetic generation does not persist examples
- **WHEN** a caller generates synthetic examples
- **THEN** the standard usage example library remains unchanged
- **AND** generated cases do not appear in list or AI Context export results unless a separate reviewed create flow persists them.
