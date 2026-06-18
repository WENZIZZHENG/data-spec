## 设计

### 表结构

`ds_standard_change_log` 使用 append-only 记录：

- `project_id`：项目 ID。
- `target_type`：`field`、`enum_dict`、`enum_value`、`rule_config`。
- `target_id`：目标记录 ID。
- `action`：`create`、`update`、`delete`、`toggle`。
- `before_json` / `after_json`：变更前后实体快照。
- `changed_at`：写入时间。

### 写入位置

日志记录放在 service 层，跟随现有 create/update/delete/toggle 入口，覆盖 UI、导入和后续 API 调用。

### 查询

第一版只提供按项目分页查询，支持可选 `targetType` 和 `targetId` 过滤，供后台或后续工作台复用。

### 失败策略

日志写入与业务写入在同一事务上下文中执行。日志序列化失败视为开发期错误，抛出业务异常，避免用户误以为已经可追溯。
