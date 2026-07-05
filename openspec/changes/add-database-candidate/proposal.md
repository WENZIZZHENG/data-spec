## Why

数据库直连反向导入当前以“连接 -> 选表 -> 预览 -> 导入”为主，用户和 AI 在导入前缺少一个只读浏览真实 schema/table/column/comment/index 的轻量视图。

## What Changes

- 在现有反向导入数据库直连流程中新增只读 metadata browser 聚合接口和前端子视图。
- 支持对已选表的 schema/table/column/comment/index metadata 做搜索、摘要展示和 AI 可复制结构摘要。
- 在浏览结果中展示字段标准命中、缺注释、类型差异、未纳管/候选状态，并允许用户勾选本次导入候选。
- 继续复用既有反向导入确认接口；浏览、预览、差异和覆盖率分析均不写源数据库、不采样业务数据行、不保存数据库密码。

## Capabilities

### New Capabilities
- `database-candidate`: 数据库直连 metadata 浏览、AI 摘要和候选选择。

### Modified Capabilities
- `db-metadata-dump`: schema dump 的表 metadata 增加 index 摘要，用于只读浏览和 AI schema-only 分析。
- `db-reverse-import-frontend`: 数据库直连页增加 metadata 浏览子视图。

## Impact

- 影响后端 `/api/reverse-import/database/*` 只读分析面，新增兼容性接口，不删除既有字段或接口。
- 影响前端反向导入页的数据库直连子流程和相关 TypeScript 类型/API wrapper。
- 更新 README/TODO/OpenSpec 以记录第一版能力、边界和验证证据。
- 强制保留安全边界：不做通用 SQL 客户端，不执行任意查询，不保存数据库密码，不采样源库业务数据行。
