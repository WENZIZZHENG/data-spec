## Why

DataSpec 已有字段格式约束、合成标准示例和 CLI/MCP 契约 fixture，但 AI 在业务仓库写单测、mock、seed 或消费 DataSpec 契约时，仍缺少一套可复用的安全测试数据包和统一消费端兼容验收入口。P6-185 与 P6-176 应合并推进：先让标准能生成确定性的 valid/invalid/boundary 用例，再用兼容套件固化 API、CLI、MCP、AI Context 和 schema registry 的消费形状。

## What Changes

- 新增标准驱动测试数据包能力：按项目字段、枚举、格式约束、敏感标记和轻量对象场景生成 deterministic JSON/CSV/SQL mock/seed 草稿，以及 valid、invalid、boundary cases。
- 新增只读后端 API、CLI 和 MCP 入口，输出 `testDataCase`、`seedProfile`、`mockPayload`、`coverageReport`、`safety` 和 `nextActions`，并明确不读取业务数据行、不写入业务数据库、不调用外部 LLM。
- 新增消费端兼容验收套件：对关键 DataSpec 消费端契约保留 golden payload、minimum supported version、breaking rules 和 adapter results，提供本地 `consumer-compat check` 命令，避免改变既有 `compat check` 版本握手语义。
- 扩展 CLI/MCP contract fixture、Schema Registry 和 OpenSpec 规格，确保新增入口、示例输出、安全 metadata 和兼容检查结果可被 AI 稳定读取。
- 第一版不做第三方认证体系、不自动修改业务仓库、不自动写入数据库、不保证覆盖所有业务规则；聚焦 DataSpec 自有消费端和字段级 / 轻量对象级样例。

## Capabilities

### New Capabilities

- `standard-test-data-package`: 标准驱动测试数据包的 API/CLI/MCP 输出、安全边界、覆盖报告和样例用例契约。
- `consumer-compatibility-suite`: DataSpec 自有消费端兼容验收套件、golden payload、breaking rules、adapter results 和本地检查命令契约。

### Modified Capabilities

- `synthetic-standard-examples`: 现有合成标准示例包需要复用确定性样例生成、安全元数据和 spec hash 语义，并与测试数据包区分 SQL/Prompt fixture 与 mock/seed 用例。
- `cli-mcp-contract-fixtures`: CLI/MCP fixture 需要覆盖测试数据生成和兼容检查入口，校验 output shape、安全 metadata、示例脱敏和 descriptor drift。
- `standard-schema-registry`: Schema Registry 需要登记测试数据包、兼容检查请求/响应、adapter result 和 breaking rule schema。
- `dataspec-cli`: CLI 需要新增只读 `test-data generate` 与 `compat check` 入口，并保持 DataSpecError 诊断与 JSON 输出兼容。
- `dataspec-mcp`: MCP 需要新增或扩展只读工具 / resource，暴露测试数据包和兼容检查结果，声明输入边界与安全 metadata。

## Impact

- 后端：新增测试数据包与兼容套件相关 model/service/controller，复用字段、枚举、格式约束、Schema Registry 和敏感信息脱敏工具；不新增数据库迁移。
- CLI/MCP/tools：新增命令、工具描述、fixture、contract checker 和测试；`consumer-compat check` 需接入现有本地验证入口。
- 前端：第一版只需要最小 API 类型 / smoke wiring 或不新增页面；如需要展示，使用现有 contract/schema 入口，不做复杂 UI。
- 文档与 OpenSpec：新增 change artifacts、spec delta 和 Verification Evidence；完成后同步 TODO/候选池/归档。
- 风险：涉及 OpenAPI、CLI/MCP/AI 外部协议和安全样例输出，按 SDD full 执行；commit 前必须进行独立子 agent 评审。
