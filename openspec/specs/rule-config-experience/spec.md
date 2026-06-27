# rule-config-experience Specification

## Purpose
TBD - created by archiving change enhance-rule-config-experience. Update Purpose after archive.
## Requirements
### Requirement: 常见规则参数结构化编辑
规则配置页 SHALL 根据规则编码为常见规则展示结构化参数编辑器，覆盖必含列、禁用字段名、推荐替换、字段后缀类型和字段前缀类型。

#### Scenario: 编辑必含列规则
- **WHEN** 用户选择 `required_columns`
- **THEN** 页面展示必含列列表编辑器，并在保存时生成包含 `requiredColumns` 的 `paramsJson`

#### Scenario: 编辑禁用字段名规则
- **WHEN** 用户选择 `forbidden_field_name`
- **THEN** 页面展示禁用字段名列表编辑器，并在保存时生成包含 `forbiddenNames` 的 `paramsJson`

#### Scenario: 编辑推荐替换规则
- **WHEN** 用户选择 `recommended_field_name`
- **THEN** 页面展示原字段名到推荐字段名的映射编辑器，并在保存时生成包含 `recommendations` 的 `paramsJson`

#### Scenario: 编辑后缀前缀类型规则
- **WHEN** 用户选择 `field_suffix_type`
- **THEN** 页面展示后缀和前缀到允许类型列表的映射编辑器，并在保存时生成包含 `suffixTypes` 和 `prefixTypes` 的 `paramsJson`

### Requirement: JSON 兜底与预览
规则配置页 SHALL 保留 JSON 文本编辑或预览能力，使未知规则和复杂参数仍可维护。

#### Scenario: 编辑未知规则
- **WHEN** 用户输入不在常见规则集合中的规则编码
- **THEN** 页面展示 JSON 参数编辑器，并用合法 JSON 作为保存内容

#### Scenario: 查看结构化参数 JSON
- **WHEN** 用户修改结构化参数表单
- **THEN** 页面同步展示将要保存的 JSON 文本

### Requirement: 规则列表展示参数摘要
规则配置页 SHALL 在规则列表中展示常见参数的可读摘要，而不是只展示原始 JSON。

#### Scenario: 查看规则列表
- **WHEN** 规则配置包含常见参数
- **THEN** 页面展示必含列、禁用词数量、推荐替换数量或后缀/前缀规则数量等摘要信息
