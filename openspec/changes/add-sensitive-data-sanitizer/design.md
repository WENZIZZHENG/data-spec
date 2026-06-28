## Context

现状已经有多套局部脱敏：

- `AiEvidencePackageServiceImpl` 内置 `[REDACTED]` 正则。
- `DatabaseReverseImportServiceImpl` 和 `JdbcDatabaseMetadataAdapter` 分别把连接错误里的 password、Bearer 和 JDBC URL 替换为 `<redacted>`。
- `ProjectBackupServiceImpl` 有敏感字段名集合和快照兜底。
- `tools/dataspec-cli.mjs` 同时存在 `redactSecrets` 和 `sanitizeSecretText`。

这些实现方向一致，但字段集合、替换占位、长度限制和递归策略不完全相同。第一版不追求识别所有自然语言敏感信息，而是先稳定 DataSpec 已明确处理的技术 secret。

## Decisions

1. **后端使用公共 sanitizer，替换局部正则。**
   - 原因：Java 后端是 evidence、备份、反向导入等核心出口的共同层。
   - 实现：`SensitiveDataSanitizer` 提供 `redactText`、`redactText(value, limit)`、`sanitizeValue`、`containsSensitiveKeyOrValue` 和 `isSensitiveKey`。

2. **占位符统一为 `[REDACTED]`，保留旧测试兼容只在必要时调整。**
   - 原因：evidence 已使用 `[REDACTED]`，AI 更容易识别为脱敏值。
   - 影响：直连错误从 `<redacted>` 调整为 `[REDACTED]` 时同步更新测试。

3. **递归清洗保守截断，不保留敏感 key 的原始 value。**
   - 原因：AI evidence 和备份摘要会携带任意 payload；敏感 key 下即使 value 看似无害也不应输出。
   - 边界：不修改持久化原始业务记录，只在导出、错误、诊断和摘要出口清洗。

4. **CLI 侧暂不抽独立 npm 模块。**
   - 原因：项目当前是单文件 CLI；抽包会增加构建/发布复杂度。
   - 实现：收敛 CLI 内重复正则为同一 helper，并用 Node test 锁定行为。

## Risks

- 误脱敏普通业务字段，例如字段名里包含 token。缓解：只在导出/错误/诊断文本中替换，核心字段标准对象不被修改。
- 文本正则不能识别所有 secret。缓解：明确第一版覆盖技术 secret；自然语言 PII 和业务高敏片段留给后续个人安全红线策略。
- 证据包摘要过度清洗影响排障。缓解：保留 key、类型、计数、非敏感上下文和 `[REDACTED]` 占位。

## Rollback

回滚公共 sanitizer 接入即可恢复旧局部逻辑；不涉及数据库迁移或外部协议变更。
