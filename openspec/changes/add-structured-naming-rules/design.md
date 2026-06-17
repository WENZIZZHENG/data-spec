## Context

个人版不需要复杂规则 DSL，但 AI 需要稳定读到命名偏好。现有规则已经覆盖 snake_case、禁用字段、推荐字段、必含列；缺口是这些规则没有统一导出模型，且 `_id/_at/_no/_count/is_` 这类命名与类型约束没有可执行 lint。

## Decisions

### 字段后缀/前缀类型规则

- 新增 `field_suffix_type`。
- 默认后缀规则：
  - `_id`: `bigint`, `integer`, `bigserial`
  - `_at`: `timestamp`, `timestamp with time zone`, `datetime`
  - `_no`: `varchar`, `char`, `text`
  - `_count`: `integer`, `bigint`
- 默认前缀规则：
  - `is_`: `boolean`
- `paramsJson` 支持 `suffixTypes` 和 `prefixTypes` 覆盖默认值。

### 结构化导出

- `rules.yaml` 保留原有 `rules:` 清单，同时新增 `naming:`。
- `naming:` 从项目规则配置读取 `required_columns`、`forbidden_field_name`、`recommended_field_name`、`field_suffix_type` 的 params；缺失时使用默认规则。
- YAML 保持简单缩进，不引入额外 YAML 库。

## Risks

- 类型匹配采用归一化字符串前缀匹配，不能覆盖所有数据库类型别名；这是第一版可接受边界。
- `paramsJson` 如果格式错误，导出会回退默认值，lint 运行仍沿用现有错误容忍策略。
