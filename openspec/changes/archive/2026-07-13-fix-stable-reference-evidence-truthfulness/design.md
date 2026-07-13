## Context

字段更新已经把变更前后 JSON 快照写入 `ds_standard_change_log`，但引用解析、检索和推荐只读取 `ds_field` 当前值。现有 AI output post-check 能提取 `dataspec://evidence/...`，却不查询任何证据来源，因此真实和伪造 claim 都返回同一个 `EVIDENCE_GAP`。Evidence Package 支持 SQL 检查、AI 作业、AI 批任务和 AI task run 等持久化来源，但 `packageId` 每次生成且不持久化，不能作为可重新验证的引用。

本变更跨字段、引用、Evidence 和 AI post-check 多个模块，涉及项目隔离和外部 AI 契约，按 SDD full 实施。约束是保持 API additive compatibility、不新增数据库 schema、不引入外部依赖，并继续使用现有 Evidence Package 和 `review-pr` 入口。

## Goals / Non-Goals

**Goals:**

- 从已有字段变更快照恢复可审计的历史名称/别名，供引用解析、检索和推荐共享。
- 为持久化 Evidence Package 来源生成稳定、项目级、可重新解析的 evidence ref。
- 让 post-check 明确区分真实、缺失、跨项目和不可验证 claim，并保持结果脱敏、有界。
- 消除 PR review 主规格中 summary-only 与 inline review 的冲突。

**Non-Goals:**

- 不新增历史别名表，不回填数据库，不恢复已逻辑删除字段，不猜测缺失快照。
- 不让历史名覆盖或高于当前名称、当前别名和 stableRef 的匹配优先级。
- 不持久化 Evidence Package JSON，不让 payload-only coverage report 获得虚假的可验证引用。
- 不新增 Evidence API、第二种证据包格式、外部 LLM 或第二个 PR reviewer。

## Decisions

### 1. 从变更日志构建请求级历史别名索引

新增内部 `FieldHistoricalAliasService`，Repository 一次读取当前项目 `targetType=field` 的变更日志，并只解析 `beforeJson`/`afterJson` 中白名单字段 `name`、`displayName`、`aliases`。服务接收当前字段集合，按 fieldId 去重并移除仍在使用的当前名称和别名，返回历史值、来源 changeLogId 和证据 URI。

引用解析使用规范化后的精确历史值；字段检索/推荐沿用现有确定性打分，但历史值最高分低于当前别名，避免旧名称反向压过现行标准。历史命中在 `matchedAlias`、match reason 和 `ExplainTrace(FIELD_CHANGE_LOG)` 中返回来源，不暴露 before/after JSON 原文。

备选方案是新增 `ds_field_alias_history` 表并在每次更新时同步写入。该方案查询更快，但需要迁移、双写一致性和历史回填，超出当前个人/小团队规模，也无法修复已有数据，因此不采用。

### 2. Evidence ref 指向持久化来源而不是临时 packageId

canonical 格式为 `dataspec://evidence/<source-type>/<source-id>`，第一版支持 `sql-check`、`ai-job`、`ai-batch-run` 和 `ai-task-run`。`AiEvidenceSource` 增加可空 `evidenceRef`：持久化来源生成 canonical ref，`coverage-report` 等 payload-only 来源保持为空。

新增内部 `EvidenceClaimResolver` 接口与实现，解析 URI 后通过现有 Repository 读取来源最小元数据并比较 projectId。结果状态固定为：

- `VERIFIED`：来源存在且属于当前项目，返回 canonical ref。
- `MISSING`：格式和来源类型有效，但记录不存在。
- `CROSS_PROJECT`：记录存在但属于其他项目，不返回标题、状态或目标项目元数据。
- `UNVERIFIABLE`：格式不受支持、source type 未知、来源没有项目归属或属于 payload-only 数据。

实现直接依赖项目现有 Repository 边界，不穿透到 MyBatis mapper；避免复用会先做当前用户项目访问检查的详情 Service，因为 resolver 需要在不泄露对象详情的前提下判定 `CROSS_PROJECT`。

备选方案是持久化整个 Evidence Package 并解析随机 packageId。它会引入新表、生命周期与清理策略，且重复保存已有来源数据，因此不采用。

### 3. Post-check 只采信 resolver 验证后的 Evidence claim

post-check 对同一 URI 去重后解析：`VERIFIED` 不产生 issue，并把 canonical ref 加入顶层 `evidenceLinks`；`MISSING` 产生 `MISSING_EVIDENCE_REFERENCE` WARN；`CROSS_PROJECT` 产生 `CROSS_PROJECT_EVIDENCE_REFERENCE` FAIL；`UNVERIFIABLE` 产生 `UNVERIFIABLE_EVIDENCE_REFERENCE` WARN。所有 inputRef、excerpt、消息和 link 继续经过现有 sanitizer。

保留 WARN 的兼容语义用于缺失/不可验证证据，避免仅凭证据缺口把普通文本升级为不可恢复失败；跨项目 claim 属于确定性错误和安全边界，因此为 FAIL。

### 4. Summary 是稳定入口，inline 是增量交付

`review-pr` 始终创建或更新单个 summary 评论。能映射到 PR changed line 且未重复的问题可额外发布 inline；无法定位、未处于 diff 或重复的问题留在 summary 并带 fallback 原因。这与现有实现和 `github-inline-review` 主规格一致，只修正 `sql-inline-review-location` 的旧 summary-only 描述。

## Risks / Trade-offs

- [变更日志量增长导致请求级解析变慢] -> Repository 使用 projectId/targetType 单次查询，消费者每次请求只构建一个索引；通过单测和性能门禁观察，再决定是否缓存或物化。
- [旧快照损坏或字段结构变化] -> 只解析白名单字符串字段；单条日志失败时按 log id 记录无敏感内容的 warning 并跳过，不影响当前名解析。
- [历史值同时属于多个当前字段] -> 返回 `AMBIGUOUS`，不猜测 canonicalRef；证据只列候选字段/变更记录。
- [Evidence URI 被构造为超长或含秘密文本] -> 复用 post-check 输入上限与 sanitizer，解析器只接受固定 scheme、allowlist source type 和正整数 ID。
- [新增 evidenceRef 影响旧客户端] -> 字段保持可空且 additive；CLI/MCP/OpenAPI fixture 验证现有字段不删除、不改类型。
- [Repository 仍是项目历史具体类而非 port 接口] -> 本次只通过现有 Repository 业务方法访问，不引入 mapper 依赖；Repository 接口化属于独立重构，不扩大 P6-190。

## Migration Plan

1. 先合入 delta specs 和失败测试，再实现历史别名索引与 Evidence resolver。
2. 不执行数据库迁移；发布后历史别名从既有日志即时派生。
3. Evidence Package 新字段为 additive，旧客户端无需迁移；新 claim 应逐步使用 canonical URI。
4. 回滚时可直接回退代码和 specs，不涉及数据回滚；既有变更日志和来源记录保持不变。

## Open Questions

无。Evidence 来源类型、URI 格式、状态与严重度在本 change 内固定；新增来源需后续单独扩展 allowlist 和契约测试。
