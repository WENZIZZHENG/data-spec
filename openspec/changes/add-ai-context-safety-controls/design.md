## Context

AI Context 目前已经支持完整/按需导出、预算预检、schema registry、capability catalog、usage examples 和通用 `SensitiveDataSanitizer`。这些能力能帮助 AI 获取更小的上下文，也能脱敏常见技术 secret，但仍有三个缺口：

- 字段注释、业务术语、样例和用户输入会被嵌入 prompt 或 Context，AI 无法稳定区分“DataSpec 指令”和“业务原文”。
- 字段只有 `sensitive` 布尔值，导出结果缺少可解释的 visibility、maskingProfile、allowedTasks 和 redactionReasons。
- CLI/MCP 已读取 `.dataspec/config.json`，但没有本地安全红线配置，无法统一表达“敏感字段默认遮蔽”“只允许本地 AI 工具”“这些文本模式永不导出”等个人策略。

本变更属于 SDD full：它改变 AI 外部协议的安全语义，并影响后端 AI Context、CLI/MCP 配置和契约测试。实现必须保持 additive，不破坏已有 Context 消费端。

## Goals / Non-Goals

**Goals:**

- 在 AI Context 包、字段目录和指导文本中显式标记可信边界，让 AI 知道业务内容不是可执行指令。
- 为字段输出确定性 `contextSafety` 和 `exportDecision`，解释字段为什么可导出、被遮蔽或需人工确认。
- 复用 `SensitiveDataSanitizer` 处理 secret-like 文本；被脱敏时保留原因摘要，不保留原始 secret。
- 支持 `.dataspec/config.json` 的可选 `securityProfile`，供 CLI/MCP 和后续导出链路读取。
- 保持旧字段兼容；新增字段不改变现有 API 路径、zip 文件名或基础文件布局。

**Non-Goals:**

- 不新增数据库表，不做企业权限审批或组织策略中心。
- 不扫描真实业务数据行，不做完整 PII/DLP。
- 不回溯删除历史 AI Context 包。
- 不引入外部 LLM 安全服务或新运行时依赖。

## Decisions

1. **采用 additive safety metadata，而不是重命名现有字段。**
   - 选择：在 `field-catalog.json` 新增 `contextSafety` 与 `exportDecision`，在 `manifest.json` 新增 `contextSafetySummary`。
   - 原因：现有 CLI/MCP/前端/业务仓库已经依赖 `fields[]`、`sensitive`、`contextScope` 等字段；additive 能降低兼容风险。
   - 替代方案：把 `sensitive` 改成多值枚举。放弃，因为它会破坏已有消费端。

2. **第一版使用确定性规则推导 visibility 和 maskingProfile。**
   - 选择：`sensitive=true` 的字段默认 `visibility=restricted`、`maskingProfile=metadata-only`，非敏感字段默认 `visibility=internal`、`maskingProfile=plain-metadata`；命中 secret-like 文本时加 `redactionReasons` 和 warning。
   - 原因：不需要迁移 schema，也能覆盖本轮“最小暴露”和“可解释导出”验收。
   - 替代方案：新增字段表列。放弃，因为这会触发存储迁移，且第一版没有足够 UI/治理需求。

3. **业务文本统一视为 untrusted-content。**
   - 选择：字段 comment、displayName、aliases、defaultValue、example、format notes、usage contract、业务需求、SQL、usage examples、glossary 等进入 AI Context 时，都在 safety metadata 或指导文本中标记为 untrusted user/business content。
   - 原因：提示注入防护的关键是边界，而不是尝试准确判断所有恶意文本。
   - 替代方案：只扫描“忽略上文”等注入短语。放弃作为唯一方案，因为覆盖不稳定；可作为 warning 规则补充。

4. **复用现有 sanitizer，补少量 prompt-injection 词法检测。**
   - 选择：继续使用 `SensitiveDataSanitizer.redactText/containsSensitiveText` 处理 secret-like 值；在 AI Context 服务内增加轻量可疑指令检测，如“ignore previous instructions”“泄漏 token”“输出密码”等。
   - 原因：保持无新依赖，且能把本轮安全边界纳入现有测试。
   - 替代方案：引入 Presidio/gitleaks 运行时。放弃，因为个人本地工具不应增加重型依赖。

5. **`securityProfile` 先在本地配置解析中落地。**
   - 选择：扩展 `dataspec-config.mjs`，规范 `securityProfile.redactionStrictness`、`sensitiveFieldPolicy`、`allowedAiTools`、`neverExportPatterns`、`localOnlyPaths`、`samplePolicy` 和 `credentialPolicy`。
   - 原因：CLI/MCP 当前已经统一读取 `.dataspec/config.json`；先把策略读成结构化对象，后续命令可逐步消费。
   - 替代方案：新增服务端 API 和前端配置页。暂不做，因为需要持久化和 UI 设计，超出第一版最小安全闭环。

## Risks / Trade-offs

- **误脱敏字段样例导致上下文可用性下降** → 保留字段名、类型、状态和脱敏原因，敏感字段默认 metadata-only，不直接删除整个字段。
- **注入检测漏报** → 不把检测当作唯一防线；所有业务文本默认标记为 untrusted，指导 AI 不得把业务原文当系统指令。
- **`securityProfile` 已解析但服务端暂不消费** → tasks 和 spec 明确第一版 CLI/MCP 读取与回显边界；服务端使用内置默认安全策略，后续再接入 profile-driven export。
- **新增 schema 字段可能遗漏描述** → `field-catalog.schema.json` 必须补 description 和测试，OpenSpec strict 与后端测试共同拦截。

## Migration Plan

- 代码以 additive 字段发布，不需要数据迁移。
- 旧客户端可继续读取现有 `fields[]`、`sensitive`、`contextScope` 和 zip 文件。
- 回滚时移除新增字段和配置解析即可恢复旧导出；不会改变持久化数据。

## Open Questions

- 暂无阻塞问题。前端安全红线配置页作为后续增强，本轮先通过配置 schema 与 CLI/MCP 读取建立可执行边界。
