## Why

P2-2 要让“基于表模板生成 DDL”从 API/CLI/MCP 能力进入前端工作台。当前后端已有 `/api/generator/ddl/preview`，但前端 `Generator.vue` 仍是占位页，用户无法在 Web 中选择模板、输入表名、预览并下载生成的 DDL。

## What Changes

- 新增前端 template API wrapper，用于读取当前项目的模板列表和模板字段。
- 改造生成器页面，支持选择当前项目模板、输入表名、生成 DDL。
- 展示生成 SQL、lint 自检统计和问题列表。
- 提供复制和本地下载 `.sql` 文件能力。
- TODO 路线图同步更新 P2-2 状态。

## Scope

- 本轮只做前端预览与下载闭环，复用现有后端 DDL generator。
- 不新增后端 DDL 生成逻辑，不执行数据库变更。
- 不补表模板 CRUD 页面；模板管理仍作为后续体验专项。

## Impact

- `Generator.vue` 从占位页升级为可用页面。
- 新增 `dataspec-web/src/api/template.ts`。
- `types/index.ts` 导出模板相关类型。
