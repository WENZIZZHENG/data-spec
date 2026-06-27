## ADDED Requirements

### Requirement: Rule baseline operations on rule config page
规则配置页 SHALL provide project-scoped rule baseline operations alongside existing single-rule CRUD.

#### Scenario: View current baseline
- **WHEN** 用户打开规则配置页且已选择项目
- **THEN** 页面展示当前项目规则基线名称、版本、来源和应用时间
- **AND** 没有基线记录时展示自定义规则状态

#### Scenario: Apply built-in baseline
- **WHEN** 用户在规则配置页选择一个内置基线并应用
- **THEN** 页面调用基线应用 API
- **AND** 成功后刷新规则列表和当前基线摘要

#### Scenario: Import and export baseline
- **WHEN** 用户导出或导入规则基线
- **THEN** 页面使用稳定 JSON 包完成下载或上传
- **AND** 导入完成后展示 created、updated、skipped 摘要
