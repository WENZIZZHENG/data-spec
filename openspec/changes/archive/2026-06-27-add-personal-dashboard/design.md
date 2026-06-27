## 设计

### 汇总指标

Dashboard 汇总接口按当前项目计算：

- `fieldCount`：标准字段数量。
- `enumDictCount`：代码集数量。
- `ruleCount`：规则配置数量。
- `forbiddenTermCount`：禁用词数量，优先读取 `forbidden_field_name.paramsJson.forbiddenNames`，否则使用内置默认数量。
- `recentCheckCount`：项目 SQL 检查记录总数。
- `fieldHitRate`：最近 20 条检查中，不包含字段标准类问题的记录占比；没有检查记录时返回 `null`。
- `trend`：最近 10 条检查的问题数量趋势。
- `recentChecks`：最近 5 条检查摘要。

### 字段标准类问题

第一版将以下规则视为字段标准相关：

- `field_naming_snake_case`
- `forbidden_field_name`
- `recommended_field_name`
- `field_suffix_type`
- `amount_field_unit`

### 前端

Dashboard 使用现有 Element Plus 组件和当前项目 store。无当前项目时显示空状态；切换项目后自动刷新。
