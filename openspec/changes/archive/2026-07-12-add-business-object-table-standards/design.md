## Context

DataSpec 当前已有字段标准、数据域、表模板、DDL preview/lint、AI Context、Schema Registry、CLI/MCP 和前端模板页面。现有表模板只覆盖字段集合和基础列属性，DDL 生成只输出列定义与 COMMENT，缺少业务对象关系、主键、唯一键、索引、外键、审计字段、软删除和 AI 使用说明。P6-76 与 P6-106 会触发数据模型、API、AI Context、CLI/MCP 外部协议和前端体验变化，因此按 SDD full 处理，commit 前必须独立子 agent 评审。

第一版目标是个人 / 小团队可用的“建表标准闭环”：用户维护业务对象和表级结构标准，DDL preview 只读消费这些标准，AI Context/Schema Registry/CLI/MCP 能读取同一份结构化契约。第一版不做完整 ER 平台、不执行数据库迁移、不自动改业务库、不读取业务数据行，也不要求历史表一次性补齐全部结构标准。

## Goals / Non-Goals

**Goals:**

- 新增项目级业务对象标准，表达 `entityName`、`tablePattern`、关联表模板、必选/可选字段、关系、外键提示、审计字段、常见反模式和是否导出到 AI Context。
- 扩展表模板结构标准，表达 `primaryKey`、`uniqueKeys`、`indexes`、`foreignKeys`、`checkHints`、`auditPolicy`、`softDeletePolicy`、`dialectNotes` 和 `aiUsageNotes`。
- DDL preview 输出可 lint 的 PostgreSQL 预览，并返回结构标准摘要与 evidence，帮助用户判断哪些约束来自模板、哪些来自业务对象提示。
- AI Context 包含机器可读表结构标准文件和可读规则摘要，支持 `scope=business-object|table-template` 的裁剪语义。
- Schema Registry、CLI、MCP 和前端跟随新增契约，保持 additive 兼容。
- 测试覆盖后端模型/API/service、DDL 生成、AI Context、Schema Registry、CLI/MCP fixture 和前端核心展示。

**Non-Goals:**

- 不做可拖拽完整 ER 建模器，不做复杂图数据库或布局引擎。
- 不执行数据库迁移、不连接业务库应用约束、不生成可直接执行的变更 apply 任务。
- 不覆盖所有数据库方言高级特性；第一版以 PostgreSQL preview 为主，其他差异进入 `dialectNotes` 和诊断。
- 不改变字段标准、表模板、DDL preview、CLI/MCP 既有必填字段，不删除或重命名现有契约字段。
- 不把数据库连接凭据、真实业务数据行、raw JDBC URL 或 token 纳入结构标准。

## Decisions

1. 第一版使用“业务对象表 + 模板结构 JSON 扩展”，不新增完整 ER 关系表。
   - 方案：新增 `ds_business_object_standard`，以 `object_key/entity_name/table_pattern/template_id` 作为核心字段，关系、外键提示、必选/可选字段、审计字段、反模式和 AI 使用说明使用受控 JSON 文本保存；扩展 `ds_template` 保存表级约束/索引/策略 JSON。
   - 理由：业务对象和表结构标准当前主要服务 DDL/Context/AI，而不是复杂在线建模。JSON 扩展可以快速表达结构化约束并保持向后兼容，后续若关系查询变重再拆子表。
   - 备选：为 relation、foreign key、index、policy 各建独立表。放弃原因是第一版开发和迁移成本高，容易把任务扩成 ER 平台。

2. 表级约束先作为模板标准，不作为字段实体属性。
   - 方案：`primaryKey/uniqueKeys/indexes/foreignKeys/checkHints/auditPolicy/softDeletePolicy/dialectNotes/aiUsageNotes` 挂在模板或业务对象标准上，字段仍保留现有字段标准语义。
   - 理由：主键、外键和索引属于表结构组合约束，直接挂字段会让同一字段在不同表中的角色混乱。
   - 备选：扩展 `TemplateField` 增加 `isPrimaryKey/isIndexed/referenceTable`。放弃原因是无法表达复合唯一键、多列索引和对象关系说明。

