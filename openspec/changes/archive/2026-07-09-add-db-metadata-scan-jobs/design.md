## Context

DataSpec 已有 `/api/reverse-import/database/scan`、`DatabaseMetadataScanReq/Result`、前端反向导入扫描面板、metadata cache、database schema dump、覆盖率报告和数据库只读安全诊断。现有扫描更像一次同步分页请求：`scanId` 只是本轮请求标识，`cursor` 只是偏移，取消只体现在当前响应里；当数据库表很多、连接慢或部分表无权限时，用户和 AI 缺少可恢复作业状态、限速提示、失败摘要和可审计 evidence。

P6-180 命中 SDD full：它扩展后端 API 契约、前端展示、AI 可读输出和安全边界，但第一版仍服务个人/小团队本地使用，不引入后台调度平台。

## Goals / Non-Goals

**Goals:**
- 在现有 scan API 上增加采集作业语义：`scanJobId`、`status`、`resumeCursor`、`cancelToken`、`pageSize`、`rateLimit`、`retryPolicy`、`sourcePressureHint`、`partialResult`、`failureSummary` 和 evidence。
- 支持分页扫描、取消、恢复和失败摘要，确保取消或失败不写源数据库、不写 DataSpec 标准字段库。
- 前端反向导入页展示采集作业进度、限速提示、失败/取消状态、继续/取消动作和当前批次选择。
- 覆盖率报告可复用采集作业部分结果生成只读报告，避免必须一次性读取完整库。
- 所有响应、resume/evidence 和前端状态继续脱敏，不包含 password、token、Authorization、完整 JDBC URL、DSN、连接串或业务数据行。

**Non-Goals:**
- 不做后台定时同步、分布式队列、跨进程持久调度或长期运行 worker。
- 不扫描业务数据行，不绕过源库权限，不把连接密码写入数据库或浏览器持久存储。
- 不自动导入标准字段，不自动采纳候选，不执行数据库迁移。
- 不替代现有 metadata cache；作业状态只引用 schema-only 结构和 cache evidence。

## Decisions

1. 在现有 scan API 上兼容扩展，而不是新增完全独立的扫描入口。

   方案：`DatabaseMetadataScanReq` 增加作业字段，`DatabaseMetadataScanResult` 增加作业状态字段；旧客户端只传 `pageSize/cursor/cancel` 时仍得到兼容响应。

   原因：前端、OpenAPI、能力目录和已有测试已经围绕 `/reverse-import/database/scan` 建立；兼容扩展能减少迁移成本。

   备选：新增 `/scan-jobs/start/status/cancel` 多 endpoint。放弃原因是第一版不做后台 worker，拆成多 endpoint 容易暗示长生命周期调度承诺。

2. 第一版使用请求驱动的轻量作业状态，不引入数据库表。

   方案：`scanJobId`、`resumeCursor` 和 `cancelToken` 是无凭据、可重建的短期标识；服务端每次请求读取当前页并返回部分结果、失败摘要和下一步动作。必要时可在内存中记录最近取消状态，但不依赖持久化完成正确性。

   原因：个人/小团队使用优先，避免新增迁移和后台状态清理；断点续扫的关键是 cursor 与安全恢复命令，而不是长生命周期任务队列。

   备选：新增 `ds_database_metadata_scan_job` 持久化表。放弃原因是会扩大存储语义和清理成本；后续若需要跨重启恢复，可另开 SDD full。

3. 限速采用“建议与服务端上限”组合。

   方案：请求可带 `rateLimit.maxTablesPerPage`、`rateLimit.minDelayMs` 等只读策略；服务端实际强制 `pageSize` 上限，并在 `sourcePressureHint` 提示降低 page size、等待、拆分 schema 或使用缓存。第一版不在请求线程里 sleep 等待。

   原因：睡眠会占用本地服务线程且不适合前端交互；用户和 AI 需要的是避免一次性拉太多、知道如何降压。

4. 失败摘要是 schema-only、分表聚合，不中断整个作业。

   方案：单表权限或 metadata 读取失败时记录 `failureSummary.failedTables[]` 的 bounded 样例、错误类别、retryable 和建议动作；可继续返回成功表的 `partialResult`。

   原因：真实数据库常见部分 schema 无权限；完全失败会迫使用户重扫，部分结果更符合 P6-180 的可恢复目标。

5. Evidence 输出复用当前响应，不读额外数据。

   方案：scan result 提供只读 evidence 摘要，包含 scanJobId、状态、页大小、处理/剩余估算、成功/失败表数、cache fingerprint、安全边界和 nextActions；不生成服务端归档文件。

   原因：AI 需要可复制证据，但第一版不需要新增 evidence package 存储链路。

## Risks / Trade-offs

- [用户误以为作业跨重启持久] → 响应字段和文档明确第一版为请求驱动轻量作业，恢复依赖 cursor 和非敏感参数。
- [限速提示不是真后台限流] → 服务端强制 pageSize 上限，UI 和 nextActions 明确建议；不承诺后台节流。
- [部分失败被忽略] → failureSummary 和前端提示必须显眼，coverage/preview 只能基于 successful tables。
- [敏感信息泄漏] → 复用现有 sanitize 规则，并补后端/前端测试覆盖 resume/evidence 不含凭据。
- [OpenAPI/前端类型漂移] → 更新类型和 frontend smoke，运行 `mvn test`、`pnpm test`、OpenSpec strict/all。

## Migration Plan

1. 先补后端模型/API 单测，验证新增字段、脱敏、取消和部分失败语义。
2. 兼容扩展 `DatabaseMetadataScanReq/Result`、service 和 controller，不改旧请求路径。
3. 更新 OpenAPI 生成类型或手工同步当前项目约定的前端类型。
4. 更新反向导入页和覆盖率页相关工具/展示测试。
5. 运行后端、前端、OpenSpec、状态检查和 diff/secrets 检查。
6. 启动独立子 agent 评审；修复 Critical/Important 后再 commit。完成确认后再按归档流程 archive。

回滚策略：删除新增字段和前端展示，保留旧 `scanId/cursor/pageSize/cancel` 分页扫描契约；由于第一版不新增数据库迁移和不写源库/标准库，不需要数据回滚。

## Open Questions

- 是否需要跨服务重启恢复采集作业？第一版不做，后续若需要持久化 job 表再单独设计。
- 覆盖率页是否直接消费 scan job partialResult，还是仅从反向导入页跳转带入 selected tables？实现时优先选择最小可用路径。
