## ADDED Requirements

### Requirement: 覆盖率报告复用采集作业部分结果
DataSpec SHALL allow field coverage reports to use schema-only partial results from metadata scan jobs.

#### Scenario: 使用成功 partial result 生成覆盖率
- **WHEN** a user requests coverage from a metadata scan job partial result containing successful table metadata
- **THEN** DataSpec generates coverage only for successful schema-only tables.
- **AND** the report identifies partial input status and failed or skipped table counts.

#### Scenario: 不完整采集结果提示边界
- **WHEN** coverage is generated from a partial, cancelled, or failed scan job
- **THEN** the report includes safe next actions explaining that coverage is partial.
- **AND** the report MUST NOT treat not-yet-scanned or failed tables as covered.
