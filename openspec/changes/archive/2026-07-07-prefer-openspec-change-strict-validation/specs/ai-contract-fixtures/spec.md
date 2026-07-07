## ADDED Requirements

### Requirement: Validation advisor prefers single OpenSpec change strict validation
The validation advisor SHALL recommend the narrowest OpenSpec validation command that safely matches the changed paths so AI agents can run fast, relevant checks before broader gates.

#### Scenario: Single active change path
- **WHEN** all OpenSpec input paths belong to the same active change under `openspec/changes/<change-id>/`
- **THEN** the `openspec-validate` recommendation uses `openspec validate <change-id> --strict`.

#### Scenario: Multiple active changes
- **WHEN** OpenSpec input paths include more than one active change id
- **THEN** the `openspec-validate` recommendation uses `openspec validate --all`.

#### Scenario: Main specs or archive paths
- **WHEN** OpenSpec input paths include main specs, archive paths, or paths outside a single active change
- **THEN** the `openspec-validate` recommendation uses `openspec validate --all`.

#### Scenario: Recommendation identity remains stable
- **WHEN** the OpenSpec validation command is narrowed to one change
- **THEN** the recommendation keeps the existing `openspec-validate` command id and category.
