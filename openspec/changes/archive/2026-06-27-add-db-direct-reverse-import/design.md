## 设计

### 后端 API

在现有 `/api/reverse-import` 下新增：

- `POST /api/reverse-import/database/test`：测试数据库连接。
- `POST /api/reverse-import/database/tables`：读取 schema/table 列表。
- `POST /api/reverse-import/database/preview`：按所选 schema/table 读取 metadata，生成 `ReverseImportPreview`。
- `POST /api/reverse-import/database/import`：确认导入字段候选到当前项目字段库。

### 数据库连接

请求体包含数据库类型、host、port、database、schema、username、password、tableNames。第一版仅在请求生命周期内使用连接信息，不入库、不写日志。连接 URL 由后端根据 `databaseType` 生成，避免前端传完整 JDBC URL。

支持：

- PostgreSQL：默认端口 `5432`，默认 schema `public`。
- MySQL：默认端口 `3306`，schema 取 database/catalog。

### metadata 到 TableDef 映射

通过 JDBC `DatabaseMetaData` 读取：

- `getTables`
- `getColumns`

映射为现有 `TableDef`/`ColumnDef`，再复用现有反向导入分析逻辑。

字段注释从 `REMARKS` 读取。nullable 根据 `DatabaseMetaData.columnNullable` 判断。默认值从 `COLUMN_DEF` 读取。数据类型优先使用 `TYPE_NAME`，附加长度/精度信息。

### 确认导入

确认导入只处理预览结果中的字段候选。后端再次按当前项目字段名去重，已存在字段跳过；新增字段写入 `ds_field`，并保留表名作为 `category`，注释作为 `comment/displayName` 的候选来源。

### 前端交互

反向导入页增加 tabs：

- SQL DDL：保留现有粘贴/读取 SQL 文件流程。
- 数据库直连：连接表单、测试连接、加载表、选择表、生成预览、确认导入。

预览结果继续复用现有 summary 和四个结果表格。
