## ADDED Requirements

### Requirement: Field catalog exports starter kit source metadata
The AI Context field catalog SHALL include additive starter kit source metadata for fields created from domain starter kits.

#### Scenario: Export field created from starter kit
- **WHEN** AI context field catalog is generated for a project with starter kit fields
- **THEN** each starter kit field includes starterKitSources with kitKey and kitVersion
- **AND** the existing field name, type, tags, aliases, status, sensitive, and example metadata remain compatible.

#### Scenario: Export project without starter kit fields
- **WHEN** AI context field catalog is generated for a project without starter kit source markers
- **THEN** the field catalog remains valid
- **AND** starterKitSources is omitted or empty for those fields.
