## Context

`SqlParserService` 基于 JSqlParser 提取表名、列名、类型、默认值、nullable 和注释，并通过正则兜底 MySQL 表注释。`FieldSuffixTypeRule` 会按字段后缀/前缀校验类型，但默认 `is_` 只允许 `boolean`。在 MySQL 项目里，`tinyint(1)` 经常表示布尔字段，`decimal unsigned`、`bigint unsigned` 也常见；这些都应稳定进入 lint 结果。

## Goals / Non-Goals

**Goals:**

- MySQL DDL 中的 `DECIMAL(p,s)` 精度和 `UNSIGNED` 修饰可被解析并保留。
- `ENGINE/DEFAULT CHARSET/COLLATE/KEY/INDEX` 不影响表和列解析。
- `is_` 前缀规则默认接受 `tinyint(1)` 和 `tinyint` 作为 MySQL 布尔等价类型。
- 用单测锁定上述行为。

**Non-Goals:**

- 不实现完整 MySQL AST 或索引模型。
- 不把 KEY/INDEX 输出到 `TableDef`，因为当前模型没有索引字段。
- 不扩展 SQL Server 等其他方言。
- 不改变现有 PostgreSQL 规则默认行为。

## Decisions

1. **在 parser 层把 `UNSIGNED` 作为类型修饰追加到 `dataType`**
   - 理由：当前 `ColumnDef` 没有 unsigned 独立字段，追加到类型字符串能让前端、反向导入和 lint 看到真实类型。
   - 替代方案：新增 structured type model；超出本轮边界。

2. **规则层做类型别名归一化**
   - 理由：`tinyint(1)` 是否表示布尔是 MySQL 习惯，不应要求用户每个项目都手写 prefixTypes。
   - 替代方案：把默认 `is_` 允许列表加上 `tinyint` 字面值；可行，但集中在 normalize 中更容易复用。

3. **KEY/INDEX 只测试“不破坏解析”**
   - 理由：DataSpec 当前 lint 关注字段标准，不管理真实数据库索引；测试解析稳定即可。

## Risks / Trade-offs

- **`tinyint` 并非所有场景都是布尔** → 只在字段名前缀命中 `is_` 等布尔规则时作为等价类型，不全局改写列类型。
- **`unsigned` 字符串影响既有类型匹配** → `FieldSuffixTypeRule.normalizeType` 会去掉 `unsigned` 后再匹配，避免 `user_id bigint unsigned` 误报。
- **MySQL 复杂 DDL 仍可能超出 JSqlParser 支持** → 本轮只锁定个人常用建表语法。
