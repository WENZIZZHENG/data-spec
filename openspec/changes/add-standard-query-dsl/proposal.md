## Why

AI、CLI、MCP 和前端现在已经能按 query、category、tag、status 等松散参数检索字段标准，但这些参数无法表达“订单域 + 金额字段 + 可用于建表 + 非敏感 + 最近验证”这类可复用组合条件。P6-167 需要在稳定引用和 AI 输出后置校验之后，补一层项目内只读 Standard Query DSL，让检索、Context 裁剪、问答和工具调用共享同一套可解释筛选语义。

## What Changes

- 新增轻量 Standard Query DSL：支持文本 query、对象类型、字段元数据筛选、逻辑组合、排序、limit 和 explain 选项。
- 新增只读查询解析与执行结果契约：输出 `querySummary`、`appliedFilters`、`ignoredFilters`、`resultCount`、`nextQueryHints` 和可复用的 normalized query。
- 字段标准搜索 additive 接入 DSL：现有 query/category/tag/status/sensitive/sourceBatchId 参数继续可用，并可被映射成等价 DSL。
- AI Context 裁剪 additive 接入 DSL：导出 field catalog/package 时可复用同一条 query expression，并在 manifest 或 metadata 中记录安全摘要。
- CLI/MCP 新增或扩展查询入口：让 AI agent 可在 API、CLI、MCP 中用同一 DSL 获得一致字段集合。
- Schema Registry 和 CLI/MCP fixture 增加 DSL schema、输出形状和安全元数据，防止协议漂移。
- 第一版不做任意 SQL 查询、不做全文搜索平台、不绕过项目边界、安全红线或字段可见性策略；只覆盖标准对象元数据筛选，优先字段标准。

## Capabilities

### New Capabilities

- `standard-query-dsl`: 定义 Standard Query DSL、解析错误、执行摘要、可解释筛选结果和安全边界。

### Modified Capabilities

- `field-standard-search`: 字段标准搜索支持 additive DSL 入参和可解释查询摘要，同时保持现有参数兼容。
- `ai-context-package`: AI Context 导出支持使用 Standard Query DSL 裁剪字段目录，并记录查询摘要。
- `standard-schema-registry`: Schema Registry 描述 Standard Query DSL 请求、错误和结果摘要 schema。
- `dataspec-cli`: CLI 提供稳定 JSON 的 Standard Query DSL 查询入口或扩展 `search-fields`。
- `dataspec-mcp`: MCP 暴露只读 Standard Query DSL 查询 tool，或扩展现有 `search_fields` tool 的 DSL 参数。
- `cli-mcp-contract-fixtures`: fixture 覆盖 DSL CLI/MCP 描述、输入 schema、安全元数据、输出 shape 和漂移检查。

## Impact

- 后端：新增 DSL model/parser/service/API；复用 `FieldService.search`、稳定引用、字段生命周期、字段可见性、字段质量和 AI Context 现有裁剪逻辑。
- 前端：字段库和 AI Context 页面可逐步以 DSL 生成/展示筛选条件；第一版优先保持现有 UI 参数不变，只补最小预览/类型支持。
- CLI/MCP：新增或扩展只读查询命令/tool；输出保持 stable JSON，错误使用 DataSpecError/JSON-RPC diagnostic。
- OpenSpec：新增 `standard-query-dsl` spec，并对字段搜索、AI Context、Schema Registry、CLI/MCP 和 fixture 契约做 delta。
- 风险：涉及 API/CLI/MCP/AI 外部协议，按 SDD standard 偏 full 执行；提交前必须独立 agent 评审。
