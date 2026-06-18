## Design

### Persistence

使用单表 `ds_sql_check_record` 保存原 SQL、修正 SQL、问题统计和序列化后的 `LintIssue` 列表。第一版保留原文，便于个人复盘和后续命中率统计；不做数据清理策略和权限隔离。

### Fixed SQL

修正 SQL 由 lint 解析结果和结构化修复建议重建，而不是对原 SQL 做字符串替换。这样可以避开缺少 source span 时误替换同名字段的问题。无法安全重建时返回 `null`，前端不展示修正 SQL 面板。

### API Shape

- `POST /api/lint` 返回 `LintResult.fixedSql`。
- `GET /api/lint/records` 返回 `PageResult<SqlCheckRecord>`。
- `GET /api/lint/records/{id}` 返回 `{ record, issues }`，其中 `issues` 是从 `issuesJson` 反序列化后的结构化问题列表。

### Frontend UX

SQL 校验页保持左右分栏：左侧编辑器、右侧结果。修正 SQL 放在 summary 下方，历史记录放在分栏下方的折叠区，避免压缩主要编辑空间。查看详情使用 dialog，展示原 SQL、修正 SQL 和 issues 表格。

### Risks

- 记录表会保存原 SQL，可能包含业务敏感信息；本轮定位个人/小团队本地工具，先不引入权限和脱敏。
- 修正 SQL 只覆盖确定性规则，不代表完整自动修复。
