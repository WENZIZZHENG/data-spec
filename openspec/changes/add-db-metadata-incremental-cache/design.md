## Context

当前数据库直连能力已经覆盖连接诊断、表级 scan plan、schema dump、metadata browser、反向导入预览、二次比对和覆盖率报告。现有实现每次操作都会通过 JDBC 重新读取目标表结构，虽然保持了无状态和不落凭据的安全边界，但在大库、重复查看和 AI 多轮分析场景下会重复消耗源库 metadata 查询。

P6-71 的目标是在不保存密码、不保存 JDBC URL、不保存业务数据行的前提下，新增结构级 metadata cache。缓存要让重复扫描可复用，让强制刷新可生成差异摘要，并给 AI 一个稳定 fingerprint 判断是否需要重跑反向导入、覆盖率或 Context 生成。

## Goals / Non-Goals

**Goals:**
- 为数据库结构 metadata 引入持久化缓存、稳定 fingerprint、lastSeenAt、expiresAt、refreshMode 和 changeSummary。
- 让 scan/browser/preview/compare/coverage 直连流程能在默认 `AUTO` 模式复用新鲜缓存，并在 `REFRESH` 模式读取源库后更新缓存。
- 差异摘要覆盖表新增、表删除、字段新增、字段删除和字段属性变化，字段属性至少包含 dataType、nullable、defaultValue、comment、ordinalPosition 和 index 关联。
- 缓存响应和 AI 摘要明确脱敏，不包含 password、token、full JDBC URL、connection string 或源库业务数据行。
- 前端展示缓存新鲜度、最近读取时间、过期时间、刷新方式、fingerprint 和变化摘要，并提供手动刷新入口。

**Non-Goals:**
- 不做实时同步、不监听 binlog、不引入后台扫描任务。
- 不保存或复用数据库密码，不为缓存命中建立服务端连接会话。
- 不默认扫描全库列 metadata；仍由用户选择表或分页加载后触发结构读取。
- 不引入外部缓存组件，第一版沿用项目数据库和 MyBatis-Plus repository 模式。

## Decisions

1. **缓存键使用项目 + 预设或脱敏连接指纹 + schema + table。**
   - 如果请求带有连接预设 ID，则缓存记录保存 `presetId` 并以其作为主要来源边界；如果没有预设，则服务端用数据库类型、host、port、databaseName、schemaName、username 等非密码字段计算 `sourceScopeHash`。
   - 持久化记录不保存 password、token、JDBC URL 或连接串；`sourceScopeHash` 只用于定位缓存，不反推出连接信息。
   - 替代方案是只按表名缓存，但跨库/跨 schema 会误命中；或保存完整连接信息，安全边界不合适。

2. **缓存以表结构快照为最小持久单元，同时返回整体 fingerprint。**
   - `ds_database_metadata_cache` 按表保存 table metadata JSON、tableFingerprint、sourceProductName、sourceProductVersion、firstSeenAt、lastSeenAt、expiresAt 和 changeSummary JSON。
   - 响应层聚合 selected tables 的 tableFingerprint，生成本次 dump/browser/coverage 的 `metadataFingerprint`，供 AI 判断是否重跑后续分析。
   - 替代方案是按一次 dump 保存整包 JSON，读取简单但难以做单表增量和局部刷新。

3. **刷新策略使用兼容默认值。**
   - 请求新增可选 `metadataCacheMode`：`AUTO`、`REFRESH`、`BYPASS`。缺省为 `AUTO`，对旧客户端兼容。
   - `AUTO` 命中新鲜缓存时不连接源库读取列 metadata；缓存缺失或过期时读取源库并更新缓存。`REFRESH` 总是重新读取源库并与旧缓存比较。`BYPASS` 不读写缓存，保留诊断和回退能力。
   - 响应返回实际 `refreshMode`/`cacheHit`/`stale`，避免 AI 误以为一定来自实时源库。

