## Context

DataSpec CLI/MCP 通过 `tools/dataspec-config.mjs` 向上查找并解析 `.dataspec/config.json`，`dataspec init` 负责生成初始文件，`doctor` 负责诊断配置和运行环境。当前 loader 已校验主要字段类型并对未知字段保持宽松，但编辑器和 AI 没有 machine-readable 字段说明，配置也没有显式版本。

本变更只影响本地 CLI 配置契约，不修改后端、数据库或 MCP tool schema。`tools/` 是无独立 package manifest 的可直接运行脚本集合，因此不能给 CLI 增加 Ajv 等运行时依赖。

## Goals / Non-Goals

**Goals:**

- 交付可由通用 JSON 编辑器和 AI 消费的 Draft 2020-12 schema。
- 新初始化配置自带本地 schema 关联和版本，离线业务仓库也能获得提示。
- doctor 能解释当前声明版本、支持版本、schema 路径和关联状态。
- 保留旧配置、未知运行时扩展和显式 CLI 参数优先级。

**Non-Goals:**

- 不开发 VS Code 插件或远程 schema registry。
- 不增加独立 fingerprint、遥测或配置上传。
- 不把 `apiToken` 写入生成配置，也不校验真实凭据。
- 不为 schema 校验引入第三方 Node 依赖或改变后端协议。

## Decisions

1. **Canonical schema 放在 `tools/schemas/dataspec-config.schema.json`，init 复制到 `.dataspec/config.schema.json`。**
   - 业务仓库使用相对 `$schema: "./config.schema.json"`，无需网络或插件。
   - 备选是引用 DataSpec 仓库绝对路径或远程 URL；前者不可移植，后者破坏离线使用，因此不采用。

2. **配置契约版本使用整数 `configVersion: 1`，旧配置缺省时按 legacy v1 消费。**
   - loader additive 暴露 `schemaRef/configVersion`；旧文件不报错，doctor 给关联 warning。
   - 高于当前支持版本时 doctor fail，且 init 在写任何文件前拒绝覆盖或降级；其他 CLI 仍保持现有字段读取语义。

3. **编辑器 schema 严格，运行时 loader 宽松。**
   - schema 对官方字段使用 `additionalProperties: false` 并允许 `x-` 前缀扩展，帮助编辑器发现拼写错误。
   - loader 继续只读取已知字段并忽略其他属性，避免现有仓库因新增元数据中断。字段类型错误仍由现有本地 loader 在 HTTP 调用前拒绝。

4. **doctor 输出摘要而不复制一套 JSON Schema validator。**
   - config check details 和顶层 `configSchema` 共享同一摘要，包含 supportedVersion、declaredVersion、effectiveVersion、schemaRef、schemaPath、schemaFilePresent 和 associationStatus。
   - 不比较 schema hash，不解析编辑器诊断；这避免重复现有版本握手与 OpenAPI drift 协议。

5. **init 对三个管理文件保持逐文件 skip/force 语义。**
   - 新仓库生成 schema、config 和 README；已有文件默认逐个跳过，`--force` 才覆盖。
   - config 继续不包含 `apiToken`，schema 将该字段标为敏感并建议使用 `DATASPEC_TOKEN`；server URL 含 username/password 时在写入前拒绝。

6. **projectId 保留有限历史兼容，不接受宽松 JavaScript 强制转换。**
   - 新配置写正整数；历史纯数字字符串继续规范为整数，schema 用 oneOf 明确表达。
   - boolean、object、空白和混合字符串直接报错，避免 `true` 被转换为项目 1。

## Risks / Trade-offs

- **编辑器 schema 比运行时更严格** → 文档明确未知官方字段需升级 schema，私有扩展使用 `x-`；CLI 仍保持兼容。
- **legacy config 只有 warning** → 保证现有自动化不中断，用户可运行 `init --force` 或手工添加 `$schema/configVersion`。
- **schema 文件被本地修改** → doctor 只报告存在性，不引入 hash 协议；需要恢复时重新运行 `init --force`。
- **未来版本声明高于当前版本** → doctor fail 并提示升级 CLI；不自动重写或降级用户配置。
- **用户控制的 schemaRef 可能包含敏感路径** → doctor 仅回显 canonical ref；其他值统一显示为 `<unexpected>`。

## Migration Plan

1. 发布 canonical schema、loader 元数据和 doctor 摘要。
2. 新 `dataspec init` 自动生成三文件；现有业务仓库无需立即迁移。
3. 现有仓库可手工复制 schema 并添加 `$schema/configVersion`，或确认可覆盖后运行 `init --force`。
4. 回滚时删除新增 schema 文件和两个可选字段即可，既有配置字段仍保持原语义。

## Open Questions

无。远程 schema 发布和 IDE 插件只有出现真实分发需求时另行评估。
