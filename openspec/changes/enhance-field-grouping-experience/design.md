## Context

字段实体已包含 `domainId`、`category`、`tags`，数据域也已有 CRUD；AI Context 已支持 `scope=domain|tag` 和字段级 `matchReasons`。当前缺口主要在体验和机器可读摘要：字段库仍是单表分页，数据域只能手填 ID，批量归组缺失，AI Context 只能看到字段本身而看不到当前包的分组覆盖。

本次属于 standard SDD：涉及后端 API、前端页面、AI Context 契约和测试，但不新增持久化模型，也不改变字段基础结构。

## Goals / Non-Goals

**Goals:**

- 在项目内按数据域、分类、标签和未分组状态浏览字段。
- 支持对选中字段批量设置数据域、分类和标签，减少逐条编辑。
- AI Context 输出分组摘要，说明裁剪包覆盖的分组范围和未分组数量。
- 保持现有字段 CRUD、分页接口、数据域管理和 AI Context 旧字段向后兼容。

**Non-Goals:**

- 不新增组织级目录、跨项目权限或审批发布流。
- 不新增独立“字段分组”表；第一版复用 `domainId/category/tags`。
- 不做复杂批量撤销；批量变更写入现有标准变更日志即可。
- 不重做整个字段库信息架构，不引入大型前端状态库。

## Decisions

1. **复用字段已有元数据，而不是新增分组表。**
   - 理由：P6-14 的目标是个人/小团队可用体验，现有 `domainId/category/tags` 已能表达数据域、模块和标签。
   - 替代方案：新增 `ds_field_group` 和字段-分组关系表。该方案更灵活，但会引入迁移、排序、层级和权限问题，第一版过重。

2. **新增字段分组摘要 API，而不是让前端自行聚合分页数据。**
   - 理由：字段库分页只拿当前页，前端无法准确知道全项目未分组数量和分组覆盖；后端可基于全项目字段生成稳定摘要，也可复用给 AI。
   - 输出包含 groupType、groupKey、groupName、fieldCount、sampleFields 和 ungrouped 标记。

3. **批量归组使用显式 patch 语义。**
   - 理由：用户可能只想设置 category，不想覆盖 tags 或 domainId。请求体需要区分“不更新”和“清空”，避免无意写坏字段。
   - 方案：请求包含 `fieldIds` 和 `updates`；`updates` 只对出现的键生效，值为 null 或空字符串表示清空。

4. **AI Context 分组摘要作为可选兼容字段。**
   - 理由：AI 合同允许向后兼容新增字段；新增 `groupSummary` 不会破坏旧消费者。
   - 摘要与 `contextScope` 同层或在其内部出现，包含分组计数和 warnings，帮助 AI 判断是否需要重新导出更大范围。

## Risks / Trade-offs

- [Risk] 批量更新误覆盖字段元数据。→ 只允许显式 keys 生效，前端提交前展示影响数量，后端逐字段写变更日志。
- [Risk] 数据域 ID 对用户不友好。→ 前端加载 `/api/domains`，用名称和 code 展示；无法匹配时仍保留 ID。
- [Risk] 标签是逗号字符串，存在中英文逗号和空白不一致。→ 第一版做轻量拆分、去空白和去重，不改变数据库存储结构。
- [Risk] 分组摘要可能过大。→ 每个分组只返回少量 sampleFields，字段详情仍通过列表查询。

## Migration Plan

无需数据库迁移。后端新增 API 和 AI Context 可选字段；前端按新接口增强字段库。回滚时保留现有字段 CRUD 和 AI Context 字段目录不受影响。

## Open Questions

- 后续是否把 `category` 升级为受控字典，留给 P6-68 多项目复用包或 P6-42 领域 Starter Kit 再判断。
