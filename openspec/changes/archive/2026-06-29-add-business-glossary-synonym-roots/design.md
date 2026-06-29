## Context

DataSpec 已有字段别名、字段推荐、字段检索、AI Context 导出和字段冲突检测，但别名仍附着在单个字段上。真实项目中，“会员/用户/账号”“手机号/联系电话/mobile”“费用/金额/price”等表达通常是项目级术语，而不是某一个字段的私有别名。P6-48 通过项目级 glossary 把这些表达集中维护，并让现有确定性推荐链路复用。

## Goals / Non-Goals

**Goals:**
- 维护项目级术语、同义词、词根、缩写、禁用词和 canonical 字段映射。
- 用 glossary 增强字段推荐和字段检索，让结果包含稳定、AI 可读的命中原因。
- 提供轻量冲突检测，发现同一术语指向多个 canonical 字段或禁用词与同义词冲突。
- 在 AI Context 包中导出精简 glossary，供离线 AI 使用。
- 提供前端维护入口，适合个人/小团队快速增删改查。

**Non-Goals:**
- 不做企业知识图谱、本体推理、跨项目继承或组织级发布流程。
- 不引入向量数据库、embedding、外部 LLM 或异步训练。
- 不自动改写字段别名，不自动采纳术语为正式标准字段。
- 不扫描业务数据行，不保存敏感样本值。

## Data Model

新增 `ds_business_glossary`：

- `id`
- `project_id`
- `term`：主术语，例如 `会员`、`订单费用`
- `synonyms`：逗号分隔同义词，例如 `用户,账号,客户`
- `root_terms`：逗号分隔英文词根，例如 `user,member,account`
- `abbreviations`：逗号分隔缩写或拼音，例如 `yh,hy,mobile`
- `disabled_terms`：逗号分隔不推荐使用词，例如 `telphone`
- `canonical_field_id`：可选推荐标准字段
- `scope_type` / `scope_value`：第一版支持 `GLOBAL`、`CATEGORY`、`DOMAIN`、`TAG` 字符串范围
- `example_fields`：逗号分隔字段名样例，用于无 canonical 字段时的提示
- `description`
- `status`：`enabled`、`disabled`
- `is_deleted`、`created_at`、`updated_at`

约束与索引：

- `(project_id, lower(term))` 在未删除记录内唯一。
- `project_id/status`、`project_id/canonical_field_id` 建索引。
- 删除采用软删除，避免破坏历史变更日志和 AI 回放语境。

## API Design

新增 `/api/glossary`：

- `GET /api/glossary?projectId=&keyword=&status=&current=&size=`：分页查询。
- `GET /api/glossary/all?projectId=&status=enabled`：AI/前端轻量读取当前项目术语。
- `GET /api/glossary/conflicts?projectId=`：返回术语冲突、禁用词冲突和 canonical 字段冲突摘要。
- `POST /api/glossary`：创建。
- `PUT /api/glossary/{id}`：更新。
- `DELETE /api/glossary/{id}`：软删除。

所有写入和读取都调用 `ProjectAccessGuard.requireProjectAccess(projectId)`；更新、删除时以实体归属项目为准，不信任请求体 projectId。

## Matching Design

第一版只做确定性匹配：

1. 规范化 query：保留中文片段，英文统一小写，按空白、下划线、短横线和标点切分。
2. 术语匹配：主术语、同义词、词根、缩写命中后生成 `GlossaryMatch`。
3. 字段推荐增强：
   - canonical 字段命中加权最高。
   - `example_fields` 与字段名/别名相同可加权。
   - 禁用词命中只产生提示，不把禁用词当正向推荐依据。
   - `matchReason` 增加 `术语表：<term> -> <field>`。
4. 字段检索增强：
   - query 可通过 glossary 扩展到 canonical 字段、示例字段、词根和同义词。
   - search item 增加 glossary 命中原因；无命中时 nextActions 提醒维护 glossary 或新增标准候选。

## AI Context Design

`field-catalog.json` 增加项目级 `glossary` 数组，默认只导出 `enabled` 条目和有限字段：

- `term`
- `synonyms`
- `rootTerms`
- `abbreviations`
- `disabledTerms`
- `canonicalFieldName`
- `scopeType`
- `scopeValue`
- `exampleFields`

包体控制：最多导出 200 条 enabled 术语；超过时在 `contextScope.warnings` 中提示按需裁剪或清理术语表。禁用词只导出词面和说明，不导出敏感样本。

## Frontend Design

新增 `BusinessGlossary.vue`：

- 顶部按当前项目、关键词、状态筛选。
- 表格展示术语、同义词、词根、缩写、canonical 字段、范围、状态和冲突提示。
- 新建/编辑弹窗使用 Element Plus 表单，canonical 字段用现有字段列表选择。
- 冲突面板展示同义词重复、禁用词冲突和多个术语指向同一 canonical 字段的提示。
- 操作入口放在“基础数据”分组，保持个人工具风格，不做审批/发布。

## Risks / Trade-offs

- [Risk] 术语过多影响推荐性能。Mitigation：第一版按项目加载 enabled 术语，保留性能测试和 200 条 AI Context 导出上限，后续可接缓存。
- [Risk] 术语与字段别名冲突。Mitigation：不自动覆盖字段别名，只在冲突检测中提示。
- [Risk] canonical 字段被删除或停用。Mitigation：读取时容忍空字段，冲突/前端提示“canonical 字段不可用”。
- [Risk] 新增 API 影响 OpenAPI 类型。Mitigation：更新前端类型或使用局部显式类型，并运行 `pnpm build`。
