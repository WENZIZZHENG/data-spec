## Context

DataSpec 目前已经有 `docs/ai-contracts.md`、AI contract fixture 测试、OpenAPI TS schema、AI Context 的 `schemaVersion` 和标准快照的 `specVersion/specHash`。问题是这些契约信息没有统一入口：AI/CLI/MCP 想确认“Field、LintResult、AI Context manifest 当前是什么版本，哪些字段稳定，哪些字段废弃”时，需要在多个文件和导出物中拼装信息。

P6-37 的第一版要把“契约说明”产品化为只读 registry。它应该是轻量、确定性、可测试的，不引入数据库表或外部服务。

## Goals / Non-Goals

**Goals:**

- 提供服务端 `/api/contracts` 和 `/api/contracts/{contractId}`，返回机器可读 registry catalog 和 contract detail。
- registry 覆盖第一批 AI 高频消费结构：Field、Enum、Rule、Template、StandardSnapshot、LintResult、AI Context manifest/field catalog、AI task profile。
- 每个 contract 声明 `contractId`、`schemaVersion`、`jsonSchema`、`stableFields`、`deprecatedFields`、`compatibility` 和 `docsRef`。
- AI Context zip 增加 `.dataspec/schema-registry.json`，manifest 增加 `contracts` 摘要。
- CLI/MCP 暴露 registry 读取入口，AI agent 可以在建表、修 SQL、反向导入前确认契约。
- 测试接入现有 `mvn test` 和 `node --test`，检测 registry 稳定字段缺失和 CLI/MCP 输出漂移。

**Non-Goals:**

- 不引入 Confluent/Apicurio 等外部 schema registry。
- 不新增数据库迁移，不做用户自定义 schema 管理。
- 不要求历史 AI Context zip 或历史 SQL 检查记录回填 registry。
- 不做完整 JSON Schema 校验引擎；第一版只提供 schema 文档和轻量 invariants 检查。
- 不把 contract version 等同于标准快照版本；前者描述输出结构，后者描述业务标准内容。

## Decisions

1. **服务端使用内置静态 registry。**
   - 做法：新增 `com.dataspec.contract` 包，提供 DTO、`SchemaContractRegistryService` 和 Controller。
   - 理由：第一版契约来自代码和文档，不需要运行时编辑；静态 registry 更容易通过单测锁定。
   - 备选：落库管理契约版本。暂不采用，因为 P6-37 明确不需要重型 registry 服务。

2. **contract schemaVersion 使用字符串，registry schemaVersion 使用整数。**
   - 做法：catalog 顶层 `schemaVersion=1`、`registryVersion="2026.06.28"`；单个 contract 使用 `"1.0"`。
   - 理由：registry 文档结构自身适合整数版本；单个契约需要表达兼容演进窗口，字符串更清楚。

3. **JSON Schema 以最小 AI 稳定字段为核心。**
   - 做法：每个 contract 的 `jsonSchema` 至少声明 `type/object/properties/required` 和稳定字段类型；不试图覆盖所有 DTO 私有字段。
   - 理由：和现有 AI contract fixtures 一致，锁定 AI 依赖面而不是冻结所有实现细节。

4. **AI Context 携带 registry 文件而不是复制全部 schema 到每个文件。**
   - 做法：zip 增加 `.dataspec/schema-registry.json`，manifest 增加 `contracts.registryVersion/schemaVersion/file/contractIds`。
   - 理由：避免 manifest 过大，离线缓存仍能读取完整契约。

5. **CLI/MCP 读取后端 registry，`contract check` 做轻量 invariants。**
   - 做法：CLI 新增 `contract list/show/check`；MCP 新增 `schema-registry` resource。
   - 理由：满足 AI/CLI/MCP 使用入口；不把检查扩展成完整 schema diff 工具，后续可在 P6-83/P6-97 等质量门禁继续增强。

## Risks / Trade-offs

- [Risk] 静态 registry 和真实 DTO 漂移。→ Mitigation：后端单测锁定必需 contract IDs、stableFields、schemaVersion；AI Context 测试锁定 manifest 和 zip 文件。
- [Risk] JSON Schema 太粗，无法证明所有字段类型。→ Mitigation：第一版只承诺 AI 稳定字段；现有 OpenAPI drift 与 contract fixtures 继续覆盖接口字段。
- [Risk] CLI/MCP 新入口增加维护面。→ Mitigation：复用现有 fetch/unwrap/format 模式，新增 Node tests 覆盖 list/show/check/resource。
- [Risk] AI 把 registry 误认为权限或写入策略。→ Mitigation：文档明确 registry 是输出结构契约，不是鉴权、审批或数据治理发布系统。

## Migration Plan

1. 新增 registry API 和单测。
2. AI Context zip 增加新文件和 manifest 摘要，保持现有文件名不变。
3. CLI/MCP 增加读取入口并补 Node tests。
4. 更新 OpenAPI TS schema、AI 契约文档、README、TODO。
5. 运行后端、前端、CLI/MCP、OpenSpec 和 diff 验证后提交并归档。