3. DDL preview 继续是只读 PostgreSQL preview。
   - 方案：生成器在列定义之后追加 `CONSTRAINT ... PRIMARY KEY`、`UNIQUE`、`FOREIGN KEY`，并追加 `CREATE INDEX` 语句；对 `checkHints`、审计/软删除策略和方言说明返回结构摘要，不自动拼接不安全 SQL 片段。
   - 理由：当前生成器已有严格 identifier/type/default 校验。新增结构标准必须沿用 allowlist 解析，不允许任意 SQL 片段进入 DDL。
   - 备选：允许用户在结构标准里填写 raw constraint SQL。放弃原因是安全和可 lint 风险过高。

4. AI Context 新增独立 `table-standards.json`，并在 `DATABASE_RULES.md` 摘要高价值结构规则。
   - 方案：AI Context zip 增加 `.dataspec/table-standards.json` 与 schema registry contract；field catalog 保持原职责，不塞入大块关系图。
   - 理由：表结构标准比字段目录更偏关系和约束，独立文件便于按对象或模板裁剪，也避免破坏既有 field catalog 消费方。
   - 备选：把结构标准全部放进 field catalog 顶层。放弃原因是 field catalog 已经承载字段、枚举、格式、使用契约等信息，继续膨胀会影响离线解析和预算评估。

5. CLI/MCP 第一版以只读发现和现有命令增强为主。
   - 方案：CLI 增加 `table-standards show/list` 或等价 read-only 命令；MCP 增加项目级 table standards resource/tool；`generate-ddl` 和 `generate_table_ddl` 保留新增 result 字段。
   - 理由：AI 客户端需要先读取结构标准再生成 DDL，但不应通过 CLI/MCP 第一版写入结构标准。
   - 备选：提供 CLI/MCP 写入业务对象和模板标准。放弃原因是写入型外部协议需要更多幂等、安全和撤销设计。

6. 前端第一版复用模板管理和 DDL 生成页面，不新建大型工作台。
   - 方案：模板管理页增加结构标准编辑区和业务对象关联；DDL 生成页展示结构标准摘要、关系图和 lint evidence。
   - 理由：用户已经在模板页维护字段、在 DDL 页预览结果，继续沿用路径最短。
   - 备选：新增完整“业务对象设计器”页面。放弃原因是第一版会产生状态同步和导航成本。

## Risks / Trade-offs

- [Risk] JSON 扩展字段长期可能变复杂。→ Mitigation：Schema Registry 和 OpenSpec 先固定 schemaVersion、字段说明和兼容策略；后续出现频繁查询再拆表。
- [Risk] 表级标准拼接 DDL 可能引入 SQL 注入或不可执行片段。→ Mitigation：只接受结构化字段名、约束名和枚举型选项；所有 identifier 复用生成器校验；`checkHints` 只作为说明或受限表达式，不拼 raw SQL。
- [Risk] AI Context 包体变大。→ Mitigation：新增 scope 支持按业务对象或表模板裁剪，默认只导出启用且 `contextExport=true` 的对象摘要。
- [Risk] CLI/MCP 契约面扩大导致兼容漂移。→ Mitigation：更新 contract fixture，并运行 `node --test tools/*.test.mjs` 或针对性 fixture 测试。
- [Risk] 前端页面承载更多表单后复杂度上升。→ Mitigation：第一版只做字段选择、JSON/结构表单摘要、关系图和 DDL preview，不做拖拽图编辑。

## Migration Plan

1. 新增 Flyway migration，创建业务对象标准表并为 `ds_template` 增加表结构标准 JSON 列；同步 `schema.sql`。
2. 后端新增 DTO/model/service/controller 和 repository 查询，所有写入先校验项目归属和结构 JSON 边界。
3. 扩展 DDL preview、AI Context、Schema Registry、CLI/MCP 和前端展示。
4. 新增或更新测试后运行 OpenSpec strict、受影响后端测试、前端测试/build、tools 测试和 `git diff --check`。
5. 回滚策略：移除新 API/Context/CLI/MCP 暴露后可停止消费新增列；数据库新增表/列为 additive，不影响旧模板和字段读取。

## Open Questions

- 第一版是否需要把业务对象标准纳入项目备份/恢复和标准复用包？本 change 默认不做写入语义扩展，只在后续发现迁移需求时单独推进。
- `checkHints` 是否允许有限表达式转成 PostgreSQL `CHECK`？本 change 默认先作为 lint/AI 提示，只有 allowlist 能安全表达时才生成约束。
