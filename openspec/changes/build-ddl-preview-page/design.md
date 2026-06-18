## Design

### Approach

生成器页面按现有后台工具风格实现：顶部表单选择模板并输入目标表名，右侧或下方展示生成结果。API 层只增加薄封装，直接复用现有 `/api/templates`、`/api/templates/{templateId}/fields` 和 `/api/generator/ddl/preview`。

### Key Decisions

- **前端下载**：后端当前只有预览接口，前端可基于返回的 `ddl` 生成 Blob 下载，避免为同一内容新增后端 download endpoint。
- **当前项目联动**：页面从 `projectStore.currentProjectId` 读取项目，项目变化时刷新模板并清空结果。
- **模板字段预览**：选择模板后显示字段列表，帮助用户理解生成 DDL 的来源。
- **生成后 lint 摘要**：使用 `DdlGenerateResult.lintResult` 中的错误/警告/建议数量和 issues，让用户知道生成 SQL 是否通过规范自检。

### Verification

- 后端继续依赖现有 `DdlGeneratorServiceTest`、`GeneratorControllerTest`。
- 前端运行 `pnpm build`，覆盖 TypeScript 类型与 Vue 模板编译。
- OpenSpec 使用 `openspec validate build-ddl-preview-page --strict`。
