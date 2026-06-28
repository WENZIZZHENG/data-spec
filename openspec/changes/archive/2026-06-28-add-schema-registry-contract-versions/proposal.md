## Why

DataSpec 已经把字段、规则、快照、AI Context、SQL lint 和 CLI/MCP 输出给 AI 使用，但这些结构的版本、兼容窗口和废弃字段说明仍散落在文档、OpenAPI 与测试里。P6-37 需要建立轻量 Schema Registry，让 AI/CLI/MCP 能先读取“当前可依赖哪些契约、版本是多少、破坏性变更如何识别”。

## What Changes

- 新增标准契约 registry，统一列出 Field、Enum、Rule、Template、StandardSnapshot、LintResult、AI Context manifest/field catalog、AI task profile 等 AI 消费结构。
- 新增只读后端 API，支持列出全部契约、查看单个契约的 JSON Schema、稳定字段、废弃字段和兼容策略。
- AI Context 包增加 schema registry 文件和 manifest 契约摘要，使离线 `.dataspec/context/` 也能携带契约版本。
- CLI/MCP 新增读取 schema registry 的命令/resource，供 coding agent 在生成或修复前确认契约版本。
- 更新 AI contract fixtures、README 和 AI 契约文档，明确新增字段、删除字段、改名和语义变化的兼容规则。
- 不引入外部 schema registry 服务，不要求历史导出包全部补齐版本，不做数据库迁移。

## Capabilities

### New Capabilities

- `standard-schema-registry`: 轻量标准契约注册表，覆盖契约 metadata、JSON Schema、版本、废弃字段和兼容策略。

### Modified Capabilities

- `ai-context-package`: AI Context 包携带 registry 摘要和 `.dataspec/schema-registry.json`。
- `dataspec-cli`: CLI 暴露契约 registry 的 list/show/check 入口。
- `dataspec-mcp`: MCP 暴露契约 registry resource，供 AI 客户端读取。
- `ai-contract-fixtures`: 现有契约测试覆盖 registry、schemaVersion 和兼容策略字段。

## Impact

- 后端：新增 contract registry model/service/controller；扩展 AI Context 导出 manifest 与 zip 文件。
- CLI/MCP：新增 `contract/contracts` 命令与 MCP resource，补 Node 测试。
- 前端契约：更新 OpenAPI TS schema/type re-export，保持类型可发现。
- 文档/规范：更新 README、docs/ai-contracts.md、TODO 和 OpenSpec specs。
