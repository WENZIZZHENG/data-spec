## Why

字段标准不只来自数据库。OpenAPI、JSON Schema、Protobuf 和事件契约里已经沉淀了接口层稳定命名、类型、必填性、枚举和说明；如果 DataSpec 只看数据库反向导入，AI 建表、修 SQL 或生成 DTO 时会漏掉这些契约来源。

P6-93 需要提供一个只读、可验证的多源契约候选预览入口，把契约字段抽成标准候选草案，并和已有标准字段、数据库反向导入候选保持同一套证据与采纳边界。

## What Changes

- 新增契约候选导入预览能力，支持 OpenAPI、JSON Schema 和 Protobuf `.proto` / descriptor 风格输入的第一版字段抽取。
- 新增后端只读 API，按项目、sourceKind 和契约文本生成 `candidateFields`、`sourceKind`、`sourcePath`、`schemaVersion`、`confidence`、`conflictReasons`、`recommendedAction`、`inboxPayload`、`diagnostics`、`safety` 和 `nextActions`。
- 新增 CLI `contract-import preview --project <id> --source-kind <openapi|json-schema|protobuf> --input <path> --format json|text`，输出同一稳定 JSON contract 或人读摘要。
- 输出候选与现有字段、同包候选做确定性去重；同名或疑似冲突时只给出 `MERGE_EXISTING` / `REVIEW_REQUIRED` 建议，不直接写入正式标准或候选库。
- 契约内容、诊断、fixtures、README 和 OpenSpec 示例默认脱敏 token、password、Authorization、API key、完整 JDBC URL、DSN 和连接串。
- 第一版不新增数据库表或迁移，不调用外部 LLM，不自动提交候选到 Inbox；如需采纳，使用 `inboxPayload` 走现有候选创建/决策流程。

## Capabilities

### New Capabilities

- `contract-candidate-import`: 多源契约候选预览包、支持的 sourceKind、稳定输出字段、去重/冲突、安全边界和验证入口。

### Modified Capabilities

- `dataspec-cli`: 新增 `contract-import preview` 命令、稳定 JSON/text 输出、退出码、错误脱敏和 help 文本。
- `standard-candidate-inbox`: 明确契约导入预览可生成兼容候选入箱 payload，但预览本身不自动持久化候选。
- `ai-contract-fixtures`: 新增后端/CLI 契约候选预览输出的稳定字段和脱敏 fixture 覆盖。
- `cli-mcp-contract-fixtures`: 新增 `contract-import preview` CLI fixture，覆盖输入边界、输出 shape、安全 metadata 和 recommended next actions。

## Impact

- 后端：新增只读 `contractimport` controller/service/model 与 parser 辅助逻辑；读取项目标准字段做去重，不新增写库路径。
- CLI/tools：新增命令分发、参数解析、JSON/text 输出、fixture 和 Node 测试。
- 测试：新增后端 service/controller/contract fixture 测试，新增 CLI 与 CLI/MCP fixture 测试。
- 文档/OpenSpec：更新 README/TODO，新增 OpenSpec delta 和 `Verification Evidence`。
- 安全：契约输入按长度和 sourceKind 做边界校验；所有输出与错误路径经过脱敏，不记录 raw 契约正文中的敏感值。