4. **差异摘要由规范化快照比较生成。**
   - 写入缓存前先规范化表、字段和索引顺序，再计算 SHA-256 fingerprint；比较旧新快照时按 `schema.table.column` 和 index key 生成 changeSummary。
   - 字段级变化摘要只描述结构属性，不引用业务行值；超长摘要需要限量并返回 totals，避免大库响应过大。
   - 替代方案是依赖数据库 `updated_at` 或系统 catalog 变更时间，但跨 PostgreSQL/MySQL 不稳定。

5. **优先复用现有 dump 到分析链路。**
   - 后端新增 `DatabaseMetadataCacheService` 负责 cache lookup、snapshot normalize、fingerprint、diff 和 upsert。
   - `DatabaseReverseImportServiceImpl.exportDump` 仍产出 `DatabaseSchemaDump`，但内部先按 cache mode 决定是否从缓存构造 dump；browser/preview/compare/coverage 继续复用 dump 到 TableDef 的既有链路。
   - 这样把缓存引入点集中在 metadata 读取层，避免在覆盖率、二次比对和预览中重复实现缓存逻辑。

## Risks / Trade-offs

- [Risk] 默认 `AUTO` 可能在 TTL 内返回旧结构。→ Mitigation：响应明确 `lastSeenAt`、`expiresAt`、`stale=false/true` 和 `refreshMode`；前端提供手动刷新，AI 可在高风险任务中使用 `REFRESH`。
- [Risk] 连接指纹归一化不当会导致误命中或误失效。→ Mitigation：统一大小写、默认端口、schema 默认值和表名排序；单测覆盖 PostgreSQL/MySQL 默认值和不同表组合。
- [Risk] 表结构 JSON 变大影响数据库体积。→ Mitigation：只缓存用户选择或已浏览表，不默认全库列级缓存；字段变化摘要限量，缓存只保留当前快照。
- [Risk] 缓存中保存的 comment/defaultValue 可能包含敏感业务文本。→ Mitigation：沿用 `SensitiveDataSanitizer` 对结构文本做脱敏；明确缓存仍只用于 schema metadata，不保存行数据。
- [Risk] 新增字段影响前端/OpenAPI 类型。→ Mitigation：新增字段全部可选，旧客户端可忽略；更新前端 schema/types 和冒烟测试。

## Migration Plan

1. 新增 Flyway 迁移创建 `ds_database_metadata_cache`，包含字段级 COMMENT 和按项目/来源/schema/table 的唯一索引。
2. 新增 cache entity、mapper、repository、service 和单元测试，不改变现有 API 必填参数。
3. 扩展请求/响应模型与 OpenAPI 类型，接入 `exportDump`、scan、browser、preview、compare、coverage。
4. 更新前端展示和工具测试，补充缓存状态、手动刷新和 AI 摘要脱敏断言。
5. 回滚时可停止使用新增字段和表；旧流程仍可通过 `BYPASS` 或移除 service 注入恢复为每次直连读取。

## Validation Strategy

- RED/GREEN 后端测试：cache miss 写入、cache hit 不打开连接、REFRESH 生成字段 diff、BYPASS 不读写缓存、缓存不包含 password/JDBC URL/业务行。
- API/controller 测试：scan/browser/coverage 返回 metadata cache 状态和 fingerprint，旧请求缺省 `AUTO` 兼容。
- 前端测试：缓存状态文案、强制刷新参数、AI 摘要脱敏和 frontend smoke。
- 质量门禁：`openspec validate add-db-metadata-incremental-cache --strict`、后端目标 `mvn -Dtest=... test`、前端目标 `node --test ...`/`pnpm test`、必要时 `pnpm build`、`git diff --check`。
- 评审门禁：本变更涉及存储和数据一致性，commit 或 archive 前必须启动独立子 agent 只读评审并关闭 agent。

## Open Questions

- 无阻塞问题。第一版 TTL 采用服务端默认值并在响应中暴露过期时间；后续如需要项目级配置，可另开 change。
