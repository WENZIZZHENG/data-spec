## Context

字段后端已有 `/api/fields` MyBatis 分页，但 `FieldLibrary.vue` 无筛选时调用 `/api/fields/all`，随后用 `slice` 浏览器分页。有搜索条件时，`/api/fields/search` 会按确定性评分排序，但只接受最多 50 条的 `limit`，页面再次浏览器分页，因此第 51 条以后无法访问。搜索还会在每次输入变化时立即请求，并重复加载分组、数据域和语义规则。

本变更跨 Spring Boot、OpenAPI、Vue 和 Playwright，按 SDD standard 执行。搜索排序必须继续兼容 glossary、历史别名、Standard Query 和 usage evidence；CLI/MCP 现有 `limit` 调用不能被分页默认值改变。

## Goals / Non-Goals

**Goals:**

- 字段库首屏和翻页只传输当前页字段。
- 搜索结果超过 50 条时仍能按稳定排序逐页访问。
- 连续输入只提交最终关键词，旧请求结果不能覆盖新状态。
- 慢请求有不改变布局的可访问状态提示。
- 旧 API、CLI 和 MCP 只传 `limit` 时保持原行为。

**Non-Goals:**

- 不重写确定性搜索评分、glossary 或历史别名匹配。
- 不引入虚拟列表、通用遥测平台或新的前端状态框架。
- 不删除 `/api/fields/all`；它继续服务确实需要完整字段集合的既有调用。
- 不在第一版优化所有字段关联数据接口或建立数据库全文索引。

## Decisions

### 1. 搜索分页采用 additive 双模式

`/api/fields/search` 新增可选 `current`、`size`。任一分页参数存在时进入分页模式，返回当前页 `items` 和 additive `page` 元数据；两者都不存在时继续使用原有 `limit`，不返回 `page`，保持 CLI/MCP 和旧客户端语义。为保留左侧现有分组能力，同一接口 additive 支持 `domainId`、`ungrouped` 和 `includeAllStatuses`，category/tag/status/sourceBatchId 继续沿用既有参数。`includeAllStatuses=true` 仅由明确展示“全部状态”的字段库发送；旧调用不传时继续默认只返回 enabled 字段。

备选方案是直接把 `limit` 改成页大小，或新建第二个搜索端点。前者会破坏旧调用，后者会复制搜索契约和评分逻辑，因此不采用。

### 2. 排序完成后分页，不下推不完整的文本条件

搜索继续加载项目候选并执行现有确定性过滤、glossary、历史别名和评分，再按 `score DESC, name ASC, fieldId ASC` 稳定排序后按 offset/size 截取。普通无筛选列表直接复用已有数据库分页 API。

备选方案是把关键词 LIKE 全部下推数据库。当前评分可由 glossary canonical、历史别名、usage contract 和 Standard Query evidence 命中，简单 LIKE 会漏召回并改变顺序，因此第一版只缩小网络与渲染窗口，不牺牲搜索正确性。

### 3. 页面请求分为字段窗口与低频元数据

翻页和关键词搜索只刷新字段窗口；分组摘要、数据域和语义规则仅在项目切换、手动刷新或字段写入后更新。字段替代和合并候选不参与首屏请求，打开对应弹窗时再调用既有全量接口并缓存到当前项目。

### 4. 前端使用 300ms 防抖、序列号和 600ms 慢状态

关键词输入延迟 300ms 后请求；状态、分组、页码和页大小变化立即请求。字段窗口使用 `loadSequence` 阻止旧响应覆盖新状态，分组、数据域和语义规则使用独立 metadata sequence，确保显式刷新不会被随后搜索废弃。请求超过 600ms 时显示 `role=status` 的固定高度状态行，请求结束后清除。

不使用 AbortController，因为当前全局 Axios interceptor 会把取消当作错误提示；本变更不扩大到全局请求取消语义。

### 5. 窄屏使用覆盖式主导航和可横向浏览的字段表格

在 `720px` 以下，应用主导航从页面布局中移出，改为有遮罩、关闭按钮、Escape、背景 inert、焦点循环和焦点返回的覆盖式导航，避免固定侧栏挤压字段库。字段库在 `640px` 以下堆叠工具栏，将分组压缩为横向可滚动选项，并取消表格固定列的 sticky 定位；字段列仍由表格自身横向滚动承载，不允许整页横向溢出。

不直接隐藏主导航，也不把宽字段表重写为卡片列表；前者会丢失移动端入口，后者会扩大本次分页改造范围并产生两套字段操作界面。

## Risks / Trade-offs

- [搜索后端仍扫描项目候选，超大项目可能超过 500ms] → 保留慢操作日志与前端慢状态；后续只有基准证明必要时再设计索引或候选缓存。
- [弹窗首次打开会按需加载完整候选] → 只在确实需要跨页替代/合并时发生，并按项目缓存；首屏、翻页和普通搜索不承担该成本。
- [分页过程中字段变化可能导致页间移动] → 使用稳定 `score DESC, name ASC` 排序；本地个人/小团队场景不引入快照游标，刷新后以最新状态为准。
- [新增字段可能让旧客户端忽略分页信息] → 所有新字段 additive，旧 `limit` 模式不返回分页元数据，契约 fixture 保持宽松兼容。

## Migration Plan

1. 先发布 additive 后端参数、分页模型和 OpenAPI。
2. 再切换同版本前端使用服务端分页。
3. 保留 `/api/fields/all` 和旧 `limit` 模式作为兼容与回滚路径。
4. 回滚前端时旧页面仍可调用 `/fields/all`；回滚后端时不影响未发送 `current/size` 的 CLI/MCP。

## Open Questions

- 无。虚拟化、数据库索引和通用请求取消只在本次浏览器与性能证据显示仍有必要时另立任务。
