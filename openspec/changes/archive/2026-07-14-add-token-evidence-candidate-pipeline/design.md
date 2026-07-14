## Context

确定性命名解析已经由 `QueryNormalizationService` 统一提供，并输出有界、脱敏的 `QueryTokenEvidence`。标准候选 Inbox 已有 PENDING、采纳、合并、忽略和延后流程，也有 `sourceType/sourceRef/evidenceJson` 字段，但通用 create API 只按项目和候选名做应用层去重，没有 token evidence 专属 dry-run、签名确认或并发幂等约束。

本变更跨后端 API、PostgreSQL migration、OpenAPI 生成类型和 Vue 候选工作台，且会写入持久化候选，因此按 SDD full 处理。现有候选表字段足够承载第一版，不新增列；只增加新来源的唯一索引。

## Goals / Non-Goals

**Goals:**

- 把未知词、歧义缩写和禁用词的 token evidence 稳定送入既有候选决策流程。
- 所有写入先 preview，再显式确认 apply；preview 内容漂移后旧 token 失效。
- 同一来源事实在串行、重试和并发请求下最多保留一条候选。
- 证据可追溯但不保存 raw sourceText、业务数据行或凭据。
- 在现有候选页面完成预览、确认、写入和后续决策，不建设平行工作台。

**Non-Goals:**

- 不接覆盖率、反向导入、AI 反馈、文档、ORM 或多数据源冲突。
- 不自动采纳、合并、修改 glossary 或改写已有标准字段。
- 不调用外部 LLM，不引入向量检索或新的命名推断器。
- 不把所有 UNRESOLVED 数字、单位或已唯一解析 token 转为候选。

## Decisions

1. **一个来源字段生成一个候选，token 作为 signals。**
   - preview 请求携带 candidateName、displayName、dataType、sourceRef 和可选 sourceText；sourceText 缺省时由字段名和显示名组成。
   - `UNRESOLVED` 的 WORD/ACRONYM/HAN、由 abbreviation 来源产生的 `AMBIGUOUS`、以及 `DISABLED` token 成为 signals；NUMBER、UNIT 和唯一 RESOLVED token 不单独入箱。
   - 备选是每个 token 一条候选，但 token 缺少完整字段类型和来源语境，会扩大 Inbox 噪声，因此不采用。

2. **新增独立 preview/apply 路由，复用既有候选决策路由。**
   - preview 永远只读，返回 `READY/NO_ACTIONABLE_SIGNAL/STANDARD_EXISTS/EXACT_DUPLICATE/NAME_CONFLICT`、候选 payload、signals、dryRunToken 和 nextActions。
   - apply 只创建 PENDING 候选；后续继续使用既有 accept/merge/ignore/postpone API。
   - `TOKEN_EVIDENCE` 是受控保留来源，通用 create API 明确拒绝该 sourceType，避免调用方伪造受控证据或绕过 preview/apply。
   - preview/apply 使用专用 API DTO；apply 响应不直接暴露 Entity 或逻辑删除等持久化内部字段。
   - 备选是扩展通用 create API 的可选参数，但会让人工创建与受控 pipeline 的安全语义混在一起，因此不采用。

3. **dry-run token 绑定完整候选元数据和 evidence hash。**
   - 使用现有 `DryRunEvidenceSigner` 签发进程内 HMAC token，payload 只含 schemaVersion、projectId、dedupeHash、evidenceHash 和完整脱敏候选 payload 的 inputHash，不含 raw sourceText 或 sourceRef 原文。
   - inputHash 覆盖 candidateName、displayName、dataType、comment、sourceType、sourceRef、evidenceJson 和 confidence；apply 验证签名、项目、去重摘要、evidence hash 与 inputHash。
   - 服务重启、任一候选元数据变化、sourceText/glossary evidence 变化后必须重新 preview。
   - 备选是客户端回传普通 hash，但可被伪造，不能作为写入确认门禁，因此不采用。

