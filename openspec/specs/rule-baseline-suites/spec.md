# rule-baseline-suites Specification

## Purpose
Define project rule baseline suites that can be applied, exported, imported, and explained to AI consumers.

## Requirements

### Requirement: Built-in rule baseline library
DataSpec SHALL expose built-in project rule baseline suites that can be listed by API and used by frontend and AI consumers.

#### Scenario: List built-in baselines
- **WHEN** a client requests the rule baseline library
- **THEN** DataSpec returns baseline key, name, version, description, rule count, and rule summaries
- **AND** the library includes `personal_default`, `strict`, and `legacy_compatible`

#### Scenario: Baseline rules are valid
- **WHEN** DataSpec starts or tests the baseline library
- **THEN** every built-in rule references a known lint rule code
- **AND** every built-in `paramsJson` is valid JSON

### Requirement: Apply rule baseline to project
DataSpec SHALL apply a selected rule baseline to a project while protecting user-edited rules by default.

#### Scenario: Apply without overwrite
- **WHEN** a user applies a built-in baseline to a project with `overwrite=false`
- **THEN** missing rule configs are created for the project
- **AND** existing rule configs with the same rule code are skipped
- **AND** the response includes created, updated, skipped counts and skipped rule codes

#### Scenario: Apply with explicit overwrite
- **WHEN** a user applies a baseline with `overwrite=true`
- **THEN** existing rule configs with the same rule code are updated from the baseline
- **AND** the response includes updated rule codes

#### Scenario: Save project baseline metadata
- **WHEN** a baseline is applied successfully
- **THEN** DataSpec stores project baseline key, name, version, source, applied time, and exported rule package metadata

### Requirement: Export and import rule baseline package
DataSpec SHALL support exporting and importing project rule baselines as stable machine-readable JSON.

#### Scenario: Export project baseline
- **WHEN** a client exports a project's rule baseline
- **THEN** the response includes schema version, baseline metadata, exported time, and all project rule configs without database IDs

#### Scenario: Import baseline package
- **WHEN** a client imports a valid baseline package into a project
- **THEN** DataSpec applies the package rules to that project using the same overwrite behavior as built-in baselines
- **AND** it records the baseline source as `imported`

#### Scenario: Reject invalid package
- **WHEN** an imported baseline package has an unsupported schema version or invalid rule payload
- **THEN** DataSpec rejects the request with a readable validation error
- **AND** no project rules are written
