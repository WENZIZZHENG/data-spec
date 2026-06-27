## Why

字段推荐已经能回答“这个字段名应匹配哪个标准字段”，但 AI 和用户在建表、修 SQL 或做标准补全前，更常见的是先检索一组相关标准字段。当前需要从字段列表、推荐接口和 AI Context 裁剪里拼接信息，缺少一个稳定的字段标准搜索入口。

## What Changes

- 新增字段标准检索 API，按项目、关键词、分类、标签、状态、敏感标记和来源批次查询标准字段。
- 返回面向 AI 的检索结果项，包含字段、分数、命中原因、推荐使用范围和下一步建议。
- CLI 增加字段检索命令，MCP 增加或升级字段检索工具，输出稳定 JSON。
- 前端字段库复用检索能力，保留现有列表筛选体验并展示命中原因。
- 新增后端、CLI/MCP 和前端源码级测试，锁定同义词、别名、拼音缩写、分类/标签筛选和空结果提示。
- 更新 README/TODO，说明 P6-19 第一版能力和边界。

## Capabilities

### New Capabilities

- `field-standard-search`: 面向人和 AI 的项目级字段标准检索契约。

### Modified Capabilities

- 无。

## Impact

- 后端：扩展 field 模块，新增检索请求/结果模型、service 方法和 controller endpoint。
- CLI/MCP：新增字段检索命令/工具或升级现有 `search_field_catalog` 行为，保持 JSON 可解析。
- 前端：字段库搜索接入字段检索 API，显示命中原因和下一步建议。
- 测试：新增后端检索单测、CLI/MCP 契约测试、前端源码级 smoke 覆盖。
- 文档：README/TODO 同步能力说明。
