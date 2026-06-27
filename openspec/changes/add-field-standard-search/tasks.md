## 1. OpenSpec

- [x] 1.1 创建 P6-19 OpenSpec change，并通过 `openspec validate add-field-standard-search`。

## 2. 后端字段检索

- [x] 2.1 新增字段检索请求与响应模型，包含 query、category、tag、status、sensitive、sourceBatchId、limit、summary、items、matchReasons 和 nextActions。
- [x] 2.2 在 `FieldService` 中实现只读检索，复用现有语义评分、别名、拼音缩写和字段状态逻辑，并支持结构化过滤。
- [x] 2.3 新增 `/api/fields/search` endpoint，保持项目边界和失败响应契约。
- [x] 2.4 新增后端单测，覆盖关键词/别名/拼音缩写、category/tag/status/sensitive/sourceBatchId 过滤、空查询校验和低命中 nextActions。

## 3. CLI/MCP 接入

- [x] 3.1 CLI 新增 `search-fields` 命令，支持 project/query/category/tag/status/sensitive/sourceBatchId/limit 参数并输出稳定 JSON。
- [x] 3.2 MCP 新增 `search_fields` tool，返回 structuredContent 和可解析 JSON text。
- [x] 3.3 新增 CLI/MCP 单测，覆盖成功搜索、过滤参数透传和 API 失败时的 DataSpecError 诊断。

## 4. 前端复用

- [x] 4.1 更新 OpenAPI 生成类型和 `src/types/index.ts` 导出字段检索类型。
- [x] 4.2 新增前端 `api/field.ts` 搜索封装。
- [x] 4.3 字段库在搜索条件存在时复用字段检索 API，并展示命中原因或下一步建议；清空条件时保留现有分页列表。
- [x] 4.4 更新前端源码级 smoke，覆盖字段库检索 API 耦合和命中原因文案。

## 5. 文档、验证与提交

- [x] 5.1 更新 README，说明字段标准检索 API/CLI/MCP/前端能力和边界。
- [x] 5.2 更新 TODO，将 P6-19 标记为已完成第一版并推进下一步顺序。
- [x] 5.3 运行后端测试、CLI/MCP 单测、前端 test/build、OpenSpec validate 和 `git diff --check`。
- [x] 5.4 进行直接代码评审，不使用子 agent；修复 findings 或记录暂不处理理由。
- [x] 5.5 创建本地 commit 后继续下一个待办。
