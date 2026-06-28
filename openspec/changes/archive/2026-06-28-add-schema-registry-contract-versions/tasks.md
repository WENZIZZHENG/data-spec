## 1. OpenSpec

- [x] 1.1 创建 P6-37 proposal、design、delta specs 和 tasks。
- [x] 1.2 通过 OpenSpec change 校验。

## 2. 后端 Schema Registry

- [x] 2.1 新增 contract registry DTO/service/controller，提供 `/api/contracts` 和 `/api/contracts/{contractId}`。
- [x] 2.2 内置核心 contracts：Field、Enum、Rule、Template、StandardSnapshot、LintResult、AI Context manifest、AI Context field catalog、AI task profile。
- [x] 2.3 每个 contract 输出 schemaVersion、JSON Schema、stableFields、deprecatedFields、compatibility、docsRef 和 examples。
- [x] 2.4 补后端单测，覆盖 catalog、detail、未知 contract 和核心字段 invariants。

## 3. AI Context 与契约版本透传

- [x] 3.1 AI Context zip 增加 `.dataspec/schema-registry.json`。
- [x] 3.2 manifest 增加 `contracts` 摘要，包含 registry schemaVersion、registryVersion、文件路径和 contractIds。
- [x] 3.3 `.dataspec/README.md` 和 `AGENTS.md.fragment` 增加读取 schema registry 的说明。
- [x] 3.4 扩展 AI Context 测试，锁定 registry 文件、manifest contract 摘要和既有字段兼容。

## 4. CLI、MCP 与前端契约

- [x] 4.1 CLI 新增 `contract/contracts list|show|check` 命令，支持 text/json 输出和 registry invariants 检查。
- [x] 4.2 MCP 新增 `schema-registry` resource，并在 prompts 中提示需要稳定字段名时先读取 registry。
- [x] 4.3 手工更新或重新生成 OpenAPI TS schema/types，导出 Schema Registry 相关类型。
- [x] 4.4 补 Node CLI/MCP 测试，覆盖 list/show/check、MCP resource 和契约漂移断言。

## 5. 文档、验证与收尾

- [x] 5.1 更新 README、TODO 和 docs/ai-contracts.md，说明 registry、兼容策略和非权限边界。
- [x] 5.2 执行后端、前端、CLI/MCP、OpenSpec 和 diff 验证。
- [x] 5.3 完成结构化代码评审并修复 findings。
- [x] 5.4 创建本地 commit。
- [x] 5.5 归档 OpenSpec change 并再次验证。
