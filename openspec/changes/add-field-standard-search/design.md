# 设计

## Context

当前字段相关能力分成两类：`/api/fields` 提供分页列表，适合前端表格；`/api/fields/suggest` 输入一个字段描述，返回少量推荐结果，适合单字段命名。P6-19 需要补一个更像目录检索的入口，让 AI 在建表、修 SQL 或补标准前先拿到一组相关标准字段，并理解命中原因与下一步动作。

字段库已经有 `name/displayName/comment/aliases/category/tags/status/sensitive`，推荐服务也已有轻量语义词库、别名匹配、拼音缩写和泛化词降权。第一版应复用这些确定性能力，不引入外部 LLM、向量数据库或新存储。

## Goals / Non-Goals

**Goals:**

- 提供项目级字段标准检索 API，支持关键词与结构化过滤并返回稳定 JSON。
- 检索结果包含字段本体、确定性分数、命中原因、推荐使用范围和 nextActions。
- CLI/MCP 可直接调用该能力，AI agent 无需读取整包 Context 再自行筛选。
- 前端字段库可复用搜索结果，至少展示命中原因并保持现有分页列表可用。

**Non-Goals:**

- 不引入向量数据库、外部 LLM、全文搜索引擎或复杂语义模型。
- 不替代 `/api/fields/suggest` 的单字段命名推荐。
- 不新增字段写入、候选采纳或合并流程。
- 不一次性重做字段库前端信息架构。

## Decisions

- **后端新建搜索模型而非扩展分页列表。** `/api/fields/search` 使用 `FieldSearchReq` / `FieldSearchResult` / `FieldSearchItem`，避免把 AI 检索所需的 `matchReasons/nextActions/contextScope` 塞进普通分页列表。
- **复用现有字段评分逻辑，增加搜索过滤。** 在 `FieldService` 中新增 `search(req)`，内部复用 `scoreField`、semantic groups、tokens、fallback 逻辑；同时按 `category/tag/status/sensitive/sourceBatchId` 过滤。这样第一版能继承已有同义词、别名和拼音缩写测试基础。
- **结果默认只读且有上限。** `limit` 归一化到保守范围，默认返回 20 条；空 query 时必须至少有结构化过滤，避免 AI 一次性拉全字段库。
- **CLI/MCP 走同一 HTTP endpoint。** CLI 增加 `search-fields`，MCP 新增或升级 `search_fields` 工具；旧 `search_field_catalog` 继续保持 AI Context 裁剪语义，不破坏既有客户端。
- **前端先复用，不重做。** 字段库搜索框仍保留，但在有关键词或过滤时调用搜索 API 并显示命中原因；无关键词时继续使用分页列表。

## Risks / Trade-offs

- [Risk] 确定性搜索不如向量检索覆盖长尾语义 → 第一版返回 `unresolvedHints` / `nextActions`，低命中时提示进入候选草案或补别名。
- [Risk] 搜索与推荐评分逻辑重复或发散 → 将公共 scoring helper 控制在 field service 内，测试覆盖同义词、别名、拼音缩写和泛化词。
- [Risk] 字段库很大时全量扫描变慢 → 复用现有性能基线和 `PerformanceProbe`，限制 limit；后续 P6-71 再做缓存/索引。
- [Risk] 前端替换列表请求可能影响分页体验 → 第一版只在搜索条件存在时使用检索结果；普通打开字段库仍走现有分页。

## Migration Plan

- 新增 endpoint 和类型为向后兼容变更，不需要数据库迁移。
- `schema.ts` 随后端 OpenAPI 更新并提交。
- CLI/MCP 新增命令/工具不移除既有行为。
