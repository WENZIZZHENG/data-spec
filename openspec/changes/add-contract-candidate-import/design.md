## Context

DataSpec 已有数据库反向导入、标准候选 Inbox、字段来源追踪、字段推荐、AI Context、Schema Registry、CLI/MCP contract fixture 和本地验证入口。P6-93 的缺口是把 API/Schema/消息契约里的字段也变成可审核的标准候选草案。

本次属于 SDD standard：会新增只读后端 API 和 CLI JSON contract，影响 AI/CI 可观察输出；但第一版不改数据库 schema、不写候选库、不新增权限模型、不替代数据库反向导入。

## Goals / Non-Goals

**Goals:**
- 新增只读 API：`POST /api/contract-import/preview`。
- 新增 CLI：`contract-import preview --project <id> --source-kind <openapi|json-schema|protobuf> --input <path> --format json`。
- 从 OpenAPI、JSON Schema、Protobuf `.proto` 文本或 descriptor 风格 JSON 中抽取字段候选。
- 输出稳定预览包，包含 `kind`、`schemaVersion`、`projectId`、`sourceKind`、`sourcePath`、`contractHash`、`summary`、`candidateFields`、`diagnostics`、`safety` 和 `nextActions`。
- 每个候选包含字段名、显示名、数据类型、必填性、枚举/示例摘要、来源路径、confidence、conflictReasons、recommendedAction 和兼容现有候选创建流程的 `inboxPayload`。
- 对现有标准字段和同一契约内重复字段做确定性去重与冲突提示。

**Non-Goals:**
- 不自动写入标准候选 Inbox 或正式标准字段。
- 不解析完整语言类型系统、泛型、代码 AST 或运行时注解。
- 不调用外部 LLM，不访问外部 URL，不上传契约内容。
- 不新增数据库表、迁移、后台任务或完整前端页面。

## Decisions

1. **新增独立 `contractimport` 只读模块，而不是复用数据库 reverse-import service。**
   - 选择：新增 controller/service/model 和内部 parser helper；只复用 `FieldService` 读取现有字段做去重。
   - 原因：契约导入的 sourceKind、sourcePath、schemaVersion 和 property path 与数据库 schema/table/column 语义不同，放进 reverse-import 会混淆证据边界。
   - 备选：扩展现有 `/api/reverse-import`。放弃原因是容易暗示会写数据库来源批次或触发确认导入流程。

2. **第一版只做确定性本地解析。**
   - 选择：OpenAPI/JSON Schema 使用 Jackson 解析 JSON/YAML tree；Protobuf 优先解析 `.proto` 文本字段行和 descriptor 风格 JSON 的 message/field 摘要。
   - 原因：能覆盖常见契约来源并保持测试稳定；复杂 oneOf/allOf/嵌套引用先输出 diagnostics 和 `REVIEW_REQUIRED`。
   - 备选：引入完整 OpenAPI/Protobuf 解析依赖。放弃原因是会增加依赖、边界和兼容成本，超出第一版最小闭环。

3. **预览输出 `inboxPayload`，不自动创建候选。**
   - 选择：每个 candidate 生成可提交给现有候选创建流程的 payload 摘要，包括 sourceType、candidateName、displayName、dataType、comment、confidence 和 evidence。
   - 原因：满足“复用候选采纳台”的衔接，同时避免自动刷入大量低置信候选。
   - 备选：新增一键导入候选 API。放弃原因是会引入幂等、批量写入、审核、撤销和权限风险。

4. **契约 hash 基于脱敏后的输入和解析参数。**
   - 选择：对 `schemaVersion`、`projectId`、`sourceKind`、`sourcePath`、脱敏 contract text、解析参数和候选摘要计算 `contractHash`。
   - 原因：同一契约重复预览稳定；契约内容变化会驱动 fixture 与 AI replay 更新。

## Risks / Trade-offs

- **[Risk] 复杂 schema 引用解析不完整。** → Mitigation：输出 `UNSUPPORTED_SCHEMA_COMPOSITION` / `REFERENCE_REVIEW_REQUIRED` diagnostics，并把候选标记为 `REVIEW_REQUIRED`。
- **[Risk] 契约正文可能包含示例 token、Authorization 或连接串。** → Mitigation：输入只用于内存解析，输出、diagnostics、hash payload 和 CLI 错误统一脱敏；测试覆盖 raw secret 不泄漏。
- **[Risk] 候选数量过多。** → Mitigation：提供 `maxCandidates` 上限，默认只返回前 100 个稳定排序候选，并输出截断 diagnostic。
- **[Risk] 同名字段跨接口语义不同。** → Mitigation：candidate 保留 sourcePath 和 conflictReasons，推荐 `MERGE_EXISTING` 或 `REVIEW_REQUIRED`，不自动采纳。

## Migration Plan

- 新增 API/CLI 均为兼容性新增；现有数据库反向导入、候选 Inbox、字段推荐和 AI Context 默认行为不变。
- 第一版完成后保留 OpenSpec active，不自动 archive。
- 回滚方式为移除新 `contractimport` 模块、CLI 命令、fixtures 和 spec delta；不涉及数据迁移或持久化回滚。
