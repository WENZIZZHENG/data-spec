## 设计

### 解析策略

继续使用 JSqlParser 解析 `CREATE TABLE` 主体。JSqlParser 能识别大部分 MySQL 列定义，但对注释信息不稳定，因此增加轻量正则补偿：

- 表注释：匹配 `COMMENT='...'` 或 `COMMENT = '...'`。
- 列注释：在 `ColumnDefinition.columnSpecs` 中查找 `COMMENT` 后的字符串。

### 兼容边界

- 支持反引号标识符规范化。
- 保留 `AUTO_INCREMENT` 但不映射为单独字段属性。
- `tinyint(1)`、`datetime` 等类型按原始类型输出，后续 lint 规则自行判断。
