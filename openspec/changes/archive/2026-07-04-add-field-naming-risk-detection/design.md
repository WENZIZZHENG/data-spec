## Context

现有 `FieldConflictServiceImpl` 已基于标准字段名、别名、显示名和内置语义组生成只读冲突报告，前端 `FieldConflicts.vue` 已能按类型和级别筛选并跳转字段库。P6-56 需要补的是“AI 生成 SQL/DDL 前必须知道哪些名字危险”：SQL 方言保留字、容易需要引用的危险词、大小写碰撞，以及 alias 与 canonical name 的歧义。

## Goals / Non-Goals

**Goals:**
- 复用现有字段冲突报告 API 和前端页面，新增命名风险冲突类型。
- 第一版内置 PostgreSQL/MySQL/通用 SQL 的高价值保留字和危险词清单。
- 对每个风险输出证据和替代命名建议，便于用户和 AI 判断下一步。
- 在 AI Context 中导出字段命名风险摘要，提醒 agent 避免直接使用风险字段名或歧义 alias。

**Non-Goals:**
- 不新增数据库表或用户自定义保留字管理页面。
- 不自动重命名字段、不阻断保存、不自动修改模板或历史 SQL。
- 不追求覆盖所有数据库方言；第一版只覆盖 PostgreSQL、MySQL 和通用 SQL 高频词。
- 不替代 SQL lint 对真实 DDL 的规则校验。

## Decisions

1. **扩展现有冲突报告，而不是新增命名风险 API。**
   - 原因：字段冲突页和 API 已承载“标准库内部会误导 AI 的字段问题”，命名风险同属只读诊断，复用能减少前端入口和类型维护成本。

2. **保留字清单以代码常量内置。**
   - 原因：第一版只需要高价值提醒，无需引入数据库迁移或外部依赖；后续若需要项目级自定义，可再升级为配置或规则包。

3. **风险以 `FieldConflictGroup` 输出，证据字段承载方言和替代建议。**
   - 原因：前端和 OpenAPI 已支持 `evidence` 与 `suggestedAction`，不新增 DTO 也能让 AI Context 和用户读取。

4. **AI Context 只导出摘要，不导出完整冲突报告。**
   - 原因：完整冲突报告可能很长；AI 主要需要“哪些名字要避让”和“推荐改成什么”，摘要足够且更节省上下文。

## Risks / Trade-offs

- [Risk] 内置清单不完整。→ Mitigation：只声明第一版覆盖 PostgreSQL/MySQL/通用高频词，证据中输出 dialect，后续可扩展。
- [Risk] 历史字段名确实需要保留。→ Mitigation：风险只提示，不阻断保存；建议动作使用“新增字段时避让/必要时引用”表述。
- [Risk] alias 与 canonical name 的歧义和现有 alias conflict 重叠。→ Mitigation：现有 alias conflict 保持 ERROR；新增 `AMBIGUOUS_ALIAS` 更强调 AI 不应把 alias 当唯一 canonical 使用。
