## Why

规则配置已经能 CRUD，但常见规则仍需要用户直接编辑 `paramsJson`。这对个人长期维护字段标准不友好，也容易写错参数名，导致 lint 和 AI 导出的规则与用户意图不一致。

本变更采用 standard SDD：它改变规则配置页的用户可见编辑方式，但不修改后端 API、数据库模型或 lint 规则执行契约。

## What Changes

- 规则配置页为常见规则提供结构化表单：必含列、禁用字段名、推荐替换、字段后缀/前缀类型。
- 根据规则编码自动展示对应参数编辑器，并继续保留 JSON 预览/兜底编辑能力。
- 保存时由前端将结构化表单转换为现有 `paramsJson`，不新增后端接口。
- 表格中展示参数摘要，降低列表扫描成本。
- 新增前端纯工具函数和测试，锁定 `paramsJson` 与结构化表单之间的转换。

## Capabilities

### New Capabilities

- `rule-config-experience`: 规则配置前端结构化编辑体验，覆盖常见规则参数表单、JSON 预览和参数摘要。

### Modified Capabilities

无。

## Impact

- 主要影响 `dataspec-web/src/views/RuleConfig.vue`。
- 新增前端工具函数与 Node 测试，接入 `dataspec-web` 的 `pnpm test`。
- 复用现有 `/api/rules` 和 `/api/lint/rules`，不修改后端 Java 代码。
