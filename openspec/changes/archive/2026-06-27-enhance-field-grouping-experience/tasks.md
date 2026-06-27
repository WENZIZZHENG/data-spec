## 1. OpenSpec 与后端测试基线

- [x] 1.1 通过 `openspec validate enhance-field-grouping-experience`，确认 proposal/design/spec/tasks 可实施。
- [x] 1.2 新增字段分组摘要服务测试，覆盖 domain/category/tag/ungrouped 统计和 sample fields。
- [x] 1.3 新增批量归组服务或控制器测试，覆盖显式更新、清空字段、跨项目拒绝和变更日志写入。
- [x] 1.4 新增 AI Context 测试，覆盖 scoped export 的 grouping summary 和向后兼容字段。

## 2. 后端字段分组能力

- [x] 2.1 新增字段分组模型，如 `FieldGroupSummary`、`FieldGroupItem`、`FieldGroupingBatchUpdateReq/Result`。
- [x] 2.2 扩展 `FieldService`/`FieldServiceImpl`，实现项目级分组摘要和批量归组，复用现有 `domainId/category/tags`，不新增表。
- [x] 2.3 扩展 `FieldController`，增加分组摘要查询接口和批量归组接口，并保持项目边界校验。
- [x] 2.4 批量归组写入现有标准变更日志，保证每个字段有 before/after 证据。

## 3. AI Context 分组摘要

- [x] 3.1 在 `AiContextExportService` 中为 scoped fields 计算 group summary，覆盖返回字段的 domain/category/tag/ungrouped 统计。
- [x] 3.2 将 group summary 作为可选字段加入 `contextScope`/field catalog schema/manifest，并在 warnings 中提示未分组字段。
- [x] 3.3 保持现有 `field-catalog.json` required 字段与旧 CLI/MCP 消费路径兼容。

## 4. 前端字段库体验

- [x] 4.1 扩展 `src/api/field.ts` 和类型导出，接入分组摘要与批量归组接口。
- [x] 4.2 改造 `FieldLibrary.vue`：加载数据域列表，展示分组面板，支持按 domain/category/tag/ungrouped 筛选字段。
- [x] 4.3 为字段表增加多选和批量归组对话框，可批量设置或清空 domain/category/tags，并刷新列表与分组摘要。
- [x] 4.4 保持现有字段创建、编辑、来源、影响、分页和 query keyword 跳转行为可用。

## 5. 文档、验证与收尾

- [x] 5.1 更新 README 和 TODO，说明 P6-14 分组视图、批量归组和 AI Context 分组摘要状态。
- [x] 5.2 运行后端相关测试与 `mvn test`，运行前端 `pnpm build`，运行 OpenSpec validate 和 `git diff --check`。
- [x] 5.3 进行直接代码评审，不使用子 agent；修复 findings 或记录暂不处理理由。
- [x] 5.4 创建本地 commit 后继续下一个待办。
