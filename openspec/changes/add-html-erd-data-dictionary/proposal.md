## Why

Markdown 数据字典适合 Git 管理，但给人浏览、分享和理解字段/枚举/模板关系时不够直观。P4-7 需要在现有 Markdown 生成能力之上，补一个可离线打开的 HTML 数据字典和轻量 ERD/关系图。

## What Changes

- 新增项目级 HTML 数据字典生成能力，复用现有数据域、字段、枚举字典和表模板数据。
- HTML 中展示概览、标准字段、枚举字典、表模板和关系图，适合浏览器离线打开。
- 新增 Mermaid ERD/关系图文本输出，用于展示字段与数据域、代码集、模板之间的关系。
- 后端新增 HTML/ERD 预览与下载接口；前端生成器页增加 HTML 预览、ERD 预览和下载入口。
- 保持现有 Markdown 预览/下载路径兼容。

## Capabilities

### New Capabilities
- `html-erd-data-dictionary`: HTML 数据字典、Mermaid ERD/关系图和前端预览/下载体验。

### Modified Capabilities
无。

## Impact

- 后端影响 `generator` 模块，新增 HTML/ERD 生成 service 或扩展现有生成器，并补充 controller API。
- 前端影响 `Generator.vue` 和 `src/api/generator.ts`。
- 不新增数据库表，不改变 Markdown 输出格式，不引入外部在线文档站。
