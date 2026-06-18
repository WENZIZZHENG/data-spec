## Why

P1-8 需要把 SQL lint 从“一次性问题列表”升级为可复盘、可给 AI 继续处理的闭环。当前后端已有结构化修复建议，但前端还不能展示 `fixedSql`，也不能查看最近 SQL 检查记录。

## What Changes

- 后端 lint 结果暴露 `fixedSql`，并保存每次 SQL 检查记录。
- 后端提供检查记录分页查询和详情查询 API。
- 前端 SQL 校验页展示修正 SQL、复制入口和最近检查记录。
- 前端 API 类型、lint API wrapper、TODO 路线图同步更新。

## Scope

- 本轮只保存和展示个人/小团队使用所需的 SQL 检查记录。
- 修正 SQL 使用既有确定性修复建议重建；不能安全重建时返回空值。
- 不做用户权限、审批、PR 评论、复杂统计看板或自动覆盖源文件。

## Impact

- 数据库新增 `ds_sql_check_record`。
- `/api/lint` 响应新增 `fixedSql`。
- 新增 `/api/lint/records` 和 `/api/lint/records/{id}`。
- `SqlLint.vue` 增加修正 SQL 面板与历史记录区。
