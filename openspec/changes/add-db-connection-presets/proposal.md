## Why

数据库直连反向导入已经是高频入口，但每次重复填写 host、port、database、schema 和表选择很繁琐。DataSpec 需要保存可复用的非敏感连接预设，同时继续坚持不保存密码、token 或完整连接串。

## What Changes

- 新增项目级数据库连接预设模型/API/前端选择器。
- 预设只保存 `databaseType`、`host`、`port`、`databaseName`、`schemaName`、`tableNames`、`name` 和更新时间等非敏感字段。
- 反向导入页支持从预设加载连接信息和表选择，并可把当前非敏感连接信息保存为预设。
- 使用预设后仍由用户当次输入密码；测试连接、加载表、metadata 预览、二次比对和确认导入继续走现有直连流程。
- 不保存 password、token、完整 JDBC URL，不引入连接池或后台同步。

## Capabilities

### New Capabilities

- `db-connection-presets`: 定义项目级数据库连接预设、敏感字段排除和反向导入页复用行为。

### Modified Capabilities

- `reverse-import`: 反向导入前端增加加载/保存预设的交互，但现有直连测试、表加载、预览、比对和确认导入语义不变。

## Impact

- 数据库：新增 `ds_database_connection_preset` 表和 Flyway migration。
- 后端：新增 preset entity/repository/service/controller；复用项目访问边界。
- 前端：新增 preset API wrapper、类型、展示测试，并改造反向导入页的直连表单。
- 文档：更新 README、TODO 和 OpenSpec tasks。
