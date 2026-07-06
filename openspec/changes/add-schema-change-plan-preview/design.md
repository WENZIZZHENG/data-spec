## Context

P6-87 要解决“当前数据库 schema 到 DataSpec 标准”的可审计迁移预览。项目已有三块基础：数据库直连 metadata dump、`ReverseImportCompareResult` 字段级标准差异、以及 DDL/lint/AI 回放等只读预览模式。第一版应复用这些能力，避免重新读取业务数据或引入迁移执行器。

本变更按 SDD standard 处理：它新增 API/CLI/前端可观察契约，但默认只读，不改变数据库 schema、持久化语义或权限边界。由于涉及 API/CLI 外部协议，commit 前仍按项目门禁启动独立子 agent 评审。

## Goals / Non-Goals

**Goals:**
- 为所选数据库表生成稳定 JSON schema plan，包含 hash、changeSet、整体风险、dry-run SQL、回滚提示、人工检查点、阻塞原因和 nextActions。
- 复用现有 metadata dump 与 compare 结果，保证 schema plan 与反向导入差异视图看到的是同一份 metadata。
- 在 CLI 和前端提供第一版预览入口，方便 AI/用户读取同一份 JSON 计划。
- 对 drop/rename/manual review 保持保守：高风险项必须进入 `manualChecks`/`blockedReasons`，不得暗示可以自动执行。

**Non-Goals:**
- 不执行迁移、不连接迁移工具、不写源数据库。
- 不替代 Flyway/Liquibase/Atlas，不生成最终迁移文件。
- 不自动推断所有字段重命名；第一版只在名称/别名命中标准时生成属性修正，未命中字段作为人工确认项。
- 不读取业务数据行，不保存 raw password、完整 JDBC URL、DSN、token 或数据库连接串。

## Decisions

### 1. 将 plan 建在 compare/dump 之上

方案：`DatabaseReverseImportServiceImpl.planSchemaChange(req)` 先通过现有 `exportDump(req)` 获取 schema-only dump，再调用 `compareDump` 生成标准差异，最后把差异转换为 schema plan。

理由：dump/compare 已处理方言差异、缓存、脱敏和标准字段命中；复用它们能减少新能力与既有反向导入结果不一致的风险。

替代方案：直接在新 service 里读 JDBC metadata 并重新比对标准字段。该方案会重复安全与方言逻辑，容易与 browse/compare 输出漂移，第一版不采用。

### 2. 输出 dry-run SQL 草案而不是可执行迁移

方案：对 `MISSING_COMMENT` 生成 `COMMENT ON COLUMN` 草案；对 `CHANGED` 中的注释变更生成 `COMMENT ON COLUMN` 草案，对字段类型、nullable 和默认值等结构属性只生成 `-- REVIEW ...` 注释化确认文本；对 `NEW`/`NON_STANDARD` 只生成注释化的 drop candidate 文本，并把风险标为 `HIGH` 或 `BLOCKED`。

理由：字段类型、nullable/default 变更可能需要数据清洗和锁表评估，且目标类型或默认值可能来自 AI/用户维护的标准字段，不能直接拼成可复制执行的 SQL；未纳管字段不能等同于应删除字段。输出草案可以帮助 AI/用户继续工作，但不会越过人工确认边界。

替代方案：直接生成完整 `DROP COLUMN` 或 rename SQL。该方案风险高，且 TODO 明确第一版不自动推断所有字段重命名，不采用。

### 3. hash 使用非敏感结构内容

方案：`currentSchemaHash` 基于 dump 的数据库类型、库名、schema、表/列/index metadata 计算；`targetSpecHash` 优先使用当前标准快照 hash，没有快照时基于 compare 中的标准字段名、显示名和标准值摘要计算。

理由：AI/CI 需要判断计划是否对应同一版源库和目标标准，但 hash 不能包含密码、JDBC URL 或业务数据行。

替代方案：复用 metadata cache fingerprint 作为唯一源 hash。该值在 dump-only 或 bypass 场景可能为空，因此只作为辅助证据，不作为唯一来源。

### 4. 前端放在反向导入页内

方案：在现有数据库直连工具区增加“生成迁移计划”按钮和预览区，展示 summary、风险 tag、blocked/manual checks、changeSet 表格和 dry-run SQL。

理由：P6-87 依赖当前连接表选择和标准比对，反向导入页已经承载这条工作流；新增独立页面会增加状态同步成本。

替代方案：放到 DDL 生成页。DDL 页以模板生成新表为主，缺少当前库 metadata 选择与连接安全诊断，不采用。

## Risks / Trade-offs

- [Risk] 用户误以为 `migrationSql` 可直接执行。→ 字段命名为 dry-run 语义，在 response、CLI、前端和 README 中都说明默认不执行，并为高风险项输出 `blockedReasons`。
- [Risk] 类型、nullable/default 等结构变更在不同方言下不完全可执行且可能带来锁表或数据兼容风险。→ 第一版只生成 `-- REVIEW` 注释化确认文本，并把方言/人工确认写入 `manualChecks`。
- [Risk] DataSpec 标准是字段库，不是完整表模型，无法安全判断“标准字段必须出现在某张表”。→ 第一版不自动为每张表补全所有标准字段，只对当前库已有字段与标准差异生成修正建议。
- [Risk] 大库计划输出过大。→ 第一版复用用户选择的 tableNames 和 metadata cache/scan 边界；后续如需分页 plan 再单独设计。

## Migration Plan

- 新增后端模型和 endpoint，旧 API/CLI/前端不受影响。
- 新增 CLI 命令和 fixture 为兼容新增，不删除旧命令。
- 前端只增加现有页面上的可选预览区，不改变反向导入确认导入流程。
- 回滚时移除新增 endpoint/CLI/UI 和 OpenSpec change；数据库和持久化数据不需要迁移。

## Open Questions

- 无阻塞性开放问题。第一版按 TODO 边界实现只读计划；后续如需真实迁移文件、rename 推断或迁移工具集成，应另起 OpenSpec。