4. **数据库唯一索引与事务锁共同约束新来源。**
   - Flyway 增加 `(project_id, candidate_name, source_type, source_ref)` partial unique index，条件为未删除且 `source_type='TOKEN_EVIDENCE'`。
   - apply 使用 `INSERT ... ON CONFLICT DO NOTHING`，再按完整 key 查询，串行重试和并发请求都返回同一条候选。
   - 专用 apply、通用候选 create、直接/批量字段 create、字段重命名/撤销和候选 accept 在同一事务中通过共享 Repository 获取 `projectId + fieldName` PostgreSQL advisory lock，再检查字段与 active 候选占用。
   - Starter Kit、复用包、项目恢复和内置标准导入按稳定顺序获取整批名称锁，并在锁后批量刷新待创建字段；不能使用加锁前快照决定插入。字段撤销恢复旧名称时按重命名处理。
   - 直接字段 create 遇到同名 active 候选时要求先在 Inbox 采纳或合并；accept 创建字段时只排除当前候选，不能绕过其他同名 active 候选。
   - 通用 create 保持原有来源语义，但不能声明保留来源 `TOKEN_EVIDENCE`。

5. **证据保存结构化安全摘要。**
   - evidenceJson 保存 schemaVersion、signal 列表、normalized token 状态和 sourceTextHash；不保存 raw sourceText。
   - sourceRef、comment 和显示字段沿用候选服务的脱敏与限长边界；candidateName/dataType 继续遵守现有列长度。

6. **前端使用同一弹窗内的 preview -> confirm -> apply。**
   - 入口位于候选页 header；预览结果用 alert 和紧凑 signals 表展示，只有 READY 且用户勾选确认时允许写入。
   - 写入成功后切换到 `TOKEN_EVIDENCE` 来源筛选并刷新列表，继续使用既有候选操作。

## Risks / Trade-offs

- **glossary 在 preview 与 apply 之间变化** → evidence hash 变化时拒绝旧 token，要求重新 preview。
- **同名候选来自不同 sourceRef** → exact key 只负责幂等；若项目已有同名 active 候选但 key 不同，preview 返回 NAME_CONFLICT 并阻止写入，避免 Inbox 重复。
- **候选或字段写入口与专用 apply 并发** → 所有已识别的字段名称写入口共用事务级 advisory lock；批量入口锁后刷新字段快照，恢复包额外拒绝同批次重复字段自然键。锁只可能因 hash 碰撞增加串行等待，不会放宽冲突检查。
- **已完成候选再次出现** → unique index 覆盖所有未删除 TOKEN_EVIDENCE 状态，同一事实不会重新入箱；需要重新评估时必须使用新的稳定 sourceRef 或显式删除旧记录。
- **历史手工声明 TOKEN_EVIDENCE** → V32 假设该新保留来源尚无重复事实；升级前若审计发现旧通用 API 曾伪造该来源，应先显式改名或去重，迁移不会自动改写历史记录。
- **进程重启导致 token 失效** → 这是现有 signer 的安全取舍；前端保留表单并提示重新预览。
- **sourceRef 可能包含敏感文本** → 入库前统一脱敏和限长，dry-run token 只绑定 hash，响应不回显 raw sourceText。

## Migration Plan

1. 执行新 Flyway migration，为尚未使用的 `TOKEN_EVIDENCE` 来源创建 partial unique index。
2. 发布后端 preview/apply API 和 OpenAPI schema，再发布前端入口。
3. 回滚应用代码时旧版本会忽略新来源候选；如需完整回滚，可删除 partial unique index，不需要改写历史数据。
4. 不自动清理已创建候选；它们仍可通过既有 Inbox 决策处理。

## Verification Strategy

1. 后端单元/契约测试覆盖受控来源、完整 inputHash 漂移、项目隔离、脱敏、精确事实幂等和专用 DTO 边界。
2. PostgreSQL 集成测试启动 Spring context 并调用真实 Service/Repository/Mapper，验证 apply 与通用候选 create、直接字段 create、候选 accept、Starter Kit 批量字段创建和字段撤销恢复名称的竞争路径；Docker 客户端不兼容时只允许连接显式授权且没有当前数据库用户对象的一次性 `dataspec_candidate_it` 数据库。
3. 前端测试覆盖项目切换、迟到 apply 响应和迟到列表响应，Browser 验收桌面/移动端 preview、确认和刷新流程。
4. 收口时运行后端、前端、tools、OpenAPI drift、OpenSpec strict/all、状态、diff 和 secrets 门禁。

## Open Questions

无。后续来源只有在首个 token evidence 管道稳定且出现真实需求时单独增量接入。
