## 背景

P3-1a 需要支持个人常见 MySQL DDL。当前解析器主要覆盖 PostgreSQL 的 `COMMENT ON` 写法，MySQL 常见的反引号、列内 `COMMENT`、表级 `COMMENT='...'`、`AUTO_INCREMENT`、`tinyint(1)` 和 `datetime` 缺少回归测试。

## 变更

- 扩展 `SqlParserService` 对 MySQL `CREATE TABLE` 基础语法的兼容。
- 支持表级 `COMMENT='...'` 和列级 `COMMENT '...'` 回填注释。
- 增加 MySQL DDL 解析测试。

## 非目标

- 不实现完整 MySQL parser 或跨数据库迁移。
- 不支持所有 MySQL 特性，仅覆盖个人常用建表 DDL。
