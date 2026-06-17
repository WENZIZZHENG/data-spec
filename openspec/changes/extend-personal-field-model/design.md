## Context

当前 `ds_field` 已有 `name`、`displayName`、`dataType`、`nullable`、`defaultValue`、`comment`、`domainId`、`tags`。这些足够支撑基础 CRUD，但 AI 生成 SQL 需要更多语义：别名用于匹配自然语言和历史字段名，状态用于避免继续推荐废弃字段，敏感标记用于提示隐私风险，代码集关联用于生成枚举约束和说明。

## Decisions

### 单表扩展

- 第一版把新增元数据直接放入 `ds_field`，不拆别名表。
- `aliases` 使用逗号分隔字符串存储，AI 导出时转换为字符串数组。
- `status` 使用字符串保存，允许值约定为 `enabled`、`disabled`、`deprecated`。
- `code_set_id` 关联 `ds_enum_dict.id`，本轮不加数据库外键，避免影响导入导出和个人迁移。
- `example_value` 映射为 Java 字段 `exampleValue`，AI 导出字段名使用 `example`。

### 默认值

- `sensitive` 默认 `false`。
- `status` 默认 `enabled`。
- `nullable` 保留现有默认 `true`。

### AI 导出

- `field-catalog.json` 对 aliases 做 trim、去空，并输出数组。
- 只有存在值时输出 `category`、`codeSetId`、`example`，避免污染旧数据目录。
- schema 同步声明新增属性，使 AI/工具可验证字段目录结构。

## Risks

- 逗号分隔 aliases 不适合复杂别名元数据；个人版足够简单，后续需要权重/语言/来源时再拆表。
- 不加外键意味着 `codeSetId` 可能指向不存在的枚举；本轮仅作为弱关联元数据输出。
