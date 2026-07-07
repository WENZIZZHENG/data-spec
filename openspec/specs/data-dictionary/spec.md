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
