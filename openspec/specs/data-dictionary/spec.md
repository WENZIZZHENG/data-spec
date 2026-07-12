# data-dictionary Specification

## Purpose
定义 Markdown 数据字典导出内容，汇总项目概览、标准字段、领域关系、枚举字典、枚举值和表模板信息，便于人工和 AI 阅读。
## Requirements
### Requirement: Enhanced Markdown Data Dictionary
The system SHALL generate a richer Markdown data dictionary from project standards.

#### Scenario: Include project overview
- **WHEN** a client previews or downloads the Markdown data dictionary
- **THEN** the document includes counts for data domains, standard fields, enum dictionaries, and table templates

#### Scenario: Include field metadata and domain relation
- **WHEN** standard fields are present
- **THEN** each field row includes its domain relation and personal metadata such as aliases, category, sensitivity, status, code set, and example value

#### Scenario: Include enum value type
- **WHEN** enum dictionaries are present
- **THEN** each enum section includes its value type and values

#### Scenario: Include table templates
- **WHEN** table templates are present
- **THEN** the document includes each template and its fields with required, nullable, default value, sort order, and linked standard field information

### Requirement: Data Dictionary Semantic Sections
The Markdown data dictionary SHALL include concise field semantic, enum lifecycle, knowledge card, naming translation, and metric mapping summaries.

#### Scenario: Field rows include semantic summary
- **WHEN** standard fields have semantic rules, naming guidance, or knowledge card risk notes
- **THEN** the data dictionary includes concise semantic summary columns or subsections for those fields
- **AND** it avoids dumping oversized card content into compact tables.

#### Scenario: Enum dictionary includes lifecycle
- **WHEN** enum values have status, aliases, replacement values, validity windows, or mapping hints
- **THEN** each enum section includes that lifecycle information in a human-readable way.

#### Scenario: Metric definitions are documented
- **WHEN** a project has metric definitions
- **THEN** the data dictionary includes metricKey, displayName, definition, measure fields, dimensions, filter rule, aggregation rule, time grain, and example SQL summary
- **AND** example SQL is clearly labeled as explanatory guidance only.
