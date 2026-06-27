# performance-baseline Specification

## Purpose
TBD - created by archiving change add-performance-baseline-observability. Update Purpose after archive.
## Requirements
### Requirement: Repeatable performance baseline

DataSpec SHALL provide a repeatable local baseline for large field-library operations without requiring a real database.

#### Scenario: Measure large synthetic field operations

- **GIVEN** a synthetic project with thousands of standard fields
- **WHEN** the performance baseline test runs
- **THEN** it measures field grouping, field suggestion, AI Context field catalog export, and reverse-import compare with deterministic input sizes.

#### Scenario: Avoid flaky hard benchmark gates

- **GIVEN** the baseline runs in local or CI environments with variable hardware
- **WHEN** operation duration is recorded
- **THEN** the test asserts functional correctness and only fails on a broad regression threshold.

### Requirement: Slow operation diagnostics

DataSpec SHALL emit readable diagnostics when core local operations exceed conservative thresholds.

#### Scenario: Slow operation warning

- **GIVEN** a core operation such as field suggestion, AI Context export, lint record paging, or reverse-import compare
- **WHEN** the measured duration exceeds its threshold
- **THEN** the server logs a warning with operation name, duration, threshold, and a short diagnostic hint.

#### Scenario: Normal operation remains quiet

- **GIVEN** a core operation completes below its threshold
- **WHEN** the operation finishes
- **THEN** it does not emit a warning-level log.

### Requirement: Performance baseline documentation

DataSpec SHALL document the local performance baseline and its boundaries.

#### Scenario: User runs the baseline

- **GIVEN** a developer wants to check large-field performance after changes
- **WHEN** they read README or TODO
- **THEN** they can find the relevant test command and understand that the baseline is synthetic and not a production capacity claim.
