## Why

标准字段库虽然已有分页 API，但页面无筛选时仍拉取项目全部字段后在浏览器切片；有筛选时又受固定 50 条上限限制。字段数量增长后，首次加载、搜索输入和分页都会产生不必要的数据传输、结果不可达和界面卡顿，因此需要把列表与搜索统一到服务端分页闭环。

## What Changes

- 字段库无筛选列表改用现有 `/api/fields` 服务端分页，不再以 `/api/fields/all` 作为页面首屏数据源。
- `/api/fields/search` 增加可选 `current`、`size`、`domainId`、`ungrouped` 参数和 additive 分页元数据；旧调用只传 `limit` 时保持原有返回和上限语义。
- 字段库搜索输入增加防抖、过期响应保护和慢请求状态，分页、状态筛选、分组筛选均由服务端返回当前页。
- 增加超过 50 条搜索结果的浏览器回归，验证下一页可达、请求参数正确且快速连续输入只触发最终检索。
- 字段替代和合并所需的全量候选改为按需加载，避免页面首屏为了弹窗选项拉取全量字段。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `field-standard-search`: 增加向后兼容的搜索分页契约，并要求字段库列表、筛选、慢状态和大结果集浏览使用服务端分页。

## Impact

- 后端：字段搜索 request/result model、Controller、Service、OpenAPI 与相关单测。
- 前端：字段 API type、字段库页面请求状态、分页与按需候选加载。
- 测试：后端分页/兼容测试、前端纯逻辑测试、Playwright 大结果集与防抖回归、OpenAPI drift 检查。
- 兼容性：不删除或重命名既有字段；CLI/MCP 继续只传 `limit`，返回结构仅新增分页元数据。
