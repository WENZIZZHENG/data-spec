## Why

P2-4 需要让 Markdown 数据字典从基础字段清单升级为可读的项目级标准文档。当前 `MarkdownGeneratorService` 已输出数据域、字段库和枚举字典，但缺少概览、字段与数据域关系、个人版字段元数据、模板结构和模板字段约束，用户和 AI 难以从文档中理解“标准字段如何用于建表”。

## What Changes

- 数据字典增加项目概览统计。
- 标准字段表增加数据域、别名、分类、敏感、状态、代码集、示例等字段元数据。
- 枚举字典展示值类型。
- 新增表模板章节，展示模板、模板字段、排序、必含、可空、默认值和关联标准字段。
- TODO 路线图同步更新 P2-4 状态。

## Scope

- 本轮只增强现有 Markdown 输出，不新增 HTML/ERD/在线文档站。
- 当前没有索引模型，因此不伪造索引信息；只展示已有的字段约束和模板约束。
- 不改变 `/api/generator/markdown/preview` 和 download 的 API 路径。

## Impact

- `MarkdownGeneratorService` 依赖 `TemplateService`。
- 新增 Markdown generator 单元测试。
