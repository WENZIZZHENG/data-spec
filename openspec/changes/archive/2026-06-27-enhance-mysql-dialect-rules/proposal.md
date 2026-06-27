## Why

当前解析器已支持常见 MySQL `CREATE TABLE`，但 `UNSIGNED`、`DECIMAL` 精度、`ENGINE/CHARSET/COLLATE/KEY` 组合场景缺少回归测试，`is_` 布尔命名规则也会把 MySQL 常用 `tinyint(1)` 误判为非布尔。P4-8 需要把个人常用 MySQL DDL 检查链路补稳。

## What Changes

- 扩展 MySQL `CREATE TABLE` 解析测试，覆盖 `ENGINE`、`DEFAULT CHARSET`、`COLLATE`、`KEY/INDEX`、`DECIMAL(p,s)`、`UNSIGNED`。
- 解析列类型时保留 `unsigned` 修饰，避免金额、计数、ID 等字段类型信息丢失。
- 字段后缀/前缀类型规则将 MySQL `tinyint(1)` 视为布尔等价类型，避免 `is_` 字段误报。
- 更新 README/TODO 中 P4-8 状态。

## Capabilities

### New Capabilities
- `mysql-dialect-rule-coverage`: MySQL DDL 解析和规则兼容增强。

### Modified Capabilities
无。

## Impact

- 后端影响 `SqlParserService` 和 `FieldSuffixTypeRule`。
- 增加 parser/rule 单元测试。
- 不新增前端页面、数据库表或外部依赖。
