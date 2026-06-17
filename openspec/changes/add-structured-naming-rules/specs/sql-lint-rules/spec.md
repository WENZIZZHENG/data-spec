## MODIFIED Requirements

### Requirement: Field Suffix Type Rules
The SQL lint engine SHALL validate common field suffix and prefix type conventions.

#### Scenario: Detect suffix type mismatch
- **WHEN** a column name ends with `_id`, `_at`, `_no`, or `_count`
- **AND** its data type does not match the configured type list
- **THEN** the lint result includes a `field_suffix_type` issue

#### Scenario: Detect boolean prefix mismatch
- **WHEN** a column name starts with `is_`
- **AND** its data type is not boolean
- **THEN** the lint result includes a `field_suffix_type` issue

#### Scenario: Override suffix rules
- **WHEN** `paramsJson` provides `suffixTypes` or `prefixTypes`
- **THEN** the rule uses those configured type lists instead of defaults
